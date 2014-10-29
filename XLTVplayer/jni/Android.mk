LOCAL_PATH:=$(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := xunleicid
LOCAL_SRC_FILES := xunleicid.cpp sha1.cpp
LOCAL_LDLIBS += -llog
include $(BUILD_SHARED_LIBRARY)

#################libmediaplyer.so##################
include $(CLEAR_VARS)
LOCAL_MODULE := mediaplayer_xunlei_jni
LOCAL_SRC_FILES := libprebuilt/libmediaplayer_xunlei_jni.so
include $(PREBUILT_SHARED_LIBRARY)

#################libbspatch.so##################
include $(CLEAR_VARS)
LOCAL_MODULE := bspatch
LOCAL_SRC_FILES := libprebuilt/libbspatch.so
include $(PREBUILT_SHARED_LIBRARY)
