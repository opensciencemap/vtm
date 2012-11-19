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

import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_EQUAL;
import static android.opengl.GLES20.GL_INVERT;
import static android.opengl.GLES20.GL_STENCIL_TEST;
import static android.opengl.GLES20.GL_TRIANGLE_FAN;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.GL_ZERO;
import static android.opengl.GLES20.glColorMask;
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

				f *= l.area.color[3];

				if (!blend) {
					glEnable(GL_BLEND);
					blend = true;
				}

				GlUtils.setColor(hPolygonColor, l.area.color, f);

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

	public static Layer draw(MapPosition pos, Layer layer,
			float[] matrix, boolean first, boolean clip) {

		int zoom = pos.zoomLevel;
		float scale = pos.scale;

		glUseProgram(polygonProgram);

		int va = hPolygonVertexPosition;
		//if (!GLRenderer.vertexArray[va]) {
		GLES20.glEnableVertexAttribArray(va);
		//	GLRenderer.vertexArray[va] = true;
		//}
		//va = va == 0 ? 1 : 0;
		//if (GLRenderer.vertexArray[va]) {
		//	GLES20.glDisableVertexAttribArray(va);
		//	GLRenderer.vertexArray[va] = false;
		//}
		// GLES20.glEnableVertexAttribArray(hPolygonVertexPosition);

		glVertexAttribPointer(hPolygonVertexPosition, 2, GLES20.GL_SHORT,
				false, 0, POLYGON_VERTICES_DATA_POS_OFFSET);

		glUniformMatrix4fv(hPolygonMatrix, 1, false, matrix, 0);

		// use stencilbuffer method for polygon drawing
		glEnable(GL_STENCIL_TEST);

		if (first) {
			mCount = 0;
			mStart = 0;
		} else {
			mStart = mCount;
		}

		Layer l = layer;

		for (; l != null && l.type == Layer.POLYGON; l = l.next) {
			PolygonLayer pl = (PolygonLayer) l;
			// fade out polygon layers (set in RederTheme)
			if (pl.area.fade > 0 && pl.area.fade > zoom)
				continue;

			if (mCount == mStart) {
				// clear stencilbuffer (tile region)

				// disable drawing to framebuffer
				glColorMask(false, false, false, false);

				// never pass the test: always apply fail op
				glStencilFunc(GLES20.GL_ALWAYS, 0, 0xFF);
				glStencilMask(0xFF);

				glStencilOp(GL_ZERO, GL_ZERO, GL_ZERO);

				if (clip) {
					// draw clip-region into depth buffer:
					// this is used for lines and polygons

					// write to depth buffer
					GLES20.glDepthMask(true);

					// to prevent overdraw gl_less restricts
					// the clip to the area where no other
					// tile has drawn
					GLES20.glDepthFunc(GLES20.GL_LESS);
				}

				glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

				if (clip) {
					first = false;
					clip = false;
					// dont modify depth buffer
					GLES20.glDepthMask(false);
					// only draw to this tile
					GLES20.glDepthFunc(GLES20.GL_EQUAL);
				}

				// stencil op for stencil method polygon drawing
				glStencilOp(GL_INVERT, GL_INVERT, GL_INVERT);

				// no need for depth test while drawing stencil
				if (clip)
					glDisable(GLES20.GL_DEPTH_TEST);

			}
			// else if (mCount == mStart) {
			// // disable drawing to framebuffer
			// glColorMask(false, false, false, false);
			//
			// // never pass the test: always apply fail op
			// glStencilFunc(GLES20.GL_ALWAYS, 0, 0xFF);
			//
			// // stencil op for stencil method polygon drawing
			// glStencilOp(GL_INVERT, GL_INVERT, GL_INVERT);
			//
			// // no need for depth test while drawing stencil
			// if (clip)
			// glDisable(GLES20.GL_DEPTH_TEST);
			// }

			mFillPolys[mCount] = pl;

			// set stencil mask to draw to
			glStencilMask(1 << mCount++);

			glDrawArrays(GL_TRIANGLE_FAN, l.offset, l.verticesCnt);

			// draw up to 8 layers into stencil buffer
			if (mCount == STENCIL_BITS) {
				/* only draw where nothing was drawn yet */
				if (clip)
					glEnable(GLES20.GL_DEPTH_TEST);

				fillPolygons(zoom, scale);
				mCount = 0;
				mStart = 0;
			}
		}

		if (mCount > 0) {
			/* only draw where nothing was drawn yet */
			if (clip)
				glEnable(GLES20.GL_DEPTH_TEST);

			fillPolygons(zoom, scale);
		}
		// maybe reset start when only few layers left in stencil buffer
		// if (mCount > 5){
		// mCount = 0;
		// mStart = 0;
		// }

		glDisable(GL_STENCIL_TEST);

		if (clip && first)
			drawDepthClip();

		GLES20.glDisableVertexAttribArray(hPolygonVertexPosition);

		return l;
	}

	private static float[] debugFillColor = { 0.3f, 0.0f, 0.0f, 0.3f };
	private static float[] debugFillColor2 = { 0.0f, 0.3f, 0.0f, 0.3f };

	private static ByteBuffer mDebugFill;

	static void debugDraw(float[] matrix, float[] coords, int color) {

		mDebugFill = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder());
		FloatBuffer buf = mDebugFill.asFloatBuffer();
		buf.put(coords);

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
		// GLES20.glDisableVertexAttribArray(hPolygonVertexPosition);
	}

	static void drawDepthClip() {
		glColorMask(false, false, false, false);
		GLES20.glDepthMask(true);
		GLES20.glDepthFunc(GLES20.GL_LESS);

		glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

		GLES20.glDepthMask(false);
		glColorMask(true, true, true, true);
		GLES20.glDepthFunc(GLES20.GL_EQUAL);
	}
}
