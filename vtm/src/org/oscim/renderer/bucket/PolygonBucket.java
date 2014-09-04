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
package org.oscim.renderer.bucket;

import static org.oscim.backend.GL20.GL_ALWAYS;
import static org.oscim.backend.GL20.GL_EQUAL;
import static org.oscim.backend.GL20.GL_INVERT;
import static org.oscim.backend.GL20.GL_KEEP;
import static org.oscim.backend.GL20.GL_LINES;
import static org.oscim.backend.GL20.GL_REPLACE;
import static org.oscim.backend.GL20.GL_SHORT;
import static org.oscim.backend.GL20.GL_TRIANGLE_FAN;
import static org.oscim.backend.GL20.GL_TRIANGLE_STRIP;
import static org.oscim.backend.GL20.GL_UNSIGNED_SHORT;
import static org.oscim.backend.GL20.GL_ZERO;
import static org.oscim.utils.FastMath.clamp;

import java.nio.ShortBuffer;

import org.oscim.core.GeometryBuffer;
import org.oscim.core.Tile;
import org.oscim.renderer.GLMatrix;
import org.oscim.renderer.GLShader;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLUtils;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.MapRenderer;
import org.oscim.theme.styles.AreaStyle;
import org.oscim.utils.geom.LineClipper;
import org.oscim.utils.math.Interpolation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Special Renderer for drawing tile polygons using the stencil buffer method
 */
public final class PolygonBucket extends RenderBucket {

	static final Logger log = LoggerFactory.getLogger(PolygonBucket.class);

	public final static int CLIP_STENCIL = 1;
	public final static int CLIP_DEPTH = 2;
	public final static int CLIP_TEST_DEPTH = 3;

	private static final float S = MapRenderer.COORD_SCALE;

	private static final boolean enableTexture = true;

	public AreaStyle area;

	PolygonBucket(int layer) {
		super(RenderBucket.POLYGON);
		level = layer;
	}

	public void addPolygon(GeometryBuffer geom) {
		addPolygon(geom.points, geom.index);
	}

	float xmin = Short.MAX_VALUE;
	float ymin = Short.MAX_VALUE;
	float xmax = Short.MIN_VALUE;
	float ymax = Short.MIN_VALUE;
	float[] bbox = new float[8];

	public void addPolygon(float[] points, int[] index) {
		short center = (short) ((Tile.SIZE >> 1) * S);

		boolean outline = area.strokeWidth > 0;

		for (int i = 0, pos = 0, n = index.length; i < n; i++) {
			int length = index[i];
			if (length < 0)
				break;

			/* need at least three points */
			if (length < 6) {
				pos += length;
				continue;
			}

			vertexItems.add(center, center);
			numVertices++;

			int inPos = pos;

			for (int j = 0; j < length; j += 2) {
				float x = (points[inPos++] * S);
				float y = (points[inPos++] * S);
				if (x > xmax)
					xmax = x;
				if (x < xmin)
					xmin = x;

				if (y > ymax)
					ymax = y;
				if (y < ymin)
					ymin = y;

				if (outline) {
					indiceItems.add((short) numVertices);
					numIndices++;
				}

				vertexItems.add((short) x, (short) y);
				numVertices++;

				if (outline) {
					indiceItems.add((short) numVertices);
					numIndices++;
				}
			}

			vertexItems.add((short) (points[pos + 0] * S),
			                (short) (points[pos + 1] * S));
			numVertices++;

			pos += length;
		}
	}

	// FIXME move to prepare
	@Override
	protected void compile(ShortBuffer vboData, ShortBuffer iboData) {
		if (area.strokeWidth == 0) {
			/* add vertices to shared VBO */
			compileVertexItems(vboData);
			return;
		}

		bbox[0] = xmin;
		bbox[1] = ymin;

		bbox[2] = xmax;
		bbox[3] = ymin;

		bbox[4] = xmax;
		bbox[5] = ymax;

		bbox[6] = xmin;
		bbox[7] = xmax;

		/* compile with indexed outline */
		super.compile(vboData, iboData);
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

		private static final int OPAQUE = 0xff000000;
		private static final int STENCIL_BITS = 8;
		public final static int CLIP_BIT = 0x80;

		private static final float FADE_START = 1.3f;

		private static PolygonBucket[] mAreaLayer;

		private static Shader polyShader;
		private static Shader texShader;

		static boolean init() {
			polyShader = new Shader("base_shader");
			texShader = new Shader("polygon_layer_tex");

			mAreaLayer = new PolygonBucket[STENCIL_BITS];

			return true;
		}

		private static void fillPolygons(GLViewport v, int start, int end, int zoom,
		        float scale, float div) {

			/* draw to framebuffer */
			GL.glColorMask(true, true, true, true);

			/* do not modify stencil buffer */
			GL.glStencilMask(0x00);
			Shader s;

			for (int i = start; i < end; i++) {
				PolygonBucket l = mAreaLayer[i];
				AreaStyle a = l.area.current();

				boolean useTexture = enableTexture && a.texture != null;
				if (useTexture)
					s = setShader(texShader, v.mvp, false);
				else
					s = setShader(polyShader, v.mvp, false);

				if (useTexture) {
					float num = clamp((Tile.SIZE / a.texture.width) >> 1, 1, Tile.SIZE);
					float transition = Interpolation.exp5.apply(clamp(scale - 1, 0, 1));
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
					/* test if color contains alpha */
					GLState.blend((a.color & OPAQUE) != OPAQUE);

					GLUtils.setColor(s.uColor, a.color, 1);
				}

				/* set stencil buffer mask used to draw this layer
				 * also check that clip bit is set to avoid overdraw
				 * of other tiles */
				GL.glStencilFunc(GL_EQUAL, 0xff, CLIP_BIT | 1 << i);

				/* draw tile fill coordinates */
				GL.glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

				if (a.strokeWidth <= 0)
					continue;

				GL.glStencilFunc(GL_EQUAL, CLIP_BIT, CLIP_BIT);

				GLState.blend(true);

				HairLineBucket.Renderer.shader.set(v);

				GLUtils.setColor(HairLineBucket.Renderer.shader.uColor,
				                 l.area.strokeColor, 1);

				GL.glVertexAttribPointer(HairLineBucket.Renderer.shader.aPos,
				                         2, GL_SHORT, false, 0,
				                         // 4 bytes per vertex
				                         l.vertexOffset << 2);

				GL.glUniform1f(HairLineBucket.Renderer.shader.uWidth,
				               a.strokeWidth);

				GL.glDrawElements(GL_LINES,
				                  l.numIndices,
				                  GL_UNSIGNED_SHORT,
				                  l.indiceOffset);
				GL.glLineWidth(1);

				///* disable texture shader */
				//if (s != polyShader)
				//	s = setShader(polyShader, v.mvp, false);
			}
		}

		/** current layer to fill (0 - STENCIL_BITS-1) */
		private static int mCount;
		/** must clear stencil for next draw */
		private static boolean mClear;

		private static Shader setShader(Shader shader, GLMatrix mvp, boolean first) {
			if (shader.useProgram() || first) {
				GLState.enableVertexArrays(shader.aPos, -1);

				GL.glVertexAttribPointer(shader.aPos, 2,
				                         GL_SHORT, false, 0, 0);

				mvp.setAsUniform(shader.uMVP);
			}
			return shader;
		}

		static float[] mBBox = new float[8];
		static LineClipper mScreenClip = new LineClipper(-1, -1, 1, 1);

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
		public static RenderBucket draw(RenderBucket renderElement, GLViewport v,
		        float div, boolean first) {

			GLState.test(false, true);

			setShader(polyShader, v.mvp, first);

			int zoom = v.pos.zoomLevel;
			float scale = (float) v.pos.getZoomScale();

			int cur = mCount;
			int start = mCount;

			/* draw to stencil buffer */
			GL.glColorMask(false, false, false, false);

			/* op for stencil method polygon drawing */
			GL.glStencilOp(GL_KEEP, GL_KEEP, GL_INVERT);

			boolean drawn = false;

			byte stencilMask = 0;

			RenderBucket l = renderElement;
			for (; l != null && l.type == POLYGON; l = l.next) {
				PolygonBucket pl = (PolygonBucket) l;
				AreaStyle area = pl.area.current();

				/* fade out polygon layers (set in RenderTheme) */
				if (area.fadeScale > 0 && area.fadeScale > zoom)
					continue;

				//	v.mvp.prj2D(pl.bbox, 0, mBBox, 0, 4);
				//	mScreenClip.clipStart(mBBox[0], mBBox[1]);
				//
				//	if (mScreenClip.clipNext(mBBox[2], mBBox[3]) == 0 &&
				//	        mScreenClip.clipNext(mBBox[4], mBBox[5]) == 0 &&
				//	        mScreenClip.clipNext(mBBox[6], mBBox[7]) == 0 &&
				//	        mScreenClip.clipNext(mBBox[0], mBBox[1]) == 0) {
				//
				//		/* check the very unlikely case where the view might be
				//		 * completly contained within mBBox */
				//		if (!ArrayUtils.withinRange(mBBox, -1f, 1f))
				//			continue;
				//	}

				if (mClear) {
					clearStencilRegion();
					/* op for stencil method polygon drawing */
					GL.glStencilOp(GL_KEEP, GL_KEEP, GL_INVERT);

					start = cur = 0;
				}

				mAreaLayer[cur] = pl;

				/* set stencil mask to draw to */
				int stencil = 1 << cur++;

				if (area.hasAlpha(zoom)) {
					GL.glStencilMask(stencil);
					stencilMask |= stencil;
				}
				else {
					stencilMask |= stencil;
					GL.glStencilMask(stencilMask);
				}

				GL.glDrawArrays(GL_TRIANGLE_FAN, l.vertexOffset, l.numVertices);

				/* draw up to 7 layers into stencil buffer */
				if (cur == STENCIL_BITS - 1) {
					//log.debug("fill1 {} {}", start, cur);
					fillPolygons(v, start, cur, zoom, scale, div);
					drawn = true;

					mClear = true;
					start = cur = 0;

					if (l.next != null && l.next.type == POLYGON) {
						setShader(polyShader, v.mvp, false);
						stencilMask = 0;
					}
				}
			}

			if (cur > 0) {
				//log.debug("fill2 {} {}", start, cur);
				fillPolygons(v, start, cur, zoom, scale, div);
				drawn = true;
			}

			if (!drawn) {
				/* fillPolygons would re-enable color-mask
				 * but it's possible that all polygon layers
				 * were skipped */
				GL.glColorMask(true, true, true, true);
				GL.glStencilMask(0x00);
			}

			mCount = cur;
			return l;
		}

		public static void clip(GLMatrix mvp, int clipMode) {
			setShader(polyShader, mvp, true);

			drawStencilRegion(clipMode);

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
		static void drawStencilRegion(int clipMode) {
			//log.debug("draw stencil {}", clipMode);
			mCount = 0;
			mClear = false;

			/* disable drawing to color buffer */
			GL.glColorMask(false, false, false, false);

			/* write to all stencil bits */
			GL.glStencilMask(0xFF);

			/* Draw clip-region into depth and stencil buffer.
			 * This is used for tile line and polygon layers.
			 * 
			 * Together with depth test (GL_LESS) this ensures to
			 * only draw where no other tile has drawn yet. */

			if (clipMode == CLIP_DEPTH) {
				/* tests GL_LESS/GL_ALWAYS and */
				/* write tile region to depth buffer */
				GLState.test(true, true);
				GL.glDepthMask(true);
			} else {
				GLState.test(false, true);
			}

			/* always pass stencil test and set clip bit */
			GL.glStencilFunc(GL_ALWAYS, CLIP_BIT, 0x00);

			/* set clip bit (0x80) for draw region */
			GL.glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE);

			/* draw a quad for the tile region */
			GL.glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

			if (clipMode == CLIP_DEPTH) {
				/* dont modify depth buffer */
				GL.glDepthMask(false);
				GLState.test(false, true);
			}
			GL.glStencilFunc(GL_EQUAL, CLIP_BIT, CLIP_BIT);
		}

		static void clearStencilRegion() {

			mCount = 0;
			mClear = false;

			/* disable drawing to color buffer */
			GL.glColorMask(false, false, false, false);

			/* write to all stencil bits except clip bit */
			GL.glStencilMask(0xFF);

			/* use clip bit from stencil buffer to clear stencil
			 * 'layer-bits' (0x7f) */
			GL.glStencilFunc(GL_EQUAL, CLIP_BIT, CLIP_BIT);

			/* set clip bit (0x80) for draw region */
			GL.glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE);

			/* draw a quad for the tile region */
			GL.glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
		}

		/**
		 * Clear stencilbuffer for a tile region by drawing
		 * a quad with func 'always' and op 'zero'. Using 'color'
		 * and 'alpha' to fake a fade effect.
		 */
		public static void drawOver(GLMatrix mvp, int color, float alpha) {
			/* TODO true could be avoided when same shader and vbo */
			setShader(polyShader, mvp, true);

			if (color == 0) {
				GL.glColorMask(false, false, false, false);
			} else {
				GLUtils.setColor(polyShader.uColor, color, alpha);
				GLState.blend(true);
			}

			// TODO always pass stencil test: <-- only if not proxy?
			//GL.glStencilFunc(GL_ALWAYS, 0x00, 0x00);

			GL.glStencilFunc(GL_EQUAL, CLIP_BIT, CLIP_BIT);

			/* write to all bits */
			GL.glStencilMask(0xFF);

			// FIXME uneeded probably
			GLState.test(false, true);

			/* zero out area to draw to */
			GL.glStencilOp(GL_KEEP, GL_KEEP, GL_ZERO);

			GL.glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

			if (color == 0)
				GL.glColorMask(true, true, true, true);
		}

		private Renderer() {
			/* Singleton */
		}
	}
}
