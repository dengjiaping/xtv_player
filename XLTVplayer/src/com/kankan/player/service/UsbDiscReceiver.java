package com.kankan.player.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.kankan.player.item.DeviceItem;
import com.kankan.player.manager.UsbManager;
import com.plugin.common.utils.UtilsConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by wangyong on 14-5-14.
 */
public class UsbDiscReceiver extends BroadcastReceiver {

    private static List<DeviceItem> mUsbDevices = new ArrayList<DeviceItem>();


    @Override
    public void onReceive(Context context, Intent intent) {

        if(intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED)){
            mUsbDevices.clear();
            mUsbDevices.addAll(UsbManager.getUsbDeviceList());
            UtilsConfig.LOGD("received mounted in bootreciveer" + mUsbDevices.size());
        }

        if(intent.getAction().equals(Intent.ACTION_MEDIA_EJECT)){

            UtilsConfig.LOGD("received unmounted in bootreciveer");

            List<String> mounts = filterMounts();

            UtilsConfig.LOGD("received unmounted  after filter in bootreciveer" +mounts.size());

            if(mounts.size() >0){
                TdScanService.startTdScanServiceUnmountDisc(context,mounts);
            }

        }
    }

    private List<String> filterMounts(){
        List<String> mounts = new ArrayList<String>();
        HashMap<String, DeviceItem> map = new HashMap<String, DeviceItem>();

        if(mUsbDevices != null){

            for(DeviceItem item : mUsbDevices){

                map.put(item.getPath(),item);
            }
        }

        List<DeviceItem> list = UsbManager.getUsbDeviceList();

        if(list != null){

            for(DeviceItem item : list){

                if(!map.containsKey(item.getPath())){
                    mounts.add(item.getPath());
                }
            }

            if(list.size() == 0){

                for(DeviceItem item : mUsbDevices){
                    mounts.add(item.getPath());
                }

            }

            mUsbDevices.clear();
            mUsbDevices.addAll(list);



        }

        return mounts;
    }
}
