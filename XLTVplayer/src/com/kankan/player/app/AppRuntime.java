package com.kankan.player.app;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.TelephonyManager;

public class AppRuntime {
    private static AppRuntime sInstance;

    private String model;
    private String channel;
    private String versionName;
    private int versionCode;
    private String imei;

    private AppRuntime(Context context) {
        model = Build.MODEL;
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            channel = appInfo.metaData.getString("UMENG_CHANNEL", "");

            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            versionName = packageInfo.versionName;
            versionCode = packageInfo.versionCode;
            imei = getIMEI(context);
        } catch (Exception e) {
        }
    }

    public static AppRuntime getInstance(Context context) {
        if (sInstance == null) {
            synchronized (AppRuntime.class) {
                if (sInstance == null) {
                    sInstance = new AppRuntime(context);
                }
            }
        }
        return sInstance;
    }

    private String getIMEI(Context context) {
        TelephonyManager mTelephonyMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return mTelephonyMgr.getDeviceId();
    }

    public String getModel() {
        return model;
    }

    public String getChannel() {
        return channel;
    }

    public String getVersionName() {
        return versionName;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public String getImei() {
        return imei;
    }
}
