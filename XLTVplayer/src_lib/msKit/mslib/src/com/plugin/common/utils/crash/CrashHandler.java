package com.plugin.common.utils.crash;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import com.plugin.common.utils.CustomThreadPool;
import com.plugin.common.utils.CustomThreadPool.TaskWrapper;
import com.plugin.common.utils.DebugLog;
import com.plugin.common.utils.UtilsRuntime;
import com.plugin.common.utils.files.DiskManager;
import com.plugin.common.utils.files.DiskManager.DiskCacheType;
import com.plugin.common.utils.files.FileInfo;

import java.io.*;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public final class CrashHandler implements UncaughtExceptionHandler {

    public interface CrashHandlerListener {

        void beforeExitProcess();

        HashMap<String, String> onCollectAppInfos();

        void onAsyncUploadCrashLog(LinkedList<FileInfo> logs);
        
        void onCrashLogReady(String crashString);
    }

    public static final int PROPERTY_DEBUG_MODE = 1;

    private static final String TAG = "CrashHandler";
    private String CRASH_DIR;

    private UncaughtExceptionHandler mDefaultHandler;
    private static CrashHandler INSTANCE = new CrashHandler();
    private Context mContext;
    private Map<String, String> infos = new HashMap<String, String>();

    private DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    private CrashHandlerListener mCrashHandlerListener;
    private boolean mIsDebugMode;

    private CrashHandler() {
        CRASH_DIR = DiskManager.tryToFetchCachePathByTypeBinding(DiskCacheType.CRASH_LOG);
    }

    public static CrashHandler getInstance() {
        return INSTANCE;
    }

    public void init(Context context) {
        mContext = context;
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    public void setProperty(int property, boolean open) {
        switch (property) {
        case PROPERTY_DEBUG_MODE:
            if (open) {
                mIsDebugMode = true;
                CRASH_DIR = DiskManager.tryToFetchCachePathByTypeBinding(DiskCacheType.CRASH_DEBUG_LOG);
            } else {
                mIsDebugMode = false;
                CRASH_DIR = DiskManager.tryToFetchCachePathByTypeBinding(DiskCacheType.CRASH_LOG);
            }
            break;
        }
    }

    public void asyncTryToOperateCrashLogs() {
        CustomThreadPool.getInstance().excute(new TaskWrapper(new Runnable() {
            public void run() {
                LinkedList<FileInfo> files = DiskManager.collectCrashLogs();
                if (files != null && files.size() > 0 && mCrashHandlerListener != null) {
                    mCrashHandlerListener.onAsyncUploadCrashLog(files);
                }
            }
        }));
    }

    public void setCrashHandlerListener(CrashHandlerListener l) {
        mCrashHandlerListener = l;
    }

    public void uncaughtException(Thread thread, Throwable ex) {
        if (!handleException(ex) && mDefaultHandler != null) {
            mDefaultHandler.uncaughtException(thread, ex);
        } else {
            if (mCrashHandlerListener != null) {
                mCrashHandlerListener.beforeExitProcess();
            }

            DebugLog.d(TAG, "[[uncaughtException]] ", ex);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Log.e(TAG, "error : ", e);
            }

//            if (!UtilsConfig.UTILS_DEBUG) {
                mDefaultHandler.uncaughtException(thread, ex);
//            }
        }
    }

    private boolean handleException(Throwable ex) {
        if (ex == null) {
            return false;
        }
        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                Toast.makeText(mContext, "Sorry, application exception, quit now.", Toast.LENGTH_LONG).show();
                Looper.loop();
            }
        }.start();
        collectDeviceInfo(mContext);
        saveCrashInfo2File(ex);
        return true;
    }

    public void collectDeviceInfo(Context ctx) {
        try {
            PackageManager pm = ctx.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(), PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                String versionName = pi.versionName == null ? "null" : pi.versionName;
                String versionCode = pi.versionCode + "";
                infos.put("versionName", versionName);
                infos.put("versionCode", versionCode);
            }
        } catch (NameNotFoundException e) {
            Log.e(TAG, "an error occured when collect package info", e);
        }
        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                infos.put(field.getName(), field.get(null).toString());
            } catch (Exception e) {
                Log.e(TAG, "an error occured when collect crash info", e);
            }
        }
        try {
            if (mCrashHandlerListener != null) {
                HashMap<String, String> addInfos = mCrashHandlerListener.onCollectAppInfos();
                if (addInfos != null) {
                    for (String key : addInfos.keySet()) {
                        infos.put(key, addInfos.get(key));
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "an error occured when collect crash info", e);
        }
    }

    private String saveCrashInfo2File(Throwable ex) {
        StringBuffer sb = new StringBuffer(512);
        for (Map.Entry<String, String> entry : infos.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(key + "=" + value + "\n");
        }

        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        String result = writer.toString();
        sb.append(result);
        try {
            String v = "v" + UtilsRuntime.getVersionName(mContext) + "-android";
            String time = formatter.format(new Date());
            String fileName = "crash-" + time + "-" + v + ".log";
            if (UtilsRuntime.isSDCardReady()) {
                File dir = new File(CRASH_DIR);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                FileOutputStream fos = new FileOutputStream(CRASH_DIR + fileName);
                fos.write(sb.toString().getBytes());
                fos.close();
            }

            if (mIsDebugMode) {
                Log.d(TAG, sb.toString());
            }
            
            if (mCrashHandlerListener != null) {
                mCrashHandlerListener.onCrashLogReady(sb.toString());
            }

            return fileName;
        } catch (Exception e) {
            Log.e(TAG, "an error occured while writing file...", e);
        }
        return null;
    }

}
