package com.kankan.player.activity;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.*;
import com.kankan.media.Media;
import com.kankan.player.adapter.FileExplorerAdapter;
import com.kankan.player.app.AppConfig;
import com.kankan.player.explorer.FileCategory;
import com.kankan.player.explorer.FileCategoryHelper;
import com.kankan.player.explorer.FileIconHelper;
import com.kankan.player.explorer.FileItem;
import com.kankan.player.item.DeviceItem;
import com.kankan.player.manager.FileExploreHistoryManager;
import com.kankan.player.util.SettingManager;
import com.kankan.player.util.SmbUtil;
import com.plugin.common.utils.CustomThreadPool;
import com.umeng.analytics.MobclickAgent;
import com.xunlei.tv.player.R;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileFilter;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;

public class SmbExplorerActivity extends BaseActivity implements AdapterView.OnItemClickListener {
    private static final String TAG = SmbExplorerActivity.class.getSimpleName();

    private static final int ERROR_CODE_USB_EJECT = 0x100;
    private static final int ERROR_CODE_SERVER_ERROR = 0x101;

    private static final String EXTRA_ROOT_PATH = "root_path";
    private static final String EXTRA_DEVICE_ITEM = "device_item";
    private static final String EXTRA_DIR_LEVEL = "dir_devel";

    private DeviceItem mDeviceItem;
    private String mRootPath;
    private int mDirLevel;

    private TextView mTitleTv;
    private View mShadowView;

    private List<FileItem> mFileItemList = new ArrayList<FileItem>();

    private ListView mListView;
    private FileExplorerAdapter mAdapter;
    private FileIconHelper mFileIconHelper;

    private ProgressBar mLoadingPb;
    private View mEmptyView;
    private View mMediaRemovedView;
    private TextView mMediaRemovedTitleTv;
    private View mCoverView;

    private FileExploreHistoryManager mFileExploreHistoryManager;
    private int mListViewTotalHeight;
    private Handler mHandler = new Handler();
    private SmbFileFilter mFileFilter;

    public static void start(Context context, DeviceItem deviceItem, String rootPath, int dirLevel) {
        Intent intent = new Intent(context, SmbExplorerActivity.class);
        if (deviceItem != null) {
            intent.putExtra(EXTRA_DEVICE_ITEM, deviceItem);
        }
        if (rootPath != null) {
            intent.putExtra(EXTRA_ROOT_PATH, rootPath);
        }
        intent.putExtra(EXTRA_DIR_LEVEL, dirLevel);
        context.startActivity(intent);
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_explorer);

        if (savedInstanceState != null) {
            mRootPath = savedInstanceState.getString(EXTRA_ROOT_PATH);
            mDeviceItem = (DeviceItem) savedInstanceState.getSerializable(EXTRA_DEVICE_ITEM);
            mDirLevel = savedInstanceState.getInt(EXTRA_DIR_LEVEL);
        } else if (getIntent() != null) {
            mRootPath = getIntent().getStringExtra(EXTRA_ROOT_PATH);
            mDeviceItem = (DeviceItem) getIntent().getSerializableExtra(EXTRA_DEVICE_ITEM);
            mDirLevel = getIntent().getIntExtra(EXTRA_DIR_LEVEL, 0);
        }

        mFileExploreHistoryManager = new FileExploreHistoryManager(getApplicationContext());
        mFileIconHelper = new FileIconHelper(this);

        if (mRootPath == null) {
            finish();
            return;
        }

        AppConfig.LOGD("[[SmbExploreActivity]] onCreate rootPath = " + mRootPath + "; dirLevel = " + mDirLevel);

        mFileIconHelper.setIconProcessFilter(null);
        mFileFilter = new SmbFileNameExtFilter(AppConfig.SUPPORT_FILE_FORMAT);
        initUI();

        CustomThreadPool.asyncWork(new Runnable() {
            @Override
            public void run() {
                onScanStart();

                try {
                    SmbFile smbFile = new SmbFile(mRootPath);

                    mFileItemList.clear();
                    if (!smbFile.exists()) {
                        onSmbError(ERROR_CODE_USB_EJECT);
                        return;
                    }

                    SmbFile[] fileList = SmbUtil.listFiles(smbFile, mFileFilter);

                    // 如果是根目录(根目录位分区列表)的话，先检测列出来的每个分区是否能正常访问，如果不能或者没有内容就过滤掉
                    // 然后如果剩下只有一个分区，那么直接显示分区里面的内容，如果多于一个分区，那么显示分区列表
                    if (mDirLevel == 0 && fileList != null) {
                        List<SmbFile> tmpList = new ArrayList<SmbFile>();
                        for (SmbFile f : fileList) {
                            SmbFile[] sfs = SmbUtil.listFiles(f, mFileFilter);
                            if (sfs != null && sfs.length > 0) {
                                tmpList.add(f);
                            }
                        }

                        if (tmpList.size() == 0) {
                            fileList = null;
                        } else if (tmpList.size() == 1) {
                            fileList = SmbUtil.listFiles(tmpList.get(0), mFileFilter);
                        } else {
                            fileList = tmpList.toArray(new SmbFile[tmpList.size()]);
                        }
                    }

                    if (fileList == null || fileList.length == 0) {
                        onScanCompleted();
                        return;
                    }

                    // 按照时间倒序排序，如果时间相同，则按名称自然排序
                    Arrays.sort(fileList, new Comparator<SmbFile>() {
                        @Override
                        public int compare(SmbFile lhs, SmbFile rhs) {
                            long ltime = lhs.getLastModified();
                            long rtime = rhs.getLastModified();
                            if (ltime < rtime) {
                                return 1;
                            } else if (ltime > rtime) {
                                return -1;
                            } else {
                                return lhs.getName().compareTo(rhs.getName());
                            }
                        }
                    });

                    for (SmbFile f : fileList) {
                        FileItem item = new FileItem();
                        String fileName = f.getName();
                        String filePath = f.getCanonicalPath();

                        if (f.isDirectory()) {
                            fileName = fileName.substring(0, fileName.length() - 1);
                            item.category = FileCategory.DIR;
                            item.fileSize = 0;
                        } else if (f.isFile()) {
                            item.category = FileCategoryHelper.getFileCategoryByName(fileName);
                            item.fileSize = f.length();
                        } else {
                            continue;
                        }

                        item.fileName = fileName;
                        item.filePath = filePath;
                        item.lastModifyTime = f.getLastModified();
                        item.canRead = f.canRead();
                        item.canWrite = f.canWrite();

                        onScanProgress(item);
                    }

                    onScanCompleted();
                } catch (final MalformedURLException e) {
                    AppConfig.LOGD("MalformedURLException " + e.getMessage());
                    onSmbError(ERROR_CODE_SERVER_ERROR);
                } catch (final IOException e) {
                    AppConfig.LOGD("IOException " + e.getMessage());
                    onSmbError(ERROR_CODE_SERVER_ERROR);
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mFileIconHelper.pause();
    }

    @Override
    protected String getUmengPageName() {
        return "SmbExplorerActivity";
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFileIconHelper.resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mFileIconHelper.stop();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            if (mDirLevel != 0) {
                finish();
                return true;
            }
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (mFileItemList.size() > 0) {
                performOnItemClick();
            }
            return true;
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
        mLoadingPb = (ProgressBar) findViewById(R.id.loading_pb);
        mMediaRemovedView = findViewById(R.id.media_removed_rl);
        mCoverView = findViewById(R.id.cover);

        mMediaRemovedTitleTv = (TextView) findViewById(R.id.media_removed_title);
        mListView = (ListView) findViewById(R.id.list_view);
        mAdapter = new FileExplorerAdapter(this, mFileItemList, mFileIconHelper);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        mListView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, final View view, int position, long id) {
                mAdapter.setSelectedId(position);
                // 如果是最后一个，并且列表超过一屏，那么不显示蒙层
                if (position == mAdapter.getCount() - 1) {
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
        mTitleTv.setText(getFolderName(mRootPath));
        ((TextView) findViewById(R.id.empty_content)).setText(R.string.smb_file_list_empty);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        performOnItemClick();
    }

    private void performOnItemClick() {
        FileItem fileItem = mFileItemList.get(mAdapter.getSelectedId());
        Log.d(TAG, fileItem.toString());

        if (fileItem.category == FileCategory.DIR) {
            SmbExplorerActivity.start(this, mDeviceItem, fileItem.filePath, mDirLevel + 1);
        } else if (fileItem.category == FileCategory.VIDEO) {
            playVideo(fileItem.filePath, mDeviceItem);
        } else if (fileItem.category == FileCategory.APK) {
            installApk(fileItem.filePath);
        }

//        mFileExploreHistoryManager.addFileToExploreHistory(fileItem, mDeviceItem);
//        if (fileItem.isNew) {
//            fileItem.isNew = false;
//            notifyDataSetChanged();
//        }
    }

    private void playVideo(String path, DeviceItem deviceItem) {
        if (SmbUtil.isSmbPath(path)) {
            path = SmbUtil.generateSmbPlayPath(path);
        }
        PlayVideoActivity.start(this, path, deviceItem);
    }

    private void installApk(String path) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setDataAndType(Uri.fromFile(new File(path)), "application/vnd.android.package-archive");
            this.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            AppConfig.LOGD("[[SmbExplorerActivity]] installApk " + e.getMessage());
        }
    }

    public void onScanStart() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mEmptyView.setVisibility(View.GONE);
                mLoadingPb.setVisibility(View.VISIBLE);
            }
        });
    }

    public void onScanProgress(final FileItem fileItem) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                //fileItem.isNew = mFileExploreHistoryManager.isFileNew(fileItem, mDeviceItem);

                mFileItemList.add(fileItem);
                notifyDataSetChanged();

                if (mAdapter.getCount() > 0) {
                    mListView.setSelection(mAdapter.getSelectedId());
                }
            }
        });
    }

    public void onScanCompleted() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mLoadingPb.setVisibility(View.GONE);

                if (mFileItemList.size() == 0) {
                    mEmptyView.setVisibility(View.VISIBLE);
                    mShadowView.setVisibility(View.GONE);
                } else {
                    mListViewTotalHeight = getTotalHeightofListView();
                    mShadowView.setVisibility(mListViewTotalHeight > mListView.getHeight() ? View.VISIBLE : View.GONE);
                }
            }
        });
    }

    private void notifyDataSetChanged() {
        mAdapter.notifyDataSetChanged();
    }

    private String getFolderName(String folderFullPath) {
        if (TextUtils.isEmpty(folderFullPath) || mDirLevel == 0) {
            String name = SmbUtil.getRouterDisplayName(null);
            return name == null ? SmbUtil.ROUTER_NAME_DEFAULT : name;
        }

        int length = folderFullPath.length();
        if (folderFullPath.charAt(length - 1) == '/') {
            folderFullPath = folderFullPath.substring(0, length - 1);
        }

        int index = folderFullPath.lastIndexOf("/");
        return folderFullPath.substring(index + 1);
    }

    private int getTotalHeightofListView() {
        int listviewElementsheight = 0;
        // 天猫魔盒getView会为空，目前先这么做，有空再来研究原因
        try {
            for (int i = 0; i < mAdapter.getCount(); i++) {
                View mView = mAdapter.getView(i, null, mListView);
                mView.measure(
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                listviewElementsheight += mView.getMeasuredHeight();
            }
        } catch (NullPointerException e) {
        }
        return listviewElementsheight;
    }

    private void onSmbError(final int errorCode) {
//        mHandler.post(new Runnable() {
//            @Override
//            public void run() {
//                if (errorCode == ERROR_CODE_USB_EJECT) {
//                    UIHelper.showAlertTips(SmbExplorerActivity.this, R.drawable.icon_warn, "存储设备已拔出", "", true);
//                } else {
//                    UIHelper.showAlertTips(SmbExplorerActivity.this, R.drawable.icon_warn, "路由器已断开连接", "", true);
//                }
//            }
//        });
        mMediaRemovedView.setVisibility(View.VISIBLE);
        mEmptyView.setVisibility(View.GONE);
        mShadowView.setVisibility(View.GONE);
        mListView.setVisibility(View.GONE);
        mCoverView.setVisibility(View.GONE);
        mMediaRemovedTitleTv.setText(errorCode == ERROR_CODE_USB_EJECT ? "存储设备已拔出" : "路由器已断开连接");
    }

    public static class SmbFileNameExtFilter implements SmbFileFilter {
        private static final List<String> FOLDER_NEED_HIDDEN = new ArrayList<String>();

        static {
            FOLDER_NEED_HIDDEN.add("System Volume Information");
            FOLDER_NEED_HIDDEN.add("$RECYCLE.BIN");
            FOLDER_NEED_HIDDEN.add("IPC$");
            FOLDER_NEED_HIDDEN.add("RECYCLER");
            FOLDER_NEED_HIDDEN.add("found.000");
            FOLDER_NEED_HIDDEN.add("found.001");
            FOLDER_NEED_HIDDEN.add("found.002");
            FOLDER_NEED_HIDDEN.add("found.003");
            FOLDER_NEED_HIDDEN.add("LOST.DIR");
        }

        private HashSet<String> mExts = new HashSet<String>();

        public SmbFileNameExtFilter(String[] exts) {
            if (exts != null) {
                for (int i = 0; i < exts.length; i++) {
                    if (exts[i] != null)
                        exts[i] = exts[i].toLowerCase();
                }
                mExts.addAll(Arrays.asList(exts));
            }
        }

        @Override
        public boolean accept(SmbFile smbFile) throws SmbException {
            String fileName = smbFile.getName();
            AppConfig.LOGD("[[SmbFileNameExtFilter]] accept fileName=" + fileName);

            if (fileName.startsWith(".")) {
                return false;
            }

            if (smbFile.isDirectory()) {
                fileName = fileName.substring(0, fileName.length() - 1);
            }

            if (smbFile.isDirectory() && !FOLDER_NEED_HIDDEN.contains(fileName)) {
                return true;
            }

            int dotPosition = fileName.lastIndexOf('.');
            if (dotPosition != -1) {
                String ext = (String) fileName.subSequence(dotPosition + 1, fileName.length());
                return mExts.contains(ext.toLowerCase());
            }

            return false;
        }
    }

}
