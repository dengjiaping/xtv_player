package com.kankan.player.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.*;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.kankan.media.Media;
import com.kankan.media.MediaPlayer;
import com.kankan.media.TimedText;
import com.kankan.player.api.rest.ads.GetAdvImagesResponse;
import com.kankan.player.app.AppConfig;
import com.kankan.player.app.AppRuntime;
import com.kankan.player.app.Constants;
import com.kankan.player.app.TVPlayerApplication;
import com.kankan.player.dao.model.Subtitle;
import com.kankan.player.dao.model.VideoHistory;
import com.kankan.player.event.LocalSubttileEvent;
import com.kankan.player.event.OnlineSubtitleEvent;
import com.kankan.player.event.UpdateSubtitleEvent;
import com.kankan.player.explorer.FileItem;
import com.kankan.player.explorer.FileUtils;
import com.kankan.player.item.DeviceItem;
import com.kankan.player.item.VideoItem;
import com.kankan.player.manager.FileExploreHistoryManager;
import com.kankan.player.manager.VideoHistoryManager;
import com.kankan.player.manager.XLRouterDownloadMgr;
import com.kankan.player.model.GetSubtitleModel;
import com.kankan.player.subtitle.Caption;
import com.kankan.player.subtitle.SubtitleType;
import com.kankan.player.subtitle.TimedTextObject;
import com.kankan.player.util.*;
import com.kankan.player.view.CustomToast;
import com.kankan.player.view.MediaController;
import com.kankan.player.view.SystemVideoView;
import com.kankan.player.view.VideoView;
import com.plugin.common.cache.CacheFactory;
import com.plugin.common.utils.CustomThreadPool;
import com.plugin.common.utils.SingleInstanceBase;
import com.plugin.common.utils.UtilsConfig;
import com.plugin.common.utils.files.FileUtil;
import com.umeng.analytics.MobclickAgent;
import com.xunlei.tv.player.R;
import de.greenrobot.event.EventBus;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;

import java.io.BufferedInputStream;
import java.io.File;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import static com.kankan.media.MediaPlayer.*;

public class PlayVideoActivity extends BaseActivity {
    public static final String ACTION_XLPLAY = "com.xunlei.tv.player.action.play";
    public static final String EXTRA_PATH = "path";

    /**
     * 用来统计，目前定义0为助手推送远程，1为助手推送离线
     */
    public static final String EXTRA_VIDEO_TYPE = "type";

    private static final int RESTART_RESERVE_TIME = 5;
    private static final long FINISH_RESERVE_TIME = 2000L;

    private static final int SUBTITLE_DISPLAY_CHECK = 100;

    private static final int REQUEST_CODE_FROM_MENU = 1;

    public static final int MSG_FULL_SIZE = 0x101;
    public static final int MSG_AUTO_SIZE = 0x102;
    public static final int MSG_LEFT_AUDIO = 0x103;
    public static final int MSG_RIGHT_AUDIO = 0x104;
    public static final int MSG_SUBTITLE = 0x105;
    public static final int MSG_UPDATE_RESTART_TIME = 0x106;
    public static final int MSG_DISMISS_RESTART_TIPS = 0x107;
    public static final int MSG_COMING_TO_END = 0x108;
    public static final int MSG_DISMISS_COMING_TO_END = 0x109;
    public static final int MSG_SUBTITLE_INNER_SELECTED = 0X110;

    private VideoItem mItem;
    private boolean mIsFromHistory;
    // 是否续播
    private boolean mIsContinue;
    // 是否播放出错，如果video不能播放的话，那么不将其插入到历史记录中
    private boolean mIsVideoError;

    private VideoView mVideoView;
    private SystemVideoView mSystemVideoView;
    private MediaController mMediaController;
    private View mRestartMessageView;
    private TextView mRestartTipsTv;
    private TextView mTipsMessageTv;
    private TextView mSubtitleTv;
    private View mAdView;
    private ImageView mAdIv;
    private RelativeLayout mTvassistantPlayTipRl;

    private VideoHistoryManager mHistoryManager;
    private AdUtil mAdUtil;

    private GetSubtitleModel mGetSubtitleModel;

    private TimedTextObject mSubtitle;

    private String mCid;
    // 只有当初次播放视频时，需要按照规则(本地字幕优先，内置字幕其次.....等等等等)选择字幕，以后都直接从历史记录里面获取设置
    private boolean mIsSubtitleFirstLoaded;

    private boolean mStartFromExternal = false;
    private boolean mStartFromTvassistant = false;

    /**
     * 记住播放设置项
     */
    private int mCurrentDisplayMode;
    private int mCurrentAudioMode;
    private int mSubtitleType;

    private List<Integer> mAudioTracks = new ArrayList<Integer>();

    // 统计使用，当前暂停广告图片的id
    private int mCurrentAdImageId;

    // 是否是系统播放器内核
    private boolean mIsSysteamVideoKernel = true;

    private Handler mSubtitleDisplayHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case MSG_FULL_SIZE:
                    if (mIsSysteamVideoKernel) {
                        mSystemVideoView.setAutoAjustSize(false);
                    } else {
                        mVideoView.setAutoAjustSize(false);
                    }
                    mCurrentDisplayMode = Constants.VIDEO_DISPLAY_FULL_MODE;
                    break;
                case MSG_AUTO_SIZE:
                    if (mIsSysteamVideoKernel) {
                        mSystemVideoView.setAutoAjustSize(true);
                    } else {
                        mVideoView.setAutoAjustSize(true);
                    }
                    mCurrentDisplayMode = Constants.VIDEO_DISPLAY_AUTO_MODE;
                    break;
                case MSG_LEFT_AUDIO:
                    if (!mIsSysteamVideoKernel) {
                        MediaPlayer playerLeft = mVideoView.getmMediaPlayer();
                        if (playerLeft != null) {
                            playerLeft.selectTrack(MEDIA_SOURCE_AUDIO, 0);
                            mCurrentAudioMode = Constants.VIDEO_AUDIO_LEFT_MODE;
                        }
                    } else {
                        try {
                            android.media.MediaPlayer mp = mSystemVideoView.getmMediaPlayer();
                            if (mAudioTracks.size() > 0) {
                                mCurrentAudioMode = mAudioTracks.get(0);
                                mp.selectTrack(mCurrentAudioMode);
                            }
                        } catch (Throwable t) {
                            AppConfig.LOGD("[[PlayVideoActivity]] startSystemVideo selectTrack error.");
                        }
                    }
                    break;
                case MSG_RIGHT_AUDIO:
                    if (!mIsSysteamVideoKernel) {
                        MediaPlayer playerRight = mVideoView.getmMediaPlayer();
                        if (playerRight != null) {
                            playerRight.selectTrack(MEDIA_SOURCE_AUDIO, 1);
                            mCurrentAudioMode = Constants.VIDEO_AUDIO_RIGHT_MODE;
                        }
                    } else {
                        try {
                            android.media.MediaPlayer mp = mSystemVideoView.getmMediaPlayer();
                            if (mAudioTracks.size() > 1) {
                                mCurrentAudioMode = mAudioTracks.get(1);
                                mp.selectTrack(mCurrentAudioMode);
                            }
                        } catch (Throwable t) {
                            AppConfig.LOGD("[[PlayVideoActivity]] startSystemVideo selectTrack error.");
                        }
                    }
                    break;
                case MSG_SUBTITLE:
                    break;
                case MSG_DISMISS_RESTART_TIPS:
                    removeMessages(MSG_UPDATE_RESTART_TIME);
                    mRestartMessageView.setVisibility(View.GONE);
                    mMediaController.setCanRestart(false);
                    break;
                case MSG_UPDATE_RESTART_TIME:
                    mRestartMessageView.setVisibility(View.VISIBLE);
                    mRestartTipsTv.setText(new StringBuilder(getResources().getString(R.string.control_restart_tips)).append(msg.arg1));
                    mMediaController.setCanRestart(true);

                    if (msg.arg1 > 0) {
                        mSubtitleDisplayHandler.sendMessageDelayed(Message.obtain(this, MSG_UPDATE_RESTART_TIME, msg.arg1 - 1, 0), 1000L);
                    } else {
                        Message.obtain(this, MSG_DISMISS_RESTART_TIPS, 0, 0).sendToTarget();
                    }
                    break;
                case MSG_COMING_TO_END:
                    mTipsMessageTv.setText(getResources().getString(R.string.tips_video_coming_end));
                    mTipsMessageTv.setVisibility(View.VISIBLE);

                    mSubtitleDisplayHandler.sendEmptyMessageDelayed(MSG_DISMISS_COMING_TO_END, FINISH_RESERVE_TIME);
                    break;
                case MSG_DISMISS_COMING_TO_END:
                    mTipsMessageTv.setVisibility(View.GONE);
                    break;
                case MSG_SUBTITLE_INNER_SELECTED:
                    if (!mIsSysteamVideoKernel) {
                        if (mVideoView != null && mVideoView.isPlaying()) {
                            MediaPlayer player = mVideoView.getmMediaPlayer();
                            if (player != null) {
                                player.selectTrack(MEDIA_SOURCE_SUBTITLE, 0);
                            }
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private Runnable mDisplaySubtitleRunner = new Runnable() {
        public void run() {
            boolean isPlaying = mIsSysteamVideoKernel ? mSystemVideoView.isPlaying() : mVideoView.isPlaying();
            if (isPlaying) {
                int currentPos = mIsSysteamVideoKernel ? mSystemVideoView.getCurrentPosition() : mVideoView.getCurrentPosition();
                Collection<Caption> subtitles = mSubtitle.captions.values();

                if (subtitles == null || subtitles.size() == 0) {
                    onTimedText(null);
                } else {
                    for (Caption caption : subtitles) {
                        if (currentPos >= caption.start.getMilliseconds() && currentPos <= caption.end.getMilliseconds()) {
                            onTimedText(caption);
                            break;
                        } else {
                            onTimedText(null);
                        }
                    }
                }
            } else {
                onTimedText(null);
            }

            mSubtitleDisplayHandler.postDelayed(this, SUBTITLE_DISPLAY_CHECK);
        }
    };

    private VideoView.MediaPlayerReleaseListener mReleaseListener = new VideoView.MediaPlayerReleaseListener() {
        @Override
        public void onRelease() {
            if (mItem != null) {
                if (mIsSysteamVideoKernel) {
                    mItem.setProgress(mSystemVideoView.getCurrentPosition());
                    mItem.setDuration(mSystemVideoView.getDuration());
                } else {
                    mItem.setProgress(mVideoView.getCurrentPosition());
                    mItem.setDuration(mVideoView.getDuration());
                }
            }
        }
    };

    private boolean processIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            mItem = (VideoItem) intent.getSerializableExtra(Constants.KEY_VIDEO_ITEM);
            mIsFromHistory = intent.getBooleanExtra(Constants.KEY_FROM_HISTORY, false);

            String action = intent.getAction();
            mStartFromExternal = Intent.ACTION_VIEW.equals(action);
            mStartFromTvassistant = ACTION_XLPLAY.equals(action);

            // 其他app通过ACTION_VIEW启动
            if (mStartFromExternal) {
                if (intent.getData() != null) {
                    String path = intent.getData().toString();
                    if (!TextUtils.isEmpty(path)) {
                        mItem = new VideoItem();
                        mItem.setFilePath(path);
                    }
                }
            }

            // TV助手启动
            if (mStartFromTvassistant) {
                String path = intent.getStringExtra(EXTRA_PATH);
                if (!TextUtils.isEmpty(path)) {
                    mItem = new VideoItem();
                    mItem.setFilePath(path);
                }
            }

        }

        if (mItem == null || TextUtils.isEmpty(mItem.getFilePath())) {
            return false;
        }

        return true;
    }

    private void showTvassistantTips() {
        if (mStartFromTvassistant) {
            mSubtitleDisplayHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mTvassistantPlayTipRl.setVisibility(View.VISIBLE);
                }
            }, 1000L);
            mSubtitleDisplayHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mTvassistantPlayTipRl.setVisibility(View.GONE);
                }
            }, 3000L);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_play_video);

        boolean hasData = processIntentData();
        if (!hasData) {
            Toast.makeText(this, "Invalid video path!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mHistoryManager = new VideoHistoryManager(getApplicationContext());
        mMediaController = new MediaController(this);
        if (AppConfig.ADVERTISE_ON) {
            mMediaController.setOnPauseListener(new MediaController.OnPauseListener() {
                @Override
                public void onPause(boolean isPause) {
                    if (isPause) {
                        showAdView();
                    } else {
                        hideAdView();
                    }
                }

                @Override
                public void onLoadAdImage() {
                    loadAdImage();
                }
            });
        }
        mMediaController.setTitle(FileUtil.getNameFromFilepath(Uri.decode(mItem.getFilePath())));
        mVideoView = (VideoView) findViewById(R.id.video);
        mSystemVideoView = (SystemVideoView) findViewById(R.id.system_video);

        mRestartMessageView = findViewById(R.id.restart_rl);
        mRestartTipsTv = (TextView) findViewById(R.id.restart_tips_tv);
        mTipsMessageTv = (TextView) findViewById(R.id.tips_msg_tv);
        mSubtitleTv = (TextView) findViewById(R.id.txtSubtitles);
        mAdView = findViewById(R.id.ad_rl);
        mAdIv = (ImageView) findViewById(R.id.ad_iv);
        mTvassistantPlayTipRl = (RelativeLayout) findViewById(R.id.tips_tvassistant_play_rl);

        EventBus.getDefault().register(this);
        mGetSubtitleModel = SingleInstanceBase.getInstance(GetSubtitleModel.class);
        mGetSubtitleModel.clearSubtitles();
        mHistoryManager = new VideoHistoryManager(getApplicationContext());
        mAdUtil = AdUtil.getInstance(this);
        showTvassistantTips();
    }

    private void loadSubtitle() {
        CustomThreadPool.asyncWork(new Runnable() {
            @Override
            public void run() {
                if (!TextUtils.isEmpty(mItem.getFilePath())) {
                    int videoDuration = 0;
                    String filePath = mItem.getFilePath();
                    if (SmbUtil.isSmbPlayUrl(filePath)) {
                        String smbPath = SmbUtil.getSmbPathFromPlayUrl(filePath);
                        try {
                            SmbFile smbFile = new SmbFile(smbPath);
                            BufferedInputStream bis = new BufferedInputStream(new SmbFileInputStream(smbFile));
                            mCid = CidUtil.queryCid(bis, smbFile.length());
                        } catch (SmbException e) {
                        } catch (MalformedURLException e) {
                        } catch (UnknownHostException e) {
                        }
                    } else {
                        mCid = CidUtil.queryCid(mItem.getFilePath());
                    }
                    mGetSubtitleModel.clearSubtitles();

                    videoDuration = Media.getDuration(Uri.parse(mItem.getFilePath()));
                    if (!TextUtils.isEmpty(mCid) && mCid.length() > 4) {
                        AppConfig.LOGD("[[PlayVideoActivity]] onCreate cid=" + mCid + ", duration=" + videoDuration);
                        mGetSubtitleModel.getSubTitleList(mCid.substring(4), mItem.getFilePath(), videoDuration);
                    }
                }
            }
        });
    }

    @Override
    public void finish() {
        super.finish();

        // 统计--播放详情
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("device", AppRuntime.getInstance(getApplicationContext()).getModel());
        map.put("if_success", mIsVideoError ? "0" : "1");
        map.put("codec", "");
        map.put("bitrate", "");
        map.put("extn", getFileExtension(mItem.getFilePath()));
        if (mIsSysteamVideoKernel) {
            map.put("resolution", mSystemVideoView.getVideoWidth() + "*" + mSystemVideoView.getVideoHeight());
        } else {
            map.put("resolution", mVideoView.getVideoWidth() + "*" + mVideoView.getVideoHeight());
        }

        MobclickAgent.onEvent(PlayVideoActivity.this, "Play_detail", map);

        // 如果是其他app启动的播放界面，那么退出时返回程序主界面
        if (mStartFromExternal || mStartFromTvassistant) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(new Intent(this, MainActivity.class));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);
        boolean hasData = processIntentData();
        if (!hasData) {
            Toast.makeText(this, "Invalid video path!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        startVideo(false);
        showTvassistantTips();
    }

    @Override
    protected String getUmengPageName() {
        return "PlayVideoActivity";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        startSystemVideo(false);
    }

    @Override
    protected void onResume() {
        super.onResume();

        playDetailStatistics();
    }

    private void playDetailStatistics() {
        // 统计--播放摘要
        int deviceType = mItem.getDeviceType();
        String from = "";
        if (deviceType == DeviceItem.DeviceType.EXTERNAL.ordinal()) {
            from = "local";
        } else if (deviceType == DeviceItem.DeviceType.HHD.ordinal() || deviceType == DeviceItem.DeviceType.USB.ordinal()) {
            from = "plug";
        } else if (deviceType == DeviceItem.DeviceType.XL_ROUTER.ordinal()) {
            String routerName = SettingManager.getInstance().getRouterName();
            if (SmbUtil.ROUTER_XIAOMI.equals(routerName)) {
                from = "xiaomirouter";
            } else if (SmbUtil.ROUTER_XUNLEI.equals(routerName)) {
                from = "xlrouter";
            } else {
                from = "smb";
            }
        }

        if (mStartFromTvassistant) {
            int type = getIntent().getIntExtra(EXTRA_VIDEO_TYPE, -1);
            if (type != -1) {
                from = (type == 1 ? "tvAssistant_offline" : "tvAssistant_remote");
            }
        }

        HashMap<String, String> map = new HashMap<String, String>();
        map.put("From", from);
        map.put("if_continue", mIsContinue ? "1" : "0");
        MobclickAgent.onEvent(this, "Play", map);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (!mIsVideoError) {
            // Add to history.
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    addVideoToHistory(mItem);
                }
            });
        }

        // stop video
        if (mIsSysteamVideoKernel) {
            mSystemVideoView.suspend();
        } else {
            mVideoView.suspend();
        }

        // 如果播放没出错，才将其插入
        if (!mIsVideoError) {
            addVideo2DB(mItem);
        }
    }

    @Override
    public void onBackPressed() {
        if (mIsFromHistory) {
            Intent intent = new Intent();
            setResult(RESULT_OK, intent);
        }

        super.onBackPressed();
    }

    private void addVideoToHistory(VideoItem videoItem) {
        if (videoItem == null) {
            return;
        }

        VideoHistory history = new VideoHistory();
        // 如果是合法的cid（ftp目前获取不到cid，但cid字段又不能为空，于是会用path填充cid字段）
        if (mCid != null && mCid.startsWith(Constants.CID_PREFIX)) {
            history.setCid(mCid);
        } else {
            history.setCid(videoItem.getFilePath());
        }
        history.setFilePath(videoItem.getFilePath());
        if (SmbUtil.isSmbPlayUrl(videoItem.getFilePath())) {
            history.setFileName(FileUtils.getNameFromFilepath(SmbUtil.getSmbPathFromPlayUrl(videoItem.getFilePath())));
        } else {
            history.setFileName(FileUtils.getNameFromFilepath(videoItem.getFilePath()));
        }
        history.setTimestamp(System.currentTimeMillis());
        history.setDuration(videoItem.getDuration());
        history.setProgress(videoItem.getProgress());
        if (mIsSysteamVideoKernel) {
            history.setWidth(mSystemVideoView.getVideoWidth());
            history.setHeight(mSystemVideoView.getVideoHeight());
        } else {
            history.setWidth(mVideoView.getVideoWidth());
            history.setHeight(mVideoView.getVideoHeight());
        }
        history.setAudioMode(mCurrentAudioMode);
        history.setDisplayMode(mCurrentDisplayMode);
        history.setSubtitleType(mSubtitleType);

        File thumbFile = PlayerUtil.makeVideoThumbnail(this, videoItem.getFilePath(), 6000);
        String thumbPath = thumbFile != null ? thumbFile.getAbsolutePath() : null;
        history.setThumbnailPath(thumbPath);
        history.setDeviceName(videoItem.getDeviceName());
        history.setDeviceType(videoItem.getDeviceType());
        mHistoryManager.saveHistory(history);
    }


    /**
     * 为了远程的new逻辑，播放完成后插入数据库
     *
     * @param videoItem
     */
    private void addVideo2DB(final VideoItem videoItem) {
        if (videoItem == null) {
            return;
        }

        CustomThreadPool.asyncWork(new Runnable() {
            @Override
            public void run() {
                FileExploreHistoryManager fileExploreHistoryManager = new FileExploreHistoryManager(getApplicationContext());
                DeviceItem deviceItem = new DeviceItem();
                FileItem fileItem = null;

                if (videoItem != null) {
                    if (videoItem.getDeviceType() == DeviceItem.DeviceType.USB.ordinal()
                            || videoItem.getDeviceType() == DeviceItem.DeviceType.HHD.ordinal()
                            || videoItem.getDeviceType() == DeviceItem.DeviceType.EXTERNAL.ordinal()) {
                        deviceItem.setType(DeviceItem.DeviceType.TD_DOWNLOAD);

                        deviceItem.setPath(videoItem.getFilePath());
                        fileItem = FileUtils.getFileItem(new File(videoItem.getFilePath()));
                        if (fileItem != null) {
                            fileItem.cid = mCid == null ? "" : mCid;
                        }


                    } else if (videoItem.getDeviceType() == DeviceItem.DeviceType.XL_ROUTER.ordinal()) {
                        deviceItem.setType(DeviceItem.DeviceType.XL_ROUTER_TDDOWNLOAD);
                        deviceItem.setPath(videoItem.getFilePath());
                        fileItem = XLRouterDownloadMgr.createFileItem(videoItem.getFilePath());
                        if (fileItem != null) {
                            fileItem.cid = mCid == null ? "" : mCid;
                        }
                    }

                    if (fileItem != null) {
                        fileExploreHistoryManager.addFileToExploreHistory(fileItem, deviceItem);
                    }
                }

            }
        });
    }

    public static void start(Activity activity, String videoPath, DeviceItem deviceItem) {
        VideoItem item = new VideoItem();
        item.setFilePath(videoPath);
        if (deviceItem != null) {
            item.setDeviceName(deviceItem.getName());
            item.setDeviceType(deviceItem.getType().ordinal());
        }
        start(activity, item, null);
    }

    public static void start(Activity activity, VideoItem item, DeviceItem deviceItem) {
        if (item == null || TextUtils.isEmpty(item.getFilePath())) {
            return;
        }

        if (deviceItem != null) {
            item.setDeviceName(deviceItem.getName());
            item.setDeviceType(deviceItem.getType().ordinal());
        }

        Intent intent = new Intent(activity, PlayVideoActivity.class);
        intent.putExtra(Constants.KEY_VIDEO_ITEM, item);
        activity.startActivity(intent);
    }

    public static void startForResult(Activity activity, VideoItem item, boolean isFromHistory, int requestCode) {
        if (item == null || TextUtils.isEmpty(item.getFilePath())) {
            return;
        }

        Intent intent = new Intent(activity, PlayVideoActivity.class);
        intent.putExtra(Constants.KEY_VIDEO_ITEM, item);
        intent.putExtra(Constants.KEY_FROM_HISTORY, isFromHistory);
        activity.startActivityForResult(intent, requestCode);
    }

    public void onEventMainThread(LocalSubttileEvent event) {
        AppConfig.LOGD("[[PlayVideoActivity]] LocalSubtitleEvent");
        if (event != null && event.obj != null) {
            mSubtitle = event.obj;
            mSubtitleDisplayHandler.post(mDisplaySubtitleRunner);
            showTipsMessage("字幕加载成功！");
        } else {
            showTipsMessage("字幕加载失败,请重试~");
        }
    }

    public void onEventMainThread(OnlineSubtitleEvent event) {
        AppConfig.LOGD("[[PlayVideoActivity]] OnlineSubtitleEvent");
        if (mIsSysteamVideoKernel) {
            if (mSystemVideoView.isPlaying()) {
                handleProcessSubtitle(mSubtitleType);
            } else {
                mSystemVideoView.setOnStartListener(new VideoView.OnStartListener() {
                    @Override
                    public void onStart() {
                        AppConfig.LOGD("SystemVideoView onStart...");
                        handleProcessSubtitle(mSubtitleType);
                        mSystemVideoView.setOnStartListener(null);
                    }
                });
            }
        } else {
            if (mVideoView.isPlaying()) {
                handleProcessSubtitle(mSubtitleType);
            } else {
                mVideoView.setOnStartListener(new VideoView.OnStartListener() {
                    @Override
                    public void onStart() {
                        AppConfig.LOGD("VideoView onStart...");
                        handleProcessSubtitle(mSubtitleType);
                        mVideoView.setOnStartListener(null);
                    }
                });
            }
        }
    }

    public void onEventMainThread(UpdateSubtitleEvent event) {
        AppConfig.LOGD("[[PlayVideoActivity]] UpdateSubtitleEvent");
        handleProcessSubtitle(event.type);
    }

    public void onTimedText(Caption text) {
        if (text == null) {
            mSubtitleTv.setVisibility(View.INVISIBLE);
            return;
        }
        Spanned span = Html.fromHtml(preProcessTimedText(text.content));
        mSubtitleTv.setText(span);
        mSubtitleTv.setVisibility(View.VISIBLE);
    }

    // 对原始字幕文件做一些处理
    private String preProcessTimedText(String text) {
        // 去掉末尾的<br/>标记，以免多出一行空行
        text = text.replaceAll("(<br\\s*/>)$", "");
        return text;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            Intent intent = new Intent(this, MenuActivity.class);
            intent.putExtra(Constants.KEY_VIDEO_PATH, mItem.getFilePath());
            intent.putExtra(Constants.KEY_DISPLAY_MODE, mCurrentDisplayMode);
            intent.putExtra(Constants.KEY_AUDIO_MODE, mCurrentAudioMode);
            intent.putExtra(Constants.KEY_AUDIO_LEFT, mAudioTracks.size() > 0);
            intent.putExtra(Constants.KEY_AUDIO_RIGHT, mAudioTracks.size() > 1);
            intent.putExtra(Constants.KEY_SUBTITLE_TYPE, mSubtitleType);
            startActivityForResult(intent, REQUEST_CODE_FROM_MENU);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            if (mMediaController.canRestart()) {
                mSubtitleTv.setVisibility(View.INVISIBLE);
            }
            mMediaController.doRestart();
            mSubtitleDisplayHandler.sendMessageDelayed(Message.obtain(mSubtitleDisplayHandler, MSG_DISMISS_RESTART_TIPS, 0, 0), 1000);

            //统计--回片头播放
            MobclickAgent.onEvent(PlayVideoActivity.this, "Play_quickback");
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_FROM_MENU) {
            if (data != null) {
                int what = data.getIntExtra(Constants.KEY_WHAT, -1);
                Message.obtain(mSubtitleDisplayHandler, what).sendToTarget();
            }
        }
    }

    private void initState() {
        mIsFromHistory = false;
        mIsContinue = false;
        mIsVideoError = false;
        mIsSubtitleFirstLoaded = true;
        mAudioTracks.clear();
        mCurrentAdImageId = 0;
        mIsSysteamVideoKernel = true;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void startSystemVideo(final boolean isRestart) {
        String filePath = mItem.getFilePath();
        AppConfig.LOGD("[[PlayVideoAcvitity]] startSystemVideo path = " + filePath);

        // 如果文件名后缀为xv或者xvx，那么默认不使用系统播放器
        int index = filePath.lastIndexOf(".");
        if (index != -1) {
            String suffix = filePath.substring(index + 1);
            if ("xv".equals(suffix) || "xvx".equals(suffix)) {
                startVideo(isRestart);
                return;
            }
        }

        initState();

        loadSubtitle();

        mSystemVideoView.setVideoPath(PlayerUtil.encodePlayUrl(filePath));
        mSystemVideoView.setMediaController(mMediaController);
        mSystemVideoView.setmMediaPlayerReleaseListener(mReleaseListener);

        int progress = 0;
        if (!isRestart) {
            VideoHistory history = mHistoryManager.getHistoryByCid(mCid == null ? filePath : mCid);
            mIsSubtitleFirstLoaded = (history == null);
            if (history != null) {
                mIsContinue = true;
                progress = mItem.getProgress();
                if (progress == 0) {
                    progress = history.getProgress();
                }
                mSystemVideoView.seekTo(progress);
                mCurrentAudioMode = history.getAudioMode();
                mCurrentDisplayMode = history.getDisplayMode();
                mSubtitleType = history.getSubtitleType();

                // 初始化显示
                if (mCurrentDisplayMode == Constants.VIDEO_DISPLAY_AUTO_MODE) {
                    mSystemVideoView.setAutoAjustSize(true);
                } else {
                    mSystemVideoView.setAutoAjustSize(false);
                }
            }
        }

        final int videoProgress = progress;
        mSystemVideoView.setOnPreparedListener(new android.media.MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(final android.media.MediaPlayer mp) {
                // 不是从头开始播放，那么就允许从头播放
                if (videoProgress > 0) {
                    Message.obtain(mSubtitleDisplayHandler, MSG_UPDATE_RESTART_TIME, RESTART_RESERVE_TIME, 0).sendToTarget();
                }

                try {
                    // 乐视电视上调用getTrackInfo会发生NoSuchMethodError
                    // 其他某些盒子上调用getTrackInfo会发生IllegalStateException或RuntimeException
                    android.media.MediaPlayer.TrackInfo[] infos = mp.getTrackInfo();
                    for (int i = 0; i < infos.length; i++) {
                        if (infos[i].getTrackType() == android.media.MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO) {
                            mAudioTracks.add(i);
                        }
                    }

                    // 初始化声道
                    android.media.MediaPlayer player = mSystemVideoView.getmMediaPlayer();
                    if (player != null) {
                        if (mAudioTracks.contains(mCurrentAudioMode) && Build.VERSION.SDK_INT >= 16) {
                            player.selectTrack(mCurrentAudioMode);
                        }
                    }
                } catch (Throwable e) {
                    AppConfig.LOGD("[[PlayVideoActivity]] startSystemVideo getTrackInfo error.");
                }
            }
        });

        mSystemVideoView.setOnCompletionListener(new android.media.MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(android.media.MediaPlayer mp) {
                startSystemVideo(true);
            }
        });

        mSystemVideoView.setOnComingToEndListener(new VideoView.OnComingToEndListener() {
            @Override
            public void onComingToEnd(int currentPosition) {
                mSubtitleDisplayHandler.sendEmptyMessage(MSG_COMING_TO_END);
            }
        });

        mSystemVideoView.setOnErrorListener(new android.media.MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(android.media.MediaPlayer mp, int what, int extra) {
                AppConfig.LOGD("[[PlayVideoActivity]] onError " + what + ", " + extra);
                startVideo(isRestart);
                return true;
            }
        });

        mSystemVideoView.start();
    }

    private void startVideo(boolean isRestart) {
        initState();

        loadSubtitle();

        mIsSysteamVideoKernel = false;
        mSystemVideoView.setVisibility(View.GONE);
        mVideoView.setVisibility(View.VISIBLE);
        // 如果少了requestFocus，有些盒子上VideoView的onKeyDown事件无法正常接收到
        mVideoView.requestFocus();

        String filePath = mItem.getFilePath();
        AppConfig.LOGD("[[PlayVideoAcvitity]] startVideo path = " + filePath);

        mVideoView.setVideoPath(PlayerUtil.encodePlayUrl(filePath));
        mVideoView.setMediaController(mMediaController);
        mVideoView.setmMediaPlayerReleaseListener(mReleaseListener);
        int progress = 0;
        if (!isRestart) {
            VideoHistory history = mHistoryManager.getHistoryByCid(mCid == null ? filePath : mCid);
            mIsSubtitleFirstLoaded = (history == null);
            if (history != null) {
                mIsContinue = true;
                progress = mItem.getProgress();
                if (progress == 0) {
                    progress = history.getProgress();
                }
                mVideoView.seekTo(progress);
                mCurrentAudioMode = history.getAudioMode();
                mCurrentDisplayMode = history.getDisplayMode();
                mSubtitleType = history.getSubtitleType();

                // 初始化显示
                if (mCurrentDisplayMode == Constants.VIDEO_DISPLAY_AUTO_MODE) {
                    mVideoView.setAutoAjustSize(true);
                } else {
                    mVideoView.setAutoAjustSize(false);
                }

                // 初始化声道
                MediaPlayer player = mVideoView.getmMediaPlayer();
                if (player != null) {
                    if (mCurrentAudioMode == Constants.VIDEO_AUDIO_LEFT_MODE) {
                        player.selectTrack(MEDIA_SOURCE_AUDIO, 0);
                    } else if (mCurrentAudioMode == Constants.VIDEO_AUDIO_RIGHT_MODE) {
                        player.selectTrack(MEDIA_SOURCE_AUDIO, 1);
                    }
                }
            }
        }

        final int videoProgress = progress;
        mVideoView.setOnPreparedListener(new OnPreparedListener() {
            @Override
            public void onPrepared(final MediaPlayer mp) {
                // 不是从头开始播放，那么就允许从头播放
                if (videoProgress > 0) {
                    Message.obtain(mSubtitleDisplayHandler, MSG_UPDATE_RESTART_TIME, RESTART_RESERVE_TIME, 0).sendToTarget();
                }

                //boolean isSupportAccelerate = mp.isVideoHardwareAcceleratedSupported();
                //boolean isSmbPath = mItem.getFilePath().contains("smb?path=smb://");
                //4k设置硬解会导致直接崩溃，所以4k的视频不允许设置硬解
                //boolean is4KVideo = Math.min(mVideoView.getVideoWidth(), mVideoView.getVideoHeight()) > 1080;
                //AppConfig.LOGD("[[PlayVideoActivity]] startVideo isVideoHardwareAcceleratedSupported=" + isSupportAccelerate + ", shouldOpenHardware:" + AppConfig.OPEN_HARDWARE_ACCELERATE + ", isSmbPath:" + isSmbPath);
                //if (AppConfig.OPEN_HARDWARE_ACCELERATE && isSupportAccelerate && !isSmbPath && !is4KVideo) {
                //    mp.setVideoHardwareAccelerated(true);
                //}

                String[] subtitleInfos = mp.getTrackInfo(MEDIA_SOURCE_SUBTITLE);
                String[] audioInfos = mp.getTrackInfo(MEDIA_SOURCE_AUDIO);

                for (int i = 0; i < audioInfos.length; i++) {
                    mAudioTracks.add(i);
                }

                if (subtitleInfos != null && subtitleInfos.length > 0) {
                    AppConfig.LOGD("[[PlayVideoActivity]] has inner subtitle...");
                    mGetSubtitleModel.setInnerSubtitle(mCid == null ? "" : mCid.substring(4), true);
                }
            }
        });

        mVideoView.setOnCompletionListener(new OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                startVideo(true);
            }
        });

        mVideoView.setOnComingToEndListener(new VideoView.OnComingToEndListener() {
            @Override
            public void onComingToEnd(int currentPosition) {
                mSubtitleDisplayHandler.sendEmptyMessage(MSG_COMING_TO_END);
            }
        });

        mVideoView.setOnErrorListener(new OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int i, int i2) {
                AppConfig.LOGD("[[PlayVideoActivity]] onError " + i + ", " + i2);

                if (mVideoView.getCurrentPosition() == 0) {
                    mIsVideoError = true;
                }
                if (isMenuActivityTop()) {
                    if (TVPlayerApplication.sMenuActivity != null) {
                        TVPlayerApplication.sMenuActivity.finish();
                    }
                }

                new CustomToast(PlayVideoActivity.this, getResources().getString(R.string.tips_play_error), new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        PlayVideoActivity.this.finish();
                    }
                }).show();

                return true;
            }
        });

        mVideoView.setOnTimedTextListener(mTimedTextListener);

        mVideoView.start();
    }

    /* 判断MenuActivity是否在前台 */
    private boolean isMenuActivityTop() {
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
        if (cn.getClassName().contains(
                "com.kankan.player.activity.MenuActivity")) {
            return true;
        }
        return false;
    }

    // 字幕加载规则:第一次加载，如果字幕接口返回值autoload为1，那么加载优先级:本地字幕->内置字幕->网络字幕->无字幕，如果autoload为0，
    // 那么默认取消所有字幕，如果接口请求失败，因为autoload值默认为1，所以也会加载:本地字幕->内置字幕->无字幕, 第一次加载之后将选项保存，
    // 以后都从数据库中取值
    private void handleProcessSubtitle(int type) {
        AppConfig.LOGD("[[PlayVideoActivity]] handleProcessSubtitle type=" + type);
        String subtitleFrom = "none";
        List<Subtitle> subtitles = mGetSubtitleModel.getDisplaySubtitleList();
        boolean autoload = mGetSubtitleModel.needAutoloadSubtitle();

        // 如果上次记录的值超出了本地从网络拉取的字幕列表值，也就是说两次网络接口有变化，那么认为是第一次加载
        // 这里如果接口两次返回值不同可能会出现问题，就是上次记录的和这次加载的不一致，因为只保存了索引，没有保存整个字幕
        if (type >= subtitles.size()) {
            mIsSubtitleFirstLoaded = true;
        }

        // 是否切换字幕，如果不是第一次自动加载，并且type和之前的不一样，那么视为切换了字幕
        boolean isChange = false;

        if (mIsSubtitleFirstLoaded) {
            mIsSubtitleFirstLoaded = false;

            // 初始为取消所有字幕
            type = 0;
            if (autoload) {
                // 按照规则，内置字幕总是排在本地字幕前面，所以如果有内置字幕和本地字幕，本地字幕永远优先
                for (int i = 0; i < subtitles.size(); i++) {
                    int subtitleType = subtitles.get(i).getType();
                    if (subtitleType == SubtitleType.INNER.ordinal()) {
                        type = i;
                        break;
                    } else if (subtitleType == SubtitleType.LOCAL.ordinal()) {
                        type = i;
                        break;
                    }
                }

                boolean needZhEn = mGetSubtitleModel.needLoadZhEn();
                // 如果没有内置字幕和本地字幕，那么加载第一条网络字幕，如果需要加载中英双字，那么加载中英双字
                if (type == 0) {
                    for (int i = 0; i < subtitles.size(); i++) {
                        if (subtitles.get(i).getType() == SubtitleType.ONLINE.ordinal()) {
                            if (!needZhEn) {
                                type = i;
                                break;
                            } else if (mGetSubtitleModel.isSubtitleZhEn(subtitles.get(i))) {
                                type = i;
                                break;
                            }
                        }
                    }
                }
            }
        } else {
            isChange = (mSubtitleType != type);
        }

        mSubtitleType = type;

        Subtitle subtitle = subtitles.get(type);
        int subtitleType = subtitle.getType();

        // 清空已经有的字幕
        mSubtitleTv.setText("");
        if (subtitleType == SubtitleType.NONE.ordinal()) {
            subtitleFrom = "none";
            MediaPlayer player = mVideoView.getmMediaPlayer();
            if (player != null) {
                player.selectTrack(MEDIA_SOURCE_SUBTITLE, -1);
            }
            if (mSubtitleDisplayHandler != null && mDisplaySubtitleRunner != null) {
                mSubtitleDisplayHandler.removeCallbacks(mDisplaySubtitleRunner);
            }
            mSubtitleTv.setVisibility(View.INVISIBLE);
        } else if (subtitleType == SubtitleType.INNER.ordinal()) {
            // 系统播放器没有内置字幕
            if (!mIsSysteamVideoKernel) {
                subtitleFrom = "incore";
                MediaPlayer player = mVideoView.getmMediaPlayer();
                if (player != null) {
                    player.selectTrack(MEDIA_SOURCE_SUBTITLE, 0);
                }
                if (mSubtitleDisplayHandler != null && mDisplaySubtitleRunner != null) {
                    mSubtitleDisplayHandler.removeCallbacks(mDisplaySubtitleRunner);
                }
                mSubtitleTv.setVisibility(View.VISIBLE);
            }
        } else if (subtitleType == SubtitleType.LOCAL.ordinal()) {
            subtitleFrom = "local";
            if (!mIsSysteamVideoKernel) {
                MediaPlayer player = mVideoView.getmMediaPlayer();
                if (player != null) {
                    player.selectTrack(MEDIA_SOURCE_SUBTITLE, -1);
                }
            }

            mGetSubtitleModel.loadSubtitle(subtitle.getLocalpath());
            showTipsMessage(R.string.tips_subtitle_local_loading, -1);
        } else if (subtitleType == SubtitleType.ONLINE.ordinal()) {
            subtitleFrom = "online";
            if (!mIsSysteamVideoKernel) {
                MediaPlayer player = mVideoView.getmMediaPlayer();
                if (player != null) {
                    player.selectTrack(MEDIA_SOURCE_SUBTITLE, -1);
                }
            }

            mGetSubtitleModel.downloadSubTitle(subtitle.getDownloadurl(), mItem.getFilePath());
            showTipsMessage(R.string.tips_subtitle_online_loading, -1);
        }

        // 统计--字幕使用
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("From", subtitleFrom);
        map.put("if_change", isChange ? "1" : "0");
        MobclickAgent.onEvent(this, "Subtitle_use", map);
    }

    private void showTipsMessage(String message, long time) {
        mTipsMessageTv.setText(message);
        mTipsMessageTv.setVisibility(View.VISIBLE);
        if (time != -1) {
            mSubtitleDisplayHandler.sendEmptyMessageDelayed(MSG_DISMISS_COMING_TO_END, time);
        }
    }

    // 默认2秒消失
    private void showTipsMessage(String message) {
        showTipsMessage(message, FINISH_RESERVE_TIME);
    }

    private void showTipsMessage(int resId, long time) {
        showTipsMessage(getResources().getString(resId), time);
    }

    private String getFileExtension(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return "";
        }

        int index = filePath.lastIndexOf(".");
        if (index < 0 || index == filePath.length() - 1) {
            return "";
        }

        return filePath.substring(index + 1);
    }

    private OnTimedTextListener mTimedTextListener = new OnTimedTextListener() {
        @Override
        public void onTimedText(MediaPlayer mediaPlayer, TimedText timedText) {
            if (mediaPlayer.isPlaying()) {
                TextView subtitles = (TextView) findViewById(R.id.txtSubtitles);
                if (timedText != null) {
                    subtitles.setText(timedText.getText());
                } else {
                    subtitles.setText(null);
                }
            }
        }
    };

    public void loadAdImage() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                final GetAdvImagesResponse.AdvImage image = mAdUtil.getRandomImage();
                String path = image == null ? null : image.images;
                if (TextUtils.isEmpty(path)) {
                    AppConfig.LOGD("[[PlayVideoActivity]] adv image path is empty.");
                    return;
                }
                AppConfig.LOGD("[[PlayVideoActivity]] adv image path: " + path);
                Bitmap bitmap = (Bitmap) CacheFactory.getCacheManager(CacheFactory.TYPE_CACHE.TYPE_IMAGE).getResourceFromMem(UtilsConfig.IMAGE_CACHE_CATEGORY_RAW,
                        path);
                if (bitmap == null) {
                    bitmap = BitmapFactory.decodeFile(path);
                    CacheFactory.getCacheManager(CacheFactory.TYPE_CACHE.TYPE_IMAGE).putResource(UtilsConfig.IMAGE_CACHE_CATEGORY_RAW, path, bitmap);
                }
                final Bitmap b = bitmap;
                mSubtitleDisplayHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mAdIv.setImageBitmap(b);
                        mCurrentAdImageId = image.id;
                    }
                });
            }
        });
    }

    public void showAdView() {
        if (mAdView.getVisibility() == View.GONE) {
            mAdView.setAnimation(AnimationUtils.loadAnimation(PlayVideoActivity.this, R.anim.fade_in));
            mAdView.setVisibility(View.VISIBLE);
        }

        HashMap<String, String> map = new HashMap<String, String>();
        map.put("From", "AD" + mCurrentAdImageId);
        MobclickAgent.onEvent(PlayVideoActivity.this, "AD_suspend", map);
    }

    public void hideAdView() {
        if (mAdView.getVisibility() == View.VISIBLE) {
            mAdView.setAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out));
            mAdView.setVisibility(View.GONE);
        }
    }
}
