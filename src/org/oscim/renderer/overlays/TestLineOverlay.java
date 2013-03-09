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
public class TestLineOverlay extends RenderOverlay {

	public TestLineOverlay(MapView mapView) {
		super(mapView);
	}

	// Interleave two segment quads in one block to be able to use
	// vertices twice. pos0 and pos1 use the same vertex array where
	// pos1 is off-setted by one vertex. The vertex shader will use
	// pos0 when the vertexId is even, pos1 when the Id is odd.
	//
	// As there is no gl_VertexId in gles 2.0 an additional 'flip'
	// array is used. Depending on 'flip' extrusion is inverted.
	//
	// Indices and flip buffers can be static.
	//
	// First pass: using even vertex array positions
	//   (used vertices are in braces)
	// vertex id   0  1  2  3  4  5  6  7
	// pos0     - (0) 1 (2) 3 (4) 5 (6) 7 -
	// pos1        - (0) 1 (2) 3 (4) 5 (6) 7 -
	// flip        0  1  0  1  0  1  0  1
	//
	// Second pass: using odd vertex array positions
	// vertex id   0  1  2  3  4  5  6  7
	// pos0   - 0 (1) 2 (3) 4 (5) 6 (7) -
	// pos1      - 0 (1) 2 (3) 4 (5) 6 (7) -
	// flip        0  1  0  1  0  1  0  1
	//
	// Vertex layout:
	// [2 short] position,
	// [2 short] extrusion,
	// [1 short] line length
	// [1 short] unused
	//
	// indices: (two indice blocks)
	// 0, 1, 2,
	// 2, 1, 3,
	// 4, 5, 6,
	// 6, 5, 7,

	private static int testProgram;
	private static int htestVertexPosition0;
	private static int htestVertexPosition1;
	private static int htestVertexLength0;
	private static int htestVertexLength1;
	private static int htestVertexFlip;
	private static int htestMatrix;
	private static int htestTexColor;
	private static int htestBgColor;
	private static int htestScale;

	private boolean initialized = false;

	final static String testVertexShader = ""
			+ "precision mediump float;"
			+ "uniform mat4 u_mvp;"
			+ "uniform vec4 u_color;"
			+ "uniform float u_scale;"
			+ "attribute vec4 a_pos0;"
			+ "attribute vec4 a_pos1;"
			+ "attribute vec2 a_len0;"
			+ "attribute vec2 a_len1;"
			+ "attribute float a_flip;"
			+ "varying vec2 v_st;"
			+ "void main() {"
			+ "  float div = (8.0 * 16.0) / max(ceil(log(u_scale)),1.0);"
			+ "  if (a_flip == 0.0){"
			+ "    vec2 dir = a_pos0.zw/16.0;"
			+ "    gl_Position = u_mvp * vec4(a_pos0.xy + dir / u_scale, 0.0, 1.0);"
			+ "     v_st = vec2(a_len0.x/div, 1.0);"
			+ "  }else {"
			+ "    vec2 dir = a_pos1.zw/16.0;"
			+ "    gl_Position = u_mvp * vec4(a_pos1.xy - dir / u_scale, 0.0, 1.0);"
			+ "    v_st = vec2(a_len1.x/div, -1.0);"
			+ " }"
			+ "}";

	final static String testFragmentShader = ""
			+ "precision mediump float;"
			+ "uniform sampler2D tex;"
			+ " uniform vec4 u_color;"
			+ " uniform vec4 u_bgcolor;"
			+ "varying vec2 v_st;"
			+ "void main() {"
			+ "  float len = texture2D(tex, v_st).a;"
			+ "  float tex_w = abs(v_st.t);"
			+ "  float line_w    = (1.0 - smoothstep(0.7, 1.0, tex_w));"
			+ "  float stipple_w = (1.0 - smoothstep(0.1, 0.6, tex_w));"
			+ "  float stipple_p = smoothstep(0.495, 0.505, len);"
			+ "  gl_FragColor = line_w * mix(u_bgcolor, u_color, min(stipple_w, stipple_p));"

			//+ "  gl_FragColor = u_color * min(abs(1.0 - mod(v_len, 20.0)/10.0), (1.0 - abs(v_st.x)));"
			+ "}";

	private int mIndicesBufferID;
	private int mVertexBufferID;
	private int mVertexFlipID;

	//private int mNumVertices;
	private int mNumIndices;

	private int mTexID;

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
			Log.e("...", "Could not create test program.");
			return;
		}
		htestMatrix = GLES20.glGetUniformLocation(testProgram, "u_mvp");
		htestTexColor = GLES20.glGetUniformLocation(testProgram, "u_color");
		htestBgColor = GLES20.glGetUniformLocation(testProgram, "u_bgcolor");
		htestScale = GLES20.glGetUniformLocation(testProgram, "u_scale");

		htestVertexPosition0 = GLES20.glGetAttribLocation(testProgram, "a_pos0");
		htestVertexPosition1 = GLES20.glGetAttribLocation(testProgram, "a_pos1");
		htestVertexLength0 = GLES20.glGetAttribLocation(testProgram, "a_len0");
		htestVertexLength1 = GLES20.glGetAttribLocation(testProgram, "a_len1");
		htestVertexFlip = GLES20.glGetAttribLocation(testProgram, "a_flip");

		int[] mVboIds = new int[3];
		GLES20.glGenBuffers(3, mVboIds, 0);
		mIndicesBufferID = mVboIds[0];
		mVertexBufferID = mVboIds[1];
		mVertexFlipID = mVboIds[2];

		float points[] = {
				-800, -800,
				800, -800,
				800, 800,
				-800, 800,
				-800, -800,
		};

		//		float[] points = new float[12 * 2];
		//		for (int i = 0; i < 24; i += 2) {
		//			points[i + 0] = (float) Math.sin(-i / 11f * Math.PI) * 8*400;
		//			points[i + 1] = (float) Math.cos(-i / 11f * Math.PI) * 8*400;
		//		}

		boolean oddSegments = points.length % 4 == 0;

		int numVertices = points.length + (oddSegments ? 2 : 0);

		short[] vertices = new short[numVertices * 6];

		int opos = 6;

		float x = points[0];
		float y = points[1];

		float scale = 255;
		boolean even = true;
		float len = 0;

		for (int i = 2; i < points.length; i += 2) {
			float nx = points[i + 0];
			float ny = points[i + 1];

			// Calculate triangle corners for the given width
			float vx = nx - x;
			float vy = ny - y;

			float a = (float) Math.sqrt(vx * vx + vy * vy);

			// normal vector
			vx /= a;
			vy /= a;

			// perpendicular to line segment
			float ux = -vy;
			float uy = vx;

			short dx = (short) (ux * scale);
			short dy = (short) (uy * scale);

			vertices[opos + 0] = (short) x;
			vertices[opos + 1] = (short) y;
			vertices[opos + 2] = dx;
			vertices[opos + 3] = dy;
			vertices[opos + 4] = (short) len;
			vertices[opos + 5] = 0;

			len += a;
			vertices[opos + 12] = (short) nx;
			vertices[opos + 13] = (short) ny;
			vertices[opos + 14] = dx;
			vertices[opos + 15] = dy;
			vertices[opos + 16] = (short) len;
			vertices[opos + 17] = 0;

			x = nx;
			y = ny;

			if (even) {
				// go to second segment
				opos += 6;
				even = false;
			} else {
				// go to next block
				even = true;
				opos += 18;
			}

		}

		// 0, 1, 0, 1
		byte[] flip = new byte[points.length];
		for (int i = 0; i < flip.length; i++)
			flip[i] = (byte) (i % 2);

		short j = 0;
		mNumIndices = ((points.length) >> 2) * 6;

		short[] indices = new short[mNumIndices];
		for (int i = 0; i < mNumIndices; i += 6, j += 4) {
			indices[i + 0] = (short) (j + 0);
			indices[i + 1] = (short) (j + 1);
			indices[i + 2] = (short) (j + 2);

			indices[i + 3] = (short) (j + 2);
			indices[i + 4] = (short) (j + 1);
			indices[i + 5] = (short) (j + 3);
		}

		ByteBuffer buf = ByteBuffer.allocateDirect(numVertices * 6 * 2)
				.order(ByteOrder.nativeOrder());

		ShortBuffer sbuf = buf.asShortBuffer();
		sbuf.put(indices);
		sbuf.flip();
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndicesBufferID);
		GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, indices.length * 2, sbuf,
				GLES20.GL_STATIC_DRAW);
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

		sbuf.clear();
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

		byte[] stipple = new byte[2];
		stipple[0] = 8;
		stipple[1] = 8;
		//stipple[2] = 16;
		//stipple[3] = 48;

		mTexID = GlUtils.loadStippleTexture(stipple);

		mMapView.getMapViewPosition().getMapPosition(mMapPosition);

		// tell GLRenderer to call 'render'
		isReady = true;
	}

	private final static int STRIDE = 12;
	private final static int LEN_OFFSET = 8;

	@Override
	public synchronized void render(MapPosition pos, Matrices m) {

		setMatrix(pos, m);
		//Matrix.multiplyMM(mv, 0, proj, 0, mv, 0);

		GLState.useProgram(testProgram);
		GLES20.glDisable(GLES20.GL_CULL_FACE);
		GLState.test(false, false);
		GLState.enableVertexArrays(-1, -1);
		GLES20.glEnableVertexAttribArray(htestVertexPosition0);
		GLES20.glEnableVertexAttribArray(htestVertexPosition1);
		GlUtils.checkGlError("-4");
		GLES20.glEnableVertexAttribArray(htestVertexLength0);
		GlUtils.checkGlError("-3");
		GLES20.glEnableVertexAttribArray(htestVertexLength1);
		GlUtils.checkGlError("-2");
		GLES20.glEnableVertexAttribArray(htestVertexFlip);

		GLES20.glUniformMatrix4fv(htestMatrix, 1, false, m.mvp, 0);
		float div = FastMath.pow(pos.zoomLevel - mMapPosition.zoomLevel);
		GLES20.glUniform1f(htestScale, pos.scale / mMapPosition.scale * div);

		GLES20.glUniform4f(htestTexColor, 1.0f, 1.0f, 1.0f, 1.0f);
		GLES20.glUniform4f(htestBgColor, 0.3f, 0.3f, 0.3f, 1.0f);

		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexID);

		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER,
				mIndicesBufferID);
		GlUtils.checkGlError("-1");
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexFlipID);
		GLES20.glVertexAttribPointer(htestVertexFlip, 1,
				GLES20.GL_BYTE, false, 0, 0);
		GlUtils.checkGlError("0");
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferID);
		GlUtils.checkGlError("1");

		// first pass
		GLES20.glVertexAttribPointer(htestVertexPosition0,
				4, GLES20.GL_SHORT, false, STRIDE, STRIDE);
		GlUtils.checkGlError("2");

		GLES20.glVertexAttribPointer(htestVertexLength0,
				2, GLES20.GL_SHORT, false, STRIDE, STRIDE + LEN_OFFSET);
		GlUtils.checkGlError("3");

		GLES20.glVertexAttribPointer(htestVertexPosition1,
				4, GLES20.GL_SHORT, false, STRIDE, 0);
		GlUtils.checkGlError("4");

		GLES20.glVertexAttribPointer(htestVertexLength1,
				2, GLES20.GL_SHORT, false, STRIDE, LEN_OFFSET);
		GlUtils.checkGlError("5");

		//GLES20.glUniform4f(htestColor, 0.5f, 0.5f, 1.0f, 1.0f);
		GLES20.glDrawElements(GLES20.GL_TRIANGLES, mNumIndices,
				GLES20.GL_UNSIGNED_SHORT, 0);

		// second pass
		GLES20.glVertexAttribPointer(htestVertexPosition0,
				4, GLES20.GL_SHORT, false, STRIDE, 2 * STRIDE);

		GLES20.glVertexAttribPointer(htestVertexLength0,
				2, GLES20.GL_SHORT, false, STRIDE, 2 * STRIDE + LEN_OFFSET);

		GLES20.glVertexAttribPointer(htestVertexPosition1,
				4, GLES20.GL_SHORT, false, STRIDE, STRIDE);

		GLES20.glVertexAttribPointer(htestVertexLength1,
				2, GLES20.GL_SHORT, false, STRIDE, STRIDE + LEN_OFFSET);

		GLES20.glDrawElements(GLES20.GL_TRIANGLES, mNumIndices,
				GLES20.GL_UNSIGNED_SHORT, 0);

		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

		GLES20.glDisableVertexAttribArray(htestVertexPosition0);
		GLES20.glDisableVertexAttribArray(htestVertexPosition1);
		GLES20.glDisableVertexAttribArray(htestVertexLength0);
		GLES20.glDisableVertexAttribArray(htestVertexLength1);
		GLES20.glDisableVertexAttribArray(htestVertexFlip);
		GlUtils.checkGlError("...");

		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
	}

	@Override
	protected void setMatrix(MapPosition curPos, Matrices m) {
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
		m.mvp[10] = 1; //scale; // 1000f;

		Matrix.multiplyMM(m.mvp, 0, m.viewproj, 0, m.mvp, 0);
	}

	@Override
	public void compile() {
	}

}
