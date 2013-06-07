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

import java.nio.FloatBuffer;

import org.oscim.core.MapPosition;
import org.oscim.renderer.BufferObject;
import org.oscim.renderer.GLRenderer;
import org.oscim.renderer.GLRenderer.Matrices;
import org.oscim.renderer.GLState;
import org.oscim.renderer.RenderLayer;
import org.oscim.utils.GlUtils;
import org.oscim.view.MapView;

import android.graphics.Color;
import android.opengl.GLES20;

/*
 * This is an example how to integrate custom OpenGL drawing routines as map overlay
 *
 * based on chapter 2 from:
 * https://github.com/dalinaum/opengl-es-book-samples/tree/master/Android
 * */

public class CustomRenderLayer2 extends RenderLayer {

	private int mProgramObject;
	private int hVertexPosition;
	private int hMatrixPosition;
	private int hColorPosition;
	private int hCenterPosition;

	//private FloatBuffer mVertices;
	private boolean mInitialized;
	private BufferObject mVBO;

	public CustomRenderLayer2(MapView mapView) {
		super(mapView);
	}

	int mZoom = -1;
	float mCellScale = 100 * GLRenderer.COORD_SCALE;

	@Override
	public void update(MapPosition pos, boolean changed, Matrices matrices) {
		if (!mInitialized) {
			if (!init())
				return;

			mInitialized = true;

			// tell GLRender to call 'compile' when data has changed
			newData = true;

			// fix current MapPosition

			mMapPosition.setPosition(53.1, 8.8);
			mMapPosition.setZoomLevel(14);

		}

//		if (mZoom != pos.zoomLevel){
//			mMapPosition.copy(pos);
//			mZoom = pos.zoomLevel;
//		}
	}

	@Override
	public void compile() {

		float[] vertices = new float[12];

		for (int i = 0; i < 6; i++){
			vertices[i*2+0] = (float)Math.cos(Math.PI * 2 * i / 6) * mCellScale;
			vertices[i*2+1] = (float)Math.sin(Math.PI * 2 * i / 6) * mCellScale;
		}
		FloatBuffer buf = GLRenderer.getFloatBuffer(12);
		buf.put(vertices);
		buf.flip();

		mVBO = BufferObject.get(0);
		mVBO.loadBufferData(buf, 12*4, GLES20.GL_ARRAY_BUFFER);
		newData = false;

		// tell GLRender to call 'render'
		isReady = true;
	}

	@Override
	public void render(MapPosition pos, Matrices m) {

		// Use the program object
		GLState.useProgram(mProgramObject);

		GLState.blend(true);
		GLState.test(false, false);

		// bind VBO data
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVBO.id);

		// set VBO vertex layout
		GLES20.glVertexAttribPointer(hVertexPosition, 2, GLES20.GL_FLOAT, false, 0, 0);

		GLState.enableVertexArrays(hVertexPosition, -1);

		/* apply view and projection matrices */
		// set mvp (tmp) matrix relative to mMapPosition
		// i.e. fixed on the map
		setMatrix(pos, m);
		m.mvp.setAsUniform(hMatrixPosition);

		int offset_x = 4;
		int offset_y = 12;
		for (int x = -offset_x; x <= offset_x; x++){
			for (int y = -offset_y; y <= offset_y; y++){
				int xx = x * 2;

				if (y % 2 == 0)
					xx -= 1;

				float yy = y * (float)Math.sqrt(3)/2;

				GLES20.glUniform2f(hCenterPosition, xx * (mCellScale * 1.5f), yy * mCellScale);

				float alpha =  (float)Math.sqrt(xx * xx + yy * yy)/10;
				GlUtils.setColor(hColorPosition, Color.BLACK, alpha);
				GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 6);

				GlUtils.setColor(hColorPosition, Color.RED, alpha*2);
				GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, 0, 6);
			}
		}


		GlUtils.checkGlError("...");
	}

	private boolean init() {
		// Load the vertex/fragment shaders
		int programObject = GlUtils.createProgram(vShaderStr, fShaderStr);

		if (programObject == 0)
			return false;

		// Handle for vertex position in shader
		hVertexPosition = GLES20.glGetAttribLocation(programObject, "a_pos");

		hMatrixPosition = GLES20.glGetUniformLocation(programObject, "u_mvp");

		hColorPosition = GLES20.glGetUniformLocation(programObject, "u_color");

		hCenterPosition = GLES20.glGetUniformLocation(programObject, "u_center");

		// Store the program object
		mProgramObject = programObject;

		return true;
	}

	private final static String vShaderStr =
			"precision mediump float;"
					+ "uniform mat4 u_mvp;"
					+ "uniform vec2 u_center;"
					+ "attribute vec2 a_pos;"
					+ "void main()"
					+ "{"
					+ "   gl_Position = u_mvp * vec4(u_center + a_pos, 0.0, 1.0);"
					+ "}";

	private final static String fShaderStr =
			"precision mediump float;"
					+ "varying float alpha;"
					+ "uniform vec4 u_color;"
					+ "void main()"
					+ "{"
					+ "  gl_FragColor = u_color;"
					+ "}";

}
