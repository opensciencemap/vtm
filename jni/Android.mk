LOCAL_PATH:= $(call my-dir)
# APP_OPTIM := debug

include $(CLEAR_VARS)

# TRILIBDEFS = -DTRILIBRARY -DREDUCED -DCDT_ONLY
LOCAL_CFLAGS := -O -DTRILIBRARY -DREDUCED -DCDT_ONLY -DNO_TIMER -Werror -std=c99
# -DLINUX -> no fpu_control in bionic, needed ?

LOCAL_MODULE    := triangle
LOCAL_SRC_FILES := triangle/TriangleJni.c triangle/triangle.c triangle/triangle_dbg.c
LOCAL_LDLIBS    := -llog

include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE    := glutils
#LOCAL_ARM_MODE := arm
LOCAL_CFLAGS    := -Werror -O2 -ffast-math -std=c99
LOCAL_SRC_FILES := gl/utils.c
LOCAL_LDLIBS    := -llog -lGLESv2

include $(BUILD_SHARED_LIBRARY)
