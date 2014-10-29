package com.kankan.player.util;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import com.kankan.player.dao.model.DaoMaster;
import com.kankan.player.dao.model.DaoSession;

public class DaoUtils {

    private static final String DATABASE_NAME = "thunder_player_db";

    private static DaoSession sDaoSession;

    public synchronized static DaoSession getDaoSession(Context context) {
        if (sDaoSession == null) {
            DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(context, DATABASE_NAME, null);
            SQLiteDatabase database = helper.getWritableDatabase();
            DaoMaster m = new DaoMaster(database);

            sDaoSession = m.newSession();
        }

        return sDaoSession;
    }

}
