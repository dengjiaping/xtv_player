/**
 * DiskTools.java
 */
package com.plugin.common.cache.image;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.text.TextUtils;

import com.plugin.common.cache.ICacheStrategy;
import com.plugin.common.utils.StringUtils;
import com.plugin.common.utils.UtilsConfig;
import com.plugin.common.utils.files.DiskManager;
import com.plugin.common.utils.files.DiskManager.DiskCacheType;
import com.plugin.common.utils.files.FileOperatorHelper;
import com.plugin.common.utils.image.ImageUtils;

/**
 * @author Guoqing Sun Jan 22, 201312:04:05 PM
 */
public final class BitmapDiskTools {
    private static final boolean DEBUG = false & UtilsConfig.UTILS_DEBUG;

    public static ICacheStrategy sDefaultCacheStrategy = new ICacheStrategy() {

        @Override
        public String onMakeImageCacheFullPath(String rootPath, String key, String ext) {
            if (DEBUG) {
                LOGD("[[onMakeImageCacheFullPath]] rootPath = " + rootPath + " key = " + key + " ext = " + ext);
            }

            StringBuilder sb = new StringBuilder(256);
            if (!rootPath.endsWith(File.separator)) {
                if (DEBUG) {
//                    sb.append(rootPath).append(File.separator).append(key.replace("/", "_").replace(":", "+").replace(".", "-")).append(ext);
                    sb.append(rootPath).append(File.separator).append(StringUtils.stringHashCode(key)).append(ext);
                } else {
                    sb.append(rootPath).append(File.separator).append(StringUtils.stringHashCode(key + ext));
                }
            } else {
                if (DEBUG) {
//                    sb.append(rootPath).append(key.replace("/", "_").replace(":", "+").replace(".", "-")).append(ext);
                    sb.append(rootPath).append(StringUtils.stringHashCode(key)).append(ext);
                } else {
                    sb.append(rootPath).append(StringUtils.stringHashCode(key + ext));
                }
            }

            if (DEBUG) {
                LOGD("[[onMakeImageCacheFullPath]] sb = " + sb.toString());
            }

            return sb.toString();
        }

        @Override
        public String onMakeFileKeyName(String category, String key) {
            StringBuilder sb = new StringBuilder(256);
            sb.append(category).append("/").append(key);
            return sb.toString();
        }

    };

    private static final String FILE_EX_NAME = UtilsConfig.UTILS_DEBUG ? ".png" : ".db_bmp";
    private static String IMAGE_CACHE_PATH_BINDING = null;
    
    public static void init(String path) {
        IMAGE_CACHE_PATH_BINDING = path;
    }

    public static boolean isExistSdcard() {
        return android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
    }

    public static boolean fileExists(String fileName) {
        String file = makeCacheImageFullPathStrategy(IMAGE_CACHE_PATH_BINDING, fileName, FILE_EX_NAME);
        if (!TextUtils.isEmpty(file)) {
            return new File(file).exists();
        }

        return false;
    }

    /**
     * 解析图片，如果reuseBt不为空的且reuseBt的宽高都等于要解析的图片的时候，会使用reuseBt的内存作为复用来解析图片
     *
     * @param strFileName
     * @param reuseBt
     * @return
     */
    public static Bitmap getBitmapFromDiskWithReuseBitmap(String strFileName, Bitmap reuseBt) {
        if (TextUtils.isEmpty(strFileName) || !isExistSdcard()) {
            return null;
        }

        File bmpFile = new File(makeCacheImageFullPathStrategy(IMAGE_CACHE_PATH_BINDING, strFileName, FILE_EX_NAME));
        if (!bmpFile.exists()) {
            if (DEBUG) {
                LOGD("[[getBitmapFromDiskWithReuseBitmap]] file name = " + bmpFile.getName() + " <<false>>");
            }
            return null;
        }

        Bitmap bmp = ImageUtils.loadBitmapWithSizeCheckAndBitmapReuse(bmpFile, reuseBt, 0);

        if (DEBUG) {
            LOGD("[[getBitmapFromDiskWithReuseBitmap]] file name = " + bmpFile.getName() + " <<true>>");
        }
        return bmp;
    }

    public static String saveRawBitmap(String fileUrl, InputStream is) {
        if (!isExistSdcard() || TextUtils.isEmpty(fileUrl) || is == null) {
            return null;
        }

        if (checkDiskFolder()) {
            String targetPath = makeCacheImageFullPathStrategy(IMAGE_CACHE_PATH_BINDING, fileUrl, FILE_EX_NAME);
            String savePath = FileOperatorHelper.saveFileByISSupportAppend(targetPath, is);

            if (ImageUtils.isBitmapData(savePath)) {
                return savePath;
            } else {
                removeBitmap(fileUrl);
                return null;
            }
        }

        return null;
    }

    public static String saveRawBitmap(String fileUrl, byte[] src) {
        if (!isExistSdcard() || TextUtils.isEmpty(fileUrl) || src != null) {
            return null;
        }

        if (checkDiskFolder()) {
            return saveBitmapToPathInternal(IMAGE_CACHE_PATH_BINDING, fileUrl, src);
        }
        return null;
    }

    public static String saveRawBitmap(String fileUrl, Bitmap bt) {
        if (bt == null || bt.isRecycled() || !isExistSdcard() || TextUtils.isEmpty(fileUrl)) {
            return null;
        }

        if (checkDiskFolder()) {
            File cacheFile = makeNewSaveFile(IMAGE_CACHE_PATH_BINDING, fileUrl);
            if (cacheFile == null) {
                return null;
            }

            if (ImageUtils.compressBitmapToFile(bt, cacheFile.getAbsolutePath())) {
                return cacheFile.getAbsolutePath();
            }
        }

        return null;
    }

    static boolean removeBitmap(String fileUrl) {
        if (!isExistSdcard() || TextUtils.isEmpty(fileUrl)) {
            return false;
        }

        if (checkDiskFolder()) {
            return new File(makeCacheImageFullPathStrategy(IMAGE_CACHE_PATH_BINDING, fileUrl, FILE_EX_NAME)).delete();
        }

        return false;
    }

    private static File makeNewSaveFile(String path, String fileUrl) {
        String cache = makeCacheImageFullPathStrategy(path, fileUrl, FILE_EX_NAME);
        File cacheFile = new File(cache);
        if (cacheFile.exists() && !cacheFile.delete()) {
            return null;
        }
        try {
            if (cacheFile.createNewFile()) {
                return cacheFile;
            }
        } catch (IOException e) {
            LOGD("Error for make file : " + cache);
            e.printStackTrace();
            return null;
        }

        return null;
    }

    private static String saveBitmapToPathInternal(String path, String fileUrl, byte[] src) {
        if (src == null || src.length == 0) {
            return null;
        }

        File cacheFile = makeNewSaveFile(path, fileUrl);
        if (cacheFile == null) {
            return null;
        }

        if (!TextUtils.isEmpty(FileOperatorHelper.saveFileByBytes(cacheFile.getAbsolutePath(), src))) {
            return cacheFile.getAbsolutePath();
        }

        return null;
    }

    private static boolean checkDiskFolder() {
        File file = new File(IMAGE_CACHE_PATH_BINDING);
        if (!file.exists() || !file.isDirectory()) {
            file.delete();
            return file.mkdirs();
        }

        return true;
    }

    protected static String getBitmapSavePath(String strFileName) {
        if (TextUtils.isEmpty(strFileName)) {
            return null;
        }

        return makeCacheImageFullPathStrategy(IMAGE_CACHE_PATH_BINDING, strFileName, FILE_EX_NAME);
    }

    private static String makeCacheImageFullPathStrategy(String rootPath, String key, String ext) {
        String fullPath = sDefaultCacheStrategy.onMakeImageCacheFullPath(rootPath, key, ext);
        if (DEBUG) {
            LOGD("[[makeCacheImageFullPathStrategy]] image full path : " + fullPath);
        }

        return fullPath;
    }

    private static final void LOGD(String msg) {
        if (DEBUG) {
            UtilsConfig.LOGD(msg);
        }
    }
}
