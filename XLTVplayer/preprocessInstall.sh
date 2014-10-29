#!/bin/bash

#:第一个参数为device设备号

APK="ThunderTVPlayer-release.apk"
MEDIA_SO="libmediaplayer_xunlei_jni.so"
CID_SO="libxunleicid.so"

adb shell mount -o remount rw /system
apkFile=`adb shell ls '/system/app/' | grep ThunderTVPlayer`
adb shell rm "/system/app/$apkFile"
adb shell rm "/system/lib/$CID_SO"
adb shell rm "/system/lib/$MEDIA_SO"

adb push "build/apk/$APK" '/system/app/'
adb shell chmod 777 "/system/app/$APK"
echo 'success upload and change mod of apk file.'

adb push "libs/armeabi/$MEDIA_SO" '/system/lib/'
adb shell chmod 777 "/system/lib/$MEDIA_SO"
echo 'success upload and change mod of mediaplayer so file.'

adb push "libs/armeabi/$CID_SO" '/system/lib/'
adb shell chmod 777 "/system/lib/$CID_SO"
echo 'success upload and change mod of cid so file.'

adb shell rm -r '/data/data/com.xunlei.tv.player'
echo 'success remove user data directory.'

adb reboot
