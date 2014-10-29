package com.kankan.player.api.rest.ads;

import com.plugin.internet.core.RequestBase;
import com.plugin.internet.core.annotations.HttpMethod;
import com.plugin.internet.core.annotations.NoNeedTicket;
import com.plugin.internet.core.annotations.RequiredParam;
import com.plugin.internet.core.annotations.RestMethodUrl;

@HttpMethod("GET")
@NoNeedTicket
@RestMethodUrl("http://api.tv.n0808.com/advImages")
public class GetAdvImagesRequest extends RequestBase<GetAdvImagesResponse> {

    @RequiredParam("resolution")
    public int resolution;

}
