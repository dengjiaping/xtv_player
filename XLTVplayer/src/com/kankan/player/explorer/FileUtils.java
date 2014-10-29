package com.kankan.player.explorer;

import android.os.Environment;
import com.kankan.player.app.AppConfig;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Created by zhangdi on 14-3-27.
 */
public class FileUtils {

    private static String ANDROID_SECURE = "/mnt/sdcard/.android_secure";

    private static String[] IGNORE_FILES = new String[]{"$RECYCLE.BIN", "RECYCLER", "System Volume Information", "FOUND.000", "FOUND.001", "FOUND.002", "FOUND.003", "LOST.DIR"};

    public static boolean isSDCardReady() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    public static String getSDCardDirectory() {
        return Environment.getExternalStorageDirectory().getPath();
    }

    public static boolean isNormalFile(String fullName) {
        return !fullName.equals(ANDROID_SECURE);
    }

    public static boolean shouldShowFile(String path) {
        return shouldShowFile(new File(path));
    }

    public static boolean shouldShowFile(File file) {
        if (file.isHidden())
            return false;

        if (file.getName().startsWith("."))
            return false;

        for (String s : IGNORE_FILES) {
            if (s.equalsIgnoreCase(file.getName()))
                return false;
        }

        return true;
    }

    public static FileItem getFileItem(File f) {
        FileItem fileItem = new FileItem();
        String filePath = f.getPath();
        File file = new File(filePath);
        fileItem.canRead = file.canRead();
        fileItem.canWrite = file.canWrite();
        fileItem.isHidden = file.isHidden();
        fileItem.fileName = f.getName();
        fileItem.filePath = filePath;
        fileItem.category = FileCategoryHelper.getFileCategory(filePath);
        fileItem.lastModifyTime = f.lastModified();

        if (fileItem.category == FileCategory.DIR) {
            //fileItem.fileSize = getDirectorySize(file);
            // 之前做new逻辑的时候需要计算文件夹的大小，但是文件夹太大子目录层次太深的时候，计算时间会非常长，这里文件夹先不计算大小
            fileItem.fileSize = 0;
        } else {
            fileItem.fileSize = file.length();
        }

        return fileItem;
    }


    public static String getExtFromFilename(String filename) {
        int dotPosition = filename.lastIndexOf('.');
        if (dotPosition != -1) {
            return filename.substring(dotPosition + 1, filename.length());
        }
        return "";
    }

    public static String getNameFromFilename(String filename) {
        int dotPosition = filename.lastIndexOf('.');
        if (dotPosition != -1) {
            return filename.substring(0, dotPosition);
        }
        return "";
    }

    public static String getPathFromFilepath(String filepath) {
        int pos = filepath.lastIndexOf('/');
        if (pos != -1) {
            return filepath.substring(0, pos);
        }
        return "";
    }

    public static String getNameFromFilepath(String filepath) {
        int pos = filepath.lastIndexOf('/');
        if (pos != -1) {
            return filepath.substring(pos + 1);
        }
        return "";
    }


    public static String makePath(String path1, String path2) {
        if (path1.endsWith(File.separator))
            return path1 + path2;

        return path1 + File.separator + path2;
    }

    public static boolean isSupportVideoFormat(String fullname){
        String format = FileUtils.getExtFromFilename(fullname);
        if(format != null){
            for(String str: AppConfig.SUPPORT_VIDEO_FORMAT){
                if(str.equals(format)){
                    return true;
                }
            }
        }
        return false;
    }

    public static long getDirectorySize(File directory) {
        if (directory == null || !directory.isDirectory()) {
            return 0;
        }

        File[] files = directory.listFiles();
        if (files == null || files.length == 0) {
            return 0;
        }

        long size = 0;
        for (File f : files) {
            if (f.isDirectory()) {
                size += getDirectorySize(f);
            } else if (f.isFile()) {
                size += f.length();
            }
        }
        return size;
    }
}
