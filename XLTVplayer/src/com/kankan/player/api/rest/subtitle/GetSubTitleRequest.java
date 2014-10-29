package com.kankan.player.api.rest.subtitle;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import com.kankan.player.api.beanrequest.PMRequestBase;
import com.kankan.player.app.AppConfig;
import com.plugin.internet.core.NetWorkException;
import com.plugin.internet.core.RequestEntity;
import com.plugin.internet.core.annotations.*;

import java.lang.reflect.Field;

@NoNeedTicket
public class GetSubTitleRequest extends PMRequestBase<GetSubTitleResponse> {

    public String cid;

    public int videoLength;

    public String fileName;

    public GetSubTitleRequest() {
    }

    public GetSubTitleRequest(String cid, int videoLength, String fileName) {
        this.cid = cid;
        this.videoLength = videoLength;
        this.fileName = fileName;
    }

    @Override
    public String getMethodUrl() {
        if (TextUtils.isEmpty(cid)) {
            throw new RuntimeException("cid is invalid.");
        }

        StringBuilder sb = new StringBuilder("http://familycloud.subtitle.kankan.xunlei.com:8000/familycloud.query/smcid=");
        sb.append(cid);
        sb.append("&");
        sb.append("videolength=");
        sb.append(videoLength);
        sb.append("&");
        sb.append("smname=");
        sb.append(Uri.encode(fileName));
        AppConfig.LOGD("[[GetSubtitleRequest]] url = " + sb.toString());
        return sb.toString();
    }

    @Override
    public Bundle getParams() throws NetWorkException {
        Class<?> c = this.getClass();
        Field[] fields = c.getDeclaredFields();
        Bundle params = new Bundle();

        params.putString("method", getMethodUrl());
        params.putString("httpMethod", "GET");
        return params;
    }

    @Override
    public RequestEntity getRequestEntity() throws NetWorkException {
        return super.getRequestEntity();
    }
}
