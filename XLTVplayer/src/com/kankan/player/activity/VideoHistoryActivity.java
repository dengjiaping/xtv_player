package com.kankan.player.activity;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.kankan.player.adapter.VideoHistoryAdapter;
import com.kankan.player.app.AppConfig;
import com.kankan.player.dao.model.VideoHistory;
import com.kankan.player.event.VideoHistoryEvent;
import com.kankan.player.explorer.FileCategory;
import com.kankan.player.explorer.FileIconLoader;
import com.kankan.player.item.VideoItem;
import com.kankan.player.manager.VideoHistoryManager;
import com.kankan.player.util.DateTimeFormatter;
import com.kankan.player.view.CustomToast;
import com.kankan.player.view.MarqueenTextView;
import com.umeng.analytics.MobclickAgent;
import com.xunlei.tv.player.R;
import de.greenrobot.event.EventBus;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VideoHistoryActivity extends BaseActivity {
    public static final int REQUEST_CODE_PLAY_VDIEO = 1;
    public static final int REQUEST_CODE_MENU = 2;

    private ListView mListView;
    private View mEmptyView;
    private View mMenuView;
    private ImageView mMenuIconIv;
    private TextView mMenuTitleTv;
    private VideoHistoryAdapter mVideoHistoryAdapter;

    private VideoHistoryManager mVideoHistoryManager;
    private List<List<VideoHistory>> mHistoryListList = new ArrayList<List<VideoHistory>>();

    private FileIconLoader mFileIconLoader;
    private ScannerReceiver mScannerReceiver;
    private boolean mIsDeviceAvailable = true;

    // video item cover
    private View mVideoItemCoverView;
    private ImageView mDensityIv;
    private ImageView mThumbnailIv;
    private MarqueenTextView mNameTv;
    private View mDeleteCoverView;
    private ProgressBar mProgressBar;
    private TextView mDurationTv;
    private View mCoverShadowView;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        initUI();

        mFileIconLoader = new FileIconLoader(this, null);
        mFileIconLoader.setThumbnailMode();
        mVideoHistoryAdapter = new VideoHistoryAdapter(this, mFileIconLoader);
        mListView.setAdapter(mVideoHistoryAdapter);

        mVideoHistoryManager = new VideoHistoryManager(this);
        mVideoHistoryAdapter.setVideoHistoryManager(mVideoHistoryManager);
        loadHistoryData();

        EventBus.getDefault().register(this);
        registerScannerReceiver();
    }

    private void initUI() {
        ((TextView) findViewById(R.id.title)).setText("历史记录");

        mEmptyView = findViewById(R.id.empty_rl);
        mListView = (ListView) findViewById(R.id.list_view);
        mListView.setItemsCanFocus(true);
        mListView.setFocusable(false);
        mMenuView = findViewById(R.id.menu_ll);
        mMenuIconIv = (ImageView) findViewById(R.id.menu_icon_iv);
        mMenuTitleTv = (TextView) findViewById(R.id.menu_title_iv);

        mMenuView.setVisibility(View.VISIBLE);
        changeMenuStatus(false);

        mVideoItemCoverView = findViewById(R.id.cover_ll);
        mDensityIv = (ImageView) findViewById(R.id.density_indicator_iv);
        mThumbnailIv = (ImageView) findViewById(R.id.thumbnail_iv);
        mNameTv = (MarqueenTextView) findViewById(R.id.name_tv);
        mDeleteCoverView = findViewById(R.id.delete_cover_ll);
        mProgressBar = (ProgressBar) findViewById(R.id.progress);
        mDurationTv = (TextView) findViewById(R.id.duration_tv);
        mCoverShadowView = findViewById(R.id.cover_shadow);
    }

    private void changeMenuStatus(boolean inEditMode) {
        if (inEditMode) {
            mMenuIconIv.setImageResource(R.drawable.history_menu_back);
            mMenuTitleTv.setText(R.string.history_menu_title_back);
        } else {
            mMenuIconIv.setImageResource(R.drawable.history_menu);
            mMenuTitleTv.setText(R.string.history_menu_title_main);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFileIconLoader.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mFileIconLoader.pause();
    }

    @Override
    protected String getUmengPageName() {
        return "VideoHistoryActivity";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mFileIconLoader.stop();
        mVideoHistoryAdapter.shutDown();
        EventBus.getDefault().unregister(this);
        unregisterReceiver(mScannerReceiver);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (mHistoryListList.size() > 0 && !mVideoHistoryAdapter.isInEditMode()) {
                startActivityForResult(new Intent(this, HistoryMenuActivity.class), REQUEST_CODE_MENU);
                return true;
            }
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mVideoHistoryAdapter.isInEditMode()) {
                changeMenuStatus(false);
                mVideoHistoryAdapter.setInEditMode(false);
                loadHistoryData();

                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public void onEventMainThread(VideoHistoryEvent event) {
        loadHistoryData();
        mListView.setSelection(0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_MENU && resultCode == RESULT_OK && data != null) {
            int mode = data.getIntExtra(HistoryMenuActivity.EXTRA_EDIT_MODE, HistoryMenuActivity.EXTRA_REMOVE_ITEM);

            if (mode == HistoryMenuActivity.EXTRA_CLEAR_ALL) {
                showClearAllConfirmDialog();
            } else {
                changeMenuStatus(true);
                mVideoHistoryAdapter.setInEditMode(true);
            }
        }
    }

    private void loadHistoryData() {
        mHistoryListList = mVideoHistoryManager.getHistoryListList();

        if (mHistoryListList == null || mHistoryListList.size() == 0) {
            mListView.setVisibility(View.GONE);
            mMenuView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
            mVideoItemCoverView.setVisibility(View.INVISIBLE);
        } else {
            mListView.setVisibility(View.VISIBLE);
            mMenuView.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);
            mVideoHistoryAdapter.setData(mHistoryListList);
        }
    }

    private void registerScannerReceiver() {
        mScannerReceiver = new ScannerReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        intentFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
        intentFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        intentFilter.addDataScheme("file");
        registerReceiver(mScannerReceiver, intentFilter);
    }

    private class ScannerReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (Intent.ACTION_MEDIA_EJECT.equals(action) || action.equals(Intent.ACTION_MEDIA_REMOVED) || action.equals(Intent.ACTION_MEDIA_BAD_REMOVAL)) {
                if (mIsDeviceAvailable) {
                    mIsDeviceAvailable = false;
                    new CustomToast(VideoHistoryActivity.this, "移动存储设备被拔出").show();
                }
            }
        }
    }

    private void showClearAllConfirmDialog() {
        final Dialog dialog = new Dialog(this, R.style.toast);
        View view = getLayoutInflater().inflate(R.layout.alert_clear_history, null);
        view.findViewById(R.id.ok_bt).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean tag = mVideoHistoryManager.clearHistory();
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("From", "clearall");
                if (tag) {
                    map.put("if_success", "1");
                } else {
                    map.put("if_success", "0");
                }
                MobclickAgent.onEvent(VideoHistoryActivity.this, "del_history", map);
                AppConfig.LOGD("[[VideoHistoryActivity]] send del_history clearall event.");
                EventBus.getDefault().post(new VideoHistoryEvent());
                loadHistoryData();
                dialog.dismiss();
            }
        });
        view.findViewById(R.id.cancel_bt).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.setContentView(view);

        WindowManager.LayoutParams p = dialog.getWindow().getAttributes();
        p.width = (int) getResources().getDimension(R.dimen.history_clear_all_alert_width);
        p.height = (int) getResources().getDimension(R.dimen.history_clear_all_alert_height);
        p.gravity = Gravity.CENTER_VERTICAL;
        dialog.getWindow().setAttributes(p);

        dialog.show();
    }

    public void zoomInVideoItem(final View view, final VideoItem videoItem, final int position, final int index) {
        view.post(new Runnable() {
            @Override
            public void run() {
                String filePath = videoItem.getFilePath();
                mProgressBar.setMax(videoItem.getDuration());
                mProgressBar.setProgress(videoItem.getProgress());
                mDurationTv.setText(DateTimeFormatter.formatDuration(videoItem.getDuration()));
                mNameTv.restartMarqueen(videoItem.getFileName());
                mFileIconLoader.loadIcon(mThumbnailIv, filePath, FileCategory.VIDEO);
                int densityResourceId = VideoHistoryAdapter.getResourceByDensity(videoItem);
                if (densityResourceId != -1) {
                    mDensityIv.setBackgroundResource(densityResourceId);
                    mDensityIv.setVisibility(View.VISIBLE);
                } else {
                    mDensityIv.setVisibility(View.INVISIBLE);
                }
                final boolean needShadow = !videoItem.isExists();

                final boolean isInEditMode = mVideoHistoryAdapter.isInEditMode();
                if (isInEditMode) {
                    mDeleteCoverView.setVisibility(View.VISIBLE);
                    mNameTv.setTextColor(Color.parseColor("#FFFFFFFF"));
                    mCoverShadowView.setBackgroundColor(Color.parseColor("#00000000"));
                } else {
                    mDeleteCoverView.setVisibility(View.GONE);
                    if (needShadow) {
                        mCoverShadowView.setBackgroundColor(Color.parseColor("#AA000000"));
                        mNameTv.setTextColor(Color.parseColor("#80FFFFFF"));
                    } else {
                        mCoverShadowView.setBackgroundColor(Color.parseColor("#00000000"));
                        mNameTv.setTextColor(Color.parseColor("#FFFFFFFF"));
                    }
                }

                final float startWidth = getResources().getDimension(R.dimen.history_video_item_width);
                final float startHeight = getResources().getDimension(R.dimen.history_video_item_height);
                final float finalWidth = getResources().getDimension(R.dimen.history_video_item_cover_width);
                final float finalHeight = getResources().getDimension(R.dimen.history_video_item_cover_height);
                final float padding = getResources().getDimension(R.dimen.history_video_item_padding);
                final float heightPixels = getResources().getDimension(R.dimen.height_pixels_normal);

                final Rect startBounds = new Rect();
                view.getGlobalVisibleRect(startBounds);

                float centerX = startBounds.left + padding + startWidth / 2;
                float centerY = 0.0f;

                AppConfig.LOGD("top=" + startBounds.top + ", viewHeight=" + view.getHeight() + ", startHeight=" + startHeight + ", heightPixels=" + heightPixels);
                if (startBounds.top + view.getHeight() > heightPixels) {
                    centerY = heightPixels - view.getHeight() + padding + startHeight / 2;
                } else {
                    centerY = startBounds.top + padding + startHeight / 2;
                }
                mVideoItemCoverView.setX(centerX - mVideoItemCoverView.getWidth() / 2);
                mVideoItemCoverView.setY(centerY - finalHeight / 2);
                mVideoItemCoverView.setVisibility(View.VISIBLE);
            }
        });
    }

}
