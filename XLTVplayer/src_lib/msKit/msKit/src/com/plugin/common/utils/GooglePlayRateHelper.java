package com.plugin.common.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: michael
 * Date: 13-8-8
 * Time: PM7:15
 * To change this template use File | Settings | File Templates.
 */
public class GooglePlayRateHelper {

    private String RATE_DUBBLER_URI = "market://details?id=";
    private String RATE_DUBBLER_BROWSER_URI = "http://play.google.com/store/apps/details?id=";

    private Context mContext;
    private String mPackageName;

    public GooglePlayRateHelper(Context context, String packageName) {
        mContext = context;
        mPackageName = packageName;

        if (TextUtils.isEmpty(mPackageName)) {
            throw new IllegalArgumentException("package name can't be empty!");
        }

        RATE_DUBBLER_URI = RATE_DUBBLER_URI + mPackageName;
        RATE_DUBBLER_BROWSER_URI = RATE_DUBBLER_BROWSER_URI + mPackageName;
    }

    /**
     * go to rate package
     */
    public void rateAppOnGooglePlay() {

        if (mContext == null) {
            return;
        }

        // 如果安装了google play则跳转到google play上此应用的地址
        if (isAvilible(mContext, "com.android.vending")) {
            // 跳转google play
            try {
                Uri downloadUri = Uri.parse(RATE_DUBBLER_URI);
                Intent it = new Intent(Intent.ACTION_VIEW, downloadUri);
                it.setClassName("com.android.vending", "com.google.android.finsky.activities.LaunchUrlHandlerActivity");
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(it);
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
            // 失败则跳转google Market
            try {
                Uri downloadUri = Uri.parse(RATE_DUBBLER_URI);
                Intent it = new Intent(Intent.ACTION_VIEW, downloadUri);
                it.setClassName("com.android.vending", "com.android.vending.SearchAssetListActivity");
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(it);
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
            // 仍失败则跳转浏览器
            try {
                Uri downloadUri = Uri.parse(RATE_DUBBLER_BROWSER_URI);
                Intent it = new Intent(Intent.ACTION_VIEW, downloadUri);
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                PackageManager packageManager = mContext.getPackageManager();
                List<ResolveInfo> activities = packageManager.queryIntentActivities(it, 0);
                boolean isIntentSafe = activities.size() > 0;

                // Start an activity if it's safe
                if (isIntentSafe) {
                    mContext.startActivity(it);
                    DebugLog.d("AboutActivity  ", "Browser is available!");
                } else {
                    DebugLog.d("AboutActivity  ", "There is no browser!");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            // 没安装google play时，跳google play浏览器
            DebugLog.d(" has installed google play----------------->", "false");
            Uri downloadUri = Uri.parse(RATE_DUBBLER_BROWSER_URI);
            Intent it = new Intent(Intent.ACTION_VIEW, downloadUri);
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PackageManager packageManager = mContext.getPackageManager();
            List<ResolveInfo> activities = packageManager.queryIntentActivities(it, 0);
            boolean isIntentSafe = activities.size() > 0;

            // Start an activity if it's safe
            if (isIntentSafe) {
                mContext.startActivity(it);
                DebugLog.d("AboutActivity  ", "Browser is available!");
            } else {
                DebugLog.d("AboutActivity  ", "There is no browser!");
            }
        }
    }

    /**
     * 判断是否安装某程序
     */
    private boolean isAvilible(Context context, String packageName) {
        final PackageManager packageManager = context.getPackageManager();// 获取packagemanager
        List<PackageInfo> pinfo = packageManager.getInstalledPackages(0);// 获取所有已安装程序的包信息
        List<String> pName = new ArrayList<String>();// 用于存储所有已安装程序的包名
        // 从pinfo中将包名字逐一取出，压入pName list中
        if (pinfo != null) {
            for (int i = 0; i < pinfo.size(); i++) {
                String pn = pinfo.get(i).packageName;
                pName.add(pn);
            }
        }
        return pName.contains(packageName);// 判断pName中是否有目标程序的包名，有TRUE，没有FALSE
    }

}
