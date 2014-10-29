package com.kankan.player.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.widget.TextView;
import com.kankan.player.api.tddownload.GetTaskListAPI;
import com.kankan.player.api.tddownload.SpeedInfo;
import com.kankan.player.api.tddownload.UnMountDiscAPI;
import com.kankan.player.app.AppConfig;
import com.kankan.player.app.Constants;
import com.kankan.player.explorer.FileItem;
import com.kankan.player.item.Device;
import com.kankan.player.manager.DevicePowerManager;
import com.kankan.player.manager.LocalTDDownloadManager;
import com.kankan.player.manager.XLRouterDownloadMgr;
import com.kankan.player.util.*;
import com.plugin.common.utils.CustomThreadPool;
import com.plugin.common.utils.UtilsRuntime;
import com.skyworthdigital.common.UsbUtil;
import com.xunlei.tv.player.R;

import java.io.*;
import java.util.List;

/**
 * Created by wangyong on 14-4-14.
 */
public class TdScanService extends Service {

    private static final int TYPE_LOCAL_REQUERY = 1;
    private static final int TYPE_ROUTER_REQUERY = 2;

    private static final String ACTION_INSTALL_FILES = "com.xunlei.tv.service.action.install";
    private static final String ACTION_START_TDSERVER = "com.xunlei.tv.service.action.start";
    private static final String ACTION_QUERY_TDSERVER = "com.xunlei.tv.service.action.tdquery";
    public static final String ACTION_SEND_QUERY_LOCAL_RESULT = "com.xunlei.tv.local.result";
    public static final String ACTION_SEND_QUERY_ROUTER_RESULT = "com.xunlei.tv.router.result";
    public static final String ACTION_STOP_TDSERVER = "com.xunlei.tv.service.action.stop";
    public static final String ACTION_INSTALL_SUCESS = "com.xunlei.localservice.start";
    public static final String ACTION_UNMOUNT_DISC = "com.xunlei.tv.unmount.disc";

    public static final String EXTRA_MOUNTS = "extra_mounts";

    private LocalTDDownloadManager mTDDownloadMgr;
    private XLRouterDownloadMgr mRouterTdDwonloadMgr;

    private List<FileItem> mDownloadlist;
    private List<FileItem> mRouterDownloadlist;
    private String mPath = null;

    private static final String KEY_PATH_PORTAL = "portal";
    private static final String KEY_PATH_EMBEDTHUNDERMANAGER = "lib/EmbedThunderManager";
    private static final String KEY_PATH_ETMDAEMON = "lib/ETMDaemon";
    private static final String KEY_PATH_VOD_HTTPSERVER = "lib/vod_httpserver";
    private static final String KEY_PATH_VERSION = "version";
    private static final String KEY_PATH_MOUNTS_CONFIG = "/thunder/cfg/thunder_mounts.cfg";
    private static final String KEY_PATH_LOG_CONFIG = "/thunder/cfg/log.conf";

    private static final int MSG_WHAT = 12345;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg == null) {
                return;
            }

            if (msg.what == MSG_WHAT) {
                AppConfig.logRemote("receive in handler start install");
                TdScanService.startTdScanServiceInstallFiles(getApplicationContext());
            }

        }
    };


    public TdScanService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mTDDownloadMgr = LocalTDDownloadManager.getInstance();
        mRouterTdDwonloadMgr = XLRouterDownloadMgr.getInstance();
        mPath = getApplicationContext().getCacheDir().getAbsolutePath() + "/";
        AppConfig.logRemote("skyworth init service path is: " + mPath);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {

            AppConfig.logRemote("get intent  " + intent.getAction());

            if (intent.getAction().equals(ACTION_INSTALL_FILES)) {
                checkVersionAndInstall();
            }

            if (intent.getAction().equals(ACTION_START_TDSERVER)) {
                executeTdServer();
            }

            if (intent.getAction().equals(ACTION_STOP_TDSERVER)) {
                stopTdServer();
            }

            if (intent.getAction().equals(ACTION_QUERY_TDSERVER)) {
                SettingManager.getInstance().setLastFetchMessageTime(System.currentTimeMillis());
                if(DeviceModelUtil.isSupportReleaseService() || DeviceModelUtil.isSupportBox()){
                    queryLocalTdServer();
                }
                if (AppConfig.isRemoteRouterOpen) {
                    queryRouterTdServer();
                }
            }

            if (intent.getAction().equals(ACTION_UNMOUNT_DISC)) {

                List<String> mounts = (List<String>) intent.getSerializableExtra(EXTRA_MOUNTS);

                for (String str : mounts) {
                    unMountDisc(str);
                }

            }
        }
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void startTdScanServiceInstallFiles(Context context) {
        Intent intent = new Intent(context.getApplicationContext(), TdScanService.class);
        intent.setAction(ACTION_INSTALL_FILES);
        context.startService(intent);
    }

    public static void startTdServer(Context context) {
        Intent intent = new Intent(context.getApplicationContext(), TdScanService.class);
        intent.setAction(ACTION_START_TDSERVER);
        context.startService(intent);
    }

    public static void startTdScanServiceQueryServer(Context context) {
        Intent intent = new Intent(context.getApplicationContext(), TdScanService.class);
        intent.setAction(ACTION_QUERY_TDSERVER);
        context.startService(intent);
    }

    public static void startTdScanServiceUnmountDisc(Context context, List<String> mounts) {
        Intent intent = new Intent(context.getApplicationContext(), TdScanService.class);
        intent.setAction(ACTION_UNMOUNT_DISC);
        intent.putExtra(EXTRA_MOUNTS, (Serializable) mounts);
        context.startService(intent);
    }

    public static void stopTdServer(Context context) {
        Intent intent = new Intent(context.getApplicationContext(), TdScanService.class);
        intent.setAction(ACTION_START_TDSERVER);
        context.startService(intent);
    }

    private void installTDFiles() {

        //如果没有网，就直接从头开始，直到有网为止
        if (!LocalTDDownloadManager.isNetworkConnect(getApplicationContext())) {
            AppConfig.logRemote("network failed send message delay");
            mHandler.sendEmptyMessageDelayed(MSG_WHAT, 1000);
            return;
        }

        //新要求：按照服务端返回结果决定是否释放服务
        if (!TextUtils.isEmpty(DeviceModelUtil.getPartnerId())) {
            Device deviceInfo = DeviceModelUtil.getVenderInfo(getApplicationContext(), DeviceModelUtil.getPartnerId());
            //服务端返回为null的话，会导致isSupportReleaseService  ＝ false，不释放服务
            if (deviceInfo != null) {
                DeviceModelUtil.setDeviceInfo(deviceInfo);
            }
        }

        AppConfig.logRemote("go to check issupport release");

        if (!DeviceModelUtil.isSupportReleaseService()) {
            AppConfig.logRemote("check and install not support return");
//            sendInstallSucessBroadcast();
            return;
        }

        String portal = installFiles(mPath + KEY_PATH_PORTAL, R.raw.portal);
        createDirectory(mPath + "lib");
        String embedThunderMgr = installFiles(mPath + KEY_PATH_EMBEDTHUNDERMANAGER, R.raw.embedthundermanager);
        String etmdaemon = installFiles(mPath + KEY_PATH_ETMDAEMON, R.raw.etmdaemon);
        String vodHttpServer = installFiles(mPath + KEY_PATH_VOD_HTTPSERVER, R.raw.vod_httpserver);
        String version = installFiles(mPath + KEY_PATH_VERSION, R.raw.version);

        createDirectory(mPath + "thunder");
        createDirectory(mPath + "thunder/cfg");
        String cfg = installFiles(mPath + KEY_PATH_MOUNTS_CONFIG, R.raw.thunder_mounts);

        String logConf = null;

        if (AppConfig.DEBUG) {
            logConf = installFiles(mPath + KEY_PATH_LOG_CONFIG, R.raw.log);
        }

        AppConfig.logRemote("check and install install");


        if (!TextUtils.isEmpty(portal) && !TextUtils.isEmpty(embedThunderMgr)
                && !TextUtils.isEmpty(etmdaemon) && !TextUtils.isEmpty(cfg)
                && !TextUtils.isEmpty(vodHttpServer) && !TextUtils.isEmpty(version)) {
            AppConfig.logRemote("skyworth********** remote service install sucess" + System.currentTimeMillis());

            setReadWritable(portal);
            setExecutable(portal);
            setReadWritable(embedThunderMgr);
            setExecutable(embedThunderMgr);
            setReadWritable(etmdaemon);
            setExecutable(etmdaemon);
            setExecutable(vodHttpServer);
            setReadWritable(vodHttpServer);
            setReadWritable(cfg);
            setExecutable(cfg);
            setReadWritable(version);

            if (AppConfig.DEBUG) {
                if (!TextUtils.isEmpty(logConf)) {
                    setReadWritable(logConf);
                    setExecutable(logConf);
                }
            }

            executeTdServer();
        }
    }

    private void checkVersionAndInstall() {

        CustomThreadPool.getInstance().excute(new CustomThreadPool.TaskWrapper(new Runnable() {
            @Override
            public void run() {
                String portalPath = mPath + KEY_PATH_PORTAL;
                String embedThunderMgr = mPath + KEY_PATH_EMBEDTHUNDERMANAGER;
                String etmdaemon = mPath + KEY_PATH_ETMDAEMON;

                int currVerionCode = RemoteUtils.getCurrentVersionCode(getApplicationContext());
                int remoteVersionCode = getRemoteVersionCode();

                AppConfig.logRemote("check and install oldverion: " + remoteVersionCode + "   currversion: " + currVerionCode);

                if (currVerionCode > remoteVersionCode) {
                    SettingManager.getInstance().setVersionCode(currVerionCode);

                    File portalFile = new File(portalPath);
                    File mgrFile = new File(embedThunderMgr);
                    File etmdFile = new File(etmdaemon);
                    if (portalFile.exists() && mgrFile.exists() && etmdFile.exists()) {
                        //如果老的远程服务文件还存在那么先停掉远程服务
                        if (AppConfig.DEBUG) {
                            AppConfig.logRemote("------ 版本更新，停止远程服务");
                        }
                        ShellUtils.execCommand("." + mPath + "portal -s", false);
                    }

                    delAllFiles();

                    installTDFiles();

                } else {

                    boolean filesComplete = checkAllFileExists();
                    if (filesComplete) {
                        executeTdServer();
                    } else {

                        delAllFiles();

                        installTDFiles();
                    }

                }
            }
        }));


    }


    private boolean checkAllFileExists() {
        String portalPath = mPath + KEY_PATH_PORTAL;
        String embedThunderMgr = mPath + KEY_PATH_EMBEDTHUNDERMANAGER;
        String etmdaemon = mPath + KEY_PATH_ETMDAEMON;
        String vodHttpServer = mPath + KEY_PATH_VOD_HTTPSERVER;
        String version = mPath + KEY_PATH_VERSION;
        String cfg = mPath + KEY_PATH_MOUNTS_CONFIG;

        File portal = new File(portalPath);
        File mgr = new File(embedThunderMgr);
        File etmd = new File(etmdaemon);
        File vod = new File(vodHttpServer);
        File ver = new File(version);
        File config = new File(cfg);

        boolean complete = portal.exists() && mgr.exists() && etmd.exists()
                && vod.exists() && ver.exists() && config.exists();

        return complete;
    }

    private void delAllFiles() {
        String portalPath = mPath + KEY_PATH_PORTAL;
        String embedThunderMgr = mPath + KEY_PATH_EMBEDTHUNDERMANAGER;
        String etmdaemon = mPath + KEY_PATH_ETMDAEMON;
        String vodHttpServer = mPath + KEY_PATH_VOD_HTTPSERVER;
        String version = mPath + KEY_PATH_VERSION;
        String cfg = mPath + KEY_PATH_MOUNTS_CONFIG;

        delFile(portalPath);
        delFile(embedThunderMgr);
        delFile(etmdaemon);
        delFile(vodHttpServer);
        delFile(version);
        delFile(cfg);

        AppConfig.logRemote("del all files");
    }

    public boolean delFile(String path) {
        if (!TextUtils.isEmpty(path)) {
            File file = new File(path);
            if (file.exists()) {
                //兼容老版本，有的盒子需要即时更改权限才可以删除
                file.setReadable(true, false);
                file.setWritable(true, false);
                return file.delete();
            }
        }
        return false;
    }


    public final void createDirectory(String strDir) {
        File file = new File(strDir);
        if (!file.exists()) {
            if (!file.isDirectory()) {
                file.mkdir();
            }
        }
    }

    private String installFiles(String fullName, int id) {
        Resources resources = getResources();
        InputStream inputStream = resources.openRawResource(id);

        String result = saveFile(fullName, inputStream);

        return result;

    }


    public String saveFile(String targetPath, InputStream is) {
        byte[] buffer = new byte[4096 * 2];
        File f = new File(targetPath);
        if (f.exists()) {
            return targetPath;
        }
        int len;
        OutputStream os = null;

        try {
            os = new FileOutputStream(f);
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            return targetPath;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        } finally {
            if (os != null) {
                try {
                    os.close();
                    is.close();
                } catch (Exception e) {
                }
            }

            buffer = null;
        }
    }

    private int getRemoteVersionCode() {
        String version = mPath + KEY_PATH_VERSION;
        StringBuilder sb = new StringBuilder();
        try {
            File file = new File(version);
            if (file.isFile() && file.exists()) {
                InputStreamReader read = new InputStreamReader(
                        new FileInputStream(file));
                BufferedReader bufferedReader = new BufferedReader(read);
                String lineTxt = null;
                while ((lineTxt = bufferedReader.readLine()) != null) {
                    sb.append(lineTxt);
                }
                read.close();
            } else {
                AppConfig.logRemote("verion file not exist");
            }
        } catch (Exception e) {
            AppConfig.logRemote("[[TdScanService]] getRemoteVersionCode " + e.getMessage());
        }

        String str = sb.toString();
        if (!TextUtils.isEmpty(str)) {
            String[] txt = str.split("=");
            if (txt != null && txt.length > 1) {
                String code = txt[1];
                return Integer.valueOf(code.trim());
            }
        }

        return 0;
    }

    private void setExecutable(String fullName) {
        if (!TextUtils.isEmpty(fullName)) {
            File file = new File(fullName);
            if (file.exists()) {
                file.setExecutable(true, false);
            }
        }
    }

    private void setReadWritable(String fullName) {
        if (!TextUtils.isEmpty(fullName)) {
            File file = new File(fullName);
            if (file.exists()) {
                file.setReadable(true, false);
                file.setWritable(true, false);
            }
        }
    }

    private void executeTdServer() {
//        ShellUtils.CommandResult resultCmd = ShellUtils.execCommand("./data/data/com.xunlei.tv.player/cache/portal -l 13032020010000014000000ck3ej3fk5400s6v3cv8", false);
        ShellUtils.CommandResult resultCmd = null;

        if (TextUtils.isEmpty(DeviceModelUtil.getPort())) {
            AppConfig.logRemote("port is null reget");
            if (!TextUtils.isEmpty(DeviceModelUtil.getPartnerId())) {
                Device deviceInfo = DeviceModelUtil.getVenderInfo(getApplicationContext(), DeviceModelUtil.getPartnerId());
                //服务端返回为null的话，会导致isSupportReleaseService  ＝ false，不释放服务
                if (deviceInfo != null) {
                    DeviceModelUtil.setDeviceInfo(deviceInfo);
                }else{
                    mHandler.sendEmptyMessageDelayed(MSG_WHAT,1000);
                    return;
                }
            }
        }

        if(!DeviceModelUtil.isSupportReleaseService()){
            AppConfig.logRemote("no support in execute return");
            return;
        }

        String decodeLicense = DeviceModelUtil.getLicense();
//  不需要再加密了
//        try {
//            decodeLicense = new DeviceLicense(DeviceLicense.KEY).decrypt(LICENSE);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }


        AppConfig.logRemote("decode license is: " + decodeLicense);

        if (TextUtils.isEmpty(decodeLicense)) {
            return;
        }

        if (AppConfig.DEBUG) {
            if (UtilsRuntime.isExternalStorageAvailable()) {
                AppConfig.logRemote("skyworth********** re portal: " + DeviceModelUtil.getPort());
                if (!TextUtils.isEmpty(DeviceModelUtil.getPartnerId())) {
                    resultCmd = ShellUtils.execCommand("." + mPath + "portal -l " + decodeLicense + " --partnerid " + DeviceModelUtil.getPartnerId() + " --listen_addr 0.0.0.0:" + DeviceModelUtil.getPort() + " --ntfs_type "+ DeviceModelUtil.getNtfsType() +" -r " + Environment.getExternalStorageDirectory().toString() + "/" + DateTimeFormatter.formateCurrentTime() + ".log", false);
                }
            } else {
                AppConfig.logRemote("skyworth******** external not prepare");
            }
        } else {
            if (!TextUtils.isEmpty(DeviceModelUtil.getPartnerId())) {
                resultCmd = ShellUtils.execCommand("." + mPath + "portal -l " + decodeLicense + " --partnerid " + DeviceModelUtil.getPartnerId() + " --listen_addr 0.0.0.0:" + DeviceModelUtil.getPort() + " --ntfs_type "+ DeviceModelUtil.getNtfsType(), false);
            }
        }

        if (resultCmd != null) {
            AppConfig.logRemote("skyworth error result is: " + resultCmd.errorMsg);
            AppConfig.logRemote("skyworth sucess result is: " + resultCmd.successMsg);
            if (resultCmd.result == 0) {
                deliverQueryBroadCast(BootReceiver.ACTION_ALARM_TIME);
//                sendInstallSucessBroadcast();
            }
        }
    }

    private void stopTdServer() {
        if (checkAllFileExists()) {

            AppConfig.logRemote("kill remote process");

            ShellUtils.CommandResult resultCmd = ShellUtils.execCommand("." + mPath + "portal -s ", false);

            if (resultCmd != null) {
                AppConfig.logRemote("skyworth error result is: " + resultCmd.errorMsg);
                AppConfig.logRemote("skyworth sucess result is: " + resultCmd.successMsg);
            }
        }

    }


    private void queryLocalTdServer() {
        CustomThreadPool.getInstance().excute(new CustomThreadPool.TaskWrapper(new Runnable() {
            @Override
            public void run() {
                mTDDownloadMgr.init(getApplicationContext());
                mDownloadlist = mTDDownloadMgr.getTDDownloadList(GetTaskListAPI.TYPE_TASK_COMPLETED);
                if (mDownloadlist != null) {
                    AppConfig.logRemote("tddownload local mdOWNLOADLIST  is: " + mDownloadlist.size());
                }

                List<FileItem> downloadingList = mTDDownloadMgr.getTDDownloadList(GetTaskListAPI.TYPE_TASK_UNCOMPLETED);
                if (downloadingList != null) {
                    mTDDownloadMgr.setDownloadingFilesNum(downloadingList.size());

                    //只有创维才限速
                    if (downloadingList.size() > 0) {
                        if (DeviceModelUtil.getSupportBoxName().equals(DeviceModelUtil.BOX_I71S)) {
                            setSpeedLimitForFat();
                        }
                    }

                }

                //只有TCL才开唤醒
                if (DeviceModelUtil.getSupportBoxName().equals(DeviceModelUtil.BOX_TCLV7)) {
                    if (downloadingList != null && downloadingList.size() > 0) {
                        AppConfig.logRemote("downloading list > 0, TCL LOCKED WAKEUP");
                        DevicePowerManager.getInstance().aquireWakeLock(getApplicationContext());
                    } else {
                        if (downloadingList != null && downloadingList.size() == 0) {
                            AppConfig.logRemote("current download size added num goto realease");
                            DevicePowerManager.getInstance().realeaseWakeLock();
                        }
                    }
                }

                sendQueryResultBroadcast(mDownloadlist, TYPE_LOCAL_REQUERY);
            }
        }));

    }

    private void queryRouterTdServer() {
        CustomThreadPool.getInstance().excute(new CustomThreadPool.TaskWrapper(new Runnable() {
            @Override
            public void run() {
                mRouterTdDwonloadMgr.init(getApplicationContext());
                mRouterDownloadlist = mRouterTdDwonloadMgr.getTDDownloadList(GetTaskListAPI.TYPE_TASK_COMPLETED);
                if (mRouterDownloadlist != null) {
                    AppConfig.logRemote("tddownload mdOWNLOADLIST  is: " + mRouterDownloadlist.size());
                }
                List<FileItem> downloadingList = mRouterTdDwonloadMgr.getTDDownloadList(GetTaskListAPI.TYPE_TASK_UNCOMPLETED);
                if (downloadingList != null) {
                    mRouterTdDwonloadMgr.setDownloadingFilesNum(downloadingList.size());
                }
                sendQueryResultBroadcast(mRouterDownloadlist, TYPE_ROUTER_REQUERY);
            }
        }));

    }


    private void sendInstallSucessBroadcast() {

        Intent intent = new Intent();
        intent.setAction(ACTION_INSTALL_SUCESS);
        sendBroadcast(intent);
    }

    private void sendQueryResultBroadcast(final List<FileItem> list, int type) {

        Intent intent = new Intent();
        if (type == TYPE_LOCAL_REQUERY) {
            intent.setAction(ACTION_SEND_QUERY_LOCAL_RESULT);
            List<FileItem> results = LocalTDDownloadManager.filterItems(list, getApplicationContext());

            if (results != null) {
                AppConfig.logRemote("tddownload local  result is : " + results.size());
            }

            LocalTDDownloadManager.getInstance().setDownloadedFileItems(results, getApplicationContext());
        }

        if (type == TYPE_ROUTER_REQUERY) {
            intent.setAction(ACTION_SEND_QUERY_ROUTER_RESULT);
            List<FileItem> results = mRouterTdDwonloadMgr.filterItems(list, getApplicationContext());

            if (results != null) {
                AppConfig.logRemote("tddownload  router result is : " + results.size());
            }

            XLRouterDownloadMgr.getInstance().setDownloadedFileItems(results, getApplicationContext());
        }

        sendBroadcast(intent);

    }

    private void unMountDisc(final String path) {
        CustomThreadPool.getInstance().excute(new CustomThreadPool.TaskWrapper(new Runnable() {
            @Override
            public void run() {
                UnMountDiscAPI unMountDiscAPI = new UnMountDiscAPI(AppConfig.TD_SERVER_URL, path);
                AppConfig.logRemote("skyworth disc removed i send unmount api");
                Integer result = null;
                try {
                    result = unMountDiscAPI.request();
                    AppConfig.logRemote("skyworth disc removed i send unmount api result is:" + result);
                } catch (IOException e) {
                    AppConfig.logRemote("[[TdScanService]] unMountDisc " + e.getMessage());
                }
            }
        }));
    }

    private boolean isMountFat() {
        UsbUtil usbUtil = new UsbUtil(getApplicationContext());
        List<String> mounts = usbUtil.getUsbTypeList();
        AppConfig.logRemote("---skyworth found mount list is:" + mounts);

        if (mounts != null) {

            for (String str : mounts) {

                AppConfig.logRemote("---skyworth found mount device is:" + str);

                if (str.toLowerCase().contains(Constants.KEY_DISC_FORMAT_FAT)) {
                    return true;
                }
            }

        }

        return false;
    }

    private void setSpeedLimitForFat() {

        CustomThreadPool.getInstance().excute(new CustomThreadPool.TaskWrapper(new Runnable() {
            @Override
            public void run() {
                SpeedInfo speedInfo = LocalTDDownloadManager.getInstance().getTDSpeed();

                if (isMountFat()) {

                    if (speedInfo != null) {

                        AppConfig.logRemote("------skyworth download speed is:" + speedInfo.downloadSpeed);

                        if (speedInfo.downloadSpeed <= 0 || speedInfo.downloadSpeed > 100) {

                            LocalTDDownloadManager.getInstance().setDownloadSpeed(Constants.KEY_DISC_FAT_DOWNLOAD_SPEED, Constants.KEY_DISC_FAT_UPLOAD_SPEED);
                        }

                    }
                } else {

                    if (speedInfo != null) {
                        AppConfig.logRemote("------skyworth download speed is:" + speedInfo.downloadSpeed);

                        if (speedInfo.downloadSpeed != Constants.KEY_DISC_DOWNLOAD_SPEED_UNLIMITED) {
                            LocalTDDownloadManager.getInstance().setDownloadSpeed(Constants.KEY_DISC_DOWNLOAD_SPEED_UNLIMITED, Constants.KEY_DISC_FAT_UPLOAD_SPEED);
                        }
                    }


                }
            }
        }));

    }


    private void deliverQueryBroadCast(String action) {
        Intent intent = new Intent();
        intent.setAction(action);
        sendBroadcast(intent);
    }
}
