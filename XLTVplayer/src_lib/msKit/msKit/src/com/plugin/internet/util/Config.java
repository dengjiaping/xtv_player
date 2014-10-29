package com.plugin.internet.util;

import com.plugin.logger.Logger;

/**
 * Created by michael on 13-12-5.
 */
public class Config {

    public static final boolean DEBUG = true;

    static {
        Logger.Option opt = new Logger.Option();
        opt.logDirFullPath = "/sdcard/.com.plugin.log/";
        Logger.getInstance().init(opt);
    }

    public static final void LOGD(String msg) {
        if (DEBUG) {
            Logger.d("com.plugin.internet", msg);
        }
    }

}
