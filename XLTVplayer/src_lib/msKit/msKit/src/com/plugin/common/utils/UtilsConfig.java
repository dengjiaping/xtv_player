/**
 * Config.java
 */
package com.plugin.common.utils;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.plugin.common.utils.SingleInstanceBase.SingleInstanceManager;

/**
 * @author Guoqing Sun Mar 11, 20133:08:25 PM
 */
public class UtilsConfig {

    public static final boolean UTILS_DEBUG = true;

    public static final boolean DEBUG_NETWORK_ST = false & UTILS_DEBUG;
    public static final boolean RELEASE_UPLOAD_CRASH_LOG = true;

//    public static final String CURRENT_PACKAGE_NAME = "com.photo_message.mobile.android";
    /**
     * 当前的binding的应用程序包名，在初始化的时候绑定
     */
    public static String CURRENT_PACKAGE_NAME_BINDING= null;

    public static String DISK_DIR_PATH_BINDING = null;
    public static String DISK_TMP_DIR_PATH_BINDING = null;

    public static String DISK_LOG_PATH_BINDING = null;
    
    public static final int BITMAP_COMPRESS_LOW = 80;
    public static final int BITMAP_COMPRESS_MEDIUM = 90;
    public static final int BITMAP_COMPRESS_HIGH = 100;

    /*
     * 各种图片CacheManager的Category
     */
    public static final String IMAGE_CACHE_CATEGORY_USER_HEAD_ROUNDED = "user_head_rounded";
    public static final String IMAGE_CACHE_CATEGORY_RAW = "image_cache_category_source";
    public static final String IMAGE_CACHE_CATEGORY_THUMB = "image_cache_category_thumb";
    public static final String IMAGE_CACHE_CATEGORY_SMALL = "image_cache_category_small";

    public static DeviceInfo DEVICE_INFO;

    /**
     * init the utils lib
     * 
     * @param context
     */
    public static void init(Context context) {
        if (context != null) {
            //设置当前包名以及相关路径
            CURRENT_PACKAGE_NAME_BINDING = UtilsRuntime.getPackageName(context);
            DISK_DIR_PATH_BINDING = Environment.getExternalStorageDirectory() + "/." + CURRENT_PACKAGE_NAME_BINDING + "/";
            DISK_TMP_DIR_PATH_BINDING = Environment.getExternalStorageDirectory() + "/." + CURRENT_PACKAGE_NAME_BINDING + ".tmp/";
            DISK_LOG_PATH_BINDING = Environment.getExternalStorageDirectory() + "/." + CURRENT_PACKAGE_NAME_BINDING + "/";
            
            SingleInstanceManager.getInstance().init(context.getApplicationContext());
            UtilsConfig.DEVICE_INFO = new DeviceInfo(context.getApplicationContext());
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static String jsonFormatter(String uglyJSONString){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(uglyJSONString);
        String prettyJsonString = gson.toJson(je);
        return prettyJsonString;
    }

    public static void LOGD(String msg, boolean withExtraInfo) {
        if (UtilsConfig.UTILS_DEBUG) {
            String method = "";
            if (withExtraInfo) {
                method = UtilsRuntime.getCurrentStackMethodName();
            }
            DebugLog.d(method, msg);
        }
    }

    public static void LOGD(String msg, Throwable t) {
        if (UtilsConfig.UTILS_DEBUG) {
            StackTraceElement ste = Thread.currentThread().getStackTrace()[4];
            String invokeMethodName = ste.getMethodName();
            String fileName = ste.getFileName();
            long line = ste.getLineNumber();
            String method = "com.plugin.common.utils.Config";
            if (!TextUtils.isEmpty(invokeMethodName)) {
                method = fileName + "::" + invokeMethodName + "::" + line;
            }
            DebugLog.d(method, msg, t);
        }
    }

    public static void LOGD(String msg) {
        LOGD(msg, true);
    }

    public static void LOGD_WITH_TIME(String msg) {
        LOGD(msg + " >>>>>> TIME : " + UtilsRuntime.debugFormatTime(System.currentTimeMillis()));
    }

    /**
     * 流量统计关键字
     */
    public static final String NETWORK_STATISTICS_TYPE = "newtwork_st";

    public static final String NETWORK_STATISTICS_CATEGORY_IMAGE = "newtwork_image";

    public static final String NETWORK_STATISTICS_UP = "up";

    public static final String NETWORK_STATISTICS_DOWN = "down";

}
