package com.kankan.player.api.tddownload;

import android.text.TextUtils;
import com.kankan.player.app.AppConfig;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Created by wangyong on 14-6-11.
 */
public class StopApI extends TDBaseAPI<Integer> {

    private static final String API_NAME = "stopthunder";
    private String apiUrl ;

    public StopApI(String baseUrl) {
        apiUrl = baseUrl + API_NAME;
    }

    @Override
    protected String getUrl() {
        return apiUrl;
    }

    @Override
    protected Integer request(String response) {
        if(TextUtils.isEmpty(response)){
            return null;
        }

        try {
            JSONArray jsonArray = new JSONArray(response);
            if(jsonArray.length()>0){
                int result = jsonArray.getInt(0);
                return result;
            }

        } catch (JSONException e) {
            AppConfig.LOGD("[[StopApI]] " + e.getMessage());
        }

        return null;
    }
}
