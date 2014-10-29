package com.kankan.player.api.tddownload;

import android.text.TextUtils;
import com.kankan.player.app.AppConfig;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Created by wangyong on 14-5-28.
 */
public class GetSpeedApI extends TDBaseAPI<SpeedInfo> {

    private static final String API_NAME = "getspeedlimit";
    private String apiUrl ;

    public GetSpeedApI() {
        apiUrl = AppConfig.TD_SERVER_URL+API_NAME;
    }

    @Override
    protected String getUrl() {
        return apiUrl;
    }

    @Override
    protected SpeedInfo request(String response) {

        if (TextUtils.isEmpty(response)) {
            LOGD("response is null");
            return null;
        }

        try {
            SpeedInfo speedInfo = new SpeedInfo();
            JSONArray jsonArray = new JSONArray(response);
            if (jsonArray.length() >= 3) {
                speedInfo.result = jsonArray.getInt(0);
                speedInfo.downloadSpeed = jsonArray.getInt(1);
                speedInfo.uploadSpeed = jsonArray.getInt(2);
            }
            return speedInfo;
        } catch (JSONException e) {
            LOGD("json array parse exception");
            return null;
        }

    }
}
