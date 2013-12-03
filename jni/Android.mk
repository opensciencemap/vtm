LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
 
LOCAL_MODULE    := vtm-jni
LOCAL_C_INCLUDES := . 
 
LOCAL_CFLAGS := $(LOCAL_C_INCLUDES:%=-I%) -O2 -Wall -D__ANDROID__ -Wall -std=c99 -O2 -ffast-math
LOCAL_CPPFLAGS := $(LOCAL_C_INCLUDES:%=-I%) -O2 -Wall -D__ANDROID__ -Wall -std=c99 -O2 -ffast-math
LOCAL_LDLIBS := -lm -llog
LOCAL_ARM_MODE  := arm
 
LOCAL_SRC_FILES := gl/utils.c\
	tessellate/TessellateJni.c\
	tessellate/render.c\
	tessellate/memalloc.c\
	tessellate/geom.c\
	tessellate/dict.c\
	tessellate/tessmono.c\
	tessellate/normal.c\
	tessellate/tessellate.c\
	tessellate/mesh.c\
	tessellate/tess.c\
	tessellate/priorityq.c\
	tessellate/sweep.c
 
include $(BUILD_SHARED_LIBRARY)
