/*
 * Copyright 2012 Hannes Janetzek
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

import org.oscim.renderer.layer.Layer;
import org.oscim.renderer.layer.TextureLayer;
import org.oscim.utils.GlUtils;

import android.opengl.GLES20;

public final class TextureRenderer {
	public final static boolean debug = false;

	private static int mTextureProgram;
	private static int hTextureMVMatrix;
	private static int hTextureProjMatrix;
	private static int hTextureVertex;
	private static int hTextureScale;
	private static int hTextureScreenScale;
	private static int hTextureTexCoord;
	private static int mIndicesVBO;

	public final static int INDICES_PER_SPRITE = 6;
	final static int VERTICES_PER_SPRITE = 4;
	final static int SHORTS_PER_VERTICE = 6;
	// per texture
	private final static int MAX_ITEMS = 50;

	static void init() {
		mTextureProgram = GlUtils.createProgram(textVertexShader,
				textFragmentShader);

		hTextureMVMatrix = GLES20.glGetUniformLocation(mTextureProgram, "u_mv");
		hTextureProjMatrix = GLES20.glGetUniformLocation(mTextureProgram, "u_proj");
		hTextureScale = GLES20.glGetUniformLocation(mTextureProgram, "u_scale");
		hTextureScreenScale = GLES20.glGetUniformLocation(mTextureProgram, "u_swidth");
		hTextureVertex = GLES20.glGetAttribLocation(mTextureProgram, "vertex");
		hTextureTexCoord = GLES20.glGetAttribLocation(mTextureProgram, "tex_coord");

		int bufferSize = MAX_ITEMS * VERTICES_PER_SPRITE
				* SHORTS_PER_VERTICE * (Short.SIZE / 8);

		ByteBuffer buf = ByteBuffer.allocateDirect(bufferSize)
				.order(ByteOrder.nativeOrder());

		ShortBuffer mShortBuffer = buf.asShortBuffer();

		// Setup triangle indices
		short[] indices = new short[MAX_ITEMS * INDICES_PER_SPRITE];
		int len = indices.length;
		short j = 0;
		for (int i = 0; i < len; i += INDICES_PER_SPRITE, j += VERTICES_PER_SPRITE) {
			indices[i + 0] = (short) (j + 0);
			indices[i + 1] = (short) (j + 1);
			indices[i + 2] = (short) (j + 2);
			indices[i + 3] = (short) (j + 2);
			indices[i + 4] = (short) (j + 3);
			indices[i + 5] = (short) (j + 0);
		}

		mShortBuffer.clear();
		mShortBuffer.put(indices, 0, len);
		mShortBuffer.flip();

		int[] mVboIds = new int[1];
		GLES20.glGenBuffers(1, mVboIds, 0);
		mIndicesVBO = mVboIds[0];

		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndicesVBO);
		GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, len * (Short.SIZE / 8),
				mShortBuffer, GLES20.GL_STATIC_DRAW);
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
	}

	public static Layer draw(Layer layer, float scale, float[] projection, float matrix[]) {
		GLState.test(false, false);
		GLES20.glEnable(GLES20.GL_BLEND);
		// GlUtils.checkGlError("draw texture >");
		GLState.useProgram(mTextureProgram);

		GLState.enableVertexArrays(hTextureTexCoord, hTextureVertex);

		TextureLayer tl = (TextureLayer) layer;

		if (tl.fixed)
			GLES20.glUniform1f(hTextureScale, (float) Math.sqrt(scale));
		else
			GLES20.glUniform1f(hTextureScale, 1);

		GLES20.glUniform1f(hTextureScreenScale, 1f / GLRenderer.mWidth);

		GLES20.glUniformMatrix4fv(hTextureProjMatrix, 1, false, projection, 0);
		GLES20.glUniformMatrix4fv(hTextureMVMatrix, 1, false, matrix, 0);

		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndicesVBO);

		for (TextureObject to = tl.textures; to != null; to = to.next) {
			//if (TextureRenderer.debug)
			//	Log.d("...", "draw texture: " + to.id + " " + to.offset + " " + to.vertices);

			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, to.id);
			int maxVertices = MAX_ITEMS * INDICES_PER_SPRITE;

			// can only draw MAX_ITEMS in each iteration
			for (int i = 0; i < to.vertices; i += maxVertices) {
				// to.offset * (24(shorts) * 2(short-bytes) / 6(indices) == 8)
				int off = (to.offset + i) * 8 + tl.offset;

				GLES20.glVertexAttribPointer(hTextureVertex, 4,
						GLES20.GL_SHORT, false, 12, off);

				GLES20.glVertexAttribPointer(hTextureTexCoord, 2,
						GLES20.GL_SHORT, false, 12, off + 8);

				int numVertices = to.vertices - i;
				if (numVertices > maxVertices)
					numVertices = maxVertices;

				GLES20.glDrawElements(GLES20.GL_TRIANGLES, numVertices,
						GLES20.GL_UNSIGNED_SHORT, 0);
			}
		}

		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

		// GlUtils.checkGlError("< draw texture");

		return layer.next;
	}

	private final static String textVertexShader = ""
			+ "precision highp float; "
			+ "attribute vec4 vertex;"
			+ "attribute vec2 tex_coord;"
			+ "uniform mat4 u_mv;"
			+ "uniform mat4 u_proj;"
			+ "uniform float u_scale;"
			+ "uniform float u_swidth;"
			+ "varying vec2 tex_c;"
			+ "const vec2 div = vec2(1.0/2048.0,1.0/2048.0);"
			+ "const float coord_scale = 0.125;"
			+ "void main() {"
			+ "  vec4 pos;"
			+ " if (mod(vertex.x, 2.0) == 0.0){"
			+ "       pos = u_proj * (u_mv * vec4(vertex.xy + vertex.zw * u_scale, 0.0, 1.0));"
			+ "  } else {" // place as billboard
			+ "    vec4 dir = u_mv * vec4(vertex.xy, 0.0, 1.0);"
			+ "    pos = u_proj * (dir + vec4(vertex.zw * (coord_scale * u_swidth), 0.1, 0.0));"
			+ "  }"
			+ "  gl_Position = pos;"
			+ "  tex_c = tex_coord * div;"
			+ "}";

	private final static String textFragmentShader = ""
			+ "precision highp float;"
			+ "uniform sampler2D tex;"
			+ "varying vec2 tex_c;"
			+ "void main() {"
			+ "   gl_FragColor = texture2D(tex, tex_c.xy);"
			+ "}";
}
