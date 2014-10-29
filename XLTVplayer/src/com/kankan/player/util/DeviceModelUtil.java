package com.kankan.player.util;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import com.kankan.player.api.remote.GetVenderInfoRequest;
import com.kankan.player.api.remote.GetVenderInfoResponse;
import com.kankan.player.app.AppConfig;
import com.kankan.player.app.DeviceLicense;
import com.kankan.player.event.DeviceEvent;
import com.kankan.player.event.DeviceInfoEvent;
import com.kankan.player.item.Device;
import com.kankan.player.service.TdScanService;
import com.plugin.common.utils.CustomThreadPool;
import com.plugin.internet.InternetUtils;
import com.plugin.internet.core.NetWorkException;
import de.greenrobot.event.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by wangyong on 14-7-15.
 */
public class DeviceModelUtil {

    private static String mCurrentDevice;

    private static List<Device> deviceList = new ArrayList<Device>();

    public static final String BOX_I71S = "创维盒子";
    public static final String BOX_TCLV7 = "TCL盒子";
    public static final String BOX_XIAOMI = "小米盒子";
    public static final String BOX_KAIBOER = "开博尔盒子";
    public static final String BOX_10MOONS = "天敏盒子";

    private static final String BASE_URL = "http://127.0.0.1:";
    private static final String URL_DEFAULT = "9000/";
    private static final String URL_XIAOMI = "19000/";

    public static final String DEVICE_MODEL_XIAOMI = "mibox";
    public static final String DEVICE_MODEL_MITV = "mitv";
    public static final String DEVICE_MODEL_HIMEDIA = "himedia";

    public static Device currentDeviceInfo;

    static {

        addDevice();

        initRemote();

    }

    private static void addDevice() {
        Device miBoxDevice = new Device(DEVICE_MODEL_XIAOMI, "小米盒子", BASE_URL + URL_XIAOMI, false, "");
        deviceList.add(miBoxDevice);

        Device miTvDevice = new Device(DEVICE_MODEL_MITV, "小米电视", BASE_URL + URL_XIAOMI, false, "");
        deviceList.add(miTvDevice);

        Device himediaDevice = new Device(DEVICE_MODEL_HIMEDIA, "海美迪盒子", BASE_URL + URL_DEFAULT, false, "");
        deviceList.add(himediaDevice);
    }

    private static void initRemote() {
        mCurrentDevice = Build.MODEL;

        if (deviceList != null) {
            for (Device device : deviceList) {

                String deviceName = device.getDeviceName();

                if (mCurrentDevice.toLowerCase().startsWith(deviceName)) {
                        AppConfig.TD_SERVER_URL = device.getRemoteUrl();
                }

            }
        }

    }

    public static Boolean isSupportReleaseService() {
        if (currentDeviceInfo != null) {
            return currentDeviceInfo.isReleaseRemote();
        }
        return false;
    }

    public static Boolean isSupportBox() {
        if(currentDeviceInfo != null && currentDeviceInfo.isReleaseRemote()){
            return true;
        }else if (deviceList != null) {
            for (Device device : deviceList) {

                String deviceName = device.getDeviceName();

                if (mCurrentDevice.toLowerCase().startsWith(deviceName)) {
                        return true;

                }

            }
        }
        return false;

    }

    public static String getSupportBoxName() {
        if (currentDeviceInfo != null) {
            return currentDeviceInfo.getBoxName();
        }else if (deviceList != null) {
            for (Device device : deviceList) {

                String deviceName = device.getDeviceName();

                if (mCurrentDevice.toLowerCase().startsWith(deviceName)) {
                    return device.getBoxName();
                }

            }
        }

        return "";
    }

    public static String getPartnerId() {
        if(AppConfig.isI71s()){
            return "830";
        }
        if(AppConfig.isTCL()){
            return "840";
        }
        return SettingManager.getInstance().getPartnerId();
    }

    public static String getLicense(){
        return SettingManager.getInstance().getLicense();
    }

    public static String getPort(){
        if(currentDeviceInfo != null){
            return currentDeviceInfo.getPort();
        }
        return null;
    }

    public static String getNtfsType(){
        return SettingManager.getInstance().getNtfsType();
    }

    public static String getCurrentDevice() {
        return mCurrentDevice;
    }

    public static Device getVenderInfo(Context context,String partnerId) {

        AppConfig.logRemote("get vender info"+partnerId);

        GetVenderInfoRequest request = new GetVenderInfoRequest(partnerId);
        try {
            GetVenderInfoResponse response = InternetUtils.request(context, request);
            if (response != null) {

                if (response.rtnCode == 0) {
                    return response.data;
                }

            }
        } catch (NetWorkException e) {
            AppConfig.logRemote("get vendor info networkexception: " + e.getMessage());
        }

        return null;
    }

    public static void asynGetVenderInfo(final Context context,final String partnerId){
        CustomThreadPool.asyncWork(new Runnable() {
            @Override
            public void run() {
                Device device = getVenderInfo(context,partnerId);

                DeviceInfoEvent deviceInfoEvent = new DeviceInfoEvent();
                deviceInfoEvent.device = device;
                EventBus.getDefault().post(deviceInfoEvent);
            }
        });
    }

    public static void setDeviceInfo(Device device){
        currentDeviceInfo = device;
        AppConfig.TD_SERVER_URL = device.getRemoteUrl();
        AppConfig.logRemote("set td server url is: "+ AppConfig.TD_SERVER_URL);
        if(device != null && !TextUtils.isEmpty(device.getReleaseLicense())){
            if(AppConfig.isTCL() || AppConfig.isTCL()){
                SettingManager.getInstance().setLicense(device.getReleaseLicense());
            }
        }
    }
}
