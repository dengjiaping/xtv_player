package com.kankan.player.manager;

import android.content.Context;
import android.os.PowerManager;

/**
 * Created by wangyong on 14-6-26.
 */
public class DevicePowerManager {

    private static DevicePowerManager mInstance = new DevicePowerManager();

    private static PowerManager.WakeLock mWakeLock;

    public static DevicePowerManager getInstance(){

        return mInstance;

    }

    public static void aquireWakeLock(Context context){
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if(mWakeLock == null){
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, context.getClass().getCanonicalName());
            mWakeLock.acquire();
        }
    }

    public static void realeaseWakeLock(){
        if(mWakeLock != null){
            mWakeLock.release();
            mWakeLock = null;
        }
    }

}
