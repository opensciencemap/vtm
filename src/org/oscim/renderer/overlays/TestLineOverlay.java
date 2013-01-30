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

	// Interleave two quads to be able to use vertices
	// twice. pos0 and pos1 use the same vertex array
	// where pos1 is off-setted by one vertex. The
	// vertex shader will use pos0 when the vertexId
	// is even, pos1 when the Id is odd.
	//
	// As there is no gl_VertexId in gles 2.0 an
	// additional 'flip' array is used.
	// Depending on 'flip' extrusion is inverted.
	//
	// Indices and flip buffers can be static.
	//
	// First pass: using even vertex array positions
	//   (used vertices are in braces)
	// vertex id   0, 1, 2, 3
	// pos0     - (0) 1 (2) 3  -
	// pos1        - (0) 1 (2) 3 -
	// flip        0  1  0  1
	//
	// Second pass: using odd vertex array positions
	// vertex id   0, 1, 2, 3
	// pos0   - 0 (1) 2 (3) -
	// pos1      - 0 (1) 2 (3) -
	// flip        0  1  0  1
	//
	// Vertex layout:
	// x/y pos[16][16], dir_x[8]|dir_y[8], start[4]|length[12]
	// - 'direction' precision 1/16, maximum line width is 2*16
	// - texture 'start'  prescision 1
	//   -> max tex width is 32
	// - segment 'length' prescision 1/4
	//   -> max line length is 2^12/4=1024
	// - texture 'end' is 'length'-'start'

	private final short[] box = {
			//  '-' start
			0, 0, 0, 0,
			// 0.
			-800, 0, 255, 0,
			// 2.
			100, 0, 255, 0,
			// 1.
			0, 0, 255, 1,
			// 3.
			800, 0, 255, 1,

			-800, 200, 127, 0,
			0, 200, 127, 0,
			0, 200, 127, 1,
			800, 200, 127, 1,

			-800, 400, 255, 0,
			0, 400, 255, 0,
			0, 400, 255, 1,
			800, 400, 255, 1,

			// '-' end
			0, 0, 0, 0,
	};

	private short[] indices = {
			0, 1, 2,
			2, 1, 3,

			4, 5, 6,
			6, 5, 7,

			8, 9, 10,
			10, 9, 11,
	};

	private byte[] flip;

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
			+ "const float ff = 256.0;"
			+ "const float ffff = 65536.0;"
			+ "void main() {"
			+ "  if (a_flip == 0.0){"
			//     extract 8 bit direction vector
			+ "    vec2 dir = a_pos0.zw/16.0;"
			+ "    gl_Position = u_mvp * vec4(a_pos0.xy + dir, 0.0, 1.0);"
			+ "    color = vec4(dir/255.0 + 0.5, 1.0,1.0);"
			+ "  }else {"
			+ "    vec2 dir = a_pos1.zw/16.0;"
			+ "    gl_Position = u_mvp * vec4(a_pos1.xy - dir, 0.0, 1.0);"
			+ "    color = vec4(dir/255.0 + 0.5, 1.0,1.0);"
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

	private int mNumVertices;
	private int mNumIndices;

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

		float points[] = {
				800, 0,
				0, 0,
				//-400, 100,
				//-600, 200,
				//-800, 100,
		};

		//		float[] points = new float[12 * 2];
		//		for (int i = 0; i < 24; i += 2) {
		//			points[i + 0] = (float) Math.sin(-i / 11f * Math.PI) * 400;
		//			points[i + 1] = (float) Math.cos(-i / 11f * Math.PI) * 400;
		//		}

		mNumVertices = (points.length - 2) * 2;

		short[] vertices = new short[(mNumVertices + 2) * 4];

		int opos = 4;

		float x = points[0];
		float y = points[1];

		float scale = 127;
		boolean even = true;

		for (int i = 2; i < points.length;) {
			float nx = points[i++];
			float ny = points[i++];

			// Calculate triangle corners for the given width
			float vx = nx - x;
			float vy = ny - y;

			float a = (float) Math.sqrt(vx * vx + vy * vy);

			// normal vector
			vx = (vx / a);
			vy = (vy / a);

			float ux = -vy;
			float uy = vx;

			short dx = (short) (ux * scale);
			short dy = (short) (uy * scale);

			vertices[opos + 0] = (short) x;
			vertices[opos + 1] = (short) y;
			vertices[opos + 2] = dx;
			vertices[opos + 3] = dy;

			vertices[opos + 8] = (short) nx;
			vertices[opos + 9] = (short) ny;
			vertices[opos + 10] = dx;
			vertices[opos + 11] = dy;

			x = nx;
			y = ny;

			if (even) {
				opos += 4;
				even = false;
			} else {
				even = true;
				opos += 12;
			}

		}

		flip = new byte[(points.length - 2)];
		for (int i = 0; i < flip.length; i++)
			flip[i] = (byte) (i % 2);

		short j = 0;
		mNumIndices = ((points.length) >> 2) * 6;

		indices = new short[mNumIndices];
		for (int i = 0; i < mNumIndices; i += 6, j += 4) {
			indices[i + 0] = (short) (j + 0);
			indices[i + 1] = (short) (j + 1);
			indices[i + 2] = (short) (j + 2);

			indices[i + 3] = (short) (j + 2);
			indices[i + 4] = (short) (j + 1);
			indices[i + 5] = (short) (j + 3);
		}

		ByteBuffer buf = ByteBuffer.allocateDirect(128 * 4)
				.order(ByteOrder.nativeOrder());

		ShortBuffer sbuf = buf.asShortBuffer();
		sbuf.put(indices);
		sbuf.flip();
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndicesBufferID);
		GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, indices.length * 2, sbuf,
				GLES20.GL_STATIC_DRAW);
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

		sbuf.clear();
		//sbuf.put(box);
		sbuf.put(vertices);
		sbuf.flip();
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferID);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertices.length * 2, sbuf,
				GLES20.GL_STATIC_DRAW);

		buf.clear();
		buf.put(flip);
		buf.flip();
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexFlipID);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, flip.length, buf,
				GLES20.GL_STATIC_DRAW);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

		mMapView.getMapViewPosition().getMapPosition(mMapPosition, null);

		// tell GLRenderer to call 'render'
		isReady = true;
	}

	@Override
	public synchronized void render(MapPosition pos, float[] mv, float[] proj) {

		setMatrix(pos, mv);
		Matrix.multiplyMM(mv, 0, proj, 0, mv, 0);

		GLState.useProgram(testProgram);
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

		GLES20.glVertexAttribPointer(htestVertexPosition0,
				4, GLES20.GL_SHORT, false, 0, 8);

		GLES20.glVertexAttribPointer(htestVertexPosition1,
				4, GLES20.GL_SHORT, false, 0, 0);

		GLES20.glUniform4f(htestColor, 0.5f, 0.5f, 1.0f, 1.0f);
		GLES20.glDrawElements(GLES20.GL_TRIANGLES, mNumIndices, GLES20.GL_UNSIGNED_SHORT, 0);

		GLES20.glVertexAttribPointer(htestVertexPosition0,
				4, GLES20.GL_SHORT, false, 0, 16);

		GLES20.glVertexAttribPointer(htestVertexPosition1,
				4, GLES20.GL_SHORT, false, 0, 8);

		GLES20.glUniform4f(htestColor, 0.5f, 1.0f, 0.5f, 1.0f);
		GLES20.glDrawElements(GLES20.GL_TRIANGLES, mNumIndices, GLES20.GL_UNSIGNED_SHORT, 0);

		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

		GLES20.glDisableVertexAttribArray(htestVertexPosition0);
		GLES20.glDisableVertexAttribArray(htestVertexPosition1);
		GLES20.glDisableVertexAttribArray(htestVertexFlip);
		GlUtils.checkGlError("...");
	}

	@Override
	protected void setMatrix(MapPosition curPos, float[] matrix) {
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
