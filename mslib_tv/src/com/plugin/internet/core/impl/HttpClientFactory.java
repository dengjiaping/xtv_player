package com.plugin.internet.core.impl;

import android.content.Context;
import com.plugin.internet.core.HttpClientInterface;

public class HttpClientFactory {

	public static HttpClientInterface createHttpClientInterface(Context context) {
		return HttpClientImpl.getInstance(context);
	}

}
