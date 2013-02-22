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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import org.oscim.core.MapPosition;
import org.oscim.renderer.layer.Layer;
import org.oscim.renderer.layer.Layers;
import org.oscim.renderer.layer.LineLayer;
import org.oscim.renderer.layer.LineTexLayer;
import org.oscim.utils.GlUtils;

import android.opengl.GLES20;
import android.util.Log;

public class LineTexRenderer {
	private final static String TAG = LineTexRenderer.class.getName();

	// factor to normalize extrusion vector and scale to coord scale
	private final static float COORD_SCALE_BY_DIR_SCALE =
			GLRenderer.COORD_MULTIPLIER / LineLayer.DIR_SCALE;

	private static int shader;
	private static int hVertexPosition0;
	private static int hVertexPosition1;
	private static int hVertexLength0;
	private static int hVertexLength1;
	private static int hVertexFlip;
	private static int hMatrix;
	private static int hTexColor;
	private static int hBgColor;
	private static int hScale;
	private static int hWidth;

	private static int mIndicesBufferID;
	private static int mVertexFlipID;

	// draw up to 100 quads in one round
	private static int maxQuads = 100;
	private static int maxIndices = maxQuads * 6;
	private static int mTexID;

	public static void init() {
		shader = GlUtils.createProgram(vertexShader, fragmentShader);
		if (shader == 0) {
			Log.e(TAG, "Could not create  program.");
			return;
		}

		hMatrix = GLES20.glGetUniformLocation(shader, "u_mvp");
		hTexColor = GLES20.glGetUniformLocation(shader, "u_color");
		hBgColor = GLES20.glGetUniformLocation(shader, "u_bgcolor");
		hScale = GLES20.glGetUniformLocation(shader, "u_scale");
		hWidth = GLES20.glGetUniformLocation(shader, "u_width");

		hVertexPosition0 = GLES20.glGetAttribLocation(shader, "a_pos0");
		hVertexPosition1 = GLES20.glGetAttribLocation(shader, "a_pos1");
		hVertexLength0 = GLES20.glGetAttribLocation(shader, "a_len0");
		hVertexLength1 = GLES20.glGetAttribLocation(shader, "a_len1");
		hVertexFlip = GLES20.glGetAttribLocation(shader, "a_flip");

		int[] mVboIds = new int[2];
		GLES20.glGenBuffers(2, mVboIds, 0);
		mIndicesBufferID = mVboIds[0];
		mVertexFlipID = mVboIds[1];

		short[] indices = new short[maxIndices];
		for (int i = 0, j = 0; i < maxIndices; i += 6, j += 4) {
			indices[i + 0] = (short) (j + 0);
			indices[i + 1] = (short) (j + 1);
			indices[i + 2] = (short) (j + 2);

			indices[i + 3] = (short) (j + 2);
			indices[i + 4] = (short) (j + 1);
			indices[i + 5] = (short) (j + 3);
		}

		ByteBuffer buf = ByteBuffer.allocateDirect(maxIndices * 2)
				.order(ByteOrder.nativeOrder());

		ShortBuffer sbuf = buf.asShortBuffer();
		sbuf.put(indices);
		sbuf.flip();
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER,
				mIndicesBufferID);
		GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER,
				indices.length * 2, sbuf, GLES20.GL_STATIC_DRAW);
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

		// 0, 1, 0, 1, 0, ...
		byte[] flip = new byte[maxQuads * 4];
		for (int i = 0; i < flip.length; i++)
			flip[i] = (byte) (i % 2);

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

	}

	private final static int STRIDE = 12;
	private final static int LEN_OFFSET = 8;

	public static Layer draw(Layers layers, Layer curLayer,
			MapPosition pos, float[] matrix, float div) {

		GLState.blend(true);
		GLState.useProgram(shader);

		GLState.enableVertexArrays(-1, -1);

		GLES20.glEnableVertexAttribArray(hVertexPosition0);
		GLES20.glEnableVertexAttribArray(hVertexPosition1);
		GLES20.glEnableVertexAttribArray(hVertexLength0);
		GLES20.glEnableVertexAttribArray(hVertexLength1);
		GLES20.glEnableVertexAttribArray(hVertexFlip);

		GLES20.glUniformMatrix4fv(hMatrix, 1, false, matrix, 0);

		GLES20.glUniform4f(hTexColor, 1.0f, 1.0f, 1.0f, 1.0f);
		//aa9988

		GLES20.glUniform4f(hBgColor, 0x99 / 255f, 0x96 / 255f, 0x93 / 255f, 0.95f);

		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexID);

		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER,
				mIndicesBufferID);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexFlipID);
		GLES20.glVertexAttribPointer(hVertexFlip, 1,
				GLES20.GL_BYTE, false, 0, 0);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, layers.vbo.id);

		int offset = layers.texLineOffset;

		float s = pos.scale / div;

		Layer l = curLayer;
		while (l != null && l.type == Layer.TEXLINE) {
			LineTexLayer ll = (LineTexLayer) l;
			//Line line = ll.line;

			// scale pattern to twice its size, then reset scale to 1.
			// (coord scale * pattern size) / scale
			GLES20.glUniform1f(hScale, (8 * 16) / Math.max((int)(s+0.25f), 1));

			GLES20.glUniform1f(hWidth, ll.width / s * COORD_SCALE_BY_DIR_SCALE);

			// first pass
			int allIndices = (ll.evenQuads * 6);
			for (int i = 0; i < allIndices; i += maxIndices) {
				int numIndices = allIndices - i;
				if (numIndices > maxIndices)
					numIndices = maxIndices;

				// i / 6 * (24 shorts per block * 2 short bytes)
				int add = offset + i * 8;

				GLES20.glVertexAttribPointer(hVertexPosition0,
						4, GLES20.GL_SHORT, false, STRIDE,
						add + STRIDE);

				GLES20.glVertexAttribPointer(hVertexLength0,
						2, GLES20.GL_SHORT, false, STRIDE,
						add + STRIDE + LEN_OFFSET);

				GLES20.glVertexAttribPointer(hVertexPosition1,
						4, GLES20.GL_SHORT, false, STRIDE,
						add);

				GLES20.glVertexAttribPointer(hVertexLength1,
						2, GLES20.GL_SHORT, false, STRIDE,
						add + LEN_OFFSET);

				GLES20.glDrawElements(GLES20.GL_TRIANGLES, numIndices,
						GLES20.GL_UNSIGNED_SHORT, 0);
			}

			// second pass
			allIndices = (ll.oddQuads * 6);
			for (int i = 0; i < allIndices; i += maxIndices) {
				int numIndices = allIndices - i;
				if (numIndices > maxIndices)
					numIndices = maxIndices;
				// i / 6 * (24 shorts per block * 2 short bytes)
				int add = offset + i * 8;

				GLES20.glVertexAttribPointer(hVertexPosition0,
						4, GLES20.GL_SHORT, false, STRIDE,
						add + 2 * STRIDE);

				GLES20.glVertexAttribPointer(hVertexLength0,
						2, GLES20.GL_SHORT, false, STRIDE,
						add + 2 * STRIDE + LEN_OFFSET);

				GLES20.glVertexAttribPointer(hVertexPosition1,
						4, GLES20.GL_SHORT, false, STRIDE,
						add + STRIDE);

				GLES20.glVertexAttribPointer(hVertexLength1,
						2, GLES20.GL_SHORT, false, STRIDE,
						add + STRIDE + LEN_OFFSET);

				GLES20.glDrawElements(GLES20.GL_TRIANGLES, numIndices,
						GLES20.GL_UNSIGNED_SHORT, 0);
			}

			l = l.next;
		}

		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

		GLES20.glDisableVertexAttribArray(hVertexPosition0);
		GLES20.glDisableVertexAttribArray(hVertexPosition1);
		GLES20.glDisableVertexAttribArray(hVertexLength0);
		GLES20.glDisableVertexAttribArray(hVertexLength1);
		GLES20.glDisableVertexAttribArray(hVertexFlip);
		GlUtils.checkGlError("...");

		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

		return l;
	}

	final static String vertexShader = ""
			+ "precision mediump float;"
			+ "uniform mat4 u_mvp;"
			+ "uniform vec4 u_color;"
			+ "uniform float u_scale;"
			+ "uniform float u_width;"
			+ "attribute vec4 a_pos0;"
			+ "attribute vec4 a_pos1;"
			+ "attribute vec2 a_len0;"
			+ "attribute vec2 a_len1;"
			+ "attribute float a_flip;"
			+ "varying vec2 v_st;"
			+ "void main() {"
			// coord scale 8 * pattern length 16
			//+ "  float div = (8.0 * 16.0) / max(ceil(log(u_scale)),1.0);"
			+ "  float div = u_scale;"
			+ "  if (a_flip == 0.0){"
			+ "    vec2 dir = u_width * a_pos0.zw;"
			+ "    gl_Position = u_mvp * vec4(a_pos0.xy + dir, 0.0, 1.0);"
			+ "    v_st = vec2(a_len0.x/div, 1.0);"
			+ "  }else {"
			+ "    vec2 dir = u_width * a_pos1.zw ;"
			+ "    gl_Position = u_mvp * vec4(a_pos1.xy - dir, 0.0, 1.0);"
			+ "    v_st = vec2(a_len1.x/div, -1.0);"
			+ " }"
			+ "}";

	final static String fragmentShader = ""
			+ "#extension GL_OES_standard_derivatives : enable\n"
			+ "precision mediump float;"
			+ "uniform sampler2D tex;"
			+ " uniform vec4 u_color;"
			+ " uniform vec4 u_bgcolor;"
			+ "varying vec2 v_st;"
			+ "void main() {"
			+ "  float len = texture2D(tex, v_st).a;"
			+ "  float tex_w = abs(v_st.t);"
			+ "  vec2 st_width = fwidth(v_st);"
			+ "  float fuzz = max(st_width.s, st_width.t);"
			+ "  float line_w    = (1.0 - smoothstep(1.0 - fuzz, 1.0, tex_w));"
			+ "  float stipple_w = (1.0 - smoothstep(0.7 - fuzz, 0.7, tex_w));"
			+ "  float stipple_p = smoothstep(0.495, 0.505, len);"
			+ "  gl_FragColor = line_w * mix(u_bgcolor, u_color, min(stipple_w, stipple_p));"
			+ "}";

}
