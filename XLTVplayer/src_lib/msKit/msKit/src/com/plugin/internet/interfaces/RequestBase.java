/**
 * Copyright 2011-2012 Renren Inc. All rights reserved.
 * － Powered by Team Pegasus. －
 */

package com.plugin.internet.interfaces;

import android.os.Bundle;
import android.text.TextUtils;
import com.plugin.internet.annotations.request.*;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 
 * All Requests inherit from this MUST add Annotation (either
 * {@link RequiredParam} or {@link OptionalParam}) to their declared fields that
 * should be send to the REST server.
 * 
 * Note : 1.Follow field should not be declared in Requests: api_key call_id sig
 * session_key format 2.REST version is set to "1.0" by default,
 * 
 * @see RequiredParam
 * @see OptionalParam
 * 
 * @param <T>
 */
public abstract class RequestBase<T> {

    private RequestEntity mRequestEntity;

    /**
     * 标记是否忽略返回信息
     */
    private boolean mIgnoreResult;

    /**
     * 是否底层自己处理错误信息
     */
    private boolean mHandleErrorSelf;

    public boolean canIgnoreResult() {
        return mIgnoreResult;
    }

    public boolean getHandleErrorSelf() {
        return mHandleErrorSelf;
    }

    public void setHandleErrorSelf(boolean self) {
        mHandleErrorSelf = self;
    }

    public void setIgnoreResult(boolean ignore) {
        mIgnoreResult = ignore;
    }

    public RequestEntity getRequestEntity() throws NetWorkException {
        if (mRequestEntity != null) {
            return mRequestEntity;
        }
        mRequestEntity = new RequestEntity();
        mRequestEntity.setBasicParams(getParams());
        mRequestEntity.setContentType(RequestEntity.REQUEST_CONTENT_TYPE_TEXT_PLAIN);
        return mRequestEntity;
    }

    protected String getMethodUrl() {
        Class<?> c = this.getClass();

        // Method name
        if (c.isAnnotationPresent(RestMethodUrl.class)) {
            RestMethodUrl restMethodName = c.getAnnotation(RestMethodUrl.class);
            return restMethodName.value();
        }

        return null;
    }

    public Bundle getParams() throws NetWorkException {
        Class<?> c = this.getClass();
        Field[] fields = c.getDeclaredFields();
        Bundle params = new Bundle();

        // Method name
        if (c.isAnnotationPresent(RestMethodUrl.class)) {
            RestMethodUrl restMethodName = c.getAnnotation(RestMethodUrl.class);
            String methodName = restMethodName.value();
            params.putString("method", methodName);
        } else {
            throw new RuntimeException("Method Name MUST be annotated!! :" + c.getName());
        }
    
        // Htpp Method name
        String methodString = "POST";
        if (c.isAnnotationPresent(HttpMethod.class)) {
            HttpMethod method = c.getAnnotation(HttpMethod.class);
            methodString = method.value();
        }
        if (TextUtils.isEmpty(methodString)) {
            throw new RuntimeException("Http Method Name Can not be annotated Empty!!");
        } else {
            if (methodString.toUpperCase().equals("GET")) {
                params.putString("httpMethod", "GET");
            } else if (methodString.toUpperCase().equals("POST")) {
                params.putString("httpMethod", "POST");
            } else {
                throw new RuntimeException("Http Method Name Must be annotated POST or GET!!");
            }
        }

        for (Field field : fields) {
            try {
                field.setAccessible(true);
                boolean hasDefault = false;
                long defaultValue = -1;
                if (field.isAnnotationPresent(IgnoreValue.class)) {
                    IgnoreValue value = field.getAnnotation(IgnoreValue.class);
                    if (value != null) {
                        hasDefault = true;
                        defaultValue = value.value();
                    }
                }
                
                if (field.isAnnotationPresent(RequiredParam.class)) {
                    RequiredParam requiredParam = field.getAnnotation(RequiredParam.class);
                    if (requiredParam != null) {
                        String name = requiredParam.value();
                        Object object = field.get(this);
                        if (object == null) {
                            throw new NetWorkException("Param " + name + " MUST NOT be null");
                        }
                        String value = String.valueOf(object);
                        if (TextUtils.isEmpty(value)) {
                            throw new NetWorkException("Param " + name + " MUST NOT be null");
                        }
                        params.putString(name, value);
                    }
                } else if (field.isAnnotationPresent(OptionalParam.class)) {
                    OptionalParam optionalParam = field.getAnnotation(OptionalParam.class);
                    if (optionalParam != null) {
                        String name = optionalParam.value();
                        Object object = field.get(this);
                        if (object != null) {
                            if (hasDefault) {
                                if (object instanceof Long) {
                                    long value = (Long)object;
                                    if (value != defaultValue) {
                                        params.putString(name, String.valueOf(value));
                                    }
                                }
                            } else {
                                String value = String.valueOf(object);
                                params.putString(name, value);
                            }
                        }
                    }
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return params;
    }

    /**
     * 获取T的类型
     * 
     * @param index
     * @return
     */
    @SuppressWarnings("unchecked")
    public Class<T> getGenericType() {
        Type genType = getClass().getGenericSuperclass();
        if (!(genType instanceof ParameterizedType)) {
            return null;
        }
        Type[] params = ((ParameterizedType) genType).getActualTypeArguments();
        if (params.length < 1) {
            throw new RuntimeException("Index outof bounds");
        }
        if (!(params[0] instanceof Class)) {
            return null;
        }
        return (Class<T>) params[0];
    }
}
