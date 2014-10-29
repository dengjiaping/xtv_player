package com.kankan.player.app;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.text.TextUtils;
import com.kankan.player.util.AdUtil;
import com.kankan.player.util.SettingManager;
import com.kankan.player.video.server.HttpService;
import com.plugin.common.utils.UtilsConfig;
import com.plugin.common.utils.crash.CrashHandler;
import com.umeng.analytics.MobclickAgent;

public class TVPlayerApplication extends Application {
    public static Activity sMenuActivity;

    @Override
    public void onCreate() {
        super.onCreate();

        SettingManager.getInstance().init(this);

        UtilsConfig.init(getApplicationContext());
        AppRuntime.getInstance(getApplicationContext());

        initUMeng();

        //启动内置服务将smb等转换为http流进行播放
        startService(new Intent(this, HttpService.class));

        //crashlog
        if(AppConfig.DEBUG){
            CrashHandler.getInstance().init(getApplicationContext());
        }

        if (AppConfig.ADVERTISE_ON) {
            AdUtil.getInstance(this).downloadImages();
        }

        initRemote();
    }

    private void initUMeng() {
        MobclickAgent.setDebugMode(AppConfig.DEBUG);
        com.umeng.common.Log.LOG = AppConfig.DEBUG;
    }

    private void initRemote(){
        //预装包 partnerid和license不为空  toC包为空
        if(!TextUtils.isEmpty(AppConfig.PARTNER_ID)){
            SettingManager.getInstance().setPartnerId(AppConfig.PARTNER_ID);
        }
        if(!TextUtils.isEmpty(AppConfig.DEVICE_LICENSE)){
            SettingManager.getInstance().setLicense(AppConfig.DEVICE_LICENSE);
        }
        if(!TextUtils.isEmpty(AppConfig.NTFS_TYPE)){
            SettingManager.getInstance().setNtfsType(AppConfig.NTFS_TYPE);
        }

    }
}
