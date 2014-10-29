package com.kankan.player.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.kankan.media.Media;
import com.kankan.player.activity.PlayVideoActivity;
import com.kankan.player.activity.VideoHistoryActivity;
import com.kankan.player.app.AppConfig;
import com.kankan.player.dao.model.VideoHistory;
import com.kankan.player.event.VideoHistoryEvent;
import com.kankan.player.explorer.FileCategory;
import com.kankan.player.explorer.FileIconLoader;
import com.kankan.player.item.VideoItem;
import com.kankan.player.manager.VideoHistoryManager;
import com.kankan.player.util.DateTimeFormatter;
import com.kankan.player.view.CustomToast;
import com.umeng.analytics.MobclickAgent;
import com.xunlei.tv.player.R;
import de.greenrobot.event.EventBus;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VideoHistoryAdapter extends BaseAdapter {
    private static final String TAG = VideoHistoryAdapter.class.getSimpleName();

    private Context mContext;
    private WeakReference<VideoHistoryActivity> mActivity;
    private LayoutInflater mLayoutInflater;
    private FileIconLoader mFileIconLoader;
    private boolean mInEditMode = false;

    private VideoHistoryManager mHistoryManager;
    private List<List<VideoHistory>> mHistoryListList = new ArrayList<List<VideoHistory>>();

    private ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private Map<String, Runnable> mTasks = new HashMap<String, Runnable>();

    private Handler mHandler = new Handler(Looper.getMainLooper());

    public VideoHistoryAdapter(VideoHistoryActivity activity, FileIconLoader fileIconLoader) {
        mContext = activity.getApplicationContext();
        mActivity = new WeakReference<VideoHistoryActivity>(activity);
        mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mFileIconLoader = fileIconLoader;
    }

    public void setVideoHistoryManager(VideoHistoryManager manager) {
        mHistoryManager = manager;
    }

    public void setInEditMode(boolean inEditMode) {
        mInEditMode = inEditMode;
        notifyDataSetChanged();
    }

    public boolean isInEditMode() {
        return mInEditMode;
    }

    public void setData(List<List<VideoHistory>> historyLL) {
        mHistoryListList.clear();
        if (historyLL != null) {
            mHistoryListList.addAll(historyLL);
        }



        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mHistoryListList != null ? mHistoryListList.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return mHistoryListList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.item_video_history, null);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (position == 0) {
            holder.indicatorIv.setImageResource(R.drawable.history_indicator_top);
            ((RelativeLayout.LayoutParams) holder.lineView.getLayoutParams()).topMargin = (int) ((mContext.getResources().getDisplayMetrics().density * 14));
        }

        final List<VideoHistory> videoHistoryList = (List<VideoHistory>) getItem(position);
        long timestamp = videoHistoryList.size() > 0 ? videoHistoryList.get(0).getTimestamp() : 0;

        holder.timeTv.setText(DateTimeFormatter.formatDate(timestamp - ((24 * 60 * 60 * 1000) * ((int) Math.random() * 3))));

        holder.gridContainer.removeAllViews();
        int totalVideoItems = videoHistoryList.size();
        for (int i = 0; i < totalVideoItems; i++) {
            final View itemView = mLayoutInflater.inflate(R.layout.item_video, null);
            itemView.setId(position * 100 + i);
            if (i == totalVideoItems - 1) {
                itemView.setNextFocusRightId(itemView.getId());
            }

            final ImageView densityIv = (ImageView) itemView.findViewById(R.id.density_indicator_iv);
            final ImageView thumbnailIv = (ImageView) itemView.findViewById(R.id.thumbnail_iv);
            final TextView nameTv = (TextView) itemView.findViewById(R.id.name_tv);
            final ProgressBar progressBar = (ProgressBar) itemView.findViewById(R.id.progress);
            final TextView durationTv = (TextView) itemView.findViewById(R.id.duration_tv);
            final View shadowView = itemView.findViewById(R.id.cover_shadow);

            final VideoItem videoItem = convert(videoHistoryList.get(i));
            LOGD("[[VideoHistoryAdpater]] getView video path is " + videoItem.getFilePath());
            final String fileName = videoItem.getFileName();
            final String filePath = videoItem.getFilePath();

            nameTv.setText(fileName);
            int densityResourceId = getResourceByDensity(videoItem);
            if (densityResourceId != -1) {
                densityIv.setImageResource(densityResourceId);
            }
            durationTv.setText(DateTimeFormatter.formatDuration(videoItem.getDuration()));
            progressBar.setMax(videoItem.getDuration());
            progressBar.setProgress(videoItem.getProgress());

            final boolean isHttpPath = filePath.startsWith("http://");
            final boolean needShadow = !isHttpPath && !new File(filePath).exists();
            videoItem.setExists(!needShadow);
            AppConfig.LOGD("\tneedShadow=" + needShadow);
            makeShadow(needShadow, shadowView, nameTv);

            if (!TextUtils.isEmpty(videoItem.getFilePath())) {
                thumbnailIv.setImageResource(R.drawable.video_thumnail_default);
                mFileIconLoader.loadIcon(thumbnailIv, filePath, FileCategory.VIDEO);
            } else {
                thumbnailIv.setImageDrawable(null);
            }

            // 用来判断网络路径是否合法
            if (isHttpPath) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        final boolean exists = isHttpPathValid(filePath);
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                videoItem.setExists(exists);
                                makeShadow(!exists, shadowView, nameTv);
                                mTasks.remove(filePath);
                            }
                        });
                    }
                };
                mTasks.put(filePath, runnable);
                mExecutor.execute(runnable);
            }

            final int currentPosition = i;
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mInEditMode) {
                        VideoHistory history = videoHistoryList.get(currentPosition);
                        boolean tag = mHistoryManager.removeHistory(history);
                        HashMap<String, String> map = new HashMap<String, String>();
                        map.put("From", "delete");
                        if (tag) {
                            map.put("if_success", "1");
                        } else {
                            map.put("if_success", "0");
                        }
                        MobclickAgent.onEvent(mContext, "del_history", map);
                        AppConfig.LOGD("[[VideoHistoryActivity]] send del_history delete event.");
                        videoHistoryList.remove(history);
                        setData(new ArrayList<List<VideoHistory>>(mHistoryListList));
                        EventBus.getDefault().post(new VideoHistoryEvent());
                        return;
                    }

                    final VideoHistoryActivity activity = mActivity.get();
                    if (activity != null && !TextUtils.isEmpty(filePath)) {
                        // 如果是本地文件
                        if (filePath.startsWith("/")) {
                            File f = new File(filePath);
                            if (f.exists()) {
                                playVideo(activity, videoItem, shadowView, nameTv);
                            } else {
                                // 文件不存在蒙层
                                makeShadow(true, shadowView, nameTv);
                                new CustomToast(activity, activity.getResources().getString(R.string.tips_not_found_title)).show();
                            }
                        } else if (filePath.startsWith("http://")) {
                            // 如果是http开头的（包括smb地址也是http开头的），需要在另外的线程中判断文件流存不存在，如果不存在则不可播放
                            AsyncTask.execute(new Runnable() {
                                @Override
                                public void run() {
                                    boolean exist = isHttpPathValid(filePath);
                                    videoItem.setExists(exist);

                                    if (exist) {
                                        mHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                playVideo(activity, videoItem, shadowView, nameTv);
                                            }
                                        });
                                    } else {
                                        mHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                // 文件不存在蒙层
                                                makeShadow(true, shadowView, nameTv);
                                                // 放大的那一层也蒙灰
                                                activity.zoomInVideoItem(itemView, videoItem, position, currentPosition);
                                                // 弹窗报错
                                                new CustomToast(activity, activity.getResources().getString(R.string.tips_not_found_title)).show();
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    }
                }
            });

            itemView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(final View v, final boolean hasFocus) {
                    AppConfig.LOGD("\t onFocusChanged " + v.getId() + ", hasFocus=" + hasFocus + ", inEditMode=" + mInEditMode);

                    if (hasFocus) {
                        nameTv.setVisibility(View.INVISIBLE);

                        VideoHistoryActivity activity = mActivity.get();
                        if (activity != null) {
                            activity.zoomInVideoItem(itemView, videoItem, position, currentPosition);
                        }
                    } else {
                        nameTv.setVisibility(View.VISIBLE);
                    }
                }
            });

            holder.gridContainer.addView(itemView);
        }

        return convertView;
    }

    static class ViewHolder {
        TextView timeTv;
        View lineView;
        ImageView indicatorIv;
        GridLayout gridContainer;

        public ViewHolder(View convertView) {
            timeTv = (TextView) convertView.findViewById(R.id.time_tv);
            lineView = convertView.findViewById(R.id.line);
            indicatorIv = (ImageView) convertView.findViewById(R.id.indicator);
            gridContainer = (GridLayout) convertView.findViewById(R.id.grid_container);
        }
    }

    private VideoItem convert(VideoHistory history) {
        VideoItem videoItem = new VideoItem();
        videoItem.setFilePath(history.getFilePath());
        videoItem.setFileName(history.getFileName());
        videoItem.setProgress(history.getProgress() != null ? history.getProgress().intValue() : 0);
        videoItem.setDuration(history.getDuration() != null ? history.getDuration().intValue() : 0);
        videoItem.setWidth(history.getWidth() != null ? history.getWidth().intValue() : 0);
        videoItem.setHeight(history.getHeight() != null ? history.getHeight().intValue() : 0);
        videoItem.setThumbnailPath(history.getThumbnailPath());
        videoItem.setDeviceType(history.getDeviceType() != null ? history.getDeviceType().intValue() : 0);
        videoItem.setDeviceName(history.getDeviceName());
        videoItem.setAudioMode(history.getAudioMode());
        videoItem.setDisplayMode(history.getDisplayMode());
        return videoItem;
    }

    public static int getResourceByDensity(VideoItem item) {
        if (item == null) {
            return -1;
        }

        int width = item.getWidth();
        int height = item.getHeight();

        VideoItem.Density density = VideoItem.Density.checkDensity(width, height);
        if (density == VideoItem.Density.UD) {
            return R.drawable.density_xxdpi;
        } else if (density == VideoItem.Density.HD) {
            return R.drawable.density_xdpi;
        } else if (density == VideoItem.Density.ND) {
            return R.drawable.density_hdpi;
        } else {
            return -1;
        }
    }

    private void LOGD(String message) {
        if (AppConfig.DEBUG) {
            Log.d(TAG, message);
        }
    }

    private void makeShadow(boolean needShadow, View shadowView, TextView nameTv) {
        if (needShadow) {
            shadowView.setBackgroundColor(Color.parseColor("#AA000000"));
            nameTv.setTextColor(Color.parseColor("#80FFFFFF"));
        } else {
            shadowView.setBackgroundColor(Color.parseColor("#00000000"));
            nameTv.setTextColor(Color.parseColor("#FFFFFFFF"));
        }
    }

    private void playVideo(Activity activity, VideoItem videoItem, View shadowView, TextView nameTv) {
        PlayVideoActivity.startForResult(activity, videoItem, true, VideoHistoryActivity.REQUEST_CODE_PLAY_VDIEO);

        // 确保文件存在的时候是不会蒙层的
        makeShadow(false, shadowView, nameTv);

        // 统计--播放概要
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("From", "history");
        map.put("if_continue", Media.getDuration(Uri.parse(videoItem.getFilePath())) == 0 ? "1" : "0");
        MobclickAgent.onEvent(mContext, "Play", map);
    }

    private boolean isHttpPathValid(String filePath) {
        boolean exist = false;
        try {
            URL url = new URL(filePath);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            InputStream is = connection.getInputStream();
            if (connection.getResponseCode() == 200) {
                exist = true;
            }
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }
        return exist;
    }

    public void shutDown() {
        if (!mExecutor.isShutdown()) {
            mExecutor.shutdown();
        }
    }
}
