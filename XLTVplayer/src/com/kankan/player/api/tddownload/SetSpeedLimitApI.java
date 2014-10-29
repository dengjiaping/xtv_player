package com.kankan.player.api.tddownload;

import android.text.TextUtils;
import com.kankan.player.app.AppConfig;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Created by wangyong on 14-5-26.
 */
public class SetSpeedLimitApI extends TDBaseAPI<Integer> {

    private static final String API_NAME = "setplanspeedlimit";
    private String apiUrl ;

    private int download_speed;
    private int upload_speed;
    private int start_time = 0;
    private int end_time = 1440;

    public SetSpeedLimitApI(String baseUrl, int download_speed, int upload_speed) {
        apiUrl = baseUrl + API_NAME;
        this.download_speed = download_speed;
        this.upload_speed = upload_speed;
    }

    @Override
    protected String getUrl() {
        return apiUrl+String.format("?download_speed=%d&upload_speed=%d&slstart_time=%d&slend_time=%d",
                download_speed, upload_speed, start_time,end_time);
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
            AppConfig.LOGD("[[SetSpeedLimitApI]] " + e.getMessage());
        }

        return null;
    }
}
