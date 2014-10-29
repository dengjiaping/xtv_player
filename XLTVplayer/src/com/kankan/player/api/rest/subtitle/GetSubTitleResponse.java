package com.kankan.player.api.rest.subtitle;

import com.kankan.player.subtitle.SubtitleType;
import com.plugin.internet.core.ResponseBase;
import com.plugin.internet.core.json.JsonProperty;

import java.util.List;

public class GetSubTitleResponse extends ResponseBase {

    @JsonProperty("autoload")
    public int autoload = 1;

    @JsonProperty("sublist")
    public List<OnlineSubtitle> sublist;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (OnlineSubtitle onlineSubtitle : sublist) {
            sb.append(onlineSubtitle.toString());
        }
        return super.toString() + sb.toString();
    }

    public static class OnlineSubtitle implements Comparable<OnlineSubtitle> {

        @JsonProperty("scid")
        public String scid;

        @JsonProperty("sname")
        public String sname;

        @JsonProperty("language")
        public String language;

        @JsonProperty("rate")
        public int rate;

        @JsonProperty("display")
        public String display;

        @JsonProperty("surl")
        public String surl;

        @JsonProperty("svote")
        public int svote;

        @JsonProperty("roffset")
        public Long roffset;

        public int type = SubtitleType.ONLINE.ordinal();

        public OnlineSubtitle() {

        }

        @Override
        public String toString() {
            return super.toString()
                    + "scid :" + scid + "\n"
                    + "sanme :" + sname + "\n"
                    + "language :" + language + "\n"
                    + "rate :" + rate + "\n"
                    + "display :" + display + "\n"
                    + "surl :" + surl + "\n"
                    + "svote :" + svote + "\n"
                    + "roffset :" + roffset +"\n";
        }

        @Override
        public int compareTo(OnlineSubtitle another) {
            if (this.svote > another.svote) {
                return -1;
            } else if (this.svote == another.svote) {
                return 0;
            } else {
                return 1;
            }
        }
    }
}
