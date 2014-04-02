/*
 * Copyright 2012 Hannes Janetzek
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
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
package org.oscim.renderer.elements;

import org.oscim.backend.GL20;
import org.oscim.core.GeometryBuffer;
import org.oscim.core.Tile;
import org.oscim.renderer.GLShader;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLUtils;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.MapRenderer;
import org.oscim.theme.styles.AreaStyle;
import org.oscim.utils.FastMath;
import org.oscim.utils.math.Interpolation;
import org.oscim.utils.pool.Inlist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Special Renderer for drawing tile polygons using the stencil buffer method
 */
public final class PolygonLayer extends RenderElement {
	static final Logger log = LoggerFactory.getLogger(PolygonLayer.class);

	public final static int CLIP_STENCIL = 1;
	public final static int CLIP_DEPTH = 2;

	private static final float S = MapRenderer.COORD_SCALE;

	private static final boolean enableTexture = true;

	public AreaStyle area;

	PolygonLayer(int layer) {
		super(RenderElement.POLYGON);

		level = layer;
		vertexItems = VertexItem.pool.get();
	}

	public void addPolygon(GeometryBuffer geom) {
		addPolygon(geom.points, geom.index);
	}

	public void addPolygon(float[] points, short[] index) {
		short center = (short) ((Tile.SIZE >> 1) * S);

		VertexItem si = Inlist.last(vertexItems);
		short[] v = si.vertices;
		int outPos = si.used;

		for (int i = 0, pos = 0, n = index.length; i < n; i++) {
			int length = index[i];
			if (length < 0)
				break;

			// need at least three points
			if (length < 6) {
				pos += length;
				continue;
			}

			numVertices += length / 2 + 2;

			int inPos = pos;

			if (outPos == VertexItem.SIZE) {
				si = VertexItem.pool.getNext(si);
				v = si.vertices;
				outPos = 0;
			}

			v[outPos++] = center;
			v[outPos++] = center;

			for (int j = 0; j < length; j += 2) {
				if (outPos == VertexItem.SIZE) {
					si = VertexItem.pool.getNext(si);
					v = si.vertices;
					outPos = 0;
				}
				v[outPos++] = (short) (points[inPos++] * S);
				v[outPos++] = (short) (points[inPos++] * S);
			}

			if (outPos == VertexItem.SIZE) {
				si = VertexItem.pool.getNext(si);
				v = si.vertices;
				outPos = 0;
			}

			v[outPos++] = (short) (points[pos + 0] * S);
			v[outPos++] = (short) (points[pos + 1] * S);

			pos += length;
		}

		si.used = outPos;
	}

	static class Shader extends GLShader {
		int uMVP, uColor, uScale, aPos;

		Shader(String shaderFile) {
			if (!create(shaderFile))
				return;

			uMVP = getUniform("u_mvp");
			aPos = getAttrib("a_pos");

			if (shaderFile == "polygon_layer_tex")
				uScale = getUniform("u_scale");
			else
				uColor = getUniform("u_color");
		}
	}

	public static final class Renderer {

		private static final int STENCIL_BITS = 8;
		private final static int CLIP_BIT = 0x80;

		private static final float FADE_START = 1.3f;

		private static AreaStyle[] mAreaFills;

		private static Shader polyShader;
		private static Shader texShader;

		static boolean init() {
			polyShader = new Shader("base_shader");
			texShader = new Shader("polygon_layer_tex");

			mAreaFills = new AreaStyle[STENCIL_BITS];

			return true;
		}

		private static void fillPolygons(GLViewport v, int start, int end, int zoom,
		        float scale, float div) {

			/* draw to framebuffer */
			GL.glColorMask(true, true, true, true);

			/* do not modify stencil buffer */
			GL.glStencilMask(0x00);
			//int shader = polyShader;

			Shader s = setShader(polyShader, v, false);

			for (int c = start; c < end; c++) {
				AreaStyle a = mAreaFills[c].current();

				if (enableTexture && a.texture != null) {
					s = setShader(texShader, v, false);
					float num = FastMath.clamp((Tile.SIZE / a.texture.width) >> 1, 1, Tile.SIZE);
					float transition = Interpolation.exp5.apply(FastMath.clamp(scale - 1, 0, 1));
					GL.glUniform2f(s.uScale, transition, div / num);

					//if (a.texture.alpha);
					GLState.blend(true);
					a.texture.bind();

				} else if (a.fadeScale >= zoom) {
					float f = 1.0f;
					/* fade in/out */
					if (a.fadeScale >= zoom) {
						if (scale > FADE_START)
							f = scale - 1;
						else
							f = FADE_START - 1;
					}
					GLState.blend(true);

					GLUtils.setColor(s.uColor, a.color, f);

				} else if (a.blendScale > 0 && a.blendScale <= zoom) {
					/* blend colors (not alpha) */
					GLState.blend(false);

					if (a.blendScale == zoom)
						GLUtils.setColorBlend(s.uColor, a.color,
						                      a.blendColor, scale - 1.0f);
					else
						GLUtils.setColor(s.uColor, a.blendColor, 1);

				} else {
					if (a.color < 0xff000000)
						GLState.blend(true);
					else
						GLState.blend(false);

					GLUtils.setColor(s.uColor, a.color, 1);
				}

				/* set stencil buffer mask used to draw this layer
				 * also check that clip bit is set to avoid overdraw
				 * of other tiles */
				GL.glStencilFunc(GL20.GL_EQUAL, 0xff, CLIP_BIT | 1 << c);

				/* draw tile fill coordinates */
				GL.glDrawArrays(GL20.GL_TRIANGLE_STRIP, 0, 4);

				/* disable texture shader */
				if (s != polyShader)
					s = setShader(polyShader, v, false);
			}
		}

		// current layer to fill (0 - STENCIL_BITS-1)
		private static int mCount;

		private static Shader setShader(Shader shader, GLViewport v, boolean first) {
			if (shader.useProgram() || first) {

				GLState.enableVertexArrays(shader.aPos, -1);

				GL.glVertexAttribPointer(shader.aPos, 2,
				                         GL20.GL_SHORT, false, 0, 0);

				v.mvp.setAsUniform(shader.uMVP);
			}
			return shader;
		}

		/**
		 * draw polygon layers (until layer.next is not polygon layer)
		 * using stencil buffer method
		 * 
		 * @param renderElement
		 *            layer to draw (referencing vertices in current vbo)
		 * @param v
		 *            GLViewport
		 * @param pos
		 *            used to fade layers according to 'fade' in
		 *            layer.area style
		 * @param div
		 *            scale relative to 'base scale' of the tile
		 * @param first
		 *            pass true to clear stencil buffer region
		 * @param clipMode
		 *            clip to first quad in current vbo
		 *            using CLIP_STENCIL / CLIP_DEPTH
		 * 
		 * @return
		 *         next layer
		 */
		public static RenderElement draw(RenderElement renderElement, GLViewport v,
		        float div, boolean first, int clipMode) {

			GLState.test(false, true);

			setShader(polyShader, v, first);

			int zoom = v.pos.zoomLevel;
			float scale = (float) v.pos.getZoomScale();

			int cur = mCount;

			/* reset start when only one layer left in stencil buffer */
			if (first || cur > 5)
				cur = 0;

			int start = cur;

			RenderElement l = renderElement;
			for (; l != null && l.type == RenderElement.POLYGON; l = l.next) {
				PolygonLayer pl = (PolygonLayer) l;

				/* fade out polygon layers (set in RenderTheme) */
				if (pl.area.fadeScale > 0 && pl.area.fadeScale > zoom)
					continue;

				if (cur == start) {
					drawStencilRegion(first, clipMode);
					first = false;

					/* op for stencil method polygon drawing */
					GL.glStencilOp(GL20.GL_KEEP, GL20.GL_KEEP, GL20.GL_INVERT);
				}

				mAreaFills[cur] = pl.area.current();

				/* set stencil mask to draw to */
				GL.glStencilMask(1 << cur++);

				GL.glDrawArrays(GL20.GL_TRIANGLE_FAN, l.offset, l.numVertices);

				/* draw up to 7 layers into stencil buffer */
				if (cur == STENCIL_BITS - 1) {
					fillPolygons(v, start, cur, zoom, scale, div);
					start = cur = 0;
				}
			}

			if (cur > 0)
				fillPolygons(v, start, cur, zoom, scale, div);

			if (clipMode > 0) {
				if (first) {
					drawStencilRegion(first, clipMode);
					/* disable writes to stencil buffer */
					GL.glStencilMask(0x00);
					/* enable writes to color buffer */
					GL.glColorMask(true, true, true, true);
				} else {
					/* set test for clip to tile region */
					GL.glStencilFunc(GL20.GL_EQUAL, CLIP_BIT, CLIP_BIT);
				}
			}

			mCount = cur;

			return l;
		}

		public static void clip(GLViewport v) {
			setShader(polyShader, v, true);

			drawStencilRegion(true, 1);
			/* disable writes to stencil buffer */
			GL.glStencilMask(0x00);
			/* enable writes to color buffer */
			GL.glColorMask(true, true, true, true);
		}

		/**
		 * Draw a tile filling rectangle to set stencil- and depth buffer
		 * appropriately
		 * 
		 * @param first in the first run the clip region is set based on
		 *            depth buffer and depth buffer is updated
		 */
		static void drawStencilRegion(boolean first, int clipMode) {

			/* disable drawing to color buffer */
			GL.glColorMask(false, false, false, false);

			/* write to all stencil bits */
			GL.glStencilMask(0xFF);

			if (first) {
				/* Draw clip-region into depth and stencil buffer.
				 * This is used for tile line and polygon layers.
				 * 
				 * Together with depth test (GL_LESS) this ensures to
				 * only draw where no other tile has drawn yet. */

				if (clipMode == CLIP_DEPTH) {
					/* test GL_LESS/GL_ALWAYS to write to depth buffer */
					GLState.test(true, true);
					GL.glDepthMask(true);
				}

				/* always pass stencil test and set clip bit */
				GL.glStencilFunc(GL20.GL_ALWAYS, CLIP_BIT, 0x00);
			} else {
				/* use clip bit from stencil buffer to clear stencil
				 * 'layer-bits' (0x7f) */
				GL.glStencilFunc(GL20.GL_EQUAL, CLIP_BIT, CLIP_BIT);
			}

			/* set clip bit (0x80) for draw region */
			GL.glStencilOp(GL20.GL_KEEP, GL20.GL_KEEP, GL20.GL_REPLACE);

			/* draw a quad for the tile region */
			GL.glDrawArrays(GL20.GL_TRIANGLE_STRIP, 0, 4);

			if (first) {
				if (clipMode == CLIP_DEPTH) {
					/* dont modify depth buffer */
					GL.glDepthMask(false);
					GLState.test(false, true);
				}
				GL.glStencilFunc(GL20.GL_EQUAL, CLIP_BIT, CLIP_BIT);
			}
		}

		/**
		 * Clear stencilbuffer for a tile region by drawing
		 * a quad with func 'always' and op 'zero'. Using 'color'
		 * and 'alpha' to fake a fade effect.
		 */
		public static void drawOver(GLViewport v, int color, float alpha) {
			// TODO true could be avoided when same shader and vbo
			setShader(polyShader, v, true);

			if (color == 0) {
				/* disable drawing to framebuffer (will be re-enabled in fill) */
				GL.glColorMask(false, false, false, false);
			} else {
				GLUtils.setColor(polyShader.uColor, color, alpha);
				GLState.blend(true);
			}

			// TODO always pass stencil test: <-- only if not proxy?
			//GL.glStencilFunc(GL_ALWAYS, 0x00, 0x00);

			GL.glStencilFunc(GL20.GL_EQUAL, CLIP_BIT, CLIP_BIT);

			/* write to all bits */
			GL.glStencilMask(0xFF);

			/* zero out area to draw to */
			GL.glStencilOp(GL20.GL_KEEP, GL20.GL_KEEP, GL20.GL_ZERO);

			GL.glDrawArrays(GL20.GL_TRIANGLE_STRIP, 0, 4);

			// FIXME needed here?
			if (color == 0)
				GL.glColorMask(true, true, true, true);
		}

		//private static float[] debugFillColor = { 0.3f, 0.0f, 0.0f, 0.3f };
		//private static float[] debugFillColor2 = { .8f, .8f, .8f, .8f };
		//private static FloatBuffer mDebugFill;

		//static void debugDraw(GLMatrix m, float[] coords, int color) {
		//	GLState.test(false, false);
		//	if (mDebugFill == null) {
		//		mDebugFill = ByteBuffer
		//		    .allocateDirect(32)
		//		    .order(ByteOrder.nativeOrder())
		//		    .asFloatBuffer();
		//		mDebugFill.put(coords);
		//	}
		//
		//	GL.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
		//
		//	mDebugFill.position(0);
		//	GLState.useProgram(polygonProgram[0]);
		//	GL.glEnableVertexAttribArray(hPolygonVertexPosition[0]);
		//
		//	GL.glVertexAttribPointer(hPolygonVertexPosition[0], 2, GL20.GL_FLOAT,
		//	                         false, 0, mDebugFill);
		//
		//	m.setAsUniform(hPolygonMatrix[0]);
		//
		//	if (color == 0)
		//		GLUtils.glUniform4fv(hPolygonColor[0], 1, debugFillColor);
		//	else
		//		GLUtils.glUniform4fv(hPolygonColor[0], 1, debugFillColor2);
		//
		//	GL.glDrawArrays(GL20.GL_TRIANGLE_STRIP, 0, 4);
		//
		//	GLUtils.checkGlError("draw debug");
		//}

		private Renderer() {
			/* Singleton */
		}
	}
}
