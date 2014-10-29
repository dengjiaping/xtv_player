/**
 * DeviceInfo.java
 */
package com.plugin.common.utils;

import java.io.File;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;

/**
 * @author Guoqing Sun Mar 26, 20135:49:24 PM
 */
public class DeviceInfo {
	
	public final int densityDpi;

	public final float density;

	public final int image_cache_size;

	public final int mem_size;

	public final int screenWidth;

	public final int screenHeight;

	public final String flashDataPath;

	public final int sdkTarget;
	
	public final int versionCode;

	public DeviceInfo(Context context) {
		densityDpi = context.getResources().getDisplayMetrics().densityDpi;
		density = context.getResources().getDisplayMetrics().density;
		int memClass = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
		mem_size = memClass;
		image_cache_size = 1024 * 1024 * memClass / 8;

		screenWidth = context.getResources().getDisplayMetrics().widthPixels;
		screenHeight = context.getResources().getDisplayMetrics().heightPixels;

		File filesDir = context.getDir("audio_download", Context.MODE_WORLD_WRITEABLE | Context.MODE_WORLD_READABLE);
		if (filesDir.exists()) {
			flashDataPath = filesDir.getAbsolutePath() + "/";
		} else {
			throw new RuntimeException("can't make audio_download folder");
		}

		sdkTarget = Build.VERSION.SDK_INT;
		
		versionCode = UtilsRuntime.getVersionCode(context);
	}

	@Override
	public String toString() {
		return "DeviceInfo [densityDpi=" + densityDpi + ", density=" + density + ", image_cache_size="
				+ image_cache_size + ", mem_size=" + mem_size + ", screenWidth=" + screenWidth + ", screenHeight="
				+ screenHeight + ", flashDataPath=" + flashDataPath + ", sdkTarget=" + sdkTarget + "]";
	}
}
