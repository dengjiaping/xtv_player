package com.kankan.player.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import com.kankan.player.app.AppConfig;
import com.kankan.player.util.ShellUtils;
import com.plugin.common.utils.UtilsConfig;

import java.io.File;

/**
 * Created by wangyong on 14-6-11.
 */
public class AppDelReceiver extends BroadcastReceiver {

    private String mPath = null;


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(action.equals(Intent.ACTION_PACKAGE_REMOVED)){

            String packageName = intent.getDataString();
            if(packageName.startsWith(context.getPackageName())){
                mPath = context.getCacheDir().getAbsolutePath() + "/";
            }

            executeStopCommand();
        }
    }

    private void executeStopCommand(){

        String portalFilePath = mPath + "portal";
        File file = new File(portalFilePath);
        if(file.exists()){
            String stopCommand = "." +mPath + "portal -s";

            if(!TextUtils.isEmpty(stopCommand)){

                ShellUtils.CommandResult resultCmd = ShellUtils.execCommand(stopCommand,false);

                AppConfig.LOGD("skyworth error result is: " + resultCmd.errorMsg);
                AppConfig.LOGD("skyworth sucess result is: " + resultCmd.successMsg);
            }
        }

    }
}
