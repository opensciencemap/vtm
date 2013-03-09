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
package org.oscim.renderer.overlays;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.renderer.GLRenderer;
import org.oscim.renderer.GLRenderer.Matrices;
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
public class ModelOverlay extends RenderOverlay {

	public ModelOverlay(MapView mapView) {
		super(mapView);
	}

	private final float[] box = {
			// north
			-200f, -200f, 0, 0.9f,
			200f, -200f, 0, 0.9f,
			-200f, -200f, 0.15f, 0.9f,
			200f, -200f, 0.15f, 0.9f,

			// west
			-200f, -200f, 0, 0.8f,
			-200f, 200f, 0, 0.8f,
			-200f, -200f, 0.15f, 0.8f,
			-200f, 200f, 0.15f, 0.8f,

			// south
			200f, 200f, 0, 0.7f,
			-200f, 200f, 0, 0.7f,
			200f, 200f, 0.15f, 0.7f,
			-200f, 200f, 0.15f, 0.7f,

			// east
			200f, -200f, 0, 1.0f,
			200f, 200f, 0, 1.0f,
			200f, -200f, 0.15f, 1.0f,
			200f, 200f, 0.15f, 1.0f,
	};

	private final short[] indices = {
			// north
			0, 1, 2,
			2, 1, 3,
			// west
			4, 5, 6,
			6, 5, 7,
			// south
			8, 9, 10,
			10, 9, 11,
			// east
			12, 13, 14,
			14, 13, 15,
			// top
			2, 3, 10,
			10, 11, 2
	};

	private static int polygonProgram;
	private static int hPolygonVertexPosition;
	private static int hPolygonLightPosition;
	private static int hPolygonMatrix;
	private static int hPolygonColor;
	//private static int hPolygonScale;

	private boolean initialized = false;

	final static String polygonVertexShader = ""
			+ "precision mediump float;"
			+ "uniform mat4 u_mvp;"
			+ "uniform vec4 u_color;"
			+ "attribute vec4 a_position;"
			+ "attribute float a_light;"
			+ "varying vec4 color;"
			+ "void main() {"
			+ "  gl_Position = u_mvp * a_position;"
			+ "  if (u_color.a == 0.0)"
			+ "  color = vec4(u_color.rgb * a_light, 0.8);"
			+ "  else"
			+ "  color = u_color;"
			+ "}";

	final static String polygonFragmentShader = ""
			+ "precision mediump float;"
			+ "varying vec4 color;"
			+ "void main() {"
			+ "  gl_FragColor = color;"
			+ "}";

	private int mIndicesBufferID;
	private int mVertexBufferID;

	@Override
	public synchronized void update(MapPosition curPos, boolean positionChanged,
			boolean tilesChanged) {

		if (initialized)
			return;
		initialized = true;

		// Set up the program for rendering polygons
		polygonProgram = GlUtils.createProgram(polygonVertexShader,
				polygonFragmentShader);
		if (polygonProgram == 0) {
			Log.e("blah", "Could not create polygon program.");
			return;
		}
		hPolygonMatrix = GLES20.glGetUniformLocation(polygonProgram, "u_mvp");
		hPolygonColor = GLES20.glGetUniformLocation(polygonProgram, "u_color");
		hPolygonVertexPosition = GLES20.glGetAttribLocation(polygonProgram, "a_position");
		hPolygonLightPosition = GLES20.glGetAttribLocation(polygonProgram, "a_light");

		int[] mVboIds = new int[2];
		GLES20.glGenBuffers(2, mVboIds, 0);
		mIndicesBufferID = mVboIds[0];
		mVertexBufferID = mVboIds[1];

		ByteBuffer buf = ByteBuffer.allocateDirect(64 * 4)
				.order(ByteOrder.nativeOrder());

		ShortBuffer sbuf = buf.asShortBuffer();
		sbuf.put(indices);
		sbuf.flip();
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndicesBufferID);
		GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, 30 * 2, sbuf, GLES20.GL_STATIC_DRAW);
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

		FloatBuffer fbuf = buf.asFloatBuffer();
		fbuf.put(box);
		fbuf.flip();
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferID);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 64 * 4, fbuf, GLES20.GL_STATIC_DRAW);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

		mMapView.getMapViewPosition().getMapPosition(mMapPosition);

		// tell GLRenderer to call 'render'
		isReady = true;
	}

	@Override
	public synchronized void render(MapPosition pos, Matrices m) {

		setMatrix(pos, m);

		GLState.useProgram(polygonProgram);

		GLState.enableVertexArrays(hPolygonVertexPosition, hPolygonLightPosition);

		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndicesBufferID);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferID);

		GLES20.glVertexAttribPointer(hPolygonVertexPosition, 3, GLES20.GL_FLOAT, false, 16, 0);
		GLES20.glVertexAttribPointer(hPolygonLightPosition, 1, GLES20.GL_FLOAT, false, 16, 12);

		GLES20.glUniformMatrix4fv(hPolygonMatrix, 1, false, m.mvp, 0);
		GLES20.glUniform4f(hPolygonColor, 0.5f, 0.5f, 0.5f, 0.7f);

		// draw to depth buffer
		GLES20.glColorMask(false, false, false, false);
		GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);
		GLState.test(true, false);
		GLES20.glDepthMask(true);
		GLES20.glDepthFunc(GLES20.GL_LESS);

		GLES20.glDrawElements(GLES20.GL_TRIANGLES, 30, GLES20.GL_UNSIGNED_SHORT, 0);

		GLES20.glColorMask(true, true, true, true);
		GLES20.glDepthMask(false);
		GLES20.glDepthFunc(GLES20.GL_EQUAL);

		// draw sides
		GLES20.glUniform4f(hPolygonColor, 0.7f, 0.7f, 0.7f, 0.0f);
		GLES20.glDrawElements(GLES20.GL_TRIANGLES, 24, GLES20.GL_UNSIGNED_SHORT, 0);

		// draw roof
		GLES20.glUniform4f(hPolygonColor, 0.7f, 0.5f, 0.5f, 0.7f);
		GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, 24 * 2);

		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
		GlUtils.checkGlError("...");
	}

	@Override
	protected void setMatrix(MapPosition curPos, Matrices m) {
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

		Matrix.setIdentityM(m.mvp, 0);

		// translate relative to map center
		m.mvp[12] = x * scale;
		m.mvp[13] = y * scale;
		// scale to current tile world coordinates
		scale = (curPos.scale / oPos.scale) / div;
		scale /= GLRenderer.COORD_SCALE;
		m.mvp[0] = scale;
		m.mvp[5] = scale;
		m.mvp[10] = scale; // 1000f;

		Matrix.multiplyMM(m.mvp, 0, m.viewproj, 0, m.mvp, 0);
	}

	@Override
	public void compile() {
		// TODO Auto-generated method stub

	}

}
