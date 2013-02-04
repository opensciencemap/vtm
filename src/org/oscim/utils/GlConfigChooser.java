package org.oscim.utils;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;

import android.opengl.GLSurfaceView;
import android.util.Log;

/**
 *
 *
 */
public class GlConfigChooser implements GLSurfaceView.EGLConfigChooser {
	static private final String TAG = "ConfigChooser";

	/**
	 *
	 */
	public static int stencilSize = 0;

	@Override
	public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
		mValue = new int[1];

		// Try to find a normal multisample configuration first.
		int[] configSpec = {
				EGL10.EGL_RED_SIZE, 5,
				EGL10.EGL_GREEN_SIZE, 6,
				EGL10.EGL_BLUE_SIZE, 5,
				EGL10.EGL_ALPHA_SIZE, 8,
				EGL10.EGL_DEPTH_SIZE, 16,
				// Requires that setEGLContextClientVersion(2) is called on the view.
				EGL10.EGL_RENDERABLE_TYPE, 4 /* EGL_OPENGL_ES2_BIT */,
				EGL10.EGL_STENCIL_SIZE, 8,
				EGL10.EGL_NONE };

		if (!egl.eglChooseConfig(display, configSpec, null, 0, mValue)) {
			throw new IllegalArgumentException("eglChooseConfig failed");
		}
		int numConfigs = mValue[0];

		if (numConfigs <= 0) {
			stencilSize = 4;

			configSpec = new int[] {
					// EGL10.EGL_RENDERABLE_TYPE, 4, EGL10.EGL_NONE };
					EGL10.EGL_RED_SIZE, 8,
					EGL10.EGL_GREEN_SIZE, 8,
					EGL10.EGL_BLUE_SIZE, 8,
					EGL10.EGL_ALPHA_SIZE, 8,
					EGL10.EGL_DEPTH_SIZE, 16,
					EGL10.EGL_RENDERABLE_TYPE, 4 /* EGL_OPENGL_ES2_BIT */,
					EGL10.EGL_STENCIL_SIZE, 8,
					EGL10.EGL_NONE };

			if (!egl.eglChooseConfig(display, configSpec, null, 0, mValue)) {
				throw new IllegalArgumentException("eglChooseConfig failed");
			}
			numConfigs = mValue[0];

			if (numConfigs <= 0) {
				throw new IllegalArgumentException("No configs match configSpec");
			}
		} else {
			stencilSize = 8;
		}

		// Get all matching configurations.
		EGLConfig[] configs = new EGLConfig[numConfigs];
		if (!egl.eglChooseConfig(display, configSpec, configs, numConfigs, mValue)) {
			throw new IllegalArgumentException("data eglChooseConfig failed");
		}

		// CAUTION! eglChooseConfigs returns configs with higher bit depth
		// first: Even though we asked for rgb565 configurations, rgb888
		// configurations are considered to be "better" and returned first.
		// You need to explicitly filter the data returned by eglChooseConfig!

		// for (int i = 0; i < configs.length; ++i) {
		// Log.i(TAG, printConfig(egl, display, configs[i]));
		// }

		// int index = -1;
		// for (int i = 0; i < configs.length; ++i) {
		// // if (findConfigAttrib(egl, display, configs[i], EGL10.EGL_RED_SIZE, 0) == 8
		// // &&
		// // findConfigAttrib(egl, display, configs[i], EGL10.EGL_ALPHA_SIZE, 0) == 0) {
		// // index = i;
		// // break;
		// // }
		// // else
		// if (findConfigAttrib(egl, display, configs[i], EGL10.EGL_RED_SIZE, 0) == 8
		// &&
		// findConfigAttrib(egl, display, configs[i], EGL10.EGL_ALPHA_SIZE, 0) == 0
		// &&
		// findConfigAttrib(egl, display, configs[i], EGL10.EGL_DEPTH_SIZE, 0) == 24) {
		// index = i;
		// break;
		// }
		// }
		// if (index == -1) {
		// Log.w(TAG, "Did not find sane config, using first");
		// index = 0;
		// }
		int index = 0;

		Log.i(TAG, "using: " + printConfig(egl, display, configs[index]));

		EGLConfig config = configs.length > 0 ? configs[index] : null;
		if (config == null) {
			throw new IllegalArgumentException("No config chosen");
		}
		return config;
	}

	// from quake2android
	private String printConfig(EGL10 egl, EGLDisplay display,
			EGLConfig config) {

		int r = findConfigAttrib(egl, display, config, EGL10.EGL_RED_SIZE, 0);
		int g = findConfigAttrib(egl, display, config, EGL10.EGL_GREEN_SIZE, 0);
		int b = findConfigAttrib(egl, display, config, EGL10.EGL_BLUE_SIZE, 0);
		int a = findConfigAttrib(egl, display, config, EGL10.EGL_ALPHA_SIZE, 0);
		int d = findConfigAttrib(egl, display, config, EGL10.EGL_DEPTH_SIZE, 0);
		int s = findConfigAttrib(egl, display, config, EGL10.EGL_STENCIL_SIZE, 0);

		/*
		 * EGL_CONFIG_CAVEAT value #define EGL_NONE 0x3038 #define
		 * EGL_SLOW_CONFIG 0x3050 #define
		 * EGL_NON_CONFORMANT_CONFIG 0x3051
		 */

		return String.format("EGLConfig rgba=%d%d%d%d depth=%d stencil=%d",
				Integer.valueOf(r), Integer.valueOf(g),
				Integer.valueOf(b), Integer.valueOf(a), Integer.valueOf(d),
				Integer.valueOf(s))
				+ " native="
				+ findConfigAttrib(egl, display, config, EGL10.EGL_NATIVE_RENDERABLE, 0)
				+ " buffer="
				+ findConfigAttrib(egl, display, config, EGL10.EGL_BUFFER_SIZE, 0)
				+ String.format(
						" caveat=0x%04x",
						Integer.valueOf(findConfigAttrib(egl, display, config,
								EGL10.EGL_CONFIG_CAVEAT, 0)));

	}

	private int findConfigAttrib(EGL10 egl, EGLDisplay display, EGLConfig config,
			int attribute, int defaultValue) {
		if (egl.eglGetConfigAttrib(display, config, attribute, mValue)) {
			return mValue[0];
		}
		return defaultValue;
	}

	private int[] mValue;

}
