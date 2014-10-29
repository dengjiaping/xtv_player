package com.kankan.player.api.tddownload;

import android.text.TextUtils;
import com.kankan.player.app.AppConfig;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Created by zhangdi on 14-4-2.
 */
public class GetSysInfoAPI extends TDBaseAPI<SysInfo> {

    private static final String API_NAME = "getsysinfo";
    private String apiUrl ;

    public GetSysInfoAPI(String baseUrl) {
        apiUrl = baseUrl + API_NAME;
    }

    @Override
    protected String getUrl() {
        return apiUrl;
    }

    @Override
    protected SysInfo request(String response) {
        if (TextUtils.isEmpty(response)) {
            LOGD("response is null");
            return null;
        }

        LOGD(response);

        try {
            SysInfo sysInfo = new SysInfo();
            JSONArray jsonArray = new JSONArray(response);
            if (jsonArray.length() >= 9) {
                sysInfo.result = jsonArray.getInt(0);
                sysInfo.isNetOk = jsonArray.getInt(1);
                sysInfo.isLicenseOk = jsonArray.getInt(2);
                sysInfo.isBindOk = jsonArray.getInt(3);
                sysInfo.bindAcktiveKey = jsonArray.getString(4);
                sysInfo.isDiskOk = jsonArray.getInt(5);
                sysInfo.version = jsonArray.getString(6);
                sysInfo.userName = jsonArray.getString(7);
                sysInfo.isEverBinded = jsonArray.getInt(8);
            }
            if (jsonArray.length() >= 11) {
                sysInfo.userId = jsonArray.getInt(9);
                sysInfo.vipLevel = jsonArray.getInt(10);
            }
            return sysInfo;
        } catch (JSONException e) {
            e.printStackTrace();
            LOGD("json array parse exception");
            return null;
        }
    }

}
