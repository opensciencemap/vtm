/*
 * Copyright 2013 Hannes Janetzek
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

import org.oscim.backend.GL20;
import org.oscim.backend.GLAdapter;
import org.oscim.backend.Log;

public class GLState {
	private final static GL20 GL = GLAdapter.get();

	private final static String TAG = GLState.class.getName();

	private final static boolean[] vertexArray = { false, false };
	private static boolean blend = false;
	private static boolean depth = false;
	private static boolean stencil = false;
	private static int shader;

	private static int currentTexId;

	public static void init() {
		vertexArray[0] = false;
		vertexArray[1] = false;
		blend = false;
		depth = false;
		stencil = false;
		shader = -1;

		GL.glDisable(GL20.GL_STENCIL_TEST);
		GL.glDisable(GL20.GL_DEPTH_TEST);

//		if (currentTexId != 0) {
//			GL.glBindTexture(GL20.GL_TEXTURE_2D, 0);
//			currentTexId = 0;
//		}
	}

	public static boolean useProgram(int shaderProgram) {
		if (shaderProgram != shader) {
			GL.glUseProgram(shaderProgram);
			shader = shaderProgram;
			return true;
		}
		return false;
	}

	public static void blend(boolean enable) {
		if (blend == enable)
			return;

		if (enable)
			GL.glEnable(GL20.GL_BLEND);
		else
			GL.glDisable(GL20.GL_BLEND);
		blend = enable;
	}

	public static void test(boolean depthTest, boolean stencilTest) {
		if (depth != depthTest) {

			if (depthTest)
				GL.glEnable(GL20.GL_DEPTH_TEST);
			else
				GL.glDisable(GL20.GL_DEPTH_TEST);

			depth = depthTest;
		}

		if (stencil != stencilTest) {

			if (stencilTest)
				GL.glEnable(GL20.GL_STENCIL_TEST);
			else
				GL.glDisable(GL20.GL_STENCIL_TEST);

			stencil = stencilTest;
		}
	}

	public static void enableVertexArrays(int va1, int va2) {
		if (va1 > 1 || va2 > 1)
			Log.d(TAG, "FIXME: enableVertexArrays...");

		if ((va1 == 0 || va2 == 0)) {
			if (!vertexArray[0]) {
				GL.glEnableVertexAttribArray(0);
				vertexArray[0] = true;
			}
		} else {
			if (vertexArray[0]) {
				GL.glDisableVertexAttribArray(0);
				vertexArray[0] = false;
			}
		}

		if ((va1 == 1 || va2 == 1)) {
			if (!vertexArray[1]) {
				GL.glEnableVertexAttribArray(1);
				vertexArray[1] = true;
			}
		} else {
			if (vertexArray[1]) {
				GL.glDisableVertexAttribArray(1);
				vertexArray[1] = false;
			}
		}
	}

	public static void bindTex2D(int id) {
//		if (GLAdapter.GDX_DESKTOP_QUIRKS){
//			GL.glBindTexture(GL20.GL_TEXTURE_2D, 0);
			//if (GLAdapter.GDX_DESKTOP_QUIRKS && id != 0)
			//	GL.glBindTexture(GL20.GL_TEXTURE_2D, 0);
//		} else

			if (currentTexId != id) {
			GL.glBindTexture(GL20.GL_TEXTURE_2D, id);
			currentTexId = id;
			}
	}
}
