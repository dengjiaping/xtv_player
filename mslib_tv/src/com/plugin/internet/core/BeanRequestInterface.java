package com.plugin.internet.core;

import android.os.Bundle;

import java.util.Map;

public interface BeanRequestInterface {

	public <T> T request(RequestBase<T> request) throws NetWorkException;

	public String getSig(Bundle params, String secret_key);

	public void setRequestAdditionalKVInfo(Map<String, String> kvInfo);

	public void setHttpHookListener(HttpConnectHookListener l);
}
