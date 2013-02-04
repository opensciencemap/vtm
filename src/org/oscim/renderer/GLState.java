/*
 * Copyright 2013 OpenScienceMap
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
package org.oscim.renderer;

import android.opengl.GLES20;
import android.util.Log;

/**
 * @author Hannes Janetzek
 */
public class GLState {
	private final static String TAG = GLState.class.getName();

	private final static boolean[] vertexArray = { false, false };
	private static boolean blend = false;
	private static boolean depth = false;
	private static boolean stencil = false;
	private static int shader;

	public static void init() {
		vertexArray[0] = false;
		vertexArray[1] = false;
		blend = false;
		depth = false;
		stencil = false;
		shader = -1;
		GLES20.glDisable(GLES20.GL_STENCIL_TEST);
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);
	}

	public static boolean useProgram(int shaderProgram) {
		if (shaderProgram != shader) {
			GLES20.glUseProgram(shaderProgram);
			shader = shaderProgram;
			return true;
		}
		return false;
	}

	public static void blend(boolean enable) {
		if (blend == enable)
			return;

		if (enable)
			GLES20.glEnable(GLES20.GL_BLEND);
		else
			GLES20.glDisable(GLES20.GL_BLEND);
		blend = enable;
	}

	public static void test(boolean depthTest, boolean stencilTest) {
		if (depth != depthTest) {

			if (depthTest)
				GLES20.glEnable(GLES20.GL_DEPTH_TEST);
			else
				GLES20.glDisable(GLES20.GL_DEPTH_TEST);

			depth = depthTest;
		}

		if (stencil != stencilTest) {

			if (stencilTest)
				GLES20.glEnable(GLES20.GL_STENCIL_TEST);
			else
				GLES20.glDisable(GLES20.GL_STENCIL_TEST);

			stencil = stencilTest;
		}
	}

	public static void enableVertexArrays(int va1, int va2) {
		if (va1 > 1 || va2 > 1)
			Log.d(TAG, "FIXME: enableVertexArrays...");

		if ((va1 == 0 || va2 == 0)) {
			if (!vertexArray[0]) {
				GLES20.glEnableVertexAttribArray(0);
				vertexArray[0] = true;
			}
		} else {
			if (vertexArray[0]) {
				GLES20.glDisableVertexAttribArray(0);
				vertexArray[0] = false;
			}
		}

		if ((va1 == 1 || va2 == 1)) {
			if (!vertexArray[1]) {
				GLES20.glEnableVertexAttribArray(1);
				vertexArray[1] = true;
			}
		} else {
			if (vertexArray[1]) {
				GLES20.glDisableVertexAttribArray(1);
				vertexArray[1] = false;
			}
		}
	}
}
