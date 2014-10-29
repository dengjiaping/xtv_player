package com.kankan.player.api.remote;

import com.kankan.player.api.beanrequest.PMRequestBase;
import com.plugin.internet.core.annotations.HttpMethod;
import com.plugin.internet.core.annotations.NoNeedTicket;
import com.plugin.internet.core.annotations.RequiredParam;
import com.plugin.internet.core.annotations.RestMethodUrl;

/**
 * Created by wangyong on 14-7-16.
 */
@HttpMethod("GET")
@NoNeedTicket
@RestMethodUrl("http://api.tv.n0808.com/vendorInfo")
public class GetVenderInfoRequest extends PMRequestBase<GetVenderInfoResponse> {

    @RequiredParam("partnerId")
    private String partnerId;

    public GetVenderInfoRequest(String partnerId) {
        this.partnerId = partnerId;
    }
}
