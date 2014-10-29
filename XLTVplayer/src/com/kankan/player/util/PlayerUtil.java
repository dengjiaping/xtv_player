package com.kankan.player.util;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import com.kankan.media.Media;
import com.kankan.player.app.AppConfig;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.StringTokenizer;

public class PlayerUtil {
    public static final int THUMBNAIL_WIDTH = 320;
    public static final int THUMBNAIL_HEIGHT = 200;

    public static File getThumbnailCacheDir(Context context) {
        File cacheDir = context.getExternalCacheDir();
        if (cacheDir == null) {
            AppConfig.LOGD("[[PlayerUtil]] external cache dir is null.");
            cacheDir = new File(Environment.getExternalStorageDirectory(), "Android/data/com.xunlei.tv.player/cache");
        }
        File thumbnailCacheDir = new File(cacheDir, "thumbnail");
        if (!thumbnailCacheDir.exists()) {
            thumbnailCacheDir.mkdirs();
        }
        return thumbnailCacheDir;
    }

    public static File getThumbnailFile(Context context, String path, int millis) {
        String fileName = path.hashCode() + "_" + millis;
        return new File(getThumbnailCacheDir(context), fileName);
    }

    public static File makeVideoThumbnail(Context context, String path, int millis) {
        if (TextUtils.isEmpty(path)) {
            return null;
        }

        File thumbFile = getThumbnailFile(context, path, millis);
        boolean exists = thumbFile.exists();
        AppConfig.LOGD("[[PlayerUtil]] makeThumbnail alreadyExists=" + exists + ", path=" + path + ", millis=" + millis);
        if (!exists) {
            long start = System.nanoTime();
            Media.makeThumbnail(Uri.parse(encodePlayUrl(path)), thumbFile, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, millis);
            long end = System.nanoTime();
            AppConfig.LOGD("\t makeThumbnail total time is: " + (end - start));
        }
        return thumbFile.exists() && thumbFile.canRead() ? thumbFile : null;
    }

    public static String encodePlayUrl(String path) {
        if (TextUtils.isEmpty(path)) {
            return null;
        }

        if (!SmbUtil.isSmbPlayUrl(path)) {
            path = encodeUri(path);
        }
        return path;
    }

    /**
     * URL-encodes everything between "/"-characters. Encodes spaces as '%20' instead of '+'.
     */
    private static String encodeUri(String uri) {
        String newUri = "";
        StringTokenizer st = new StringTokenizer(uri, "/ ", true);
        while (st.hasMoreTokens()) {
            String tok = st.nextToken();
            if (tok.equals("/"))
                newUri += "/";
            else if (tok.equals(" "))
                newUri += "%20";
            else {
                try {
                    newUri += URLEncoder.encode(tok, "UTF-8");
                } catch (UnsupportedEncodingException ignored) {
                }
            }
        }
        return newUri;
    }
}
