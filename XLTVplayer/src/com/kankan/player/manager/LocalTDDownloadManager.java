package com.kankan.player.manager;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.util.Log;
import com.kankan.player.api.tddownload.*;
import com.kankan.player.app.AppConfig;
import com.kankan.player.app.Constants;
import com.kankan.player.event.SublistEvent;
import com.kankan.player.event.UnbindResultEvent;
import com.kankan.player.explorer.FileCategory;
import com.kankan.player.explorer.FileItem;
import com.kankan.player.explorer.FileUtils;
import com.kankan.player.item.DeviceItem;
import com.kankan.player.service.BootReceiver;
import com.kankan.player.util.CidUtil;
import com.kankan.player.util.DeviceModelUtil;
import com.plugin.common.utils.CustomThreadPool;
import de.greenrobot.event.EventBus;
import org.apache.http.conn.ConnectTimeoutException;
import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhangdi on 14-4-2.
 */
public class LocalTDDownloadManager extends TDDownloadMgr{

    private static final String TAG = LocalTDDownloadManager.class.getSimpleName();

    private static LocalTDDownloadManager instance = new LocalTDDownloadManager();

    private boolean mSupportTD = false;

    private int isNetOK;

    private int isDiscOk;

    private int isEverbinded;

    private SysInfo mSysInfo;

    private boolean isTimeOut;

    private List<FileItem> mfileitems;

    private int mDownloadingFilesNum;
    private int mDownloadedFilesNum;

    private int mVersionCode;

    public static LocalTDDownloadManager getInstance() {
        return instance;
    }

    @Override
    public void init(Context context) {
        try {

            if(!DeviceModelUtil.isSupportBox()){
                return;
            }

            GetSysInfoAPI sysInfoAPI = new GetSysInfoAPI(AppConfig.TD_SERVER_URL);
            mSysInfo = sysInfoAPI.request();
            mSupportTD = true;
            if (mSupportTD) {
                BootReceiver.setFetchTimeDelayBind();
                AppConfig.logRemote("skyworth********** remote service sucess" + System.currentTimeMillis());
            }else{
                BootReceiver.setFetchTimeDelayUndind();
            }

            if (mSysInfo != null) {
                isNetOK = mSysInfo.isNetOk;
                isDiscOk = mSysInfo.isDiskOk;
                isEverbinded = mSysInfo.isEverBinded;
                getVersion();

                if (AppConfig.DEBUG) {

                    List<DeviceItem> mounts = UsbManager.getUsbDeviceList();
                    AppConfig.logRemote("remote server disk status is: " + isDiscOk);
                    if (mounts != null && mounts.size() > 0) {
                        for (int i = 0; i < mounts.size(); i++) {
                            DeviceItem item = mounts.get(i);
                            AppConfig.logRemote("myapp get disk status mounts " + i + "  " + item.getPath());
                        }
                    } else {
                        AppConfig.logRemote("my app get mounts disk status null");
                    }
                }

            }
            isTimeOut = false;

        } catch (ConnectTimeoutException e) {
//            e.printStackTrace();
            isTimeOut = true;
            AppConfig.logRemote("Remote service timeout------------");
        } catch (IOException e) {
            e.printStackTrace();
            mSupportTD = false;
            AppConfig.logRemote("skyworth********** remote service failed" + System.currentTimeMillis());
        }
    }

    @Override
    public SysInfo getSysInfo() {
        return mSysInfo;
    }

    /**
     * 是否支持迅雷远程下载
     */
    @Override
    public boolean isSupportTD() {
        return mSupportTD;
    }


    @Override
    public List<FileItem> getTDDownloadList(int complete) {
        List<FileItem> fileItems = new ArrayList<FileItem>();

        int pageIndex = 0;
        int pageCapacity = 100;

        List<FileItem> list = getTasklist(complete, pageIndex, pageCapacity);
        if (list != null && list.size() > 0) {
            fileItems.addAll(list);
        }
        while (list != null && list.size() >= 100) {
            pageIndex++;
            list = getTasklist(complete, pageIndex, pageCapacity);
            if(list !=null && list.size() > 0){
                fileItems.addAll(list);
            }
        }

        if(complete == GetTaskListAPI.TYPE_TASK_COMPLETED){
            AppConfig.logRemote("tddownloadedreturn tasklist " + fileItems.size());
            // 取本地文件cid
            for(FileItem item: fileItems){
                if(!TextUtils.isEmpty(item.filePath)){
                    String cid = CidUtil.queryCid(item.filePath);
                    if(!TextUtils.isEmpty(cid)){
                        item.cid = cid;
                    }else{
                        item.cid ="";
                    }
                }
            }
        }else{
//            for(int i = fileItems.size()-1; i>=0;i--){
//                FileItem item = fileItems.get(i);
//                if(item.fileStatus != Constants.TDTASK_STATUS_DOWNLOADING){
//                    fileItems.remove(item);
//                }
//            }
            //list == null 认为超时,超时返回null,便于判断超时了
            if(list == null && fileItems.size() == 0){
                return null;
            }

            AppConfig.logRemote("tddownloading return tasklist " + fileItems.size());
        }
        return fileItems;
    }


    private List<FileItem> getTasklist(int complete, int pageindex, int pageCapacity) {
        List<FileItem> fileItems = new ArrayList<FileItem>();

        GetTaskListAPI taskListAPI = new GetTaskListAPI(AppConfig.TD_SERVER_URL, complete, pageindex, pageCapacity, mVersionCode,true);
        TaskList taskList = null;
        try {
            taskList = taskListAPI.request();
            if (taskList != null) {
                if(taskList.tasks != null){
                    for (TaskList.Task task : taskList.tasks) {
                        String path = task.filePath;
                        if (!TextUtils.isEmpty(path) && FileUtils.isNormalFile(path) && FileUtils.shouldShowFile(path)) {
                            FileItem fileItem = FileUtils.getFileItem(new File(path));
                            fileItem.fileStatus = task.stat;
                            fileItems.add(fileItem);
                        }
                    }
                }

                return fileItems;

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

    public void unbindThunderAccount(Context context) {

        if (!isNetworkConnect(context)) {
            UnbindResultEvent event = new UnbindResultEvent();
            event.result = Constants.KEY_REMOTE_NETWORK_ERR;
            EventBus.getDefault().post(event);
            return;
        }

        CustomThreadPool.getInstance().excute(new CustomThreadPool.TaskWrapper(new Runnable() {
            @Override
            public void run() {
                UnbindAPI unbindAPI = new UnbindAPI(AppConfig.TD_SERVER_URL);
                Integer result = null;
                try {
                    result = unbindAPI.request();
                } catch (IOException e) {
                }
                if (result != null) {
                    UnbindResultEvent event = new UnbindResultEvent();
                    event.result = result;
                    EventBus.getDefault().post(event);
                }
            }
        }));

    }

    private void LOGD(String msg) {
        if (AppConfig.DEBUG && !TextUtils.isEmpty(msg)) {
            Log.d(TAG, msg);
        }
    }

    @Override
    public void setDownloadedFileItems(List<FileItem> list, Context context) {
        mfileitems = list;
        if(mfileitems != null){
            mDownloadedFilesNum = mfileitems.size();
        }
    }

    @Override
    public void setDownloadingFilesNum(int num) {
        mDownloadingFilesNum = num;
    }

    @Override
    public int getDownloadingFilesNum() {
        return mDownloadingFilesNum;
    }


    @Override
    public List<FileItem> getFileItems() {
        return mfileitems;
    }

    public static List<FileItem> filterItems(List<FileItem> list, Context context) {

        List<FileItem> result = new ArrayList<FileItem>();

        FileExploreHistoryManager fileExploreHistoryManager = new FileExploreHistoryManager(context);

        DeviceItem deviceItem = new DeviceItem();
        deviceItem.setType(DeviceItem.DeviceType.TD_DOWNLOAD);

        if (list != null) {

            for (FileItem fileItem : list) {

                File file = new File(fileItem.filePath);

                if (file != null && file.exists() && file.canRead()) {

                    AppConfig.logRemote("int get list file item : "+ fileItem.toString());

                    fileItem.isNew = fileExploreHistoryManager.isFileNew(fileItem, deviceItem);

                    AppConfig.logRemote("is new file: ?"+ fileItem.isNew);

                    if (fileItem.category == FileCategory.VIDEO) {
                        result.add(fileItem);
                    }

                    if (fileItem.category == FileCategory.DIR) {
                        File[] childrens = file.listFiles();
                        if (childrens != null && childrens.length > 0) {

                            for (int i = 0; i < childrens.length; i++) {
                                FileItem childItem = FileUtils.getFileItem(childrens[i]);
                                if (childItem.category == FileCategory.VIDEO || childItem.category == FileCategory.DIR) {
                                    result.add(fileItem);
                                    break;
                                }
                            }

                        }
                    }
                }else{
                    AppConfig.logRemote("file path is: "+ file.getPath());
                    AppConfig.logRemote("file exist is: "+ file.exists() + "   file.canreadle: "+ file.canRead());
                }
            }

        }

        AppConfig.logRemote("tddownload local after filter : " + result.size());

        return result;
    }

    @Override
    public int getTDownloadNewFilesNum() {
        if (mfileitems != null) {
            int i = 0;

            for (FileItem item : mfileitems) {

                if (item.isNew) {
                    i++;
                }
            }

            return i;
        }

        return 0;
    }


    @Override
    public int getTDFilesCounts() {
        if (mfileitems != null) {
            return mfileitems.size();
        }

        return 0;
    }

    public static boolean isNetworkConnect(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();
        if (info != null && info.isAvailable()) {
            //能联网
            return true;
        } else {
            //不能联网
            return false;
        }
    }

    @Override
    public void getSublistFileItems(final FileItem file, final Context context) {

        CustomThreadPool.getInstance().excute(new CustomThreadPool.TaskWrapper(new Runnable() {
            @Override
            public void run() {
                if (file != null) {

                    FileExploreHistoryManager fileExploreHistoryManager = new FileExploreHistoryManager(context);

                    DeviceItem deviceItem = new DeviceItem();
                    deviceItem.setType(DeviceItem.DeviceType.TD_DOWNLOAD);

                    String filepath = file.filePath;

                    List<FileItem> list = new ArrayList<FileItem>();

                    File f = new File(filepath);
                    if (f.exists()) {

                        File[] childrens = f.listFiles();
                        if (childrens != null) {
                            for (File child : childrens) {
                                FileItem fileItem = FileUtils.getFileItem(child);
                                String cid = CidUtil.queryCid(fileItem.filePath);
                                if(!TextUtils.isEmpty(cid)){
                                    fileItem.cid = cid;
                                }else{
                                    fileItem.cid ="";
                                }
                                if (fileItem.category == FileCategory.DIR || fileItem.category == FileCategory.VIDEO) {
                                    fileItem.isNew = fileExploreHistoryManager.isFileNew(fileItem, deviceItem);
                                    list.add(fileItem);
                                }
                            }
                        }
                    }

                    SublistEvent event = new SublistEvent();
                    event.list = list;
                    EventBus.getDefault().post(event);


                }
            }
        }));

    }

    public void setDownloadSpeed(int downloadSpeed, int uploadSpeed) {
        SetSpeedLimitApI setSpeedLimitApI = new SetSpeedLimitApI(AppConfig.TD_SERVER_URL, downloadSpeed, uploadSpeed);

        try {

            AppConfig.logRemote("---------skyworth go to set speed");

            Integer result = setSpeedLimitApI.request();

            if (result == 0) {

                AppConfig.logRemote("---------skyworth set speed sucess");
            }

        } catch (IOException e) {
            AppConfig.logRemote("[[LocalTdDownloadManager]] setDownloadSpeed " + e.getMessage());
        }

    }

    public SpeedInfo getTDSpeed() {
        GetSpeedApI getSpeedApI = new GetSpeedApI();

        try {
            SpeedInfo speedInfo = getSpeedApI.request();
            return speedInfo;
        } catch (IOException e) {
            AppConfig.logRemote("[[LocalTdDownloadManager]] getTDSpeed " + e.getMessage());
        }

        return null;
    }

    private void getVersion(){
        if(mSysInfo != null){
            String version = mSysInfo.version;
            String[] codes = version.split("\\.");
            if(codes != null && codes.length >=4){
                String v = codes[codes.length -1];
                mVersionCode = Integer.parseInt(v);
            }
        }
    }

    public int getDownloadedFilesNum(){
        return mDownloadedFilesNum;
    }

    public boolean getIsTimeOut(){
        return isTimeOut;
    }

}
