package com.kankan.player.manager;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import com.kankan.media.Media;
import com.kankan.player.adapter.VideoAdapter;
import com.kankan.player.app.AppConfig;
import com.kankan.player.event.VideoItemEvent;
import com.kankan.player.event.VideoListEvent;
import com.kankan.player.explorer.FileUtils;
import com.kankan.player.item.DeviceItem;
import com.kankan.player.item.VideoItem;
import com.plugin.common.utils.CustomThreadPool;
import de.greenrobot.event.EventBus;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by zhangdi on 14-4-8.
 */
public class VideoScanner {
    private final Context mContext;
    private static VideoScanner videoScanner;

    private AtomicBoolean mIsScanning = new AtomicBoolean(false);

    private List<String> mRootDirs = new ArrayList<String>();

    /**
     * 待扫描路径列表
     */
    private List<String> mWaitingDirs = new ArrayList<String>();

    private List<VideoItem> mVideoList = new ArrayList<VideoItem>();

    private HashMap<String, Boolean> mVideoFormatMap = new HashMap<String, Boolean>();

    private AtomicBoolean mStop = new AtomicBoolean(false);


    public synchronized static VideoScanner getInstance(Context context) {
        if (videoScanner == null) {
            videoScanner = new VideoScanner(context);
        }
        return videoScanner;
    }

    private VideoScanner(Context context) {
        mContext = context;
        for (String ext : AppConfig.SUPPORT_VIDEO_FORMAT) {
            if (!TextUtils.isEmpty(ext)) {
                mVideoFormatMap.put(ext, true);
            }
        }
    }

    public List<VideoItem> getVideoList() {
        return mVideoList;
    }

    public void scanVideo() {
        if (mIsScanning.get()) {
            return;
        }
        mIsScanning.set(true);
        mStop.set(false);

        CustomThreadPool.asyncWork(new Runnable() {
            @Override
            public void run() {
                synchronized (mRootDirs) {
                    if (mRootDirs == null) {
                        mRootDirs = new ArrayList<String>();
                    }
                    mRootDirs.clear();

                    List<DeviceItem> usbDevices = DeviceManager.getInstance(mContext).getUsbDeviceList();
                    if (usbDevices != null) {
                        for (DeviceItem deviceItem : usbDevices) {
                            if (!TextUtils.isEmpty(deviceItem.getPath())) {
                                mRootDirs.add(deviceItem.getPath());
                            }
                        }
                    }
                    if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                        mRootDirs.add(Environment.getExternalStorageDirectory().getAbsolutePath());
                    }
                }

                mVideoList.clear();

                while (!mStop.get()) {
                    String path = getScanDir();
                    if (TextUtils.isEmpty(path)) {
                        break;
                    }

                    File file = new File(path);
                    if (!file.exists() || !file.isDirectory()) {
                        continue;
                    }

                    LOGD("scan file " + file.getAbsolutePath());

                    File[] files = file.listFiles(mVideoFilter);
                    if (files != null) {
                        for (File f : files) {
                            if (f.isDirectory()) {
                                addScanDir(f.getAbsolutePath());
                            } else {
                                VideoItem videoItem = new VideoItem();
                                videoItem.setFilePath(f.getAbsolutePath());
                                int duration = Media.getDuration(Uri.parse(f.getAbsolutePath()));
                                videoItem.setDuration(duration);
                                scanItem(videoItem);
                            }
                        }
                    }
                }

                mStop.set(true);
                mIsScanning.set(false);

                VideoListEvent event = new VideoListEvent();
                event.videoItemList = mVideoList;
                EventBus.getDefault().post(event);
            }
        });
    }

    public void stop() {
        mStop.set(true);
    }

    private String getScanDir() {
        synchronized (mWaitingDirs) {
            if (mWaitingDirs.size() > 0) {
                return mWaitingDirs.remove(0);
            } else {
                synchronized (mRootDirs) {
                    if (mRootDirs.size() > 0) {
                        return mRootDirs.remove(0);
                    }
                }
            }
            return null;
        }
    }

    private void addScanDir(String path) {
        synchronized (mWaitingDirs) {
            mWaitingDirs.add(path);
        }
    }

    private void scanItem(VideoItem videoItem) {
        LOGD("found video " + videoItem);
        if (!isReduplicative(videoItem)) {
            mVideoList.add(videoItem);
            VideoItemEvent event = new VideoItemEvent();
            event.videoItem = videoItem;
            EventBus.getDefault().post(event);
        }
    }

    private boolean isReduplicative(VideoItem videoItem) {
        for (VideoItem item : mVideoList) {
            if (item.getFilePath() == null || item.getFilePath().equals(videoItem.getFilePath())) {
                return true;
            }
        }
        return false;
    }

    private FilenameFilter mVideoFilter = new FilenameFilter() {

        @Override
        public boolean accept(File dir, String filename) {
            File file = new File(dir + File.separator + filename);
            if (!FileUtils.isNormalFile(file.getAbsolutePath()) || !FileUtils.shouldShowFile(file)) {
                return false;
            }

            if (file.isDirectory()) {
                return true;
            }

            int dotPosition = filename.lastIndexOf('.');
            if (dotPosition != -1) {
                String ext = (String) filename.subSequence(dotPosition + 1, filename.length());
                if (!TextUtils.isEmpty(ext)) {
                    return mVideoFormatMap.containsKey(ext);
                }
            }
            return false;
        }
    };

    private void LOGD(String msg) {
        if (AppConfig.DEBUG && !TextUtils.isEmpty(msg)) {
            Log.d(VideoScanner.class.getSimpleName(), msg);
        }
    }
}
