package com.kankan.player.explorer;

import android.os.Handler;
import android.text.TextUtils;
import com.kankan.player.app.AppConfig;
import com.plugin.common.utils.CustomThreadPool;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Created by zhangdi on 14-4-1.
 */
public class FileScanHelper {

    public static interface FileScanListener {
        public void onScanStarted();

        public void onScanProgress(FileItem fileItem);

        public void onScanCompleted();
    }

    private Handler mHandler;

    public FileScanHelper() {
        mHandler = new Handler();
    }

    public void scanFile(final String path, final FilenameFilter filter, final FileScanListener listener) {
        scanStarted(listener);

        if (TextUtils.isEmpty(path)) {
            scanCompleted(listener);
            return;
        }

        CustomThreadPool.asyncWork(new Runnable() {
            @Override
            public void run() {
                File rootFile = new File(path);
                AppConfig.LOGD("[[FileScanHelper]] scanFile rootPath=" + rootFile.getAbsolutePath());
                File[] files = sortByLastModifyTime(rootFile);
                if (files != null && filter != null) {
                    for (File file : files) {
                        AppConfig.LOGD("\t scanFile " + file.getAbsolutePath());
                        if (filter.accept(rootFile, file.getName())) {
                            if (FileUtils.isNormalFile(file.getAbsolutePath()) && FileUtils.shouldShowFile(file)) {
                                FileItem fileItem = FileUtils.getFileItem(file);
                                scanProgress(listener, fileItem);
                            }

                        }
                    }
                } else {
                    if (files != null) {
                        for (File file : files) {
                            if (FileUtils.isNormalFile(file.getAbsolutePath()) && FileUtils.shouldShowFile(file)) {
                                FileItem fileItem = FileUtils.getFileItem(file);
                                scanProgress(listener, fileItem);
                            }
                        }
                    }
                }

                scanCompleted(listener);
            }
        });
    }

    private void scanStarted(final FileScanListener listener) {
        if (listener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onScanStarted();
                }
            });
        }
    }

    private void scanProgress(final FileScanListener listener, final FileItem fileItem) {
        if (listener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onScanProgress(fileItem);
                }
            });
        }
    }

    private void scanCompleted(final FileScanListener listener) {
        if (listener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onScanCompleted();
                }
            });
        }
    }

    private File[] sortByLastModifyTime(File rootFile){
        File[] files = rootFile.listFiles();
        if(files != null){
            Arrays.sort(files,new Comparator<File>() {
                @Override
                public int compare(File file, File file2) {

                    if(file2.lastModified() > file.lastModified()){
                        return 1;
                    }

                    if(file2.lastModified() < file.lastModified()){
                        return -1;
                    }

                    return 0;
                }
            });
        }

        return files;
    }

}
