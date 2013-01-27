/*
 * Copyright 2012 OpenScienceMap
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
package org.oscim.renderer.overlays;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.renderer.GLRenderer;
import org.oscim.renderer.GLState;
import org.oscim.utils.FastMath;
import org.oscim.utils.GlUtils;
import org.oscim.view.MapView;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

/**
 * @author Hannes Janetzek
 */
public class TestLineOverlay extends RenderOverlay {

	public TestLineOverlay(MapView mapView) {
		super(mapView);
	}

	private final float[] box = {
			// pos      dir len
			0, 0, 0, 0, // start offset

			-800f, 0f, 1, 0,  // first
			100f, 0f, 1, 0.25f,  // third
			0f, 0f, 1, 1, // second
			800f, 0f, 1, 1, // fourth

			-800f, 200f, 1, 0,  // first
			0f, 200f, 1, 0.25f,  // third
			0f, 200f, 1, 1, // second
			800f, 200f, 1, 1, // fourth

			-800f, 400f, 1, 0,  // first
			0f, 400f, 1, 0.25f,  // third
			0f, 400f, 1, 1, // second
			800f, 400f, 1, 1, // fourth

			0, 0, 0, 0, // end
	};

	private final short[] indices = {
			0, 1, 2,
			2, 1, 3,
			4, 5, 6,
			6, 5, 7,
			8, 9, 10,
			10, 9, 11,
	};

	private byte[] flip = { 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1 };

	private static int testProgram;
	private static int htestVertexPosition0;
	private static int htestVertexPosition1;
	private static int htestVertexFlip;
	private static int htestMatrix;
	private static int htestColor;

	private boolean initialized = false;

	final static String testVertexShader = ""
			+ "precision mediump float;"
			+ "uniform mat4 u_mvp;"
			+ "uniform vec4 u_color;"
			+ "attribute vec4 a_pos0;"
			+ "attribute vec4 a_pos1;"
			+ "attribute float a_flip;"
			+ "varying vec4 color;"
			+ "void main() {"
			+ "  if (a_flip == 0.0){"
			+ "    gl_Position = u_mvp * vec4(a_pos0.x, a_pos0.y + a_pos0.z * 100.0, 0.0, 1.0);"
			+ "    color = vec4(0.0,1.0,a_pos0.w,1.0);"
			+ "  }else {"
			+ "    gl_Position = u_mvp * vec4(a_pos1.x, a_pos1.y - a_pos1.z * 100.0, 0.0, 1.0);"
			+ "    color = vec4(1.0,0.5,a_pos1.w,1.0);"
			+ "}}";

	final static String testFragmentShader = ""
			+ "precision mediump float;"
			+ "varying vec4 color;"
			+ "void main() {"
			+ "  gl_FragColor = color;"
			+ "}";

	private int mIndicesBufferID;
	private int mVertexBufferID;
	private int mVertexFlipID;

	@Override
	public synchronized void update(MapPosition curPos, boolean positionChanged,
			boolean tilesChanged) {

		if (initialized)
			return;
		initialized = true;

		// Set up the program for rendering tests
		testProgram = GlUtils.createProgram(testVertexShader,
				testFragmentShader);
		if (testProgram == 0) {
			Log.e("blah", "Could not create test program.");
			return;
		}
		htestMatrix = GLES20.glGetUniformLocation(testProgram, "u_mvp");
		htestColor = GLES20.glGetUniformLocation(testProgram, "u_color");
		htestVertexPosition0 = GLES20.glGetAttribLocation(testProgram, "a_pos0");
		htestVertexPosition1 = GLES20.glGetAttribLocation(testProgram, "a_pos1");
		htestVertexFlip = GLES20.glGetAttribLocation(testProgram, "a_flip");

		int[] mVboIds = new int[3];
		GLES20.glGenBuffers(3, mVboIds, 0);
		mIndicesBufferID = mVboIds[0];
		mVertexBufferID = mVboIds[1];
		mVertexFlipID = mVboIds[2];

		ByteBuffer buf = ByteBuffer.allocateDirect(128 * 4)
				.order(ByteOrder.nativeOrder());

		ShortBuffer sbuf = buf.asShortBuffer();
		sbuf.put(indices);
		sbuf.flip();
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndicesBufferID);
		GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, 18 * 2, sbuf, GLES20.GL_STATIC_DRAW);
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

		FloatBuffer fbuf = buf.asFloatBuffer();
		fbuf.put(box);
		fbuf.flip();
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferID);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 56 * 4, fbuf, GLES20.GL_STATIC_DRAW);

		buf.clear();
		buf.put(flip);
		buf.flip();
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexFlipID);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 12, buf, GLES20.GL_STATIC_DRAW);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

		mMapView.getMapViewPosition().getMapPosition(mMapPosition, null);

		// tell GLRenderer to call 'render'
		isReady = true;
	}

	@Override
	public synchronized void render(MapPosition pos, float[] mv, float[] proj) {

		setMatrix(pos, mv);
		Matrix.multiplyMM(mv, 0, proj, 0, mv, 0);

		GLES20.glUseProgram(testProgram);
		GLES20.glDisable(GLES20.GL_CULL_FACE);
		GLState.test(false, false);
		GLState.enableVertexArrays(-1, -1);
		GLES20.glEnableVertexAttribArray(htestVertexPosition0);
		GLES20.glEnableVertexAttribArray(htestVertexPosition1);
		GLES20.glEnableVertexAttribArray(htestVertexFlip);

		GLES20.glUniformMatrix4fv(htestMatrix, 1, false, mv, 0);

		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndicesBufferID);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexFlipID);
		GLES20.glVertexAttribPointer(htestVertexFlip, 1, GLES20.GL_BYTE, false, 0, 0);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferID);

		GLES20.glVertexAttribPointer(htestVertexPosition0, 4, GLES20.GL_FLOAT, false, 0, 16);
		GLES20.glVertexAttribPointer(htestVertexPosition1, 4, GLES20.GL_FLOAT, false, 0, 0);
		GLES20.glUniform4f(htestColor, 0.5f, 0.5f, 1.0f, 1.0f);
		GLES20.glDrawElements(GLES20.GL_TRIANGLES, 18, GLES20.GL_UNSIGNED_SHORT, 0);

		GLES20.glVertexAttribPointer(htestVertexPosition0, 4, GLES20.GL_FLOAT, false, 0, 32);
		GLES20.glVertexAttribPointer(htestVertexPosition1, 4, GLES20.GL_FLOAT, false, 0, 16);
		GLES20.glUniform4f(htestColor, 0.5f, 1.0f, 0.5f, 1.0f);
		GLES20.glDrawElements(GLES20.GL_TRIANGLES, 18, GLES20.GL_UNSIGNED_SHORT, 0);

		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

		GLES20.glDisableVertexAttribArray(htestVertexPosition0);
		GLES20.glDisableVertexAttribArray(htestVertexPosition1);
		GLES20.glDisableVertexAttribArray(htestVertexFlip);
		GlUtils.checkGlError("...");
	}

	@Override
	protected void setMatrix(MapPosition curPos, float[] matrix) {
		// TODO if oPos == curPos this could be simplified

		MapPosition oPos = mMapPosition;

		byte z = oPos.zoomLevel;

		float div = FastMath.pow(z - curPos.zoomLevel);
		float x = (float) (oPos.x - curPos.x * div);
		float y = (float) (oPos.y - curPos.y * div);

		// flip around date-line
		float max = (Tile.TILE_SIZE << z);
		if (x < -max / 2)
			x = max + x;
		else if (x > max / 2)
			x = x - max;

		float scale = curPos.scale / div;

		Matrix.setIdentityM(matrix, 0);

		// translate relative to map center
		matrix[12] = x * scale;
		matrix[13] = y * scale;
		// scale to current tile world coordinates
		scale = (curPos.scale / oPos.scale) / div;
		scale /= GLRenderer.COORD_MULTIPLIER;
		matrix[0] = scale;
		matrix[5] = scale;
		matrix[10] = 1; //scale; // 1000f;

		Matrix.multiplyMM(matrix, 0, curPos.viewMatrix, 0, matrix, 0);
	}

}
