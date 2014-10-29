package com.kankan.player.explorer;

import com.kankan.player.app.AppConfig;

import java.io.File;
import java.util.Hashtable;

/**
 * Created by zhangdi on 14-3-28.
 */
public class FileCategoryHelper {

    private static final String[] SUPPORT_VIDEO_FORMAT = AppConfig.SUPPORT_VIDEO_FORMAT;

    private static final String[] SUPPORT_APK_FORMAT = AppConfig.SUPPORT_APK_FORMAT;

    private static Hashtable<String, FileCategory> mCachedCategoryMap = new Hashtable<String, FileCategory>();

    static {
        for (String format : SUPPORT_APK_FORMAT) {
            mCachedCategoryMap.put(format, FileCategory.APK);
        }
        for (String format : SUPPORT_VIDEO_FORMAT) {
            mCachedCategoryMap.put(format, FileCategory.VIDEO);
        }
    }

    public static FileCategory getFileCategory(String path) {
        if (path != null) {
            File file = new File(path);
            if (file.isDirectory()) {
                return FileCategory.DIR;
            }

            int lastDot = path.lastIndexOf(".");
            if (lastDot > 0) {
                FileCategory category = mCachedCategoryMap.get(path.substring(lastDot + 1).toLowerCase());
                if (category != null) {
                    return category;
                }
            }
        }
        return FileCategory.OTHER;
    }

    public static FileCategory getFileCategoryByName(String name) {
        if (name != null) {
            int lastDot = name.lastIndexOf(".");
            if (lastDot > 0) {
                FileCategory category = mCachedCategoryMap.get(name.substring(lastDot + 1).toLowerCase());
                if (category != null) {
                    return category;
                }
            }
        }
        return FileCategory.OTHER;
    }
}
