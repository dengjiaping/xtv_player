package com.kankan.player.video.server;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import com.kankan.player.app.AppConfig;
import com.kankan.player.util.SettingManager;

import java.io.IOException;

public class HttpService extends Service {
    public static final int PORT_MIN = 18710;
    public static final int PORT_MAX = 18730;

    VideoHttpServer mVideoHttpServer;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        boolean success = false;
        int port = PORT_MIN;

        while (!success && port <= PORT_MAX) {
            try {
                mVideoHttpServer = new VideoHttpServer(port);
                mVideoHttpServer.start();
                success = true;
                SettingManager.getInstance().setVideoServerPort(port);
                AppConfig.LOGD("video server start...listen on port " + port);
            } catch (IOException e) {
                AppConfig.LOGD("video server start failure, " + e.getMessage());
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mVideoHttpServer.stop();
        AppConfig.LOGD("video server stop...");
    }

}
