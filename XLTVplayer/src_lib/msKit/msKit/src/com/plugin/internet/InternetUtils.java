package com.plugin.internet;

import java.io.InputStream;

import android.content.Context;

import com.plugin.internet.impl.BeanRequestFactory;
import com.plugin.internet.impl.HttpClientFactory;
import com.plugin.internet.interfaces.HttpConnectHookListener;
import com.plugin.internet.interfaces.HttpRequestHookListener;
import com.plugin.internet.interfaces.NetWorkException;
import com.plugin.internet.interfaces.RequestBase;

public final class InternetUtils {

	/**
	 * 同步接口 发送REST请求
	 * 
	 * @param <T>
	 * @param request
	 *            REST请求
	 * @return REST返回
	 * @throws NetWorkException
	 */
	public static <T> T request(Context context, RequestBase<T> request) throws NetWorkException {
		if (context != null && BeanRequestFactory.createBeanRequest(context.getApplicationContext()) != null) {
			return BeanRequestFactory.createBeanRequest(context.getApplicationContext()).request(request);
		}

		return null;
	}

	public static byte[] requestImageByte(Context context, String imageUrl) throws NetWorkException {
		if (context != null && HttpClientFactory.createHttpClient(context.getApplicationContext()) != null) {
			return HttpClientFactory.createHttpClient(context.getApplicationContext()).getResource(
					byte[].class, imageUrl, "GET", null);
		}

		return null;
	}

	/**
	 * 大文件下载接口
	 * 
	 * @param context
	 * @param imageUrl
	 * @return
	 * @throws NetWorkException
	 */
	public static String requestBigResourceWithCache(Context context, String imageUrl) throws NetWorkException {
		if (context != null && HttpClientFactory.createHttpClient(context.getApplicationContext()) != null) {
			return HttpClientFactory.createHttpClient(context.getApplicationContext()).getResource(
					InputStream.class, String.class, imageUrl, "GET", null);
		}

		return null;
	}

	public static void setHttpHookListener(Context context, HttpConnectHookListener l) {
		if (context != null && BeanRequestFactory.createBeanRequest(context.getApplicationContext()) != null) {
			BeanRequestFactory.createBeanRequest(context.getApplicationContext()).setHttpHookListener(l);
		}
	}

	public static void setHttpReturnListener(Context context, HttpRequestHookListener l) {
		if (context != null && HttpClientFactory.createHttpClient(context.getApplicationContext()) != null) {
			HttpClientFactory.createHttpClient(context.getApplicationContext()).setHttpReturnListener(l);
		}
	}

}
