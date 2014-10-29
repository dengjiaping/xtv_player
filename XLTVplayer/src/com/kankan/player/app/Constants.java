package com.kankan.player.app;

public class Constants {
    public static final String KEY_VIDEO_ITEM = "key_video_item";
    public static final String KEY_VIDEO_PATH = "key_video_path";

    public static final String KEY_WHAT = "key_what";

    public static final String KEY_FROM_HISTORY = "key_from_history";

    public static final String KEY_IS_BIND = "key_is_bind";
    public static final String KEY_TDDOWNLOAD_LIST = "key_tddownload_list";
    public static final String KEY_TDDOWNLOAD_SUBITEM = "key_tddownload_subitem";
    public static final String KEY_THUNDER_USERNAME = "key_thunder_username";
    public static final String KEY_DEIVICE_NUMBER = "key_devices_number";

    public static final String KEY_AUDIO_LEFT = "key_audio_left";
    public static final String KEY_AUDIO_RIGHT = "key_audio_right";

    public static final String KEY_SUBTITLE_TYPE = "key_subtitle_type";

    public static final String CID_PREFIX = "cid:";


    /**
     * 视频设置常量
     */
    public static final int VIDEO_DISPLAY_AUTO_MODE = 0;
    public static final int VIDEO_DISPLAY_FULL_MODE = 1;
    public static final int VIDEO_AUDIO_LEFT_MODE = 0;
    public static final int VIDEO_AUDIO_RIGHT_MODE = 1;

    public static final String KEY_DISPLAY_MODE ="key_display_mode";
    public static final String KEY_AUDIO_MODE = "key_audio_mode";

    /**
     * 远程状态
     */
    public static final int KEY_REMOTE_BIND_SUCESS = 1;
    public static final int KEY_REMOTE_BIND_FAILED = 0;
    public static final int KEY_REMOTE_BIND_ERROR = -1;
    public static final int KEY_REMOTE_NETWORK_ERR = -2;
    public static final int KEY_REMOTE_CONNECT_SERVER = -3;
    public static final int KEY_REMOTE_DISK_SUCESS = 1;
    public static final int KEY_REMOTE_DISK_FAIL = 0;

    public static final String KEY_REMOTE_STATUS = "KEY_REMOTE_STATUS";

    public static final String KEY_GUIDE_TYPE = "key_guide_type";

    public static final String KEY_NEED_POP_DIALOG = "key_need_pop_dialog";

    public static final String KEY_FRAGMENT_WEB_BIND = "Key_fragment_web_bind";
    public static final String KEY_FRAGMENT_MOBILE_BIND = "Key_fragment_mobile_bind";

    public static final String KEY_FRAGMENT_WEB_DOWNLOAD = "Key_fragment_web_download";
    public static final String KEY_FRAGMENT_MOBILE_DOWNLOAD = "Key_fragment_mobile_download";

    public static final String KEY_FRAGMENT_DOWNLIST_SUB = "key_fragment_downlist_sub";

    public static final String KEY_FRAGMENT_BIND_ENTRY = "key_fragment_bind_entry";

    public static final String KEY_FRAGMENT_ENTRY_GUIDE = "key_fragment_entry_guide";

    public static final int KEY_FRAGMENT_ENTRY_DOWNLOAD = 1;
    public static final int KEY_FRAGMENT_ENTRY_DISK = 2;

    public static final int KEY_REMOTE_FILE_TYPE_DIR = 1;
    public static final int KEY_REMOTE_FILE_TYPE_VIDEO = 2;

    public static final String KEY_SUBLIST_FRAGMENT = "key_sublist_fragment";
    public static final String KEY_SUBLIST_TITLE = "key_sublist_title";

    /**
     * 是否现实菜单栏的如何下载
     */
    public static final String KEY_SHOW_GUIDE_DOWNLOAD = "key_show_guide_download";
    public static final String KEY_SHOW_BIND = "key_show_bind";
    public static final String KEY_SHOW_GUIDE = "key_show_guide";
    public static final int KEY_SHOW_WEB_GUIDE = 1;
    public static final int KEY_SHOW_MOBILE_GUIDE = 2;

    /**
     * 兼容既有远程服务的情况下区别是本地远程服务还是路由器远程服务
     */
    public static final String KEY_REMOTE_TYPE = "key_remote_type";
    public static final int KEY_REMOTE_LOCAL = 5;
    public static final int KEY_REMOTE_ROUTER = 6;

    /**
     * 这个时间为2000-01-01-00-00-00-000，如果当前取到的系统时间小于此时间，我们认为时间是异常的
     */
    public static final long SYSTEM_TIME_BASE = 946656000000L;

    /**
     * 限速
     */
    public static final String KEY_DISC_FORMAT_FAT = "fat";

    public static final int KEY_DISC_FAT_DOWNLOAD_SPEED = 90;
    public static final int KEY_DISC_FAT_UPLOAD_SPEED = -1;
    public static final int KEY_DISC_DOWNLOAD_SPEED_UNLIMITED = -1;


    /**
     * 从279版本开始，远程支持abs_path
     */
    public static final int KEY_REMOTE_VERSION = 279;

    /**
     * 远程下载任务状态，这个字段是我试出来的，远程未确认
     */
    public static final int TDTASK_STATUS_DOWNLOADING = 1;


    /**
     * 远程页面actionbar title
     */
    public static final String KEY_PAGE_TTILE = "key_page_title";

    /**
     * 远程帮助页面url
     */
    public static final String KEY_REMOTE_HELP_URL = "key_remote_help_url";

}
