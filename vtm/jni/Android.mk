LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
 
LOCAL_MODULE    := vtm-jni
LOCAL_C_INCLUDES := . 
 
LOCAL_CFLAGS := $(LOCAL_C_INCLUDES:%=-I%) -O2 -Wall -D__ANDROID__ -Wall -std=c99 -O2 -DTRILIBRARY -DREDUCED -DCDT_ONLY -DNO_TIMER
LOCAL_CPPFLAGS := $(LOCAL_C_INCLUDES:%=-I%) -O2 -Wall -D__ANDROID__ -Wall -std=c99 -O2 -DTRILIBRARY -DREDUCED -DCDT_ONLY -DNO_TIMER
LOCAL_LDLIBS := -lm -llog
LOCAL_ARM_MODE  := arm
 
LOCAL_SRC_FILES := gl/utils.c\
	triangle/triangle_dbg.c\
	triangle/TriangleJni.c\
	triangle/triangle.c
 
include $(BUILD_SHARED_LIBRARY)
