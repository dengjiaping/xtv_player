package com.kankan.player.api.remote;

import com.kankan.player.api.beanrequest.PMRequestBase;
import com.kankan.player.item.Device;
import com.plugin.internet.core.ResponseBase;
import com.plugin.internet.core.json.JsonProperty;

/**
 * Created by wangyong on 14-7-16.
 */
public class GetVenderInfoResponse extends ResponseBase{

    @JsonProperty("rtnCode")
    public int rtnCode;

    @JsonProperty("data")
    public Device data;
}
