package com.kankan.player.app;

import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class AppConfig {

    public static final boolean DEBUG = true;

    public static final boolean DEBUG_REMOTE = true;

    public static String PARTNER_ID = null;

    public static String DEVICE_LICENSE = null;

    public static String NTFS_TYPE = null;

    public static final boolean isForXLRouter = true;

    public static final boolean isRemoteRouterOpen = true;

    public static final boolean OPEN_HARDWARE_ACCELERATE = true;

    // 播放广告位功能编译开关
    public static final boolean ADVERTISE_ON = true;

    /**
     * 小米1、小米1s和小米电视1代上面放大和跑马灯两个效果同时出现的时候跑马灯会断裂，这个bug不知道咋解，目前策略在这些设备上不放大
     */
    public static final List<String> BOX_NO_ZOOM = new ArrayList<String>();

    public static final String[] SUPPORT_VIDEO_FORMAT = new String[]{"mp4", "3gp", "wmv",
            "ts", "rmvb", "mov", "m4v", "avi", "m3u8", "3gpp", "3gpp2", "mkv", "flv",
            "divx", "f4v", "rm", "asf", "ram", "mpg", "v8", "swf", "m2v", "asx", "ra",
            "ndivx", "xvid", "vob", "xv", "dat", "mpe", "mod", "mpeg"};

    public static final String[] SUPPORT_APK_FORMAT = new String[]{"apk"};

    public static final String[] SUPPORT_FILE_FORMAT;

    static {
        SUPPORT_FILE_FORMAT = new String[SUPPORT_VIDEO_FORMAT.length + SUPPORT_APK_FORMAT.length];
        int i = 0;
        for (String format : SUPPORT_APK_FORMAT) {
            SUPPORT_FILE_FORMAT[i++] = format;
        }
        for (String format : SUPPORT_VIDEO_FORMAT) {
            SUPPORT_FILE_FORMAT[i++] = format;
        }

        BOX_NO_ZOOM.add("mibox");
        BOX_NO_ZOOM.add("mibox1s");
    }

    public static final String REST_URL = "http://huawei.subtitle.kankan.xunlei.com:8000/";
    public static final String APP_SECRET = "";

    public static String TD_SERVER_URL = "http://127.0.0.1:9000/";
    public static String ROUTER_TD_SERVER_URl = "http://192.168.111.1:9000/";

    public static String TD_USR_HELP_URL = "http://api.tv.n0808.com/playHelper?pageName=help";

    public static final String TAG = "ThunderTVPlayer";

    public static final String REMOTE_TAG = "ThunderRemote";

    public static boolean needZoom() {
        return !BOX_NO_ZOOM.contains(Build.MODEL.toLowerCase());
    }

    public static boolean isMibox1s() {
        return Build.MODEL.toLowerCase().equals("mibox1s");
    }

    public static boolean isKiui() {
        return Build.MODEL.toLowerCase().startsWith("kiui");
    }

    public static boolean isI71s(){
        return Build.MODEL.toLowerCase().startsWith("i71");
    }

    public static boolean isTCL(){
        return Build.MODEL.toLowerCase().startsWith("k200");
    }

    public static void LOGD(String message) {
        if (DEBUG) {
            Log.d(TAG, message);
        }
    }

    public static void logRemote(String message){
        if (DEBUG && DEBUG_REMOTE) {
            Log.d(REMOTE_TAG, message);
        }
    }

}
