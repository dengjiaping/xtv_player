package com.kankan.player.manager;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import com.kankan.player.app.AppConfig;
import com.kankan.player.event.DeviceEvent;
import com.kankan.player.event.DeviceInfoEvent;
import com.kankan.player.item.Device;
import com.kankan.player.item.DeviceItem;
import com.kankan.player.util.DeviceModelUtil;
import com.kankan.player.util.SettingManager;
import com.kankan.player.util.SmbUtil;
import com.xunlei.tv.player.R;
import de.greenrobot.event.EventBus;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhangdi on 14-3-27.
 */
public class DeviceManager {
    private static final String TAG = DeviceManager.class.getSimpleName();

    private final Context mContext;
    private volatile static DeviceManager instance;

    private List<DeviceItem> mUsbDeviceList = new ArrayList<DeviceItem>();

    public static DeviceManager getInstance(Context context) {
        if (instance == null) {
            synchronized (DeviceManager.class) {
                if (instance == null) {
                    instance = new DeviceManager(context);
                }
            }
        }
        return instance;
    }

    private DeviceManager(Context context) {
        mContext = context;
    }

    // 常驻入口：文件列表，历史记录
    public void refreshNormalEntries() {
        List<DeviceItem> items = new ArrayList<DeviceItem>();
        List<DeviceItem.DeviceType> types = new ArrayList<DeviceItem.DeviceType>();

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            String externalPath = Environment.getExternalStorageDirectory().getAbsolutePath();
            items.add(new DeviceItem("本地文件", DeviceItem.DeviceType.EXTERNAL, externalPath, 0, "浏览盒子的文件"));
        }

        items.add(new DeviceItem("历史记录", DeviceItem.DeviceType.HISTORY, "", 0, "您看过的视频"));

        types.add(DeviceItem.DeviceType.EXTERNAL);
        types.add(DeviceItem.DeviceType.HISTORY);

        DeviceEvent event = new DeviceEvent();
        event.deviceItems = items;
        event.types = types;
        EventBus.getDefault().post(event);
    }

    public void refreshTdDownload() {
        List<DeviceItem> items = new ArrayList<DeviceItem>();
        List<DeviceItem.DeviceType> types = new ArrayList<DeviceItem.DeviceType>();

        Device deviceInfo = null;
        if(!TextUtils.isEmpty(DeviceModelUtil.getPartnerId())){
            deviceInfo = DeviceModelUtil.getVenderInfo(mContext, DeviceModelUtil.getPartnerId());
        }

        if (AppConfig.isRemoteRouterOpen) {
            /**
             * 为路由器修改的逻辑
             */
//            XLRouterDownloadMgr routerDownloadManager = XLRouterDownloadMgr.getInstance();
//            routerDownloadManager.init(mContext);

            if((deviceInfo != null && deviceInfo.isReleaseRemote()) || DeviceModelUtil.isSupportBox()){
                items.add(new DeviceItem(mContext.getString(R.string.remote_title_mydownload), DeviceItem.DeviceType.TD_DOWNLOAD, "", 0, mContext.getString(R.string.remote_how_download)));
            }else{
                LOGD("tv not support td download");
            }

        } else {
            /**
             * 不考虑路由器的逻辑
             */
            if((deviceInfo != null && deviceInfo.isReleaseRemote())|| DeviceModelUtil.isSupportBox()){
                items.add(new DeviceItem(mContext.getString(R.string.remote_title_mydownload), DeviceItem.DeviceType.TD_DOWNLOAD, "", 0, mContext.getString(R.string.remote_how_download)));
            }else{
                LOGD("tv not support td download");
            }
        }

        types.add(DeviceItem.DeviceType.TD_DOWNLOAD);

        DeviceEvent event = new DeviceEvent();
        event.deviceItems = items;
        event.types = types;
        EventBus.getDefault().post(event);


        DeviceInfoEvent deviceInfoEvent = new DeviceInfoEvent();
        deviceInfoEvent.device = deviceInfo;
        EventBus.getDefault().post(deviceInfoEvent);
    }

    public void refreshUsbDevices() {
        List<DeviceItem.DeviceType> types = new ArrayList<DeviceItem.DeviceType>();

        mUsbDeviceList = UsbManager.getUsbDeviceList();
        List<DeviceItem> usbDeviceList = mUsbDeviceList;
        if (usbDeviceList != null) {
            if (usbDeviceList.size() == 1) {
                usbDeviceList.get(0).setName("移动存储设备");
            } else {
                for (int i = 1; i <= usbDeviceList.size(); i++) {
                    usbDeviceList.get(i - 1).setName("移动存储设备" + i);
                }
            }
        }

        types.add(DeviceItem.DeviceType.USB);
        types.add(DeviceItem.DeviceType.HHD);

        DeviceEvent event = new DeviceEvent();
        event.deviceItems = usbDeviceList;
        event.types = types;
        EventBus.getDefault().post(event);
    }

    public void refreshRouter() {
        List<DeviceItem> items = new ArrayList<DeviceItem>();
        List<DeviceItem.DeviceType> types = new ArrayList<DeviceItem.DeviceType>();

        String smbRootPath = SmbUtil.isSmbServerExists(mContext);
        if (!TextUtils.isEmpty(smbRootPath)) {
            AppConfig.LOGD("[[refreshRouter]] smb server exists, path=" + smbRootPath);
            SettingManager.getInstance().setSmbEnable(true);
            String routerName = SmbUtil.getRouterName(mContext);
            if (routerName == null) {
                routerName = "";
            }
            items.add(new DeviceItem(routerName, DeviceItem.DeviceType.XL_ROUTER, smbRootPath, 0, "路由器硬盘 | 网络共享"));
        } else {
            AppConfig.LOGD("[[refreshRouter]] smb server not exists.");
            SettingManager.getInstance().setSmbEnable(false);
        }

        types.add(DeviceItem.DeviceType.XL_ROUTER);

        DeviceEvent event = new DeviceEvent();
        event.deviceItems = items;
        event.types = types;
        EventBus.getDefault().post(event);
    }

    public void refreshDevices() {
        refreshNormalEntries();
        refreshUsbDevices();
        refreshTdDownload();
        if(AppConfig.isForXLRouter){
            refreshRouter();
        }

    }

    public List<DeviceItem> getUsbDeviceList() {
        return mUsbDeviceList;
    }

    private void LOGD(String msg) {
        if (AppConfig.DEBUG && !TextUtils.isEmpty(msg)) {
            Log.d(TAG, msg);
        }
    }

}
