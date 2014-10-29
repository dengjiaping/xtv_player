/**
 * UtilsRuntime.java
 */
package com.plugin.common.utils;

import java.text.SimpleDateFormat;
import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Parcelable;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

/**
 * @author Guoqing Sun Feb 18, 20133:16:14 PM
 */
public final class UtilsRuntime {

    private static final String DEBUG_DATE_FORMAT = "MM-dd HH:mm:ss:SSS";

    public static String debugFormatTime(long time) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DEBUG_DATE_FORMAT);
        return dateFormat.format(time);
    }

    public static void createShortCut(Activity act, int iconResId, int appnameResId) {
        Intent shortcutintent = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
        // 不允许重复创建
        shortcutintent.putExtra("duplicate", false);
        // 需要现实的名称
        shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_NAME, act.getString(appnameResId));
        // 快捷图片
        Parcelable icon = Intent.ShortcutIconResource.fromContext(act.getApplicationContext(), iconResId);
        shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);
        // 点击快捷图片，运行的程序主入口
        shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, new Intent(act.getApplicationContext(), act.getClass()));
        // 发送广播
        act.sendBroadcast(shortcutintent);
    }

    //判断小时是否为24小时制
    public static boolean isHourto24(Context context) {
        ContentResolver cr = context.getContentResolver();
        String strFormatTime = android.provider.Settings.System.getString(cr, android.provider.Settings.System.TIME_12_24);
        if (null != strFormatTime && strFormatTime.equals("24")) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = (cm != null) ? cm.getActiveNetworkInfo() : null;
        if (info != null && info.isAvailable() && info.isConnected()) {
            return true;
        }

        return false;
    }

    public static String getCurrentStackMethodName() {
        String method = "";
        StackTraceElement ste = Thread.currentThread().getStackTrace()[4];
        String invokeMethodName = ste.getMethodName();
        String fileName = ste.getFileName();
        long line = ste.getLineNumber();
        if (!TextUtils.isEmpty(invokeMethodName)) {
            method = fileName + "::" + invokeMethodName + "::" + line;
        }

        return method;
    }

    public static String getPackageName(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return info.packageName; // 包名
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    public static String getIMSI(Context context) {
        TelephonyManager mTelephonyMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String imsi = mTelephonyMgr.getSubscriberId();

        return imsi;
    }

    public static String getIMEI(Context context) {
        TelephonyManager mTelephonyMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String imei = mTelephonyMgr.getDeviceId();
        return imei;
    }

    public static boolean isSDCardReady() {
        return android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
    }

    public static String getLocalMacAddress(Context context) {
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifi.getConnectionInfo();
        if (info != null) {
            return info.getMacAddress();
        }

        return null;
    }

    public static String getVersionName(Context context) {
        try {
            // 获取packagemanager的实例
            PackageManager packageManager = context.getPackageManager();
            // getPackageName()是你当前类的包名，0代表是获取版本信息
            PackageInfo packInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            String version = packInfo.versionName;
            return version;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "0.0";
    }

    public static int getVersionCode(Context context) {
        int versionCode = 0;
        // String versionName=null;
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            versionCode = info.versionCode;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return versionCode;
    }

    public static String getCurProcessName(Context context) {
        int pid = android.os.Process.myPid();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return null;
        }
        for (RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess == null) {
                continue;
            }
            if (appProcess.pid == pid) {
                return appProcess.processName;
            }
        }
        return null;
    }

    /**
     * 获得本机电话号码
     *
     * @param context
     * @return
     */
    public static String getCurrentPhoneNumber(Context context) {
        TelephonyManager mTelephonyMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return mTelephonyMgr.getLine1Number();
    }

}
