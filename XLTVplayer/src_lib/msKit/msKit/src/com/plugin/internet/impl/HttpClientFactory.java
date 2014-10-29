package com.plugin.internet.impl;

import android.content.Context;
import com.plugin.internet.interfaces.HttpClientInterface;

public class HttpClientFactory {

	public static HttpClientInterface createHttpClient(Context context) {
		return HttpClientImpl.getInstance(context);
	}

}
