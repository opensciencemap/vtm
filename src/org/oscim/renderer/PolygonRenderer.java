/*
 * Copyright 2012 Hannes Janetzek
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General License for more details.
 *
 * You should have received a copy of the GNU Lesser General License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.oscim.renderer;

import static android.opengl.GLES20.GL_ALWAYS;
import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_EQUAL;
import static android.opengl.GLES20.GL_INVERT;
import static android.opengl.GLES20.GL_LESS;
import static android.opengl.GLES20.GL_SHORT;
import static android.opengl.GLES20.GL_TRIANGLE_FAN;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.GL_ZERO;
import static android.opengl.GLES20.glColorMask;
import static android.opengl.GLES20.glDepthFunc;
import static android.opengl.GLES20.glDepthMask;
import static android.opengl.GLES20.glDisable;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glStencilFunc;
import static android.opengl.GLES20.glStencilMask;
import static android.opengl.GLES20.glStencilOp;
import static android.opengl.GLES20.glUniform4fv;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import org.oscim.core.MapPosition;
import org.oscim.renderer.layer.Layer;
import org.oscim.renderer.layer.PolygonLayer;
import org.oscim.utils.GlUtils;

import android.opengl.GLES20;

public final class PolygonRenderer {
	// private static final String TAG = "PolygonRenderer";

	// private static final int NUM_VERTEX_SHORTS = 2;
	private static final int POLYGON_VERTICES_DATA_POS_OFFSET = 0;
	private static final int STENCIL_BITS = 8;

	private static final float FADE_START = 1.3f;

	private static PolygonLayer[] mFillPolys;

	private static int polygonProgram;
	private static int hPolygonVertexPosition;
	private static int hPolygonMatrix;
	private static int hPolygonColor;

	static boolean init() {

		// Set up the program for rendering polygons
		//		polygonProgram = GlUtils.createProgram(Shaders.polygonVertexShaderZ,
		//				Shaders.polygonFragmentShaderZ);
		polygonProgram = GlUtils.createProgram(Shaders.polygonVertexShader,
				Shaders.polygonFragmentShader);

		if (polygonProgram == 0) {
			// Log.e(TAG, "Could not create polygon program.");
			return false;
		}
		hPolygonMatrix = glGetUniformLocation(polygonProgram, "u_mvp");
		hPolygonColor = glGetUniformLocation(polygonProgram, "u_color");
		hPolygonVertexPosition = glGetAttribLocation(polygonProgram, "a_position");

		mFillPolys = new PolygonLayer[STENCIL_BITS];

		return true;
	}

	private static void fillPolygons(int zoom, float scale) {
		boolean blend = false;

		/* draw to framebuffer */
		glColorMask(true, true, true, true);

		/* do not modify stencil buffer */
		glStencilMask(0);

		for (int c = mStart; c < mCount; c++) {
			PolygonLayer l = mFillPolys[c];

			float f = 1.0f;

			if (l.area.fade >= zoom || l.area.color[3] != 1.0) {
				/* fade in/out || draw alpha color */
				if (l.area.fade >= zoom) {
					f = (scale > FADE_START ? scale : FADE_START) - f;
					if (f > 1.0f)
						f = 1.0f;
				}

				if (!blend) {
					glEnable(GL_BLEND);
					blend = true;
				}
				if (f != 1) {
					f *= l.area.color[3];
					GlUtils.setColor(hPolygonColor, l.area.color, f);
				} else {
					glUniform4fv(hPolygonColor, 1, l.area.color, 0);
				}
			} else if (l.area.blend == zoom) {
				/* blend colors */
				f = scale - 1.0f;
				if (f > 1.0f)
					f = 1.0f;
				else if (f < 0)
					f = 0;

				GlUtils.setBlendColors(hPolygonColor,
						l.area.color, l.area.blendColor, f);

			} else {
				/* draw solid */
				if (blend) {
					glDisable(GL_BLEND);
					blend = false;
				}
				if (l.area.blend <= zoom && l.area.blend > 0)
					glUniform4fv(hPolygonColor, 1, l.area.blendColor, 0);
				else
					glUniform4fv(hPolygonColor, 1, l.area.color, 0);
			}

			// if (alpha < 1) {
			// if (!blend) {
			// glEnable(GL_BLEND);
			// blend = true;
			// }
			// } else if (blend) {
			// glDisable(GL_BLEND);
			// blend = false;
			// }

			/* set stencil buffer mask used to draw this layer */
			glStencilFunc(GL_EQUAL, 0xff, 1 << c);

			/* draw tile fill coordinates */
			glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
		}

		if (blend)
			glDisable(GL_BLEND);
	}

	// layers to fill
	private static int mCount;
	// stencil buffer index to start fill
	private static int mStart;

	/**
	 * draw polygon layers (unil layer.next is not polygon layer)
	 * using stencil buffer method
	 * @param pos
	 *            used to fade layers accorind to 'fade'
	 *            in layer.area.
	 * @param layer
	 *            layer to draw (referencing vertices in current vbo)
	 * @param matrix
	 *            mvp matrix
	 * @param first
	 *            pass true to clear stencil buffer region
	 * @param drawClipped
	 *            clip to first quad in current vbo
	 * @return
	 *         next layer
	 */
	public static Layer draw(MapPosition pos, Layer layer,
			float[] matrix, boolean first, boolean drawClipped) {

		int zoom = pos.zoomLevel;
		float scale = pos.scale;

		glUseProgram(polygonProgram);

		GLState.enableVertexArrays(hPolygonVertexPosition, -1);

		glVertexAttribPointer(hPolygonVertexPosition, 2, GL_SHORT,
				false, 0, POLYGON_VERTICES_DATA_POS_OFFSET);

		glUniformMatrix4fv(hPolygonMatrix, 1, false, matrix, 0);

		// reset start when only two layer left in stencil buffer
		//	if (mCount > 5) {
		//	mCount = 0;
		//	mStart = 0;
		//	} 

		if (first) {
			mCount = 0;
			mStart = 0;
		} else {
			mStart = mCount;
		}

		GLState.test(drawClipped, true);

		Layer l = layer;

		for (; l != null && l.type == Layer.POLYGON; l = l.next) {
			PolygonLayer pl = (PolygonLayer) l;
			// fade out polygon layers (set in RederTheme)
			if (pl.area.fade > 0 && pl.area.fade > zoom)
				continue;

			if (mCount == mStart) {
				// clear stencilbuffer (tile region) by drawing
				// a quad with func 'always' and op 'zero'

				// disable drawing to framebuffer
				glColorMask(false, false, false, false);

				// never pass the test: always apply fail op
				glStencilFunc(GL_ALWAYS, 0, 0xFF);
				glStencilMask(0xFF);
				glStencilOp(GL_ZERO, GL_ZERO, GL_ZERO);

				// draw clip-region into depth buffer:
				// this is used for lines and polygons
				if (first && drawClipped) {
					// write to depth buffer
					glDepthMask(true);

					// to prevent overdraw gl_less restricts the 
					// clip to the area where no other tile has drawn
					glDepthFunc(GL_LESS);
				}

				glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

				if (first && drawClipped) {
					first = false;
					// do not modify depth buffer anymore
					glDepthMask(false);
					// only draw to this tile
					glDepthFunc(GL_EQUAL);
				}

				// op for stencil method polygon drawing
				glStencilOp(GL_INVERT, GL_INVERT, GL_INVERT);
			}

			// no need for depth test while drawing stencil
			GLState.test(false, true);

			mFillPolys[mCount] = pl;

			// set stencil mask to draw to
			glStencilMask(1 << mCount++);

			glDrawArrays(GL_TRIANGLE_FAN, l.offset, l.verticesCnt);

			// draw up to 8 layers into stencil buffer
			if (mCount == STENCIL_BITS) {
				/* only draw where nothing was drawn yet */
				if (drawClipped)
					GLState.test(true, true);

				fillPolygons(zoom, scale);
				mCount = 0;
				mStart = 0;
			}
		}

		if (mCount > 0) {
			/* only draw where nothing was drawn yet */
			if (drawClipped)
				GLState.test(true, true);

			fillPolygons(zoom, scale);
		}

		if (drawClipped && first) {
			GLState.test(true, false);
			GLES20.glColorMask(false, false, false, false);
			GLES20.glDepthMask(true);
			GLES20.glDepthFunc(GLES20.GL_LESS);

			GLES20.glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

			GLES20.glDepthMask(false);
			GLES20.glColorMask(true, true, true, true);
			GLES20.glDepthFunc(GLES20.GL_EQUAL);
		}
		return l;
	}

	private static float[] debugFillColor = { 0.3f, 0.0f, 0.0f, 0.3f };
	private static float[] debugFillColor2 = { .8f, .8f, .8f, .8f };
	private static FloatBuffer mDebugFill;

	static void debugDraw(float[] matrix, float[] coords, int color) {
		GLState.test(false, false);
		if (mDebugFill == null)
			mDebugFill = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder())
					.asFloatBuffer();

		mDebugFill.put(coords);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

		mDebugFill.position(0);
		glUseProgram(polygonProgram);
		GLES20.glEnableVertexAttribArray(hPolygonVertexPosition);

		glVertexAttribPointer(hPolygonVertexPosition, 2, GLES20.GL_FLOAT,
				false, 0, mDebugFill);

		glUniformMatrix4fv(hPolygonMatrix, 1, false, matrix, 0);

		if (color == 0)
			glUniform4fv(hPolygonColor, 1, debugFillColor, 0);
		else
			glUniform4fv(hPolygonColor, 1, debugFillColor2, 0);

		glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

		GlUtils.checkGlError("draw debug");
	}
}
