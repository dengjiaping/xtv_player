package com.kankan.player.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;


public class AlarmUtils {

    public static void startAlarmForAction(Context context, String action, long delay) {
        cancelAlarmForAction(context, action);
        Intent intent = new Intent();
        intent.setAction(action);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.RTC, System.currentTimeMillis() + delay, sender);
    }

    public static void cancelAlarmForAction(Context context, String action) {
        Intent intent = new Intent();
        intent.setAction(action);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(sender);
    }

    public static void startAlarmForActionNoRepeat(Context context, String action, long delay, Class targetClass) {
        cancelAlarmForActionNoRepeat(context, action, targetClass);
        Intent intent = new Intent();
        intent.setAction(action);
        intent.setClass(context, targetClass);
        PendingIntent sender = PendingIntent.getService(context, 0, intent, 0);
        long cur = System.currentTimeMillis();
        long firstTime = cur + delay;

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.RTC, firstTime, sender);
    }

    public static void cancelAlarmForActionNoRepeat(Context context, String action, Class targetClass) {
        Intent intent = new Intent();
        intent.setAction(action);
        intent.setClass(context, targetClass);
        PendingIntent sender = PendingIntent.getService(context, 0, intent, 0);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(sender);
    }

}
