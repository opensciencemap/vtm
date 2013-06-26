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
package org.oscim.renderer.sublayers;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import org.oscim.backend.GL20;
import org.oscim.backend.GLAdapter;
import org.oscim.backend.Log;
import org.oscim.core.MapPosition;
import org.oscim.renderer.GLRenderer;
import org.oscim.renderer.GLRenderer.Matrices;
import org.oscim.renderer.GLState;
import org.oscim.theme.renderinstruction.Area;
import org.oscim.utils.FastMath;
import org.oscim.utils.GlUtils;
import org.oscim.utils.Matrix4;

/**
 * Special Renderer for drawing tile polygon layers
 */
public final class PolygonRenderer {
	private static final String TAG = PolygonRenderer.class.getName();

	private static GL20 GL;

	private static final int POLYGON_VERTICES_DATA_POS_OFFSET = 0;
	private static final int STENCIL_BITS = 8;
	private final static int CLIP_BIT = 0x80;

	private static final float FADE_START = 1.3f;

	private static PolygonLayer[] mFillPolys;

	private static boolean enableTexture = false;

	private static int numShaders = 2;
	private static int polyShader = 0;
	private static int texShader = 1;

	private static int[] polygonProgram = new int[numShaders];

	private static int[] hPolygonVertexPosition = new int[numShaders];
	private static int[] hPolygonMatrix = new int[numShaders];
	private static int[] hPolygonColor = new int[numShaders];
	private static int[] hPolygonScale = new int[numShaders];

	private static int mTexWater;
	private static int mTexWood;
	private static int mTexGrass;

	static boolean init() {
		GL = GLAdapter.get();

		for (int i = 0; i < numShaders; i++) {

			// Set up the program for rendering polygons
			if (i == 0) {
				if (GLRenderer.debugView)
					polygonProgram[i] = GlUtils.createProgram(polygonVertexShaderZ,
							polygonFragmentShaderZ);
				else
					polygonProgram[i] = GlUtils.createProgram(polygonVertexShader,
							polygonFragmentShader);
			} else if (i == 1) {
				polygonProgram[i] = GlUtils.createProgram(textureVertexShader,
						textureFragmentShader);

			}

			if (polygonProgram[i] == 0) {
				Log.e(TAG, "Could not create polygon program.");
				return false;
			}
			hPolygonMatrix[i] = GL.glGetUniformLocation(polygonProgram[i], "u_mvp");
			hPolygonColor[i] = GL.glGetUniformLocation(polygonProgram[i], "u_color");
			hPolygonScale[i] = GL.glGetUniformLocation(polygonProgram[i], "u_scale");

			hPolygonVertexPosition[i] = GL.glGetAttribLocation(polygonProgram[i], "a_pos");
		}

		mFillPolys = new PolygonLayer[STENCIL_BITS];
		//		if (enableTexture) {
		//			mTexWood = loadSprite("jar:grass3.png");
		//			mTexWater = loadSprite("jar:water2.png");
		//			mTexGrass = loadSprite("jar:grass2.png");
		//		}
		return true;
	}

	//	private static int loadSprite(String name) {
	//		int[] textures = new int[1];
	//
	//		try {
	//			Bitmap b = BitmapUtils.createBitmap(name);
	//			GL.glGenTextures(1, textures, 0);
	//
	//			GL.glBindTexture(GL20.GL_TEXTURE_2D, textures[0]);
	//
	//			GlUtils.setTextureParameter(GL20.GL_LINEAR, GL20.GL_LINEAR,
	//					GL20.GL_REPEAT, GL20.GL_REPEAT);
	//
	//			b.uploadToTexture(false);
	//
	//			//GLUtils.texImage2D(GL20.GL_TEXTURE_2D, 0, b, 0);
	//
	//		} catch (IOException e) {
	//			e.printStackTrace();
	//		}
	//		return textures[0];
	//	}

	private static void fillPolygons(Matrices m, int start, int end, int zoom, float scale,
			float div) {

		/* draw to framebuffer */
		GL.glColorMask(true, true, true, true);

		/* do not modify stencil buffer */
		GL.glStencilMask(0x00);
		int shader = polyShader;

		for (int c = start; c < end; c++) {
			Area a = mFillPolys[c].area;
			if (enableTexture && (a.color == 0xFFAFC5E3
					|| a.color == 0xffd1dbc7
					|| a.color == 0xffa3ca7b)) {
				shader = texShader;
				setShader(texShader, m);

				GL.glUniform2f(hPolygonScale[1], FastMath.clamp(scale - 1, 0, 1), div);
				if (a.color == 0xFFAFC5E3)
					GL.glBindTexture(GL20.GL_TEXTURE_2D, mTexWater);
				else if (a.color == 0xffd1dbc7)
					GL.glBindTexture(GL20.GL_TEXTURE_2D, mTexWood);
				else
					GL.glBindTexture(GL20.GL_TEXTURE_2D, mTexGrass);
			} else if (a.fade >= zoom) {
				float f = 1.0f;
				/* fade in/out */
				if (a.fade >= zoom) {
					if (scale > FADE_START)
						f = scale - 1;
					else
						f = FADE_START - 1;
				}
				GLState.blend(true);

				GlUtils.setColor(hPolygonColor[shader], a.color, f);

			} else if (a.blend > 0 && a.blend <= zoom) {
				/* blend colors (not alpha) */
				GLState.blend(false);

				if (a.blend == zoom)
					GlUtils.setColorBlend(hPolygonColor[shader],
							a.color, a.blendColor, scale - 1.0f);
				else
					GlUtils.setColor(hPolygonColor[shader], a.blendColor, 1);

			} else {
				if (a.color < 0xff000000)
					GLState.blend(true);
				else
					GLState.blend(false);

				GlUtils.setColor(hPolygonColor[shader], a.color, 1);
			}

			// set stencil buffer mask used to draw this layer
			// also check that clip bit is set to avoid overdraw
			// of other tiles
			GL.glStencilFunc(GL20.GL_EQUAL, 0xff, CLIP_BIT | 1 << c);

			/* draw tile fill coordinates */
			GL.glDrawArrays(GL20.GL_TRIANGLE_STRIP, 0, 4);

			if (shader != polyShader) {
				setShader(polyShader, m);
				shader = polyShader;
			}
		}
	}

	// current layer to fill (0 - STENCIL_BITS-1)
	private static int mCount;

	private static void setShader(int shader, Matrices m) {
		//if (
		GLState.useProgram(polygonProgram[shader]);
		//		) {

		GLState.enableVertexArrays(hPolygonVertexPosition[shader], -1);

		GL.glVertexAttribPointer(hPolygonVertexPosition[shader], 2, GL20.GL_SHORT,
				false, 0, POLYGON_VERTICES_DATA_POS_OFFSET);

		m.mvp.setAsUniform(hPolygonMatrix[shader]);
		//}
	}

	/**
	 * draw polygon layers (unil layer.next is not polygon layer)
	 * using stencil buffer method
	 *
	 * @param pos
	 *            used to fade layers accorind to 'fade'
	 *            in layer.area.
	 * @param layer
	 *            layer to draw (referencing vertices in current vbo)
	 * @param m
	 *            current Matrices
	 * @param first
	 *            pass true to clear stencil buffer region
	 * @param div
	 *            scale relative to 'base scale' of the tile
	 * @param clip
	 *            clip to first quad in current vbo
	 * @return
	 *         next layer
	 */
	public static Layer draw(MapPosition pos, Layer layer,
			Matrices m, boolean first, float div, boolean clip) {

		GLState.test(false, true);

		setShader(polyShader, m);

		int zoom = pos.zoomLevel;
		float scale = (float) pos.getZoomScale();

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
				GL.glStencilOp(GL20.GL_KEEP, GL20.GL_KEEP, GL20.GL_INVERT);
			}

			mFillPolys[cur] = pl;

			// set stencil mask to draw to
			GL.glStencilMask(1 << cur++);

			GL.glDrawArrays(GL20.GL_TRIANGLE_FAN, l.offset, l.verticesCnt);

			// draw up to 7 layers into stencil buffer
			if (cur == STENCIL_BITS - 1) {
				fillPolygons(m, start, cur, zoom, scale, div);
				start = cur = 0;
			}
		}

		if (cur > 0)
			fillPolygons(m, start, cur, zoom, scale, div);

		if (clip) {
			if (first) {
				drawStencilRegion(first);
				// disable writes to stencil buffer
				GL.glStencilMask(0x00);
				// enable writes to color buffer
				GL.glColorMask(true, true, true, true);
			} else {
				// set test for clip to tile region
				GL.glStencilFunc(GL20.GL_EQUAL, CLIP_BIT, CLIP_BIT);
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
		//		 GL.glStencilMask(0x7F);
		//			GL.glClear(GL20.GL_STENCIL_BUFFER_BIT);
		//			// disable drawing to color buffer
		//		 GL.glColorMask(false, false, false, false);
		//		 GL.glStencilFunc(GL_EQUAL, CLIP_BIT, CLIP_BIT);
		//		}

		// disable drawing to color buffer
		GL.glColorMask(false, false, false, false);

		// write to all stencil bits
		GL.glStencilMask(0xFF);

		if (first) {
			// clear previous clip-region from stencil buffer
			//GL.glClear(GL20.GL_STENCIL_BUFFER_BIT);

			// Draw clip-region into depth and stencil buffer
			// this is used for tile line and polygon layers.
			// Depth offset is increased for each tile. Together
			// with depth test (GL_LESS) this ensures to only
			// draw where no other tile has drawn yet.
			GL.glEnable(GL20.GL_POLYGON_OFFSET_FILL);

			// test GL_LESS and write to depth buffer
			GLState.test(true, true);
			GL.glDepthMask(true);

			// always pass stencil test and set clip bit
			GL.glStencilFunc(GL20.GL_ALWAYS, CLIP_BIT, 0x00);
		} else {
			// use clip bit from stencil buffer
			// to clear stencil 'layer-bits' (0x7f)
			GL.glStencilFunc(GL20.GL_EQUAL, CLIP_BIT, CLIP_BIT);
		}

		// set clip bit (0x80) for draw region
		GL.glStencilOp(GL20.GL_KEEP, GL20.GL_KEEP, GL20.GL_REPLACE);

		// draw a quad for the tile region
		GL.glDrawArrays(GL20.GL_TRIANGLE_STRIP, 0, 4);

		if (first) {
			// dont modify depth buffer
			GL.glDepthMask(false);
			// test only stencil
			GLState.test(false, true);

			GL.glDisable(GL20.GL_POLYGON_OFFSET_FILL);

			GL.glStencilFunc(GL20.GL_EQUAL, CLIP_BIT, CLIP_BIT);
		}
	}

	public static void drawOver(Matrices m, boolean drawColor, int color) {
		setShader(polyShader, m);

		/*
		 * clear stencilbuffer (tile region) by drawing
		 * a quad with func 'always' and op 'zero'
		 */

		if (drawColor) {
			GlUtils.setColor(hPolygonColor[0], color, 1);
			GLState.blend(true);
		} else {
			// disable drawing to framebuffer (will be re-enabled in fill)
			GL.glColorMask(false, false, false, false);
		}
		// always pass stencil test:
		//glStencilFunc(GL_ALWAYS, 0x00, 0x00);
		GL.glStencilFunc(GL20.GL_EQUAL, CLIP_BIT, CLIP_BIT);

		// write to all bits
		GL.glStencilMask(0xFF);
		// zero out area to draw to
		GL.glStencilOp(GL20.GL_KEEP, GL20.GL_KEEP, GL20.GL_ZERO);

		GL.glDrawArrays(GL20.GL_TRIANGLE_STRIP, 0, 4);

		if (!drawColor)
			GL.glColorMask(true, true, true, true);
	}

	private static float[] debugFillColor = { 0.3f, 0.0f, 0.0f, 0.3f };
	private static float[] debugFillColor2 = { .8f, .8f, .8f, .8f };
	private static FloatBuffer mDebugFill;

	static void debugDraw(Matrix4 m, float[] coords, int color) {
		GLState.test(false, false);
		if (mDebugFill == null) {
			mDebugFill = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder())
					.asFloatBuffer();
			mDebugFill.put(coords);
		}

		GL.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);

		mDebugFill.position(0);
		GLState.useProgram(polygonProgram[0]);
		GL.glEnableVertexAttribArray(hPolygonVertexPosition[0]);

		GL.glVertexAttribPointer(hPolygonVertexPosition[0], 2, GL20.GL_FLOAT,
				false, 0, mDebugFill);

		m.setAsUniform(hPolygonMatrix[0]);

		if (color == 0)
			GlUtils.glUniform4fv(hPolygonColor[0], 1, debugFillColor);
		else
			GlUtils.glUniform4fv(hPolygonColor[0], 1, debugFillColor2);

		GL.glDrawArrays(GL20.GL_TRIANGLE_STRIP, 0, 4);

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
			+ "  gl_FragColor = vec4(z + 1.0, 0.0, 0.0, 1.0)*0.8;"
			+ "else if (z < 1.0)"
			+ "  gl_FragColor = vec4(0.0, 0.0, z, 1.0)*0.8;"
			+ "else"
			+ "  gl_FragColor = vec4(0.0, z - 1.0, 0.0, 1.0)*0.8;"
			+ "}";

	private final static String textureVertexShader = ""
			+ "precision mediump float;"
			+ "uniform mat4 u_mvp;"
			+ "uniform vec2 u_scale;"
			+ "attribute vec4 a_pos;"
			+ "varying vec2 v_st;"
			+ "varying vec2 v_st2;"
			+ "void main() {"
			+ "  v_st = clamp(a_pos.xy, 0.0, 1.0) * (2.0 / u_scale.y);"
			+ "  v_st2 = clamp(a_pos.xy, 0.0, 1.0) * (4.0 / u_scale.y);"
			+ "  gl_Position = u_mvp * a_pos;"
			+ "}";

	private final static String textureFragmentShader = ""
			+ "precision mediump float;"
			+ "uniform vec4 u_color;"
			+ "uniform sampler2D tex;"
			+ "uniform vec2 u_scale;"
			+ "varying vec2 v_st;"
			+ "varying vec2 v_st2;"
			+ "void main() {"
			+ "  gl_FragColor = mix(texture2D(tex, v_st), texture2D(tex, v_st2), u_scale.x);"
			+ "}";
}
