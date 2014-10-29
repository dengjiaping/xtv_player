package com.kankan.player.api.tddownload;

import android.text.TextUtils;
import com.kankan.player.app.AppConfig;
import com.plugin.common.utils.UtilsConfig;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Created by wangyong on 14-5-8.
 */
public class UnMountDiscAPI extends TDBaseAPI<Integer> {

    private static final String API_NAME = "unbind";
    private String apiUrl ;

    private String mPath;

    public UnMountDiscAPI(String baseUrl,String path) {
        apiUrl = baseUrl + API_NAME;
        mPath = path;
    }

    @Override
    protected String getUrl() {
        return apiUrl+String.format("http://127.0.0.1:9000/notifydiskumounting?mountpath=%s",mPath);
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
            AppConfig.LOGD("[[UnmountDiscAPI]] " + e.getMessage());
        }

        return null;
    }
}
