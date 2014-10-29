package com.kankan.player.api.tddownload;

/**
 * Created by zhangdi on 14-4-2.
 */
public class SysInfo {

    public int result;

    public int isNetOk;

    public int isLicenseOk;

    public int isBindOk;

    public String bindAcktiveKey;

    public int isDiskOk;

    public String version;

    public String userName;

    public int isEverBinded;

    public int userId;

    public int vipLevel;

    @Override
    public String toString() {
        return "SysInfo{" +
                "result=" + result +
                ", isNetOk=" + isNetOk +
                ", isLicenseOk=" + isLicenseOk +
                ", isBindOk=" + isBindOk +
                ", bindAcktiveKey='" + bindAcktiveKey + '\'' +
                ", isDiskOk=" + isDiskOk +
                ", version='" + version + '\'' +
                ", userName='" + userName + '\'' +
                ", isEverBinded=" + isEverBinded +
                ", userId=" + userId +
                ", vipLevel=" + vipLevel +
                '}';
    }
}
