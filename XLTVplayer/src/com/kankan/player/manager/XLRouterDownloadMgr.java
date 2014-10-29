package com.kankan.player.manager;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import com.kankan.player.api.tddownload.GetSysInfoAPI;
import com.kankan.player.api.tddownload.GetTaskListAPI;
import com.kankan.player.api.tddownload.SysInfo;
import com.kankan.player.api.tddownload.TaskList;
import com.kankan.player.app.AppConfig;
import com.kankan.player.app.Constants;
import com.kankan.player.event.SublistEvent;
import com.kankan.player.explorer.FileCategory;
import com.kankan.player.explorer.FileCategoryHelper;
import com.kankan.player.explorer.FileItem;
import com.kankan.player.item.DeviceItem;
import com.kankan.player.util.CidUtil;
import com.kankan.player.util.SettingManager;
import com.kankan.player.util.SmbUtil;
import com.plugin.common.utils.CustomThreadPool;
import com.plugin.common.utils.UtilsConfig;
import de.greenrobot.event.EventBus;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import org.apache.http.conn.ConnectTimeoutException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wangyong on 14-5-19.
 */
public class XLRouterDownloadMgr extends TDDownloadMgr{

    private static final String TAG = XLRouterDownloadMgr.class.getSimpleName();

    private static XLRouterDownloadMgr instance = new XLRouterDownloadMgr();

    private boolean mSupportTD = false;

    private SysInfo mSysInfo;

    private List<FileItem> mfileitems;
    private int mDownloadingFilesNum;

    private int mVersionCode;

    private int isNetOK;

    private int isDiscOk;

    private int isEverbinded;

    private String mRouterIp;
    private String mRouterName;

    public static final String SCHEMA_HTTP = "http://";
    public static final int ROUTER_PORT_DEFAULT = 9000;
    public static final int ROUTER_PORT_XIAOMI = 9000;
    public static final int ROUTER_PORT_XUNLEI = 9000;

    public static final String ROUTER_XIAOMI = "小米路由器";
    public static final String ROUTER_XUNLEI = "迅雷路由器";
    public static final Map<String, String> ROUTER_NAMES = new HashMap<String, String>();

    static{
        ROUTER_NAMES.put(ROUTER_XIAOMI,"");
        ROUTER_NAMES.put(ROUTER_XUNLEI,"");
    }


    public static XLRouterDownloadMgr getInstance() {
        return instance;
    }

    @Override
    public void init(Context context) {
        try {

            //todo 每次都请求routerip 是否恰当，待考虑
            mRouterIp = createRouterIp(context);

            if(mRouterIp == null || !isSupportRouter()){
                return;
            }
            GetSysInfoAPI sysInfoAPI = new GetSysInfoAPI(mRouterIp);
            mSysInfo = sysInfoAPI.request();
            mSupportTD = true;
            if (mSupportTD) {
                AppConfig.logRemote("skyworth********** remote service sucess" + System.currentTimeMillis());
            }

            if (mSysInfo != null) {
                isNetOK = mSysInfo.isNetOk;
                isDiscOk = mSysInfo.isDiskOk;
                isEverbinded = mSysInfo.isEverBinded;
                getVersion();
            }
        } catch (ConnectTimeoutException e) {
            AppConfig.logRemote("Remote service timeout------------");
        } catch (IOException e) {
            mSupportTD = false;
            UtilsConfig.LOGD("skyworth********** remote service failed" + System.currentTimeMillis());
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
            if(list != null && list.size() > 0){
                fileItems.addAll(list);
            }
        }

        if(complete == GetTaskListAPI.TYPE_TASK_COMPLETED){
            // 取路由器文件cid
            for(FileItem item : fileItems){
                if(!TextUtils.isEmpty(item.filePath)){
                    String smbPath = SmbUtil.getSmbPathFromPlayUrl(item.filePath);
                    try {
                        SmbFile smbFile = new SmbFile(smbPath);
                        BufferedInputStream bis = new BufferedInputStream(new SmbFileInputStream(smbFile));
                        String cid = CidUtil.queryCid(bis, smbFile.length());
                        if(!TextUtils.isEmpty(cid)){
                            item.cid = cid;
                        }else{
                            item.cid ="";
                        }
                    } catch (SmbException e) {
                    } catch (MalformedURLException e) {
                    } catch (UnknownHostException e) {
                    }
                }
            }

        }else{
            for(int i = fileItems.size()-1; i>=0;i--){
                FileItem item = fileItems.get(i);
                if(item.fileStatus != Constants.TDTASK_STATUS_DOWNLOADING){
                    fileItems.remove(item);
                }
            }
        }
        return fileItems;
    }


    private List<FileItem> getTasklist(int complete, int pageindex, int pageCapacity) {
        List<FileItem> fileItems = new ArrayList<FileItem>();

        GetTaskListAPI taskListAPI = new GetTaskListAPI(mRouterIp, complete, pageindex, pageCapacity, mVersionCode,false);
        AppConfig.logRemote("router tddownload get tasklist");
        TaskList taskList = null;
        try {
            taskList = taskListAPI.request();
            if (taskList != null && taskList.tasks != null) {
                for (TaskList.Task task : taskList.tasks) {
                    String path = task.filePath;
                    String smbPath = getRouterSmbPath(path);
                    AppConfig.logRemote("router tddownload file path is: " + smbPath);

                    if(complete == GetTaskListAPI.TYPE_TASK_COMPLETED){
                        FileItem item = createFileItem(smbPath);
                        if(item != null){
                            AppConfig.logRemote("router item: "+ item.filePath + "  category:"+item.category);
                            item.fileStatus = task.stat;
                            if(item.category == FileCategory.DIR || item.category == FileCategory.VIDEO){
                                fileItems.add(item);
                            }
                        }
                    }else{
                        FileItem item = new FileItem();
                        item.fileStatus = task.stat;
                        item.filePath = task.filePath;
                        fileItems.add(item);
                    }

                }

                AppConfig.logRemote("router gettasklist size is: " + fileItems.size());

                return fileItems;

            }
        } catch (IOException e) {
            AppConfig.logRemote("[[XLRouterDownloadMgr]] getTaskList IOException:" + e.getMessage());
        }

        return null;

    }

    public static FileItem createFileItem(String smbPath){
        try {
            SmbFile smbFile = new SmbFile(smbPath);
            if (!smbFile.exists()) {
                return null;
            }

            FileItem item = new FileItem();
            String fileName = smbFile.getName();
            String filePath = smbFile.getCanonicalPath();

            if (smbFile.isDirectory()) {
                fileName = fileName.substring(0, fileName.length() - 1);
                item.category = FileCategory.DIR;
                item.fileSize = 0;
            } else if (smbFile.isFile()) {
                item.category = FileCategoryHelper.getFileCategoryByName(fileName);
                item.fileSize = smbFile.getContentLength();
            }

            item.fileName = fileName;
            item.filePath = filePath;
            item.lastModifyTime = smbFile.getLastModified();
            item.canRead = smbFile.canRead();
            item.canWrite = smbFile.canWrite();
            item.cid = "";

            return item;

        } catch (MalformedURLException e) {
            AppConfig.logRemote("[[XlrounterDownloadMgr]] createFileItem MalformedURLException:" + e.getMessage());
        } catch (SmbException e) {
            AppConfig.logRemote("[[XlrounterDownloadMgr]] createFileItem SmbException:" + e.getMessage());
        }

        return null;
    }

    @Override
    public void setDownloadedFileItems(List<FileItem> list, Context context) {
        mfileitems = list;
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
        deviceItem.setType(DeviceItem.DeviceType.XL_ROUTER_TDDOWNLOAD);

        if (list != null) {

            for (FileItem fileItem : list) {

                fileItem.isNew = fileExploreHistoryManager.isFileNew(fileItem, deviceItem);

                if (fileItem.category == FileCategory.VIDEO) {
                    result.add(fileItem);
                }

                if (fileItem.category == FileCategory.DIR) {
                    AppConfig.logRemote("router file.getpaht is: " + fileItem.filePath);
                    List<FileItem> childrens = getChildFiles(fileItem.filePath+File.separator,context);
                    if (childrens != null && childrens.size() > 0) {

                        for (int i = 0; i < childrens.size(); i++) {
                            FileItem childItem = childrens.get(i);
                            if (childItem.category == FileCategory.VIDEO || childItem.category == FileCategory.DIR) {
                                result.add(fileItem);
                                break;
                            }
                        }

                    }
                }
            }

        }

        AppConfig.logRemote("router tddownload after filter : " + result.size());

        return result;
    }

    public static List<FileItem> getChildFiles(String smbPath, Context context){
        try {
            List<FileItem> list = new ArrayList<FileItem>();

            SmbFile smbRootFile = new SmbFile(smbPath);

            if (!smbRootFile.exists()) {
                return null;
            }

            SmbFile[] fileList = smbRootFile.listFiles();
            if (fileList == null || fileList.length == 0) {
                return null;
            }

            FileExploreHistoryManager fileExploreHistoryManager = new FileExploreHistoryManager(context);

            DeviceItem deviceItem = new DeviceItem();
            deviceItem.setType(DeviceItem.DeviceType.XL_ROUTER_TDDOWNLOAD);

            for (SmbFile smbFile : fileList) {
                FileItem item = new FileItem();
                String fileName = smbFile.getName();
                String filePath = smbFile.getCanonicalPath();
                item.filePath = filePath;
                item.lastModifyTime = smbFile.getLastModified();
                item.canRead = smbFile.canRead();
                item.canWrite = smbFile.canWrite();
                try {
                    BufferedInputStream bis = new BufferedInputStream(new SmbFileInputStream(smbFile));
                    String cid = CidUtil.queryCid(bis, smbFile.length());
                    if(!TextUtils.isEmpty(cid)){
                        item.cid = cid;
                    }else{
                        item.cid ="";
                    }
                } catch (SmbException e) {
                } catch (MalformedURLException e) {
                } catch (UnknownHostException e) {
                }

                if (smbFile.isDirectory()) {
                    fileName = fileName.substring(0, fileName.length() - 1);
                    item.fileName = fileName;

                    item.category = FileCategory.DIR;
                    item.fileSize = 0;

                    item.isNew = fileExploreHistoryManager.isFileNew(item, deviceItem);


                    list.add(item);
                } else if (smbFile.isFile()) {
                    item.category = FileCategoryHelper.getFileCategoryByName(fileName);
                    item.fileSize = smbFile.getContentLength();
                    item.fileName = fileName;

                    if(item.category == FileCategory.VIDEO){
                        item.isNew = fileExploreHistoryManager.isFileNew(item, deviceItem);
                        list.add(item);
                    }
                }

            }


            return list;

        } catch (MalformedURLException e) {
            AppConfig.logRemote("[[XlrounterDownloadMgr]] createFileItem MalformedURLException:" + e.getMessage());
        } catch (SmbException e) {
            AppConfig.logRemote("[[XlrounterDownloadMgr]] createFileItem SmbException:" + e.getMessage());
        }

        return null;
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

    private String getRouterSmbPath(String path) {
        if(!TextUtils.isEmpty(path)){

            int index = path.lastIndexOf(":");

            if(index > 0){
                char rootDir = path.charAt(index -1);
                String smbDir = path.substring(index+1);
                String smbRoot = SettingManager.getInstance().getRouterSmbRootPath();

                if(mRouterName.equals(SmbUtil.ROUTER_NAMES.get(SmbUtil.ROUTER_XUNLEI))){
                    String finalPath = smbRoot + rootDir + smbDir;
                    return finalPath;
                }else if(mRouterName.equals(SmbUtil.ROUTER_NAMES.get(SmbUtil.ROUTER_XIAOMI))){
                    String finalPath = smbRoot + "XiaoMi" + smbDir;
                    return finalPath;
                }else{
                    return null;
                }

            }

        }

        return null;
    }

    private void getVersion() {
        if (mSysInfo != null) {
            String version = mSysInfo.version;
            if(!TextUtils.isEmpty(version)){
                String[] codes = version.split(".");
                if (codes != null && codes.length >= 4) {
                    String v = codes[codes.length - 1];
                    mVersionCode = Integer.parseInt(v);
                }
            }
        }
    }

    private String createRouterIp(Context context){
        String  routerIp = SmbUtil.getRouterIp(context);

        if(TextUtils.isEmpty(routerIp)){
            return null;
        }

        mRouterName = SmbUtil.getRouterName(context);
        if(!TextUtils.isEmpty(mRouterName)){

            if(mRouterName.equals(SmbUtil.ROUTER_NAMES.get(SmbUtil.ROUTER_XUNLEI))){
                return new StringBuilder(SCHEMA_HTTP).append(routerIp).append(":").append(ROUTER_PORT_XUNLEI).append(File.separator).toString();
            }else if(mRouterName.equals(SmbUtil.ROUTER_NAMES.get(SmbUtil.ROUTER_XIAOMI))){
                return new StringBuilder(SCHEMA_HTTP).append(routerIp).append(":").append(ROUTER_PORT_XIAOMI).append(File.separator).toString();
            }else{
                return new StringBuilder(SCHEMA_HTTP).append(routerIp).append(":").append(ROUTER_PORT_DEFAULT).append(File.separator).toString();
            }
        }

        return null;
    }

    @Override
    public void getSublistFileItems(final FileItem file, final Context context) {

        CustomThreadPool.getInstance().excute(new CustomThreadPool.TaskWrapper(new Runnable() {
            @Override
            public void run() {
                if (file != null) {

                    List<FileItem> children = getChildFiles(file.filePath+File.separator,context);

                    SublistEvent event = new SublistEvent();
                    event.list = children;
                    EventBus.getDefault().post(event);


                }
            }
        }));

    }

    private boolean isSupportRouter(){
        if(!TextUtils.isEmpty(mRouterName)){
            return ROUTER_NAMES.containsKey(mRouterName);
        }
        return false;
    }
}
