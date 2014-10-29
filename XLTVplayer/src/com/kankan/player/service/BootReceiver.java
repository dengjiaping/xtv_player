package com.kankan.player.service;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.kankan.player.app.AppConfig;
import com.kankan.player.dao.model.TDVideo;
import com.kankan.player.item.DeviceItem;
import com.kankan.player.manager.UsbManager;
import com.kankan.player.util.AlarmUtils;
import com.kankan.player.util.DeviceModelUtil;
import com.kankan.player.util.SettingManager;
import com.plugin.common.utils.UtilsConfig;
import com.umeng.analytics.MobclickAgent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by wangyong on 14-4-17.
 */
public class BootReceiver extends BroadcastReceiver {


    public static final long FETCH_BIND_STATUS = 1000;
    public static final long FETCH_TIME_DELAY_BIND = ((long) 5) * 1000;
    public static final long FETCH_TIME_DELAY_UNDIND = ((long) 1) * 1000;

    public static  long FETCH_DELAY_TIME = FETCH_TIME_DELAY_UNDIND;

    public static final String ACTION_ALARM_TIME = "com.xunlei.tv.alarm.time";

    public static final String ACTION_FETCH_STATUS_TIME = "com.xunlei.tv.fetch.status";

    public static final String ACTION_FETCH_NOW = "com.xunlei.tv.fetch.now";

    public boolean isAppOnForeground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasksInfo = activityManager.getRunningTasks(1);
        if (tasksInfo.size() > 0) {
            if (context.getPackageName().equals(tasksInfo.get(0).topActivity
                    .getPackageName())) {
                return true;
            }
        }
        return false;
    }


    @Override
    public void onReceive(Context context, Intent intent) {

        AppConfig.logRemote("********** receive broadcast   " + intent.getAction());

        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {

            AppConfig.logRemote("receive boot completed broadcast   " + intent.getAction());

            TdScanService.startTdScanServiceInstallFiles(context);

            MobclickAgent.onEvent(context, "device_active");

        }

        if (intent.getAction().equals(TdScanService.ACTION_INSTALL_SUCESS)) {

            TdScanService.startTdScanServiceQueryServer(context);
        }

        if (intent.getAction().equals(ACTION_ALARM_TIME)) {
            AlarmUtils.cancelAlarmForAction(context, ACTION_FETCH_STATUS_TIME);

            long lastFetchTime = SettingManager.getInstance().getLastFetchMessageTime();
            long curTime = System.currentTimeMillis();

            //应付创维时间不对问题
            if (lastFetchTime > curTime) {
                lastFetchTime = curTime - FETCH_DELAY_TIME - 10;
            }

            if ((curTime - lastFetchTime) <= (FETCH_DELAY_TIME - 2)) {
                AlarmUtils.startAlarmForAction(context, ACTION_ALARM_TIME, FETCH_DELAY_TIME);
                return;
            }

            AppConfig.logRemote("********** receive broadcast  startquery " + intent.getAction());
            AlarmUtils.startAlarmForAction(context, ACTION_ALARM_TIME, FETCH_DELAY_TIME);
            TdScanService.startTdScanServiceQueryServer(context);

            // 当退出后台的时候杀进程释放内存
            if(AppConfig.isI71s()){
                boolean isForeground = isAppOnForeground(context);
                boolean hasKilled = SettingManager.getInstance().isProcessKilled();
                if (!isForeground) {
                    if (!hasKilled) {
                        SettingManager.getInstance().setProcessKilled(true);
                        int pid = android.os.Process.myPid();
                        android.os.Process.killProcess(pid);
                    }
                } else {
                    SettingManager.getInstance().setProcessKilled(false);
                }
            }


            return;
        }

        if (intent.getAction().equals(ACTION_FETCH_STATUS_TIME)) {
            AlarmUtils.cancelAlarmForAction(context, ACTION_ALARM_TIME);

            long lastFetchTime = SettingManager.getInstance().getLastFetchMessageTime();
            long curTime = System.currentTimeMillis();

            //应付创维时间不对问题
            if (lastFetchTime > curTime) {
                lastFetchTime = curTime - FETCH_DELAY_TIME - 10;
            }

            if ((curTime - lastFetchTime) <= (FETCH_DELAY_TIME - 2)) {

                AlarmUtils.startAlarmForAction(context, ACTION_ALARM_TIME, FETCH_BIND_STATUS);
                return;
            }

            AlarmUtils.startAlarmForAction(context, ACTION_ALARM_TIME, FETCH_BIND_STATUS);
            TdScanService.startTdScanServiceQueryServer(context);
            return;
        }

        if (intent.getAction().equals(ACTION_FETCH_NOW)) {
            AlarmUtils.cancelAlarmForAction(context, ACTION_ALARM_TIME);
            AlarmUtils.cancelAlarmForAction(context, ACTION_FETCH_STATUS_TIME);

            AlarmUtils.startAlarmForAction(context, ACTION_ALARM_TIME, FETCH_DELAY_TIME);
            TdScanService.startTdScanServiceQueryServer(context);
            return;
        }


    }

    public static void setFetchTimeDelayBind(){
        if(FETCH_DELAY_TIME != FETCH_TIME_DELAY_BIND){
            FETCH_DELAY_TIME = FETCH_TIME_DELAY_BIND;
        }
    }

    public static void setFetchTimeDelayUndind(){
        if(FETCH_DELAY_TIME != FETCH_TIME_DELAY_UNDIND){
            FETCH_DELAY_TIME = FETCH_TIME_DELAY_UNDIND;
        }
    }


}
