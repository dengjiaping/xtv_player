package com.kankan.player.api.beanrequest;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import com.plugin.internet.core.InternetStringUtils;
import com.plugin.internet.core.NetWorkException;
import com.plugin.internet.core.RequestBase;
import com.plugin.internet.core.RequestEntity;
import com.plugin.internet.core.annotations.NeedTicket;
import com.plugin.internet.core.annotations.NoNeedTicket;
import com.plugin.internet.core.annotations.NoRestMethod;
import com.plugin.internet.core.annotations.OptionalTicket;
import com.kankan.player.app.AppConfig;
import com.kankan.player.util.SettingManager;

import java.util.TreeMap;
import java.util.Vector;

/**
 * Created by dajie on 13-12-23.
 */
public class PMRequestBase<T> extends RequestBase<T> {

    public static String BASE_API_URL = AppConfig.REST_URL;

    private static final String KEY_METHOD = "method";
    private static final String KEY_HTTP_METHOD = "httpMethod";

    @Override
    public RequestEntity getRequestEntity() throws NetWorkException {
        RequestEntity entity = super.getRequestEntity();
        return entity;
    }

    @Override
    public Bundle getParams() throws NetWorkException {
        Bundle params = super.getParams();

        Class<?> c = this.getClass();
        String ticket = null;
        String userSecret = null;
        // Method name
        boolean checkTicket = false;
        boolean noNeedTicket = false;
        boolean optionalTicket = false;

        if (c.isAnnotationPresent(NeedTicket.class)) {
            checkTicket = true;
            ticket = SettingManager.getInstance().getTicket();
            userSecret = SettingManager.getInstance().getSecretKey();
        } else if (c.isAnnotationPresent(NoNeedTicket.class)) {
            noNeedTicket = true;
        } else if (c.isAnnotationPresent(OptionalTicket.class)) {
            optionalTicket = true;
            ticket = SettingManager.getInstance().getTicket();
            userSecret = SettingManager.getInstance().getSecretKey();
        } else {    //默认为NeedTicket
            checkTicket = true;
            ticket = SettingManager.getInstance().getTicket();
            userSecret = SettingManager.getInstance().getSecretKey();
        }

        if (checkTicket) {
            if (TextUtils.isEmpty(ticket) || TextUtils.isEmpty(userSecret)) {
                //TODO not login
                if (AppConfig.DEBUG) {
//                    AppConfig.LOGD("[PMRequestBase]" + "====No valid ticket==");
                }
                return null;
            }
        }

        String method = params.getString(KEY_METHOD);
        if (TextUtils.isEmpty(method)) {
            throw new RuntimeException("Method Name MUST NOT be NULL");
        }

        if (!method.startsWith("http://")) {
            method = BASE_API_URL + method.replace(".", "/");
        }

        if (!noNeedTicket && !TextUtils.isEmpty(ticket)) {
            params.putString("t", ticket);
        }

        if (c.isAnnotationPresent(UseHttps.class)) {
            method = method.replace("http", "https");
            method = method.replaceAll(":(\\d+)/", "/");
        }

        String httpMethod = params.getString(KEY_HTTP_METHOD);
        params.remove(KEY_HTTP_METHOD);
        params.remove(KEY_METHOD);
        params.putString("v", "1.0");   //TODO
        params.putString("call_id", String.valueOf(System.currentTimeMillis()));
        params.putString("gz", "compression");
        if (noNeedTicket) {
            params.putString("sig", getSig(params, AppConfig.APP_SECRET, null));
        }
        if (optionalTicket) {
            if (TextUtils.isEmpty(ticket)) {
                params.putString("sig", getSig(params, AppConfig.APP_SECRET, null));
            } else {
                params.putString("sig", getSig(params, null, userSecret));
            }
        }
        if (checkTicket) {
            params.putString("sig", getSig(params, null, userSecret));
        }
        params.putString("sig", getSig(params, noNeedTicket ? AppConfig.APP_SECRET : null, noNeedTicket ? null : userSecret));
        params.putString(KEY_METHOD, method);
        params.putString(KEY_HTTP_METHOD, httpMethod);

        return params;
    }

    private String getSig(Bundle params, String appSecretKey, String userSecretKey) {
        if (params == null) {
            return null;
        }

        if (params.size() == 0) {
            return "";
        }


        TreeMap<String, String> sortParams = new TreeMap<String, String>();
        for (String key : params.keySet()) {
            sortParams.put(key, params.getString(key));
        }

        Vector<String> vecSig = new Vector<String>();
        for (String key : sortParams.keySet()) {
            String value = sortParams.get(key);
            vecSig.add(key + "=" + value);
        }

        String[] nameValuePairs = new String[vecSig.size()];
        vecSig.toArray(nameValuePairs);

        for (int i = 0; i < nameValuePairs.length; i++) {
            for (int j = nameValuePairs.length - 1; j > i; j--) {
                if (nameValuePairs[j].compareTo(nameValuePairs[j - 1]) < 0) {
                    String temp = nameValuePairs[j];
                    nameValuePairs[j] = nameValuePairs[j - 1];
                    nameValuePairs[j - 1] = temp;
                }
            }
        }
        StringBuffer nameValueStringBuffer = new StringBuffer();
        for (int i = 0; i < nameValuePairs.length; i++) {
            nameValueStringBuffer.append(nameValuePairs[i]);
        }

        if (!TextUtils.isEmpty(appSecretKey)) {
            nameValueStringBuffer.append(appSecretKey);
        }

        if (!TextUtils.isEmpty(userSecretKey)) {
            nameValueStringBuffer.append(userSecretKey);
        }

        if (AppConfig.DEBUG) {
            for (int i = 0; i < nameValueStringBuffer.toString().length(); ) {
                if (i + 1024 < nameValueStringBuffer.toString().length()) {
                    Log.v("signa", nameValueStringBuffer.toString().substring(i, i + 1024));
                } else {
                    Log.v("signa", nameValueStringBuffer.toString().substring(i));
                }
                i = i + 1024;
            }

            Log.v("signa", "[[gtiSig]] sig raw : " + nameValueStringBuffer.toString());
        }

        String sig = InternetStringUtils.MD5Encode(nameValueStringBuffer.toString());
        return sig;

    }

}

