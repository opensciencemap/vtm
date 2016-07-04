LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
 
LOCAL_MODULE    := vtm-jni
LOCAL_C_INCLUDES := . libtess2/Include 
 
LOCAL_CFLAGS := $(LOCAL_C_INCLUDES:%=-I%) -O2 -Wall -D__ANDROID__ -Wall -std=c99 -O2 -ffast-math -DNDEBUG
LOCAL_CPPFLAGS := $(LOCAL_C_INCLUDES:%=-I%) -O2 -Wall -D__ANDROID__ -Wall -std=c99 -O2 -ffast-math -DNDEBUG
LOCAL_LDLIBS := -lm -llog
LOCAL_ARM_MODE  := arm
 
LOCAL_SRC_FILES := org.oscim.utils.TessJNI.cpp\
	libtess2/Source/sweep.c\
	libtess2/Source/priorityq.c\
	libtess2/Source/bucketalloc.c\
	libtess2/Source/geom.c\
	libtess2/Source/tess.c\
	libtess2/Source/dict.c\
	libtess2/Source/mesh.c\
	gl/utils.c
 
include $(BUILD_SHARED_LIBRARY)
