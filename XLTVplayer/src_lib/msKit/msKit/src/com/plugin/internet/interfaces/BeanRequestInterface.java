package com.plugin.internet.interfaces;

public interface BeanRequestInterface {

	public <T> T request(RequestBase<T> request) throws NetWorkException;

	public void setHttpHookListener(HttpConnectHookListener l);
}
