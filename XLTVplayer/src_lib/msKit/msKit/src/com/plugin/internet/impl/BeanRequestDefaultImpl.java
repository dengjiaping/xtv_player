package com.plugin.internet.impl;

import android.content.Context;
import android.os.Bundle;
import com.plugin.internet.interfaces.*;
import com.plugin.internet.util.Config;
import com.plugin.internet.util.JsonUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class BeanRequestDefaultImpl implements BeanRequestInterface {

    private static final String TAG = "BeanRequestImpl";
    private static final boolean DEBUG = Config.DEBUG;
    private static final boolean DEBUG_SERVER_CODE = false;

    private static final String KEY_METHOD = "method";

    private static final String KEY_HTTP_METHOD = "httpMethod";

    private static final String KEY_METHOD_EXT = "methodExt";

    private static BeanRequestDefaultImpl mInstance;

    private HttpClientInterface mHttpClientInterface;

    private HttpConnectHookListener mHttpHookListener;

    private static Object lockObject = new Object();

    public static BeanRequestDefaultImpl getInstance(Context context) {
        if (mInstance == null) {
            synchronized (lockObject) {
                if (mInstance == null) {
                    mInstance = new BeanRequestDefaultImpl(context);
                }
            }
        }
        return mInstance;
    }

    private BeanRequestDefaultImpl(Context context) {
        mHttpClientInterface = HttpClientFactory.createHttpClient(context);
    }

    @Override
    public <T> T request(RequestBase<T> request) throws NetWorkException {
        long entryTime = System.currentTimeMillis();
        if (DEBUG) {
            Config.LOGD("Entery Internet request, current time = " + entryTime + "ms from 1970");
        }

        if (request == null) {
            if (mHttpHookListener != null) {
                mHttpHookListener.onHttpConnectError(NetWorkException.REQUEST_NULL, "Request can't be NUll", request);
            }

            throw new NetWorkException(NetWorkException.REQUEST_NULL, "Request can't be NUll", null);
        }

        boolean ignore = request.canIgnoreResult();
        if (!mHttpClientInterface.isNetworkAvailable()) {
            if (!ignore && mHttpHookListener != null) {
                mHttpHookListener.onHttpConnectError(NetWorkException.NETWORK_NOT_AVILABLE, "网络连接错误，请检查您的网络", request);
            }

            throw new NetWorkException(NetWorkException.NETWORK_NOT_AVILABLE, "网络连接错误，请检查您的网络", null);
        }

        RequestEntity requestEntity = request.getRequestEntity();
        Bundle baseParams = requestEntity.getBasicParams();

        if (baseParams == null) {
            if (!ignore && mHttpHookListener != null) {
                mHttpHookListener.onHttpConnectError(NetWorkException.PARAM_EMPTY, "网络请求参数列表不能为空", request);
            }

            throw new NetWorkException(NetWorkException.PARAM_EMPTY, "网络请求参数列表不能为空", null);
        }

        String api_url = baseParams.getString(KEY_METHOD);
        baseParams.remove(KEY_METHOD);
        String httpMethod = baseParams.getString(KEY_HTTP_METHOD);
        baseParams.remove(KEY_HTTP_METHOD);
        if (baseParams.containsKey(KEY_METHOD_EXT)) {
            String ext = baseParams.getString(KEY_METHOD_EXT);
            api_url = api_url + ext;
            baseParams.remove(KEY_METHOD_EXT);
        }
        if (mHttpHookListener != null) {
            mHttpHookListener.onPreHttpConnect(api_url, api_url, baseParams);
        }

        String contentType = requestEntity.getContentType();
        if (contentType == null) {
            if (!ignore && mHttpHookListener != null) {
                mHttpHookListener.onHttpConnectError(NetWorkException.MISS_CONTENT_TYPE, "Content Type MUST be specified",
                                                        request);
            }

            throw new NetWorkException(NetWorkException.MISS_CONTENT_TYPE, "Content Type MUST be specified", null);
        }

        if (DEBUG) {
            StringBuilder param = new StringBuilder();
            if (baseParams != null) {
                for (String key : baseParams.keySet()) {
                    param.append("|    ").append(key).append(" : ").append(baseParams.get(key)).append("\n");
                }
            }

            Config.LOGD("\n\n//***\n| [[request::" + request + "]] \n" + "| RestAPI URL = " + api_url
                            + "\n| after getSig bundle params is = \n" + param + " \n\\\\***\n");
        }

        int size = 0;
        HttpEntity entity = null;
        if (contentType.equals(RequestEntity.REQUEST_CONTENT_TYPE_TEXT_PLAIN)) {
            if (httpMethod.equals("POST")) {
                List<NameValuePair> paramList = convertBundleToNVPair(baseParams);
                if (paramList != null) {
                    try {
                        entity = new UrlEncodedFormEntity(paramList, HTTP.UTF_8);
                    } catch (UnsupportedEncodingException e) {
                        if (!ignore && mHttpHookListener != null) {
                            mHttpHookListener.onHttpConnectError(NetWorkException.ENCODE_HTTP_PARAMS_ERROR,
                                                                    "Unable to encode http parameters", request);
                        }

                        throw new NetWorkException(NetWorkException.ENCODE_HTTP_PARAMS_ERROR,
                                                      "Unable to encode http parameters", null);
                    }
                }
            } else if (httpMethod.equals("GET")) {
                StringBuilder sb = new StringBuilder(api_url);
                sb.append("?");
                for (String key : baseParams.keySet()) {
                    sb.append(key).append("=").append(baseParams.getString(key)).append("&");
                }
                api_url = sb.substring(0, sb.length() - 1);
                if (DEBUG) {
                    Config.LOGD("\n\n//***\n| GET url : " + api_url + "\n\\\\***\n");
                }
            }
        } else if (contentType.equals(RequestEntity.REQUEST_CONTENT_TYPE_MUTIPART)) {
            requestEntity.setBasicParams(baseParams);
            entity = new MultipartHttpEntity(requestEntity);
        }

        if (DEBUG) {
            Config.LOGD("before get internet data from server, time cost from entry = "
                            + (System.currentTimeMillis() - entryTime) + "ms");
        }

        String response = mHttpClientInterface.getResource(String.class, api_url, httpMethod, entity);
        if (DEBUG_SERVER_CODE) {
            response = "{code:7,data:\"测试code7\"}";
        }

        if (DEBUG) {
            Config.LOGD(response);
            long endTime = System.currentTimeMillis();
            StringBuilder sb = new StringBuilder(1024);
            sb.append("\n\n")
                .append("//***\n")
                .append("| ------------- begin response ------------\n")
                .append("|\n")
                .append("| [[request::" + request + "]] " + " cost time from entry : " + (endTime - entryTime)
                            + "ms. " + "raw response String = \n");
            Config.LOGD(sb.toString());
            sb.setLength(0);
            if (response != null) {
                try {
                    sb.append("| " + response + " \n");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                sb.append("| " + response + "\n");
            }
            int step = 1024;
            int index = 0;
            do {
                if (index >= sb.length()) {
                    break;
                } else {
                    if ((index + step) < sb.length()) {
                        Config.LOGD(sb.substring(index, index + step));
                    } else {
                        Config.LOGD(sb.substring(index, sb.length()));
                    }
                }
                index = index + step;
            } while (index < sb.length());
            sb.setLength(0);
            sb.append("|\n|\n").append("| ------------- end response ------------\n").append("\\\\***");
            Config.LOGD(sb.toString());
        }

        if (mHttpHookListener != null) {
            mHttpHookListener.onPostHttpConnect(response, 200);
        }

        if (response == null) {
            if (!ignore && mHttpHookListener != null) {
                mHttpHookListener.onHttpConnectError(NetWorkException.SERVER_ERROR, "服务器错误，请稍后重试", request);
            }

            throw new NetWorkException(NetWorkException.SERVER_ERROR, "服务器错误，请稍后重试", null);
        }

        T ret = null;
        try {
            ret = JsonUtils.parse(response, request.getGenericType());
            if (DEBUG) {
                Config.LOGD("Before return, after success get the data from server, parse cost time from entry = "
                                + (System.currentTimeMillis() - entryTime) + "ms" + " response parse result = " + ret);
            }

            if (ret == null && mHttpHookListener != null) {
                mHttpHookListener.onHttpConnectError(NetWorkException.SERVER_RETURN_DATA_PARSE_ERROR, response, request);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ret;
    }

    private List<NameValuePair> convertBundleToNVPair(Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        ArrayList<NameValuePair> list = new ArrayList<NameValuePair>();
        Set<String> keySet = bundle.keySet();
        for (String key : keySet) {
            list.add(new BasicNameValuePair(key, bundle.getString(key)));
        }

        return list;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.plugin.internet.core.BeanRequestInterface#setHttpHookListener()
     */
    @Override
    public void setHttpHookListener(HttpConnectHookListener l) {
        mHttpHookListener = l;
    }

}
