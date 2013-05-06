/*
 * Copyright 2012, 2013 Hannes Janetzek
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

package org.oscim.renderer.sublayers;

import org.oscim.renderer.GLRenderer;
import org.oscim.renderer.GLRenderer.Matrices;
import org.oscim.renderer.GLState;
import org.oscim.utils.GlUtils;

import android.opengl.GLES20;

// TODO merge with TextureRenderer

public final class BitmapRenderer {

	//private final static String TAG = BitmapRenderer.class.getName();
	public final static boolean debug = true;

	private static int mTextureProgram;
	private static int hTextureMVMatrix;
	private static int hTextureProjMatrix;
	private static int hTextureVertex;
	private static int hTextureScale;
	private static int hTextureScreenScale;
	private static int hTextureTexCoord;

	public final static int INDICES_PER_SPRITE = 6;
	final static int VERTICES_PER_SPRITE = 4;
	final static int SHORTS_PER_VERTICE = 6;

	static void init() {
		mTextureProgram = GlUtils.createProgram(textVertexShader,
				textFragmentShader);

		hTextureMVMatrix = GLES20.glGetUniformLocation(mTextureProgram, "u_mv");
		hTextureProjMatrix = GLES20.glGetUniformLocation(mTextureProgram, "u_proj");
		hTextureScale = GLES20.glGetUniformLocation(mTextureProgram, "u_scale");
		hTextureScreenScale = GLES20.glGetUniformLocation(mTextureProgram, "u_swidth");
		hTextureVertex = GLES20.glGetAttribLocation(mTextureProgram, "vertex");
		hTextureTexCoord = GLES20.glGetAttribLocation(mTextureProgram, "tex_coord");
	}

	public static Layer draw(Layer layer, float scale, Matrices m) {
		GLState.test(false, false);
		GLState.blend(true);

		GLState.useProgram(mTextureProgram);

		GLState.enableVertexArrays(hTextureTexCoord, hTextureVertex);

		TextureLayer tl = (TextureLayer) layer;

		if (tl.fixed)
			GLES20.glUniform1f(hTextureScale, (float) Math.sqrt(scale));
		else
			GLES20.glUniform1f(hTextureScale, 1);

		GLES20.glUniform1f(hTextureScreenScale, 1f / GLRenderer.screenWidth);

		m.proj.setAsUniform(hTextureProjMatrix);

		m.mvp.setAsUniform(hTextureMVMatrix);

		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, GLRenderer.mQuadIndicesID);

		for (TextureItem ti = tl.textures; ti != null; ti = ti.next) {
			//Log.d(TAG, "render texture " + ti.id);

			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ti.id);
			int maxVertices = GLRenderer.maxQuads * INDICES_PER_SPRITE;

			// draw up to maxVertices in each iteration
			for (int i = 0; i < ti.vertices; i += maxVertices) {
				// to.offset * (24(shorts) * 2(short-bytes) / 6(indices) == 8)
				int off = (ti.offset + i) * 8 + tl.offset;

				GLES20.glVertexAttribPointer(hTextureVertex, 4,
						GLES20.GL_SHORT, false, 12, off);

				GLES20.glVertexAttribPointer(hTextureTexCoord, 2,
						GLES20.GL_SHORT, false, 12, off + 8);

				int numVertices = ti.vertices - i;
				if (numVertices > maxVertices)
					numVertices = maxVertices;

				GLES20.glDrawElements(GLES20.GL_TRIANGLES, numVertices,
						GLES20.GL_UNSIGNED_SHORT, 0);
			}
		}

		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

		return layer.next;
	}

	//private final static double TEX_COORD_DIV = 1.0 / (1024 * COORD_SCALE);
	//private final static double COORD_DIV = 1.0 / GLRenderer.COORD_SCALE;

	private final static String textVertexShader = ""
			+ "precision mediump float; "
			+ "attribute vec4 vertex;"
			+ "attribute vec2 tex_coord;"
			+ "uniform mat4 u_mv;"
			+ "uniform mat4 u_proj;"
			+ "uniform float u_scale;"
			+ "uniform float u_swidth;"
			+ "varying vec2 tex_c;"
			//+ "const vec2 div = vec2(" + TEX_COORD_DIV + "," + TEX_COORD_DIV + ");"
			//+ "const float coord_scale = " + COORD_DIV + ";"
			+ "void main() {"
			+ "  gl_Position = u_mv * vec4(vertex.xy, 0.0, 1.0);"
			+ "  tex_c = tex_coord;" // * div;"
			+ "}";

	private final static String textFragmentShader = ""
			+ "precision mediump float;"
			+ "uniform sampler2D tex;"
			+ "varying vec2 tex_c;"
			+ "void main() {"
			+ "   gl_FragColor = texture2D(tex, tex_c.xy);"
			+ "}";
}
