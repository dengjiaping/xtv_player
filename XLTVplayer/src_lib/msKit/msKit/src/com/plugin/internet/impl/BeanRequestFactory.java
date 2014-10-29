package com.plugin.internet.impl;

import android.content.Context;
import com.plugin.internet.interfaces.BeanRequestInterface;

public class BeanRequestFactory {

	private static BeanRequestInterface gBeanRequestInterface;

	public synchronized static BeanRequestInterface createBeanRequest(Context context) {
		if (gBeanRequestInterface == null) {
			gBeanRequestInterface = BeanRequestDefaultImpl.getInstance(context);
		}

		return gBeanRequestInterface;
	}

	public synchronized static void setgBeanRequestInterfaceImpl(BeanRequestInterface impl) {
		gBeanRequestInterface = impl;
	}
}
