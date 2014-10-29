package com.plugin.common.utils;

import java.text.SimpleDateFormat;

import android.text.TextUtils;
import android.util.Log;

public class LogUtil {
	
	private static String TAG = "DebugLog";
	
    private static final String DEBUG_DATE_FORMAT = "MM-dd HH:mm:ss:SSS";
	
    public static final boolean UTILS_DEBUG = true;
	public static final boolean LOGD = true;

    public static void LOGD(String msg, boolean withExtraInfo) {
        if (LogUtil.UTILS_DEBUG) {
            String method = "";
            if (withExtraInfo) {
                method = getCurrentStackMethodName();
            }
            d(method, msg);
        }
    }

    public static void LOGD(String msg, Throwable t) {
        if (LogUtil.UTILS_DEBUG) {
            StackTraceElement ste = Thread.currentThread().getStackTrace()[4];
            String invokeMethodName = ste.getMethodName();
            String fileName = ste.getFileName();
            long line = ste.getLineNumber();
            String method = "com.plugin.common.utils.Config";
            if (!TextUtils.isEmpty(invokeMethodName)) {
                method = fileName + "::" + invokeMethodName + "::" + line;
            }
            d(method, msg, t);
        }
    }

    public static void LOGD(String msg) {
        LOGD(msg, true);
    }

    public static void LOGD_WITH_TIME(String msg) {
        LOGD(msg + " >>>>>> TIME : " + debugFormatTime(System.currentTimeMillis()));
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
    
    public static String debugFormatTime(long time) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DEBUG_DATE_FORMAT);
        return dateFormat.format(time);
    }
    
	public static void d(String tag, String msg) {
		d(tag, msg, null);
	}
	
	public static void d(String tag, String msg, Throwable tr) {
		if (!LOGD) {
			return;
		}


		Log.d(TAG, "[[" + tag + "]]" + msg, tr);
		
	}
}
