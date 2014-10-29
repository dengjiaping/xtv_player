package com.kankan.player.activity;

import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.*;
import com.kankan.media.Media;
import com.kankan.player.adapter.FileExplorerAdapter;
import com.kankan.player.app.AppConfig;
import com.kankan.player.explorer.*;
import com.kankan.player.item.DeviceItem;
import com.kankan.player.manager.FileExploreHistoryManager;
import com.kankan.player.service.TdScanService;
import com.umeng.analytics.MobclickAgent;
import com.xunlei.tv.player.R;
import de.greenrobot.event.EventBus;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by zhangdi on 14-3-27.
 */
public class FileExplorerActivity extends BaseActivity implements AdapterView.OnItemClickListener, FileScanHelper.FileScanListener {
    private static final String TAG = FileExplorerActivity.class.getSimpleName();

    private static final String EXTRA_ROOT_PATH = "root_path";
    private static final String EXTRA_DEVICE_ITEM = "device_item";
    private static final String EXTRA_IS_TD_DOWNLOAD_ROOT = "is_td_download_root";
    private static final String EXTRA_IS_ROOT_DIR = "is_root_dir";

    private DeviceItem mDeviceItem;
    private String mRootPath;
    private boolean mIsRootDir;

    private TextView mTitleTv;
    private View mShadowView;
    private FilenameFilter mFilenameFilter;

    private List<FileItem> mFileItemList = new ArrayList<FileItem>();

    private ListView mListView;

    private FileExplorerAdapter mFileExplorerAdapter;

    private FileIconHelper mFileIconHelper;

    private FileScanHelper mFileScanHelper;

    private ProgressBar mLoadingPb;
    private View mEmptyView;
    private View mMediaRemovedView;
    private View mCoverView;

    private FileExploreHistoryManager mFileExploreHistoryManager;
    private ScannerReceiver mScannerReceiver;
    private int mListViewTotalHeight;

    // 如果U盘已拔出，那么直接返回主界面
    private boolean mIsDeviceAvailable = true;

    /**
     * 启动activity
     *
     * @param context
     * @param deviceItem
     * @param rootPath         根路径
     * @param isTDDownloadRoot
     */
    public static void startActivity(Context context, DeviceItem deviceItem, String rootPath, boolean isTDDownloadRoot, boolean mIsRootDir) {
        Intent intent = new Intent(context, FileExplorerActivity.class);
        intent.putExtra(EXTRA_DEVICE_ITEM, deviceItem);
        intent.putExtra(EXTRA_ROOT_PATH, rootPath);
        intent.putExtra(EXTRA_IS_TD_DOWNLOAD_ROOT, isTDDownloadRoot);
        intent.putExtra(EXTRA_IS_ROOT_DIR, mIsRootDir);
        context.startActivity(intent);
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_explorer);

        if (savedInstanceState != null) {
            mRootPath = savedInstanceState.getString(EXTRA_ROOT_PATH);
            mDeviceItem = (DeviceItem) savedInstanceState.getSerializable(EXTRA_DEVICE_ITEM);
            mIsRootDir = savedInstanceState.getBoolean(EXTRA_IS_ROOT_DIR);
        } else if (getIntent() != null) {
            mRootPath = getIntent().getStringExtra(EXTRA_ROOT_PATH);
            mDeviceItem = (DeviceItem) getIntent().getSerializableExtra(EXTRA_DEVICE_ITEM);
            mIsRootDir = getIntent().getBooleanExtra(EXTRA_IS_ROOT_DIR, false);
        }

        mFileExploreHistoryManager = new FileExploreHistoryManager(getApplicationContext());

        mFileIconHelper = new FileIconHelper(this);
        mFileIconHelper.setIconProcessFilter(null);
        initUI();

        mFilenameFilter = new FilenameExtFilter(AppConfig.SUPPORT_FILE_FORMAT);
        mFileScanHelper = new FileScanHelper();

        registerScannerReceiver();

        refreshFileList();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mFileIconHelper.pause();
    }

    @Override
    protected String getUmengPageName() {
        return "FileExplorerActivity";
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFileIconHelper.resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        unregisterReceiver(mScannerReceiver);
        mFileIconHelper.stop();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            if (!mIsRootDir) {
                finish();
                return true;
            }
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (mFileItemList.size() > 0) {
                performOnItemClick();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!mIsDeviceAvailable) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            }
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(EXTRA_DEVICE_ITEM, mDeviceItem);
        outState.putString(EXTRA_ROOT_PATH, mRootPath);
    }

    private void initUI() {
        mTitleTv = (TextView) findViewById(R.id.title);
        mShadowView = findViewById(R.id.shadow_iv);
        mEmptyView = findViewById(R.id.empty_rl);
        mMediaRemovedView = findViewById(R.id.media_removed_rl);
        mCoverView = findViewById(R.id.cover);
        mLoadingPb = (ProgressBar) findViewById(R.id.loading_pb);
        mListView = (ListView) findViewById(R.id.list_view);
        mFileExplorerAdapter = new FileExplorerAdapter(this, mFileItemList, mFileIconHelper);
        mListView.setAdapter(mFileExplorerAdapter);
        mListView.setOnItemClickListener(this);
        mListView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, final View view, final int position, long id) {
                mFileExplorerAdapter.setSelectedId(position);
                // 如果是最后一个，并且列表超过一屏，那么不显示蒙层
                if (position == mFileExplorerAdapter.getCount() - 1) {
                    mShadowView.setVisibility(View.GONE);
                } else if (mListViewTotalHeight > mListView.getHeight()) {
                    mShadowView.setVisibility(View.VISIBLE);
                }

                mCoverView.post(new Runnable() {
                    @Override
                    public void run() {
                        final int[] h = new int[2];
                        view.getLocationOnScreen(h);
                        mCoverView.setY(h[1] - getResources().getDimension(R.dimen.file_list_item_cover_top_margin));
                        if (mCoverView.getVisibility() != View.VISIBLE) {
                            mCoverView.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        ((ImageView) mEmptyView.findViewById(R.id.empty_icon_iv)).setImageResource(R.drawable.empty_device);
        if (Environment.getExternalStorageDirectory().getAbsolutePath().equals(mRootPath)) {
            if (mDeviceItem != null) {
                mTitleTv.setText(mDeviceItem.getName());
            }
        } else {
            if (mIsRootDir && mDeviceItem != null) {
                String deviceName = mDeviceItem.getName();
                mTitleTv.setText(deviceName == null ? "移动存储设备" : deviceName);
            } else {
                mTitleTv.setText(getFolderName(mRootPath));
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        performOnItemClick();
    }

    private void performOnItemClick() {
        FileItem fileItem = mFileItemList.get(mFileExplorerAdapter.getSelectedId());
        Log.d(TAG, fileItem.toString());

        if (fileItem.category == FileCategory.DIR) {
            FileExplorerActivity.startActivity(this, mDeviceItem, fileItem.filePath, false, false);
        } else if (fileItem.category == FileCategory.VIDEO) {
            playVideo(fileItem.filePath, mDeviceItem);
        } else if (fileItem.category == FileCategory.APK) {
            installApk(fileItem.filePath);
        }

        //mFileExploreHistoryManager.addFileToExploreHistory(fileItem, mDeviceItem.getName(), mDeviceItem.getType().ordinal());
//        mFileExploreHistoryManager.addFileToExploreHistory(fileItem, mDeviceItem);
//        if (fileItem.isNew) {
//            fileItem.isNew = false;
//            notifyDataSetChanged();
//        }
    }

    private void refreshFileList() {
        mEmptyView.setVisibility(View.GONE);
        mLoadingPb.setVisibility(View.VISIBLE);

        LOGD("Normal File Explorer");
        mFileScanHelper.scanFile(mRootPath, mFilenameFilter, this);
    }

    private void playVideo(String path, DeviceItem deviceItem) {
        PlayVideoActivity.start(this, path, deviceItem);
    }

    private void installApk(String path) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setDataAndType(Uri.fromFile(new File(path)), "application/vnd.android.package-archive");
            this.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            AppConfig.LOGD("[[FileExplorerActivity]] installApk " + e.getMessage());
        }
    }


    @Override
    public void onScanStarted() {

    }

    @Override
    public void onScanProgress(FileItem fileItem) {
        LOGD("onScanProgress:" + fileItem.toString());

        int deviceType = mDeviceItem.getType().ordinal();
        String deviceName = mDeviceItem.getName();

        //fileItem.isNew = mFileExploreHistoryManager.isFileNew(fileItem, deviceName, deviceType);
        //fileItem.isNew = mFileExploreHistoryManager.isFileNew(fileItem, mDeviceItem);

        mFileItemList.add(fileItem);
        notifyDataSetChanged();

        if (mFileExplorerAdapter.getCount() > 0) {
            mListView.setSelection(mFileExplorerAdapter.getSelectedId());
        }
    }

    @Override
    public void onScanCompleted() {
        mLoadingPb.setVisibility(View.GONE);

        if (mFileItemList.size() == 0) {
            mEmptyView.setVisibility(View.VISIBLE);
            mShadowView.setVisibility(View.GONE);
        } else {
            mListViewTotalHeight = getTotalHeightofListView();
            mShadowView.setVisibility(mListViewTotalHeight > mListView.getHeight() ? View.VISIBLE : View.GONE);
        }
    }

    private void notifyDataSetChanged() {
        mFileExplorerAdapter.notifyDataSetChanged();
    }

    private void LOGD(String msg) {
        if (!TextUtils.isEmpty(msg) && AppConfig.DEBUG) {
            Log.d(TAG, msg);
        }
    }

    private String getFolderName(String folderFullPath) {
        if (TextUtils.isEmpty(folderFullPath)) {
            return null;
        }

        int length = folderFullPath.length();
        if (folderFullPath.charAt(length - 1) == '/') {
            folderFullPath = folderFullPath.substring(0, length - 1);
        }

        int index = folderFullPath.lastIndexOf("/");
        return folderFullPath.substring(index + 1);
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
        intentFilter.addAction(TdScanService.ACTION_SEND_QUERY_LOCAL_RESULT);
        intentFilter.addAction(TdScanService.ACTION_SEND_QUERY_ROUTER_RESULT);
        intentFilter.addDataScheme("file");
        registerReceiver(mScannerReceiver, intentFilter);
    }

    private class ScannerReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (Intent.ACTION_MEDIA_EJECT.equals(action)
                    || action.equals(Intent.ACTION_MEDIA_REMOVED)
                    || action.equals(Intent.ACTION_MEDIA_BAD_REMOVAL)) {
                if (mDeviceItem.getType() == DeviceItem.DeviceType.EXTERNAL) //fix bug5036
                    return;
                mMediaRemovedView.setVisibility(View.VISIBLE);
                mEmptyView.setVisibility(View.GONE);
                mShadowView.setVisibility(View.GONE);
                mListView.setVisibility(View.GONE);
                mTitleTv.setText(mDeviceItem.getName());
                mCoverView.setVisibility(View.GONE);

                mIsDeviceAvailable = false;
            }
        }
    }

    private int getTotalHeightofListView() {
        int listviewElementsheight = 0;
        // 天猫魔盒getView会为空，目前先这么做，有空再来研究原因
        try {
            for (int i = 0; i < mFileExplorerAdapter.getCount(); i++) {
                View mView = mFileExplorerAdapter.getView(i, null, mListView);
                mView.measure(
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                listviewElementsheight += mView.getMeasuredHeight();
            }
        } catch (NullPointerException e) {
        }
        return listviewElementsheight;
    }
}
