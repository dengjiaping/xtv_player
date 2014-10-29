/**
 * UtilsRuntime.java
 */
package com.plugin.common.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Parcelable;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * @author Guoqing Sun Feb 18, 20133:16:14 PM
 */
public final class UtilsRuntime {

	private static final String DEBUG_DATE_FORMAT = "MM-dd HH:mm:ss:SSS";

	public static String debugFormatTime(long time) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(DEBUG_DATE_FORMAT);
		return dateFormat.format(time);
	}

	public static void createShortCut(Activity act, int iconResId,
			int appnameResId) {
		Intent shortcutintent = new Intent(
				"com.android.launcher.action.INSTALL_SHORTCUT");
		// 涓����璁搁��澶����寤�
		shortcutintent.putExtra("duplicate", false);
		// ���瑕���板��������绉�
		shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_NAME,
				act.getString(appnameResId));
		// 蹇���峰�剧��
		Parcelable icon = Intent.ShortcutIconResource.fromContext(
				act.getApplicationContext(), iconResId);
		shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);
		// ��瑰�诲揩��峰�剧��锛�杩�琛����绋�搴�涓诲�ュ��
		shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_INTENT,
				new Intent(act.getApplicationContext(), act.getClass()));
		// ������骞挎��
		act.sendBroadcast(shortcutintent);
	}

	// ��ゆ��灏���舵�����涓�24灏���跺��
	public static boolean isHourto24(Context context) {
		ContentResolver cr = context.getContentResolver();
		String strFormatTime = android.provider.Settings.System.getString(cr,
				android.provider.Settings.System.TIME_12_24);
		if (null != strFormatTime && strFormatTime.equals("24")) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean isOnline(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = (cm != null) ? cm.getActiveNetworkInfo() : null;
		if (info != null && info.isAvailable() && info.isConnected()) {
			return true;
		}

		return false;
	}

	public static boolean isWifiOn(Context c) {
		boolean bRet = false;
		ConnectivityManager cm = (ConnectivityManager) c
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo wifiInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (null != wifiInfo && wifiInfo.isAvailable()
				&& wifiInfo.isConnected()) {
			bRet = true;
		}
		return bRet;
	}

	public static String getCurrentStackMethodName() {
		String method = "";
		StackTraceElement ste = Thread.currentThread().getStackTrace()[0];        
		String invokeMethodName = ste.getMethodName();
		String fileName = ste.getFileName();
		long line = ste.getLineNumber();
		if (!TextUtils.isEmpty(invokeMethodName)) {
			method = fileName + "::" + invokeMethodName + "::" + line;
		}

		return method;
	}

	public static String getPackageName(Context context) {
		try {
			PackageInfo info = context.getPackageManager().getPackageInfo(
					context.getPackageName(), 0);
			return info.packageName; // ������
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	public static String getIMSI(Context context) {
		TelephonyManager mTelephonyMgr = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		String imsi = mTelephonyMgr.getSubscriberId();

		return imsi;
	}

	public static String getIMEI(Context context) {
		TelephonyManager mTelephonyMgr = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		String imei = mTelephonyMgr.getDeviceId();
		return imei;
	}

	public static boolean isSDCardReady() {
		return Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED);
	}
	
	public static boolean isExternalStorageAvailable() {
		String state = Environment.getExternalStorageState();
		boolean externalStorageAvailable = false;
		boolean externalStorageWriteable = false;

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// We can read and write the media
			externalStorageAvailable = externalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// We can only read the media
			externalStorageAvailable = true;
			externalStorageWriteable = false;
		} else {
			// Something else is wrong. It may be one of many other states, but
			// all we need
			// to know is we can neither read nor write
			externalStorageAvailable = externalStorageWriteable = false;
		}

		if (externalStorageAvailable == true
				&& externalStorageWriteable == true) {
			File sdcard = Environment.getExternalStorageDirectory();
			return sdcard == null ? false : (sdcard.canWrite() ? true : false);
		} else {
			return false;
		}
	}


	public static String getLocalMacAddress(Context context) {
		WifiManager wifi = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		WifiInfo info = wifi.getConnectionInfo();
		if (info != null) {
			return info.getMacAddress();
		}

		return null;
	}

	public static String getVersionName(Context context) {
		try {
			// ��峰��packagemanager���瀹�渚�
			PackageManager packageManager = context.getPackageManager();
			// getPackageName()���浣�褰����绫荤��������锛�0浠ｈ〃�����峰��������淇℃��
			PackageInfo packInfo = packageManager.getPackageInfo(
					context.getPackageName(), 0);
			String version = packInfo.versionName;
			return version;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "0.0";
	}

	public static int getVersionCode(Context context) {
		int versionCode = 0;
		// String versionName=null;
		try {
			PackageInfo info = context.getPackageManager().getPackageInfo(
					context.getPackageName(), 0);
			versionCode = info.versionCode;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return versionCode;
	}

	public static String getCurProcessName(Context context) {
		int pid = android.os.Process.myPid();
		ActivityManager activityManager = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningAppProcessInfo> appProcesses = activityManager
				.getRunningAppProcesses();
		if (appProcesses == null) {
			return null;
		}
		for (RunningAppProcessInfo appProcess : appProcesses) {
			if (appProcess == null) {
				continue;
			}
			if (appProcess.pid == pid) {
				return appProcess.processName;
			}
		}
		return null;
	}

	/**
	 * ��峰�������虹�佃����风��
	 * 
	 * @param context
	 * @return
	 */
	public static String getCurrentPhoneNumber(Context context) {
		TelephonyManager mTelephonyMgr = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		return mTelephonyMgr.getLine1Number();
	}

	/**
	 * ��峰��灞�骞�size
	 * 
	 * @param context
	 * @return
	 */
	public static String getDisplaySize(Context context) {
		int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
		int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
		return screenHeight + "*" + screenWidth;
	}

	/**
	 * ��峰��搴���ㄥ����版��
	 * 
	 * @param context
	 * @param key
	 * @return
	 */
	public static String getMetadata(Context context, String key) {
		ApplicationInfo appInfo;
		try {
			appInfo = context.getPackageManager().getApplicationInfo(
					context.getPackageName(), PackageManager.GET_META_DATA);
		} catch (NameNotFoundException e) {
			return "";
		}
		String value = appInfo == null ? "" : (appInfo.metaData == null ? ""
				: String.valueOf(appInfo.metaData.get(key)));
		return value;
	}
}
