package com.kankan.player.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.kankan.player.video.server.HttpService;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by wangyong on 14-3-31.
 */
public class SettingManager {

    private static SettingManager mIstance = null;

    private Context mContext;

    private SharedPreferences.Editor mEditor;

    private SharedPreferences mSharedPreferences;

    public static SettingManager getInstance() {

        if (mIstance == null) {

            synchronized (SettingManager.class) {

                if (mIstance == null) {

                    mIstance = new SettingManager();
                }
            }


        }

        return mIstance;
    }

    private SettingManager() {

    }

    public void init(Context context) {
        if (mContext == null) {
            mContext = context.getApplicationContext();
        }

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        mEditor = mSharedPreferences.edit();
    }


    public String getTicket() {
        return null;
    }

    public String getSecretKey() {
        return null;
    }

    private static final String URL_SUFFIX = "url_suffix";

    public void setUrlSuffix(String suffix) {
        mEditor.putString(URL_SUFFIX, suffix).commit();
    }

    public String getUrlSuffix() {
        return mSharedPreferences.getString(URL_SUFFIX, "");
    }

    private static final String LAST_FETCH_TIME = "last_fetch_time";

    public void setLastFetchMessageTime(long time) {
        mEditor.putLong(LAST_FETCH_TIME, time).commit();
    }

    public long getLastFetchMessageTime() {
        return mSharedPreferences.getLong(LAST_FETCH_TIME, 0);
    }

    //是否第一次启动应用
    private static final String KEY_APP_FIRST_LAUNCH = "key_app_first_launch";

    public void setAppLaunched() {
        mEditor.putBoolean(KEY_APP_FIRST_LAUNCH, true).commit();
    }

    public boolean isAppFirstLaunch() {
        return mSharedPreferences.getBoolean(KEY_APP_FIRST_LAUNCH, false);
    }

    private static final String KEY_SMB_ENABLE = "key_ftp_enable";

    public void setSmbEnable(boolean enable) {
        mEditor.putBoolean(KEY_SMB_ENABLE, enable).commit();
    }

    public boolean isSmbEnable() {
        return mSharedPreferences.getBoolean(KEY_SMB_ENABLE, false);
    }

    private static final String KEY_SHOW_REMOTE_SUPPORT_DIALOG = "key_show_remote_support_dialog";

    public void setShowRemoteSupportDialog(boolean tag){
        mEditor.putBoolean(KEY_SHOW_REMOTE_SUPPORT_DIALOG, tag).commit();
    }

    public boolean isShowRemoteSupportDialog(){
        return mSharedPreferences.getBoolean(KEY_SHOW_REMOTE_SUPPORT_DIALOG,true);
    }

    private static final String KEY_PROCESS_KILLED = "key_process_killed";

    public void setProcessKilled(boolean killed) {
        mEditor.putBoolean(KEY_PROCESS_KILLED, killed).commit();
    }

    public boolean isProcessKilled() {
        return mSharedPreferences.getBoolean(KEY_PROCESS_KILLED, false);
    }

    private static final String KEY_ROUTER_NAME = "key_router_name";

    public void setRouterName(String name) {
        mEditor.putString(KEY_ROUTER_NAME, name).commit();
    }

    public String getRouterName() {
        return mSharedPreferences.getString(KEY_ROUTER_NAME, null);
    }

    private static final String KEY_VIDEO_SERVER_PORT = "key_video_server_port";

    public void setVideoServerPort(int port) {
        mEditor.putInt(KEY_VIDEO_SERVER_PORT, port).commit();
    }

    public int getVideoServerPort() {
        return mSharedPreferences.getInt(KEY_VIDEO_SERVER_PORT, HttpService.PORT_MIN);
    }

    private static final String KEY_VERSION_UPDATE = "key_version_update";

    public void setVersionUpdate(boolean tag){
        mEditor.putBoolean(KEY_VERSION_UPDATE,tag).commit();
    }

    public boolean getVersionUpdate(){
        return mSharedPreferences.getBoolean(KEY_VERSION_UPDATE,false);
    }

    private static final String KEY_VERSION_CODE = "key_version_code";

    public void setVersionCode(int code){
        mEditor.putInt(KEY_VERSION_CODE,code).commit();
    }

    public int getVersionCode(){
        return mSharedPreferences.getInt(KEY_VERSION_CODE,0);
    }

    private final String KEY_ROUTER_SMB_ROOTPATH = "key_router_smb_rootpath";

    public void setRouterSmbRootPath(String path){
        mEditor.putString(KEY_ROUTER_SMB_ROOTPATH,path).commit();
    }

    public String getRouterSmbRootPath(){
        return mSharedPreferences.getString(KEY_ROUTER_SMB_ROOTPATH,"");
    }

    private final String KEY_IS_SHOW_NOTIFY = "key_is_show_notify";

    public void setShowNotify(boolean tag){
        mEditor.putBoolean(KEY_IS_SHOW_NOTIFY,tag).commit();
    }

    public boolean isShowNotify(){
        return mSharedPreferences.getBoolean(KEY_IS_SHOW_NOTIFY,true);
    }

    private final String KEY_ADV_LAST_FETCH_TIME = "key_adv_last_fetch_time";

    public void setAdvLastFetchTime(long millis) {
        mEditor.putLong(KEY_ADV_LAST_FETCH_TIME, millis).commit();
    }

    public long getAdvLastFetchTime() {
        return mSharedPreferences.getLong(KEY_ADV_LAST_FETCH_TIME, 0);
    }

    private final String KEY_DEBUG_LICENSE = "key_debug_license";

    public void setDebugLicense(String license){
        mEditor.putString(KEY_DEBUG_LICENSE,license).commit();
    }

    public String getDebugLicense(){
        return mSharedPreferences.getString(KEY_DEBUG_LICENSE,"");
    }

    private final String KEY_RELEASE_LICENSE = "key_release_license";

    public void setReleaseLicense(String license){
        mEditor.putString(KEY_RELEASE_LICENSE,license).commit();
    }

    public String getReleaseLicense(){
        return mSharedPreferences.getString(KEY_RELEASE_LICENSE,"");
    }

    private final String KEY_PARTNER_ID = "key_partner_id";

    public void setPartnerId(String partnerId){
        mEditor.putString(KEY_PARTNER_ID,partnerId).commit();
    }

    public String getPartnerId(){
        return mSharedPreferences.getString(KEY_PARTNER_ID,"");
    }

    private final String KEY_LICENSE = "key_license";

    public void setLicense(String license){
        mEditor.putString(KEY_LICENSE,license).commit();
    }

    public String getLicense(){
        return mSharedPreferences.getString(KEY_LICENSE,"");
    }

    private final String KEY_NTFS_TYPE = "key_ntfs_type";

    public void setNtfsType(String type){
        mEditor.putString(KEY_NTFS_TYPE,type).commit();
    }

    public String getNtfsType(){
        return mSharedPreferences.getString(KEY_NTFS_TYPE,"0");
    }
}
