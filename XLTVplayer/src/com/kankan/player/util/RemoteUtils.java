package com.kankan.player.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

/**
 * Created by wangyong on 14-6-23.
 */
public class RemoteUtils {

    public static int getCurrentVersionCode(Context context){
        PackageManager packageManager = context.getPackageManager();
        try {
            PackageInfo packageInfos = packageManager.getPackageInfo(context.getPackageName(),0);
            if(packageInfos != null){

                int versionCode = packageInfos.versionCode;
                return versionCode;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return 0;
    }



}
