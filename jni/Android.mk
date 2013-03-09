LOCAL_PATH:= $(call my-dir)
# APP_OPTIM := debug

include $(CLEAR_VARS)

# TRILIBDEFS = -DTRILIBRARY -DREDUCED -DCDT_ONLY
LOCAL_CFLAGS := -O -DTRILIBRARY -DREDUCED -DCDT_ONLY -DNO_TIMER -Werror -std=c99
# -DLINUX -> no fpu_control in bionic, needed ?

LOCAL_MODULE    := triangle-jni
LOCAL_SRC_FILES := TriangleJni.c triangle.c
LOCAL_LDLIBS    := -llog

include $(BUILD_SHARED_LIBRARY)
