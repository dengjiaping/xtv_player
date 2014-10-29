package com.kankan.player.item;

import com.plugin.internet.core.json.JsonProperty;

/**
 * Created by wangyong on 14-7-15.
 */
public class Device {

    @JsonProperty("deviceName")
    public String deviceName;

    @JsonProperty("boxName")
    public String boxName;

    @JsonProperty("remoteUrl")
    public String remoteUrl;

    @JsonProperty("isReleaseRemote")
    public boolean isReleaseRemote;

    @JsonProperty("debugLicense")
    public String debugLicense;

    @JsonProperty("releaseLicense")
    public String releaseLicense;

    @JsonProperty("partnerId")
    public String partnerId;

    @JsonProperty("remotePort")
    public String port = "9000";

    public Device() {

    }

    public Device(String deviceName,String boxName, String remoteUrl, boolean isReleaseRemote, String partnerId) {
        this.deviceName = deviceName;
        this.boxName = boxName;
        this.remoteUrl = remoteUrl;
        this.isReleaseRemote = isReleaseRemote;
        this.partnerId = partnerId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getBoxName() {
        return boxName;
    }

    public void setBoxName(String boxName) {
        this.boxName = boxName;
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public void setRemoteUrl(String remoteUrl) {
        this.remoteUrl = remoteUrl;
    }

    public boolean isReleaseRemote() {
        return isReleaseRemote;
    }

    public void setReleaseRemote(boolean isReleaseRemote) {
        this.isReleaseRemote = isReleaseRemote;
    }

    public String getDebugLicense() {
        return debugLicense;
    }

    public void setDebugLicense(String debugLicense) {
        this.debugLicense = debugLicense;
    }

    public String getReleaseLicense() {
        return releaseLicense;
    }

    public void setReleaseLicense(String releaseLicense) {
        this.releaseLicense = releaseLicense;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }
}
