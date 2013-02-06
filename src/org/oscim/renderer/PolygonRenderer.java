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
import static android.opengl.GLES20.GL_EQUAL;
import static android.opengl.GLES20.GL_INVERT;
import static android.opengl.GLES20.GL_SHORT;
import static android.opengl.GLES20.GL_TRIANGLE_FAN;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glColorMask;
import static android.opengl.GLES20.glDepthMask;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glStencilFunc;
import static android.opengl.GLES20.glStencilMask;
import static android.opengl.GLES20.glStencilOp;
import static android.opengl.GLES20.glUniform4fv;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glVertexAttribPointer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import org.oscim.core.MapPosition;
import org.oscim.renderer.layer.Layer;
import org.oscim.renderer.layer.PolygonLayer;
import org.oscim.theme.renderinstruction.Area;
import org.oscim.utils.GlUtils;

import android.opengl.GLES20;

public final class PolygonRenderer {
	//private static final String TAG = PolygonRenderer.class.getName();

	private static final int POLYGON_VERTICES_DATA_POS_OFFSET = 0;
	private static final int STENCIL_BITS = 8;
	private final static int CLIP_BIT = 0x80;

	private static final float FADE_START = 1.3f;

	private static PolygonLayer[] mFillPolys;

	private static int polygonProgram;
	private static int hPolygonVertexPosition;
	private static int hPolygonMatrix;
	private static int hPolygonColor;

	static boolean init() {

		// Set up the program for rendering polygons
		if (GLRenderer.debugView) {
			polygonProgram = GlUtils.createProgram(polygonVertexShaderZ,
					polygonFragmentShaderZ);
		} else {
			polygonProgram = GlUtils.createProgram(polygonVertexShader,
					polygonFragmentShader);
		}
		if (polygonProgram == 0) {
			// Log.e(TAG, "Could not create polygon program.");
			return false;
		}
		hPolygonMatrix = glGetUniformLocation(polygonProgram, "u_mvp");
		hPolygonColor = glGetUniformLocation(polygonProgram, "u_color");
		hPolygonVertexPosition = glGetAttribLocation(polygonProgram, "a_pos");

		mFillPolys = new PolygonLayer[STENCIL_BITS];

		return true;
	}

	private static void fillPolygons(int start, int end, int zoom, float scale) {

		/* draw to framebuffer */
		glColorMask(true, true, true, true);

		/* do not modify stencil buffer */
		glStencilMask(0x00);

		for (int c = start; c < end; c++) {
			Area a = mFillPolys[c].area;

			if (a.fade >= zoom) {
				float f = 1.0f;
				/* fade in/out */
				if (a.fade >= zoom) {
					if (scale > FADE_START)
						f = scale - 1;
					else
						f = FADE_START - 1;
				}
				GLState.blend(true);

				if (f < 1) {
					GlUtils.setColor(hPolygonColor, a.color,
							f * a.color[3]);
				} else {
					glUniform4fv(hPolygonColor, 1, a.color, 0);
				}
			} else if (a.blend > 0 && a.blend <= zoom) {
				/* blend colors (not alpha) */
				GLState.blend(false);

				if (a.blend == zoom)
					GlUtils.setBlendColors(hPolygonColor,
							a.color, a.blendColor, scale - 1.0f);
				else
					glUniform4fv(hPolygonColor, 1, a.blendColor, 0);

			} else {
				if (a.color[3] != 1)
					GLState.blend(true);
				else
					GLState.blend(false);

				glUniform4fv(hPolygonColor, 1, a.color, 0);
			}

			// set stencil buffer mask used to draw this layer
			// also check that clip bit is set to avoid overdraw
			// of other tiles
			glStencilFunc(GL_EQUAL, 0xff, CLIP_BIT | 1 << c);

			/* draw tile fill coordinates */
			glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
		}
	}

	// current layer to fill (0 - STENCIL_BITS-1)
	private static int mCount;

	/**
	 * draw polygon layers (unil layer.next is not polygon layer)
	 * using stencil buffer method
	 *
	 * @param pos
	 *            used to fade layers accorind to 'fade'
	 *            in layer.area.
	 * @param layer
	 *            layer to draw (referencing vertices in current vbo)
	 * @param matrix
	 *            mvp matrix
	 * @param first
	 *            pass true to clear stencil buffer region
	 * @param clip
	 *            clip to first quad in current vbo
	 * @return
	 *         next layer
	 */
	public static Layer draw(MapPosition pos, Layer layer,
			float[] matrix, boolean first, boolean clip) {

		GLState.test(false, true);

		GLState.useProgram(polygonProgram);
		GLState.enableVertexArrays(hPolygonVertexPosition, -1);
		glVertexAttribPointer(hPolygonVertexPosition, 2, GL_SHORT,
				false, 0, POLYGON_VERTICES_DATA_POS_OFFSET);
		glUniformMatrix4fv(hPolygonMatrix, 1, false, matrix, 0);

		int zoom = pos.zoomLevel;
		int cur = mCount;

		// reset start when only one layer left in stencil buffer
		if (first || cur > 5)
			cur = 0;

		int start = cur;

		Layer l = layer;
		for (; l != null && l.type == Layer.POLYGON; l = l.next) {
			PolygonLayer pl = (PolygonLayer) l;

			// fade out polygon layers (set in RenderTheme)
			if (pl.area.fade > 0 && pl.area.fade > zoom)
				continue;

			if (cur == start) {
				drawStencilRegion(first);
				first = false;

				// op for stencil method polygon drawing
				glStencilOp(GLES20.GL_KEEP, GLES20.GL_KEEP, GL_INVERT);
			}

			mFillPolys[cur] = pl;

			// set stencil mask to draw to
			glStencilMask(1 << cur++);

			glDrawArrays(GL_TRIANGLE_FAN, l.offset, l.verticesCnt);

			// draw up to 7 layers into stencil buffer
			if (cur == STENCIL_BITS - 1) {
				fillPolygons(start, cur, zoom, pos.scale);
				start = cur = 0;
			}
		}

		if (cur > 0)
			fillPolygons(start, cur, zoom, pos.scale);

		if (clip) {
			if (first) {
				drawStencilRegion(first);
				// disable writes to stencil buffer
				glStencilMask(0x00);
				// enable writes to color buffer
				glColorMask(true, true, true, true);
			} else {
				// set test for clip to tile region
				glStencilFunc(GL_EQUAL, CLIP_BIT, CLIP_BIT);
			}
		}

		mCount = cur;

		return l;
	}

	/**
	 * Draw a tile filling rectangle to set stencil- and depth buffer
	 * appropriately
	 *
	 * @param first in the first run the clip region is set based on
	 *            depth buffer and depth buffer is updated
	 */
	static void drawStencilRegion(boolean first) {

		//		if (!first) {
		//			glStencilMask(0x7F);
		//			GLES20.glClear(GLES20.GL_STENCIL_BUFFER_BIT);
		//			// disable drawing to color buffer
		//			glColorMask(false, false, false, false);
		//			glStencilFunc(GL_EQUAL, CLIP_BIT, CLIP_BIT);
		//		}

		// disable drawing to color buffer
		glColorMask(false, false, false, false);

		// write to all stencil bits
		glStencilMask(0xFF);

		if (first) {
			// clear previous clip-region from stencil buffer
			//GLES20.glClear(GLES20.GL_STENCIL_BUFFER_BIT);

			// Draw clip-region into depth and stencil buffer
			// this is used for tile line and polygon layers.
			// Depth offset is increased for each tile. Together
			// with depth test (GL_LESS) this ensures to only
			// draw where no other tile has drawn yet.
			GLES20.glEnable(GLES20.GL_POLYGON_OFFSET_FILL);

			// test GL_LESS and write to depth buffer
			GLState.test(true, true);
			glDepthMask(true);

			// always pass stencil test and set clip bit
			glStencilFunc(GL_ALWAYS, CLIP_BIT, 0x00);
		} else {
			// use clip bit from stencil buffer
			// to clear stencil 'layer-bits' (0x7f)
			glStencilFunc(GL_EQUAL, CLIP_BIT, CLIP_BIT);
		}

		// set clip bit (0x80) for draw region
		glStencilOp(GLES20.GL_KEEP, GLES20.GL_KEEP, GLES20.GL_REPLACE);

		// draw a quad for the tile region
		glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

		if (first) {
			// dont modify depth buffer
			glDepthMask(false);
			// test only stencil
			GLState.test(false, true);

			GLES20.glDisable(GLES20.GL_POLYGON_OFFSET_FILL);

			glStencilFunc(GL_EQUAL, CLIP_BIT, CLIP_BIT);
		}
	}

	static void drawOver(float[] matrix) {
		if (GLState.useProgram(polygonProgram)) {

			GLState.enableVertexArrays(hPolygonVertexPosition, -1);

			glVertexAttribPointer(hPolygonVertexPosition, 2, GL_SHORT,
					false, 0, POLYGON_VERTICES_DATA_POS_OFFSET);

			glUniformMatrix4fv(hPolygonMatrix, 1, false, matrix, 0);
		}

		/*
		 * clear stencilbuffer (tile region) by drawing
		 * a quad with func 'always' and op 'zero'
		 */

		// disable drawing to framebuffer (will be re-enabled in fill)
		glColorMask(false, false, false, false);

		// always pass stencil test:
		glStencilFunc(GL_ALWAYS, 0x00, 0x00);
		// write to all bits
		glStencilMask(0xFF);
		// zero out area to draw to
		glStencilOp(GLES20.GL_KEEP, GLES20.GL_KEEP, GLES20.GL_ZERO);

		glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
		glColorMask(true, true, true, true);
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
		GLState.useProgram(polygonProgram);
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

	private final static String polygonVertexShader = ""
			+ "precision mediump float;"
			+ "uniform mat4 u_mvp;"
			+ "attribute vec4 a_pos;"
			+ "void main() {"
			+ "  gl_Position = u_mvp * a_pos;"
			+ "}";

	private final static String polygonFragmentShader = ""
			+ "precision mediump float;"
			+ "uniform vec4 u_color;"
			+ "void main() {"
			+ "  gl_FragColor = u_color;"
			+ "}";

	private final static String polygonVertexShaderZ = ""
			+ "precision highp float;"
			+ "uniform mat4 u_mvp;"
			+ "attribute vec4 a_pos;"
			+ "varying float z;"
			+ "void main() {"
			+ "  gl_Position = u_mvp * a_pos;"
			+ "  z = gl_Position.z;"
			+ "}";
	private final static String polygonFragmentShaderZ = ""
			+ "precision highp float;"
			+ "uniform vec4 u_color;"
			+ "varying float z;"
			+ "void main() {"
			+ "if (z < -1.0)"
			+ "  gl_FragColor = vec4(0.0, z + 2.0, 0.0, 1.0)*0.8;"
			+ "else if (z < 0.0)"
			+ "  gl_FragColor = vec4(z * -1.0, 0.0, 0.0, 1.0)*0.8;"
			+ "else if (z < 1.0)"
			+ "  gl_FragColor = vec4(0.0, 0.0, z, 1.0)*0.8;"
			+ "else"
			+ "  gl_FragColor = vec4(0.0, z - 1.0, 0.0, 1.0)*0.8;"
			+ "}";

	//	private final static String polygonTexVertexShader = ""
	//			+ "precision mediump float;"
	//			+ "uniform mat4 u_mvp;"
	//			+ "attribute vec4 a_pos;"
	//			+ "varying vec2 v_st;"
	//			+ "void main() {"
	//			+ "  if(gl_VertexID == 0)"
	//			+ "    v_st = vec2(0.0,0.0);"
	//			+ "  else if(gl_VertexID == 1)"
	//			+ "    v_st = vec2(1.0,0.0);"
	//			+ "  else if(gl_VertexID == 2)"
	//			+ "    v_st = vec2(1.0,1.0);"
	//			+ "  else if(gl_VertexID == 3)"
	//			+ "    v_st = vec2(0.0,1.0);"
	//			+ "  gl_Position = u_mvp * a_pos;"
	//			+ "}";
	//	private final static String polygonTexFragmentShader = ""
	//			+ "precision mediump float;"
	//			+ "uniform vec4 u_color;"
	//			+ "uniform sampler2D tex;"
	//			+ "varying vec2 v_st;"
	//			+ "void main() {"
	//			+ "  gl_FragColor =  u_color * texture2D(tex, v_st);"
	//			+ "}";
}
