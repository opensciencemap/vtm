/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.oscim.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.oscim.backend.GL20;
import org.oscim.backend.GLAdapter;
import org.oscim.backend.Log;
import org.oscim.renderer.GLRenderer;

/**
 * Utility functions
 */
public class GlUtils {
	private static GL20 GL = GLAdapter.get();

	// public static native void setColor(int location, int color, float alpha);
	// public static native void setColorBlend(int location, int color1, int
	// color2, float mix);

	public static void setColor(int location, int color, float alpha) {
		GL = GLAdapter.get();
		if (alpha >= 1)
			alpha = ((color >>> 24) & 0xff) / 255f;
		else if (alpha < 0)
			alpha = 0;
		else
			alpha *= ((color >>> 24) & 0xff) / 255f;

		if (alpha == 1) {
			GL.glUniform4f(location,
			               ((color >>> 16) & 0xff) / 255f,
			               ((color >>> 8) & 0xff) / 255f,
			               ((color >>> 0) & 0xff) / 255f,
			               alpha);
		} else {
			GL.glUniform4f(location,
			               ((color >>> 16) & 0xff) / 255f * alpha,
			               ((color >>> 8) & 0xff) / 255f * alpha,
			               ((color >>> 0) & 0xff) / 255f * alpha,
			               alpha);
		}
	}

	public static void setColorBlend(int location, int color1, int color2, float mix) {
		float a1 = (((color1 >>> 24) & 0xff) / 255f) * (1 - mix);
		float a2 = (((color2 >>> 24) & 0xff) / 255f) * mix;
		GL = GLAdapter.get();
		GL.glUniform4f
		        (location,
		         ((((color1 >>> 16) & 0xff) / 255f) * a1 + (((color2 >>> 16) & 0xff) / 255f) * a2),
		         ((((color1 >>> 8) & 0xff) / 255f) * a1 + (((color2 >>> 8) & 0xff) / 255f) * a2),
		         ((((color1 >>> 0) & 0xff) / 255f) * a1 + (((color2 >>> 0) & 0xff) / 255f) * a2),
		         (a1 + a2));
	}

	private static String TAG = "GlUtils";

	public static void setTextureParameter(int min_filter, int mag_filter, int wrap_s, int wrap_t) {
		GL = GLAdapter.get();
		GL.glTexParameterf(GL20.GL_TEXTURE_2D,
		                   GL20.GL_TEXTURE_MIN_FILTER,
		                   min_filter);
		GL.glTexParameterf(GL20.GL_TEXTURE_2D,
		                   GL20.GL_TEXTURE_MAG_FILTER,
		                   mag_filter);
		GL.glTexParameterf(GL20.GL_TEXTURE_2D,
		                   GL20.GL_TEXTURE_WRAP_S,
		                   wrap_s); // Set U Wrapping
		GL.glTexParameterf(GL20.GL_TEXTURE_2D,
		                   GL20.GL_TEXTURE_WRAP_T,
		                   wrap_t); // Set V Wrapping
	}

	// /**
	// * @param bitmap
	// * ...
	// * @return textureId
	// */
	// public static int loadTextures(Bitmap bitmap) {
	//
	// int[] textures = new int[1];
	// GLES20.glGenTextures(1, textures, 0);
	//
	// int textureID = textures[0];
	//
	// GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureID);
	//
	// setTextureParameter(GLES20.GL_LINEAR, GLES20.GL_LINEAR,
	// GLES20.GL_CLAMP_TO_EDGE, GLES20.GL_CLAMP_TO_EDGE);
	//
	// GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
	//
	// return textureID;
	// }

	public static int loadTexture(byte[] pixel, int width, int height, int format,
	                              int min_filter, int mag_filter, int wrap_s, int wrap_t) {
		int[] textureIds = GlUtils.glGenTextures(1);
		GL = GLAdapter.get();
		GL.glBindTexture(GL20.GL_TEXTURE_2D, textureIds[0]);

		setTextureParameter(min_filter, mag_filter, wrap_s, wrap_t);

		ByteBuffer buf = ByteBuffer.allocateDirect(width * height).order(ByteOrder.nativeOrder());
		buf.put(pixel);
		buf.position(0);
		IntBuffer intBuf = buf.asIntBuffer();
		GL.glTexImage2D(GL20.GL_TEXTURE_2D, 0, format, width, height, 0, format,
		                GL20.GL_UNSIGNED_BYTE, intBuf);

		GL.glBindTexture(GL20.GL_TEXTURE_2D, 0);
		return textureIds[0];
	}

	public static int loadStippleTexture(byte[] stipple) {
		GL = GLAdapter.get();

		int sum = 0;
		for (byte flip : stipple)
			sum += flip;

		byte[] pixel = new byte[sum];

		boolean on = true;
		int pos = 0;
		for (byte flip : stipple) {
			float max = flip;

			for (int s = 0; s < flip; s++) {
				float color = Math.abs(s / (max - 1) - 0.5f);
				if (on)
					color = 255 * (1 - color);
				else
					color = 255 * color;

				pixel[pos + s] = FastMath.clampToByte((int) color);
			}
			on = !on;
			pos += flip;
		}

		return loadTexture(pixel, sum, 1, GL20.GL_ALPHA,
		                   GL20.GL_LINEAR, GL20.GL_LINEAR,
		                   // GLES20.GL_NEAREST, GLES20.GL_NEAREST,
		                   GL20.GL_REPEAT, GL20.GL_REPEAT);
	}

	/**
	 * @param shaderType
	 *            shader type
	 * @param source
	 *            shader code
	 * @return gl identifier
	 */
	public static int loadShader(int shaderType, String source) {

		int shader = GL.glCreateShader(shaderType);
		if (shader != 0) {
			GL.glShaderSource(shader, source);
			GL.glCompileShader(shader);
			IntBuffer compiled = GLRenderer.getIntBuffer(1);

			GL.glGetShaderiv(shader, GL20.GL_COMPILE_STATUS, compiled);
			compiled.position(0);
			if (compiled.get() == 0) {
				Log.e(TAG, "Could not compile shader " + shaderType + ":");
				Log.e(TAG, GL.glGetShaderInfoLog(shader));
				GL.glDeleteShader(shader);
				shader = 0;
			}
		}
		return shader;
	}

	/**
	 * @param vertexSource
	 *            ...
	 * @param fragmentSource
	 *            ...
	 * @return gl identifier
	 */
	public static int createProgram(String vertexSource, String fragmentSource) {
		GL = GLAdapter.get();

		int vertexShader = loadShader(GL20.GL_VERTEX_SHADER, vertexSource);
		if (vertexShader == 0) {
			return 0;
		}

		int pixelShader = loadShader(GL20.GL_FRAGMENT_SHADER, fragmentSource);
		if (pixelShader == 0) {
			return 0;
		}

		int program = GL.glCreateProgram();
		if (program != 0) {
			checkGlError("glCreateProgram");
			GL.glAttachShader(program, vertexShader);
			checkGlError("glAttachShader");
			GL.glAttachShader(program, pixelShader);
			checkGlError("glAttachShader");
			GL.glLinkProgram(program);
			IntBuffer linkStatus = GLRenderer.getIntBuffer(1);
			GL.glGetProgramiv(program, GL20.GL_LINK_STATUS, linkStatus);
			linkStatus.position(0);
			if (linkStatus.get() != GL20.GL_TRUE) {
				Log.e(TAG, "Could not link program: ");
				Log.e(TAG, GL.glGetProgramInfoLog(program));
				GL.glDeleteProgram(program);
				program = 0;
			}
		}
		return program;
	}

	/**
	 * @param op
	 *            ...
	 */
	public static void checkGlError(String op) {
		GL = GLAdapter.get();

		int error;
		while ((error = GL.glGetError()) != 0) { // GL20.GL_NO_ERROR) {
			Log.e(TAG, op + ": glError " + error);
			// throw new RuntimeException(op + ": glError " + error);
		}
	}

	public static boolean checkGlOutOfMemory(String op) {
		GL = GLAdapter.get();

		int error;
		boolean oom = false;
		while ((error = GL.glGetError()) != 0) {// GL20.GL_NO_ERROR) {
			Log.e(TAG, op + ": glError " + error);
			// throw new RuntimeException(op + ": glError " + error);
			if (error == 1285)
				oom = true;
		}
		return oom;
	}

	// public static void setBlendColors(int handle, float[] c1, float[] c2,
	// float mix) {
	// if (mix <= 0f)
	// GLES20.glUniform4fv(handle, 1, c1, 0);
	// else if (mix >= 1f)
	// GLES20.glUniform4fv(handle, 1, c2, 0);
	// else {
	// GLES20.glUniform4f(handle,
	// c1[0] * (1 - mix) + c2[0] * mix,
	// c1[1] * (1 - mix) + c2[1] * mix,
	// c1[2] * (1 - mix) + c2[2] * mix,
	// c1[3] * (1 - mix) + c2[3] * mix);
	// }
	// }

	public static void setColor(int handle, float[] c, float alpha) {
		GL = GLAdapter.get();

		if (alpha >= 1) {
			GL.glUniform4f(handle, c[0], c[1], c[2], c[3]);
		} else {
			if (alpha < 0) {
				Log.d(TAG, "setColor: " + alpha);
				alpha = 0;
				GL.glUniform4f(handle, 0, 0, 0, 0);
			}

			GL.glUniform4f(handle,
			               c[0] * alpha, c[1] * alpha,
			               c[2] * alpha, c[3] * alpha);
		}
	}

	public static float[] colorToFloat(int color) {
		float[] c = new float[4];
		c[3] = (color >> 24 & 0xff) / 255.0f;
		c[0] = (color >> 16 & 0xff) / 255.0f;
		c[1] = (color >> 8 & 0xff) / 255.0f;
		c[2] = (color >> 0 & 0xff) / 255.0f;
		return c;
	}

	// premultiply alpha
	public static float[] colorToFloatP(int color) {
		float[] c = new float[4];
		c[3] = (color >> 24 & 0xff) / 255.0f;
		c[0] = (color >> 16 & 0xff) / 255.0f * c[3];
		c[1] = (color >> 8 & 0xff) / 255.0f * c[3];
		c[2] = (color >> 0 & 0xff) / 255.0f * c[3];
		return c;
	}

	/**
	 * public-domain function by Darel Rex Finley from
	 * http://alienryderflex.com/saturation.html
	 *
	 * @param color
	 *            The passed-in RGB values can be on any desired scale, such as
	 *            0 to 1, or 0 to 255.
	 * @param change
	 *            0.0 creates a black-and-white image. 0.5 reduces the color
	 *            saturation by half. 1.0 causes no change. 2.0 doubles the
	 *            color saturation.
	 */
	public static void changeSaturation(float color[], float change) {
		float r = color[0];
		float g = color[1];
		float b = color[2];
		double p = Math.sqrt(r * r * 0.299f + g * g * 0.587f + b * b * 0.114f);
		color[0] = FastMath.clampN((float) (p + (r - p) * change));
		color[1] = FastMath.clampN((float) (p + (g - p) * change));
		color[2] = FastMath.clampN((float) (p + (b - p) * change));
	}

	public static void glUniform4fv(int location, int count, float[] val) {
		GL = GLAdapter.get();

		FloatBuffer buf = GLRenderer.getFloatBuffer(count * 4);
		buf.put(val);
		buf.flip();
		GL.glUniform4fv(location, count, buf);
	}

	public static int[] glGenBuffers(int num) {
		GL = GLAdapter.get();

		IntBuffer buf = GLRenderer.getIntBuffer(num);
		buf.position(0);
		buf.limit(num);
		GL.glGenBuffers(num, buf);
		int[] ret = new int[num];
		buf.position(0);
		buf.limit(num);
		buf.get(ret);
		return ret;
	}

	public static void glDeleteBuffers(int num, int[] ids) {
		GL = GLAdapter.get();

		IntBuffer buf = GLRenderer.getIntBuffer(num);
		buf.put(ids, 0, num);
		buf.position(0);
		GL.glDeleteBuffers(num, buf);
	}

	public static int[] glGenTextures(int num) {
		GL = GLAdapter.get();
		int[] ret = new int[num];
		IntBuffer buf = GLRenderer.getIntBuffer(num);

		if (GLAdapter.GDX_WEBGL_QUIRKS) {
			for (int i = 0; i < num; i++) {
				GL.glGenTextures(num, buf);
				buf.position(0);
				ret[i] = buf.get();
				buf.position(0);
			}
		} else {
			GL.glGenTextures(num, buf);
			buf.position(0);
			buf.get(ret);
		}

		return ret;
	}

	public static void glDeleteTextures(int num, int[] ids) {
		GL = GLAdapter.get();

		IntBuffer buf = GLRenderer.getIntBuffer(num);
		buf.put(ids, 0, num);
		buf.position(0);
		GL.glDeleteTextures(num, buf);
	}

	// private final static float[] mIdentity = {
	// 1, 0, 0, 0,
	// 0, 1, 0, 0,
	// 0, 0, 1, 0,
	// 0, 0, 0, 1 };
	//
	// public static void setTileMatrix(float[] matrix, float tx, float ty,
	// float s) {
	// System.arraycopy(mIdentity, 0, matrix, 0, 16);
	// // scale tile relative to map scale
	// matrix[0] = matrix[5] = s / GLRenderer.COORD_SCALE;
	// // translate relative to map center
	// matrix[12] = tx * s;
	// matrix[13] = ty * s;
	// }
	//
	// public static void setTranslation(float[] matrix, float x, float y, float
	// z) {
	// System.arraycopy(mIdentity, 0, matrix, 0, 16);
	// matrix[12] = x;
	// matrix[13] = y;
	// matrix[14] = z;
	// }
	//
	// public static void setMatrix(float[] matrix, float tx, float ty, float
	// scale) {
	// System.arraycopy(mIdentity, 0, matrix, 0, 16);
	// matrix[12] = tx;
	// matrix[13] = ty;
	// matrix[0] = scale;
	// matrix[5] = scale;
	// //matrix[10] = scale;
	// }
	//
	// public static void setIdentity(float[] matrix) {
	// System.arraycopy(mIdentity, 0, matrix, 0, 16);
	// }
	//
	// public static void setScaleM(float[] matrix, float sx, float sy, float
	// sz) {
	// System.arraycopy(mIdentity, 0, matrix, 0, 16);
	// matrix[0] = sx;
	// matrix[5] = sy;
	// matrix[10] = sz;
	// }
	//
	// public static void addOffsetM(float[] matrix, int delta) {
	// // from http://www.mathfor3dgameprogramming.com/code/Listing9.1.cpp
	// // float n = MapViewPosition.VIEW_NEAR;
	// // float f = MapViewPosition.VIEW_FAR;
	// // float pz = 1;
	// // float epsilon = -2.0f * f * n * delta / ((f + n) * pz * (pz + delta));
	// float epsilon = 1.0f / (1 << 11);
	//
	// matrix[10] *= 1.0f + epsilon * delta;
	// }
}
