package com.plugin.internet.interfaces;

public class NetWorkException extends Exception {

    //本地网络错误码全部为负数

    //网络不可用
    public static final int NETWORK_NOT_AVILABLE = -1;
    //服务器错误
    public static final int NETWORK_ERROR = -2;
    //网络请求参数错误
    public static final int PARAM_EMPTY = -3;
    //content type 为空
    public static final int MISS_CONTENT_TYPE = -4;
    //请求参数编辑错误
    public static final int ENCODE_HTTP_PARAMS_ERROR = -5;
    //请求为空
    public static final int REQUEST_NULL = -6;

    //服务器返回错误
    public static final int SERVER_ERROR = 10001;
    //服务器返回数据解码错误
    public static final int SERVER_RETURN_DATA_PARSE_ERROR = 10002;
    
    private static final long serialVersionUID = 1L;
    
    private int mExceptionCode;
    private String mDeveloperExceptionMsg;
    private String mUserExceptionMsg;
    
    public NetWorkException(String exceptionMsg) {
        super(exceptionMsg);
        mDeveloperExceptionMsg = exceptionMsg;
    }
    
    public NetWorkException(int code, String msg, String description) {
        super(msg);
        mExceptionCode = code;
        mDeveloperExceptionMsg = msg;
        mUserExceptionMsg = description;
    }
    
    public int getErrorCode() {
        return mExceptionCode;
    }
    
    public String getDeveloperExceptionMsg() {
        return mDeveloperExceptionMsg;
    }
    
    public String getUserExceptionMsg() {
        return mUserExceptionMsg;
    }
    
    @Override
    public String toString() {
        return "NetWorkException [mExceptionCode=" + mExceptionCode + ", mExceptionMsg=" + mDeveloperExceptionMsg
                + ", mExceptionDescription=" + mUserExceptionMsg + "]";
    }
    
}
