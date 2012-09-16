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
package org.oscim.view.renderer;

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

import java.nio.ShortBuffer;

import org.oscim.utils.GlUtils;

import android.opengl.GLES20;

class PolygonRenderer {
	private static final int NUM_VERTEX_SHORTS = 2;
	private static final int POLYGON_VERTICES_DATA_POS_OFFSET = 0;
	private static int STENCIL_BITS = 8;

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
		hPolygonMatrix = glGetUniformLocation(polygonProgram, "mvp");
		hPolygonVertexPosition = glGetAttribLocation(polygonProgram, "a_position");
		hPolygonColor = glGetUniformLocation(polygonProgram, "u_color");

		mFillPolys = new PolygonLayer[STENCIL_BITS];

		return true;
	}

	private static void fillPolygons(int count, double zoom, float scale) {
		boolean blend = false;

		// draw to framebuffer
		glColorMask(true, true, true, true);

		// do not modify stencil buffer
		glStencilMask(0);

		glEnable(GLES20.GL_DEPTH_TEST);

		for (int c = 0; c < count; c++) {
			PolygonLayer l = mFillPolys[c];

			float alpha = 1.0f;

			if (l.area.fade >= zoom || l.area.color[3] != 1.0) {
				// draw alpha blending, fade in/out
				if (l.area.fade >= zoom) {
					alpha = (scale > 1.3f ? scale : 1.3f) - alpha;
					if (alpha > 1.0f)
						alpha = 1.0f;
				}

				alpha *= l.area.color[3];

				if (!blend) {
					glEnable(GL_BLEND);
					blend = true;
				}

				GlUtils.setColor(hPolygonColor, l.area.color, alpha);

			} else if (l.area.blend == zoom) {
				// fade in/out
				alpha = scale - 1.0f;
				if (alpha > 1.0f)
					alpha = 1.0f;
				else if (alpha < 0)
					alpha = 0;

				GlUtils.setBlendColors(hPolygonColor,
						l.area.color, l.area.blendColor, alpha);

			} else {
				// draw solid
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

			// set stencil buffer mask used to draw this layer
			glStencilFunc(GL_EQUAL, 0xff, 1 << c);

			// draw tile fill coordinates
			glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
		}

		if (blend)
			glDisable(GL_BLEND);
	}

	static PolygonLayer drawPolygons(PolygonLayer layer, int next,
			float[] matrix, double zoom, float scale, boolean clip) {
		int cnt = 0;

		glUseProgram(polygonProgram);
		GLES20.glEnableVertexAttribArray(hPolygonVertexPosition);

		glVertexAttribPointer(hPolygonVertexPosition, 2,
				GLES20.GL_SHORT, false, 0,
				POLYGON_VERTICES_DATA_POS_OFFSET);

		glUniformMatrix4fv(hPolygonMatrix, 1, false, matrix, 0);

		glEnable(GL_STENCIL_TEST);

		PolygonLayer l = layer;

		boolean first = clip;

		for (; l != null && l.layer < next; l = l.next) {
			// fade out polygon layers (set in RederTheme)
			if (l.area.fade > 0 && l.area.fade > zoom)
				continue;

			if (cnt == 0) {
				// disable drawing to framebuffer
				glColorMask(false, false, false, false);

				// never pass the test, i.e. always apply first stencil op (sfail)
				glStencilFunc(GLES20.GL_ALWAYS, 0, 0xff);

				// clear stencilbuffer
				glStencilMask(0xFF);
				// glClear(GL_STENCIL_BUFFER_BIT);

				// clear stencilbuffer (tile region)
				glStencilOp(GL_ZERO, GL_ZERO, GL_ZERO);

				if (first) {
					// draw clip-region into depth buffer
					GLES20.glDepthMask(true);
					// to prevent overdraw gl_less restricts
					// the clip to the area where no other
					// tile was drawn
					GLES20.glDepthFunc(GLES20.GL_LESS);
				}

				glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

				if (first) {
					first = false;
					GLES20.glDepthMask(false);
					GLES20.glDepthFunc(GLES20.GL_EQUAL);
				}

				// stencil op for stencil method polygon drawing
				glStencilOp(GL_INVERT, GL_INVERT, GL_INVERT);

				glDisable(GLES20.GL_DEPTH_TEST);
			}
			mFillPolys[cnt] = l;

			// set stencil mask to draw to
			glStencilMask(1 << cnt++);

			glDrawArrays(GL_TRIANGLE_FAN, l.offset, l.verticesCnt);

			// draw up to 8 layers into stencil buffer
			if (cnt == STENCIL_BITS) {
				fillPolygons(cnt, zoom, scale);
				cnt = 0;
			}
		}

		if (cnt > 0)
			fillPolygons(cnt, zoom, scale);

		glDisable(GL_STENCIL_TEST);

		if (clip && first)
			drawDepthClip();

		// required on GalaxyII, Android 2.3.3 (cant just VAA enable once...)
		GLES20.glDisableVertexAttribArray(hPolygonVertexPosition);
		return l;
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

	static int sizeOf(PolygonLayer layers) {
		int size = 0;

		for (PolygonLayer l = layers; l != null; l = l.next)
			size += l.verticesCnt;

		size *= NUM_VERTEX_SHORTS;

		return size;
	}

	static void compileLayerData(PolygonLayer layers, ShortBuffer sbuf) {
		int pos = 4;

		ShortItem last = null, items = null;

		for (PolygonLayer l = layers; l != null; l = l.next) {

			for (ShortItem item = l.pool; item != null; item = item.next) {

				if (item.next == null) {
					sbuf.put(item.vertices, 0, item.used);
				} else {
					// item.used = ShortItem.SIZE;
					sbuf.put(item.vertices);
				}

				last = item;
			}

			l.offset = pos;
			pos += l.verticesCnt;

			if (last != null) {
				last.next = items;
				items = l.pool;
			}

			l.pool = null;
		}

		ShortPool.add(items);
	}

	static void clear(PolygonLayer layers) {
		for (PolygonLayer l = layers; l != null; l = l.next) {
			if (l.pool != null)
				ShortPool.add(l.pool);
		}
	}
}
