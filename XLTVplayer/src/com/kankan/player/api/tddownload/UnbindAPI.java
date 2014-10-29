package com.kankan.player.api.tddownload;

import android.text.TextUtils;
import com.kankan.player.app.AppConfig;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Created by wangyong on 14-4-17.
 */
public class UnbindAPI extends TDBaseAPI<Integer> {

    private static final String API_NAME = "unbind";
    private String apiUrl ;

    public UnbindAPI(String baseUrl) {
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
            AppConfig.LOGD("[[UnbindApi]] " + e.getMessage());
        }

        return null;
    }
}
