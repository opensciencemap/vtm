/*
 * Copyright 2013 Hannes Janetzek.org
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
package org.oscim.renderer.layers;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import org.oscim.backend.GL20;
import org.oscim.backend.GLAdapter;
import org.oscim.core.MapPosition;
import org.oscim.renderer.GLRenderer.Matrices;
import org.oscim.renderer.GLState;
import org.oscim.renderer.RenderLayer;
import org.oscim.utils.GlUtils;
import org.oscim.view.Map;


/*
 * This is an example how to integrate custom OpenGL drawing routines as map overlay
 *
 * based on chapter 2 from:
 * https://github.com/dalinaum/opengl-es-book-samples/tree/master/Android
 * */

public class CustomRenderLayer extends RenderLayer {

	private static final GL20 GL = GLAdapter.get();

	private final Map mMap;

	private int mProgramObject;
	private int hVertexPosition;
	private int hMatrixPosition;

	private FloatBuffer mVertices;
	private final float[] mVerticesData = {
			-200, -200, 1.0f,
			200, 200, 0,
			-200, 200, 0.5f,
			200, -200, 0.5f,
	};
	private boolean mInitialized;

	public CustomRenderLayer(Map map) {
		mMap = map;
	}

	// ---------- everything below runs in GLRender Thread ----------
	@Override
	protected void update(MapPosition pos, boolean changed, Matrices matrices) {
		if (!mInitialized) {
			if (!init())
				return;

			mInitialized = true;

			// fix current MapPosition
			mMapPosition.copy(pos);

			compile();
		}
	}

	@Override
	protected void compile() {
		// modify mVerticesData and put in FloatBuffer

		mVertices.clear();
		mVertices.put(mVerticesData);
		mVertices.flip();

		setReady(true);
	}

	@Override
	protected void render(MapPosition pos, Matrices m) {

		// Use the program object
		GLState.useProgram(mProgramObject);

		GLState.blend(true);
		GLState.test(false, false);

		// unbind previously bound VBOs
		GL.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);

		// Load the vertex data
		//mVertices.position(0);
		GL.glVertexAttribPointer(hVertexPosition, 3, GL20.GL_FLOAT, false, 0, mVertices);
		//mVertices.position(2);
		//GL.glVertexAttribPointer(hVertexPosition, 2, GL20.GL_FLOAT, false, 4, mVertices);

		GLState.enableVertexArrays(hVertexPosition, -1);

		/* apply view and projection matrices */
		// set mvp (tmp) matrix relative to mMapPosition
		// i.e. fixed on the map

		float ratio = 1f / mMap.getWidth();

		m.mvp.setScale(ratio, ratio, 1);
		m.mvp.multiplyLhs(m.proj);
		m.mvp.setAsUniform(hMatrixPosition);

		// Draw the triangle
		GL.glDrawArrays(GL20.GL_TRIANGLE_STRIP, 0, 4);

		GlUtils.checkGlError("...");
	}

	private boolean init() {
		// Load the vertex/fragment shaders
		int programObject = GlUtils.createProgram(vShaderStr, fShaderStr);

		if (programObject == 0)
			return false;

		// Handle for vertex position in shader
		hVertexPosition = GL.glGetAttribLocation(programObject, "a_pos");

		hMatrixPosition = GL.glGetUniformLocation(programObject, "u_mvp");

		// Store the program object
		mProgramObject = programObject;

		mVertices = ByteBuffer.allocateDirect(mVerticesData.length * 4)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();

		return true;
	}

	private final static String vShaderStr =
			"precision mediump float;"
					+ "uniform mat4 u_mvp;"
					+ "attribute vec4 a_pos;"
					+ "varying float alpha;"
					+ "void main()"
					+ "{"
					+ "   gl_Position = u_mvp * vec4(a_pos.xy, 0.0, 1.0);"
					+ "   alpha = a_pos.z;"
					+ "}";

	private final static String fShaderStr =
			"precision mediump float;"
					+ "varying float alpha;"
					+ "void main()"
					+ "{"
					+ "  gl_FragColor = vec4 (alpha, 1.0-alpha, 0.0, 0.7 );"
					+ "}";

}
