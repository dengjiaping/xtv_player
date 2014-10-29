package com.kankan.player.util;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by zhangdi on 14-4-1.
 */
public class DateTimeFormatter {

    private static Calendar calendar1;
    private static Calendar calendar2;

    /**
     * 格式化视频长度 hh:mm:ss
     *
     * @param millis 毫秒数
     * @return
     */
    public static String formatDuration(long millis) {
        int hours = (int) (millis / (60 * 60 * 1000));
        int minutes = (int) ((millis - hours * 60 * 60 * 1000) / (60 * 1000));
        int seconds = (int) ((millis - hours * 60 * 60 * 1000 - minutes * 60 * 1000) / 1000);

        StringBuilder time = new StringBuilder();
        if (hours > 0) {
            time.append(hours + ":");
        }
        if (minutes > 9) {
            time.append(minutes + ":");
        } else {
            time.append("0" + minutes + ":");
        }
        if (seconds > 9) {
            time.append(seconds);
        } else {
            time.append("0" + seconds);
        }
        return time.toString();
    }

    /**
     * 格式化日期
     *
     * @param milliseconds
     * @return
     */
    public static String formatDate(long milliseconds) {
        int days = daysBetween(milliseconds, System.currentTimeMillis());
        if (days == 0) {
            return "今天";
        } else if (days == 1) {
            return "昨天";
        } else {
            // TODO
            if (calendar1 == null) {
                calendar1 = Calendar.getInstance();
            }
            calendar1.setTimeInMillis(milliseconds);
            return String.format("%tb", calendar1) + calendar1.get(Calendar.DAY_OF_MONTH);
        }
    }

    /**
     * 间隔自然天数
     *
     * @param milliseconds1
     * @param milliseconds2
     * @return
     */
    public static int daysBetween(long milliseconds1, long milliseconds2) {
        if (calendar1 == null) {
            calendar1 = Calendar.getInstance();
        }
        calendar1.setTimeInMillis(milliseconds1);
        calendar1.set(Calendar.HOUR_OF_DAY, 0);
        calendar1.set(Calendar.MINUTE, 0);
        calendar1.set(Calendar.SECOND, 0);

        if (calendar2 == null) {
            calendar2 = Calendar.getInstance();
        }
        calendar2.setTimeInMillis(milliseconds2);
        calendar2.set(Calendar.HOUR_OF_DAY, 0);
        calendar2.set(Calendar.MINUTE, 0);
        calendar2.set(Calendar.SECOND, 0);

        // 除以1000是为了忽略毫秒数，防止毫秒对计算结果造成影响
        long deltaMillis = Math.abs(calendar1.getTimeInMillis() / 1000 - calendar2.getTimeInMillis() / 1000);
        return (int) (deltaMillis / (24 * 60 * 60L));
    }

    public static String formateCurrentTime(){
        DateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
        String s = format.format(new Date());
        return s;
    }

    public static void main(String[] args) {
        System.out.println(formatDate(System.currentTimeMillis() - 29 * 3600000l));
    }
}
