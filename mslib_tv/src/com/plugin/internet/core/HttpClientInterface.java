package com.plugin.internet.core;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;

import java.util.List;

public interface HttpClientInterface {

    public <T> T getResource(Class<T> resourceType, String url, String method, HttpEntity entity)
            throws NetWorkException;

    public <T, V> V getResource(Class<T> inputResourceType, Class<V> retResourceType, String url, String method, HttpEntity entity)
            throws NetWorkException;

    public <T> T getResource(Class<T> resourceType, String url, String method, HttpEntity entity, List<NameValuePair> headers)
        throws NetWorkException;

    public <T, V> V getResource(Class<T> inputResourceType, Class<V> retResourceType, String url, String method, HttpEntity entity,
                                List<NameValuePair> headers)
        throws NetWorkException;

    public boolean isNetworkAvailable();

    public void setHttpReturnListener(HttpRequestHookListener l);
}
