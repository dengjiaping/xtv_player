package com.kankan.player.api.rest.ads;

import com.plugin.internet.core.ResponseBase;
import com.plugin.internet.core.json.JsonProperty;

public class GetAdvImagesResponse extends ResponseBase {

    @JsonProperty("rtnCode")
    public int rtnCode;

    @JsonProperty("data")
    public AdvImage[] data;

    public static class AdvImage {
        public int id;

        @JsonProperty("images")
        public String images;

        @JsonProperty("orderVal")
        public int orderVal;
    }
}
