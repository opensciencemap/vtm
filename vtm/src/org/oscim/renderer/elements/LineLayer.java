/*
 * Copyright 2013 Hannes Janetzek
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

import java.nio.ShortBuffer;

import org.oscim.backend.GL20;
import org.oscim.backend.GLAdapter;
import org.oscim.backend.Log;
import org.oscim.backend.canvas.Paint.Cap;
import org.oscim.core.GeometryBuffer;
import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.renderer.MapRenderer;
import org.oscim.renderer.GLState;
import org.oscim.renderer.MapRenderer.Matrices;
import org.oscim.theme.renderinstruction.Line;
import org.oscim.utils.FastMath;
import org.oscim.utils.GlUtils;

/**
 */
public final class LineLayer extends RenderElement {
	private final static String TAG = LineLayer.class.getName();

	private static final float COORD_SCALE = MapRenderer.COORD_SCALE;
	// scale factor mapping extrusion vector to short values
	public static final float DIR_SCALE = 2048;
	// mask for packing last two bits of extrusion vector with texture
	// coordinates
	private static final int DIR_MASK = 0xFFFFFFFC;

	// lines referenced by this outline layer
	public LineLayer outlines;
	public Line line;
	public float width;

	public boolean roundCap;

	LineLayer(int layer) {
		this.level = layer;
		this.type = RenderElement.LINE;
	}

	public void addOutline(LineLayer link) {
		for (LineLayer l = outlines; l != null; l = l.outlines)
			if (link == l)
				return;

		link.outlines = outlines;
		outlines = link;
	}

	/**
	 * line extrusion is based on code from GLMap
	 * (https://github.com/olofsj/GLMap/)
	 *
	 * @param points
	 *            array of points as x,y pairs.
	 * @param index
	 *            array of indices holding the length of the individual
	 *            line coordinates (i.e. points * 2).
	 *            when index is null one a line with points.length
	 *            is assumed.
	 * @param closed
	 *            whether to connect start- and end-point.
	 */
	public void addLine(float[] points, short[] index, boolean closed) {
		addLine(points, index, -1, closed);
	}

	public void addLine(GeometryBuffer geom) {
		if (geom.isPoly())
			addLine(geom.points, geom.index, -1, true);
		else if (geom.isLine())
			addLine(geom.points, geom.index, -1, false);
	}

	public void addLine(float[] points, int numPoints, boolean closed) {
		if (numPoints >= 4)
			addLine(points, null, numPoints, closed);
	}

	private void addLine(float[] points, short[] index, int numPoints, boolean closed) {
		float x, y, nextX, nextY;
		float a, ux, uy, vx, vy, wx, wy;

		int tmax = Tile.SIZE + 4;
		int tmin = -4;

		boolean rounded = false;
		boolean squared = false;

		if (line.cap == Cap.ROUND)
			rounded = true;
		else if (line.cap == Cap.SQUARE)
			squared = true;

		if (vertexItems == null)
			curItem = vertexItems = VertexItem.pool.get();

		VertexItem si = curItem;
		short v[] = si.vertices;
		int opos = si.used;

		// FIXME: remove this when switching to oscimap MapDatabase
		//if (!MapView.enableClosePolygons)
		//	closed = false;

		// Note: just a hack to save some vertices, when there are more than 200 lines
		// per type
		if (rounded) {
			int cnt = 0;
			for (int i = 0, n = index.length; i < n; i++, cnt++) {
				if (index[i] < 0)
					break;
				if (cnt > 400) {
					rounded = false;
					break;
				}
			}
		}
		roundCap = rounded;

		int n;
		int length = 0;

		if (index == null) {
			n = 1;
			if (numPoints > 0) {
				length = numPoints;
			} else {
				length = points.length;
			}
		} else {
			n = index.length;
		}

		for (int i = 0, pos = 0; i < n; i++) {
			if (index != null)
				length = index[i];

			// check end-marker in indices
			if (length < 0)
				break;

			// need at least two points
			if (length < 4) {
				pos += length;
				continue;
			}

			// amount of vertices used
			// + 2 for drawing triangle-strip
			// + 4 for round caps
			// + 2 for closing polygons
			verticesCnt += length + (rounded ? 6 : 2) + (closed ? 2 : 0);

			int ipos = pos;

			x = points[ipos++];
			y = points[ipos++];

			nextX = points[ipos++];
			nextY = points[ipos++];

			// Calculate triangle corners for the given width
			vx = nextX - x;
			vy = nextY - y;

			// Unit vector to next node
			a = (float) Math.sqrt(vx * vx + vy * vy);
			vx /= a;
			vy /= a;

			// perpendicular on the first segment
			ux = -vy;
			uy = vx;

			int ddx, ddy;

			// vertex point coordinate
			short ox = (short) (x * COORD_SCALE);
			short oy = (short) (y * COORD_SCALE);

			// vertex extrusion vector, last two bit
			// encode texture coord.
			short dx, dy;

			// when the endpoint is outside the tile region omit round caps.
			boolean outside = (x < tmin || x > tmax || y < tmin || y > tmax);

			if (opos == VertexItem.SIZE) {
				si = si.next = VertexItem.pool.get();
				v = si.vertices;
				opos = 0;
			}

			if (rounded && !outside) {
				// add first vertex twice
				ddx = (int) ((ux - vx) * DIR_SCALE);
				ddy = (int) ((uy - vy) * DIR_SCALE);
				dx = (short) (0 | ddx & DIR_MASK);
				dy = (short) (2 | ddy & DIR_MASK);

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = dx;
				v[opos++] = dy;

				if (opos == VertexItem.SIZE) {
					si = si.next = VertexItem.pool.get();
					v = si.vertices;
					opos = 0;
				}

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = dx;
				v[opos++] = dy;

				if (opos == VertexItem.SIZE) {
					si = si.next = VertexItem.pool.get();
					v = si.vertices;
					opos = 0;
				}

				ddx = (int) (-(ux + vx) * DIR_SCALE);
				ddy = (int) (-(uy + vy) * DIR_SCALE);

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = (short) (2 | ddx & DIR_MASK);
				v[opos++] = (short) (2 | ddy & DIR_MASK);

				if (opos == VertexItem.SIZE) {
					si = si.next = VertexItem.pool.get();
					v = si.vertices;
					opos = 0;
				}

				// Start of line
				ddx = (int) (ux * DIR_SCALE);
				ddy = (int) (uy * DIR_SCALE);

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = (short) (0 | ddx & DIR_MASK);
				v[opos++] = (short) (1 | ddy & DIR_MASK);

				if (opos == VertexItem.SIZE) {
					si = si.next = VertexItem.pool.get();
					v = si.vertices;
					opos = 0;
				}

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = (short) (2 | -ddx & DIR_MASK);
				v[opos++] = (short) (1 | -ddy & DIR_MASK);

			} else {
				// outside means line is probably clipped
				// TODO should align ending with tile boundary
				// for now, just extend the line a little
				float tx = vx;
				float ty = vy;

				if (squared) {
					tx = 0;
					ty = 0;
				} else if (!outside) {
					tx *= 0.5;
					ty *= 0.5;
				}

				if (rounded)
					verticesCnt -= 2;

				// add first vertex twice
				ddx = (int) ((ux - tx) * DIR_SCALE);
				ddy = (int) ((uy - ty) * DIR_SCALE);
				dx = (short) (0 | ddx & DIR_MASK);
				dy = (short) (1 | ddy & DIR_MASK);

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = dx;
				v[opos++] = dy;

				if (opos == VertexItem.SIZE) {
					si = si.next = VertexItem.pool.get();
					v = si.vertices;
					opos = 0;
				}

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = dx;
				v[opos++] = dy;

				if (opos == VertexItem.SIZE) {
					si = si.next = VertexItem.pool.get();
					v = si.vertices;
					opos = 0;
				}

				ddx = (int) (-(ux + tx) * DIR_SCALE);
				ddy = (int) (-(uy + ty) * DIR_SCALE);

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = (short) (2 | ddx & DIR_MASK);
				v[opos++] = (short) (1 | ddy & DIR_MASK);

			}

			x = nextX;
			y = nextY;
			boolean flip = false;
			// Unit vector pointing back to previous node
			vx *= -1;
			vy *= -1;

			int end = pos + length;

			for (;;) {
				if (ipos < end) {
					nextX = points[ipos++];
					nextY = points[ipos++];
				} else if (closed && ipos < end + 2) {
					// add startpoint == endpoint
					nextX = points[pos];
					nextY = points[pos + 1];
					ipos += 2;
				} else
					break;

				// Unit vector pointing forward to next node
				wx = nextX - x;
				wy = nextY - y;
				a = (float) Math.sqrt(wx * wx + wy * wy);
				wx /= a;
				wy /= a;

				// Sum of these two vectors points
				ux = vx + wx;
				uy = vy + wy;

				// cross-product
				a = wx * uy - wy * ux;

				if (FastMath.abs(a) < 0.01f) {
					// Almost straight
					ux = -wy;
					uy = wx;
				} else {
					ux /= a;
					uy /= a;

					// avoid miter going to infinity.
					// TODO add option for round joints
					if (FastMath.absMaxCmp(ux, uy, 4f)) {
						ux = vx - wx;
						uy = vy - wy;

						a = -wy * ux + wx * uy;
						ux /= a;
						uy /= a;
						flip = !flip;
					}
				}

				ox = (short) (x * COORD_SCALE);
				oy = (short) (y * COORD_SCALE);

				ddx = (int) (ux * DIR_SCALE);
				ddy = (int) (uy * DIR_SCALE);

				if (flip) {
					ddx = -ddx;
					ddy = -ddy;
				}
				if (opos == VertexItem.SIZE) {
					si = si.next = VertexItem.pool.get();
					v = si.vertices;
					opos = 0;
				}

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = (short) (0 | ddx & DIR_MASK);
				v[opos++] = (short) (1 | ddy & DIR_MASK);

				if (opos == VertexItem.SIZE) {
					si = si.next = VertexItem.pool.get();
					v = si.vertices;
					opos = 0;
				}

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = (short) (2 | -ddx & DIR_MASK);
				v[opos++] = (short) (1 | -ddy & DIR_MASK);

				x = nextX;
				y = nextY;

				// flip unit vector to point back
				vx = -wx;
				vy = -wy;
			}

			ux = vy;
			uy = -vx;

			outside = (x < tmin || x > tmax || y < tmin || y > tmax);

			if (opos == VertexItem.SIZE) {
				si.next = VertexItem.pool.get();
				si = si.next;
				opos = 0;
				v = si.vertices;
			}

			ox = (short) (x * COORD_SCALE);
			oy = (short) (y * COORD_SCALE);

			if (rounded && !outside) {
				ddx = (int) (ux * DIR_SCALE);
				ddy = (int) (uy * DIR_SCALE);

				if (flip) {
					ddx = -ddx;
					ddy = -ddy;
				}

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = (short) (0 | ddx & DIR_MASK);
				v[opos++] = (short) (1 | ddy & DIR_MASK);

				if (opos == VertexItem.SIZE) {
					si = si.next = VertexItem.pool.get();
					v = si.vertices;
					opos = 0;
				}

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = (short) (2 | -ddx & DIR_MASK);
				v[opos++] = (short) (1 | -ddy & DIR_MASK);

				if (opos == VertexItem.SIZE) {
					si = si.next = VertexItem.pool.get();
					v = si.vertices;
					opos = 0;
				}

				// For rounded line edges
				ddx = (int) ((ux - vx) * DIR_SCALE);
				ddy = (int) ((uy - vy) * DIR_SCALE);

				dx = (short) (0 | (flip ? -ddx : ddx) & DIR_MASK);
				dy = (short) (0 | (flip ? -ddy : ddy) & DIR_MASK);

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = dx;
				v[opos++] = dy;

				if (opos == VertexItem.SIZE) {
					si = si.next = VertexItem.pool.get();
					v = si.vertices;
					opos = 0;
				}

				// add last vertex twice
				ddx = (int) (-(ux + vx) * DIR_SCALE);
				ddy = (int) (-(uy + vy) * DIR_SCALE);
				dx = (short) (2 | (flip ? -ddx : ddx) & DIR_MASK);
				dy = (short) (0 | (flip ? -ddy : ddy) & DIR_MASK);

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = dx;
				v[opos++] = dy;

				if (opos == VertexItem.SIZE) {
					si = si.next = VertexItem.pool.get();
					v = si.vertices;
					opos = 0;
				}

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = dx;
				v[opos++] = dy;

			} else {
				if (squared) {
					vx = 0;
					vy = 0;
				} else if (!outside) {
					vx *= 0.5;
					vy *= 0.5;
				}

				if (rounded)
					verticesCnt -= 2;

				ddx = (int) ((ux - vx) * DIR_SCALE);
				ddy = (int) ((uy - vy) * DIR_SCALE);

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = (short) (0 | (flip ? -ddx : ddx) & DIR_MASK);
				v[opos++] = (short) (1 | (flip ? -ddy : ddy) & DIR_MASK);

				if (opos == VertexItem.SIZE) {
					si = si.next = VertexItem.pool.get();
					v = si.vertices;
					opos = 0;
				}

				// add last vertex twice
				ddx = (int) (-(ux + vx) * DIR_SCALE);
				ddy = (int) (-(uy + vy) * DIR_SCALE);
				dx = (short) (2 | (flip ? -ddx : ddx) & DIR_MASK);
				dy = (short) (1 | (flip ? -ddy : ddy) & DIR_MASK);

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = dx;
				v[opos++] = dy;

				if (opos == VertexItem.SIZE) {
					si = si.next = VertexItem.pool.get();
					v = si.vertices;
					opos = 0;
				}

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = dx;
				v[opos++] = dy;
			}
			pos += length;
		}

		si.used = opos;
		curItem = si;
	}

	@Override
	public void clear() {
		if (vertexItems != null) {
			VertexItem.pool.releaseAll(vertexItems);
			vertexItems = null;
			curItem = null;
		}
		verticesCnt = 0;
	}

	@Override
	protected void compile(ShortBuffer sbuf) {
	}

	public static final class Renderer {

		private static GL20 GL;

		private static final int LINE_VERTICES_DATA_POS_OFFSET = 0;

		// factor to normalize extrusion vector and scale to coord scale
		private final static float COORD_SCALE_BY_DIR_SCALE =
				MapRenderer.COORD_SCALE / LineLayer.DIR_SCALE;

		// shader handles
		private static int[] lineProgram = new int[2];
		private static int[] hLineVertexPosition = new int[2];
		private static int[] hLineColor = new int[2];
		private static int[] hLineMatrix = new int[2];
		private static int[] hLineScale = new int[2];
		private static int[] hLineWidth = new int[2];
		private static int[] hLineMode = new int[2];
		public static int mTexID;

		static boolean init() {
			GL = GLAdapter.get();

			lineProgram[0] = GlUtils.createProgram(lineVertexShader,
					lineFragmentShader);
			if (lineProgram[0] == 0) {
				Log.e(TAG, "Could not create line program.");
				//return false;
			}

			lineProgram[1] = GlUtils.createProgram(lineVertexShader,
					lineSimpleFragmentShader);
			if (lineProgram[1] == 0) {
				Log.e(TAG, "Could not create simple line program.");
				return false;
			}

			for (int i = 0; i < 2; i++) {
				if (lineProgram[i] == 0)
					continue;

				hLineMatrix[i] = GL.glGetUniformLocation(lineProgram[i], "u_mvp");
				hLineScale[i] = GL.glGetUniformLocation(lineProgram[i], "u_wscale");
				hLineWidth[i] = GL.glGetUniformLocation(lineProgram[i], "u_width");
				hLineColor[i] = GL.glGetUniformLocation(lineProgram[i], "u_color");
				hLineMode[i] = GL.glGetUniformLocation(lineProgram[i], "u_mode");
				hLineVertexPosition[i] = GL.glGetAttribLocation(lineProgram[i], "a_pos");
			}

			// create lookup table as texture for 'length(0..1,0..1)'
			// using mirrored wrap mode for 'length(-1..1,-1..1)'
			byte[] pixel = new byte[128 * 128];

			for (int x = 0; x < 128; x++) {
				float xx = x * x;
				for (int y = 0; y < 128; y++) {
					float yy = y * y;
					int color = (int) (Math.sqrt(xx + yy) * 2);
					if (color > 255)
						color = 255;
					pixel[x + y * 128] = (byte) color;
				}
			}

			mTexID = GlUtils.loadTexture(pixel, 128, 128, GL20.GL_ALPHA,
					GL20.GL_NEAREST, GL20.GL_NEAREST,
					GL20.GL_MIRRORED_REPEAT, GL20.GL_MIRRORED_REPEAT);

			Log.d(TAG, "TEX ID: " + mTexID);
			return true;
		}

		public static RenderElement draw(ElementLayers layers, RenderElement curLayer, MapPosition pos,
				Matrices m, float div, int mode) {

			if (curLayer == null)
				return null;

			// FIXME HACK: fallback to simple shader
			if (lineProgram[mode] == 0)
				mode = 1;

			GLState.useProgram(lineProgram[mode]);

			GLState.blend(true);

			// Somehow we loose the texture after an indefinite
			// time, when label/symbol textures are used.
			// Debugging gl on Desktop is most fun imaginable,
			// so for now:
			if (!GLAdapter.GDX_DESKTOP_QUIRKS)
				GLState.bindTex2D(mTexID);

			int uLineScale = hLineScale[mode];
			int uLineMode = hLineMode[mode];
			int uLineColor = hLineColor[mode];
			int uLineWidth = hLineWidth[mode];

			GLState.enableVertexArrays(hLineVertexPosition[mode], -1);

			GL.glVertexAttribPointer(hLineVertexPosition[mode], 4, GL20.GL_SHORT,
					false, 0, layers.lineOffset + LINE_VERTICES_DATA_POS_OFFSET);

			//glUniformMatrix4fv(hLineMatrix[mode], 1, false, matrix, 0);
			m.mvp.setAsUniform(hLineMatrix[mode]);

			//int zoom = FastMath.log2((int) pos.absScale);
			int zoom = pos.zoomLevel;

			double scale = pos.getZoomScale();

			// Line scale factor for non fixed lines: Within a zoom-
			// level lines would be scaled by the factor 2 by view-matrix.
			// Though lines should only scale by sqrt(2). This is achieved
			// by inverting scaling of extrusion vector with: width/sqrt(s).
			// within one zoom-level: 1 <= s <= 2
			double s = scale / div;
			float lineScale = (float) Math.sqrt(s * 2 / 2.2);

			// scale factor to map one pixel on tile to one pixel on screen:
			// only works with orthographic projection
			float pixel = 0;

			if (mode == 1)
				pixel = (float) (1.5 / s);

			GL.glUniform1f(uLineScale, pixel);
			int lineMode = 0;
			GL.glUniform1f(uLineMode, lineMode);

			boolean blur = false;

			RenderElement l = curLayer;
			for (; l != null && l.type == RenderElement.LINE; l = l.next) {
				LineLayer ll = (LineLayer) l;
				Line line = ll.line;
				float width;

				if (line.fade < zoom) {
					GlUtils.setColor(uLineColor, line.color, 1);
				} else if (line.fade > zoom) {
					continue;
				} else {
					float alpha = (float) (scale > 1.2 ? scale : 1.2) - 1;
					GlUtils.setColor(uLineColor, line.color, alpha);
				}

				if (mode == 0 && blur && line.blur == 0) {
					GL.glUniform1f(uLineScale, 0);
					blur = false;
				}

				if (line.outline) {
					// draw linelayers references by this outline
					for (LineLayer o = ll.outlines; o != null; o = o.outlines) {

						if (o.line.fixed /* || strokeMaxZoom */) {
							width = (float) ((ll.width + o.width) / s);
						} else {
							width = (float) (ll.width / s + o.width / lineScale);

							// check min-size for outline
							if (o.line.min > 0 && o.width * lineScale < o.line.min * 2)
								continue;
						}

						GL.glUniform1f(uLineWidth, width * COORD_SCALE_BY_DIR_SCALE);

						if (line.blur != 0) {
							GL.glUniform1f(uLineScale, (float) (1 - (line.blur / s)));
							blur = true;
						} else if (mode == 1) {
							GL.glUniform1f(uLineScale, pixel / width);
						}

						if (o.roundCap) {
							if (lineMode != 1) {
								lineMode = 1;
								GL.glUniform1f(uLineMode, lineMode);
							}
						} else if (lineMode != 0) {
							lineMode = 0;
							GL.glUniform1f(uLineMode, lineMode);
						}
						GL.glDrawArrays(GL20.GL_TRIANGLE_STRIP, o.offset, o.verticesCnt);
					}
				} else {

					if (line.fixed /* || strokeMaxZoom */) {
						// invert scaling of extrusion vectors so that line
						// width stays the same.
						width = (float) (ll.width / s);
					} else {
						// reduce linear scaling of extrusion vectors so that
						// line width increases by sqrt(2.2).
						width = ll.width / lineScale;

						// min-size hack to omit outline when line becomes
						// very thin
						if ((ll.line.min > 0) && (ll.width * lineScale < ll.line.min * 2))
							width = (ll.width - 0.2f) / lineScale;
					}

					GL.glUniform1f(uLineWidth, width * COORD_SCALE_BY_DIR_SCALE);

					if (line.blur != 0) {
						GL.glUniform1f(uLineScale, line.blur);
						blur = true;
					} else if (mode == 1) {
						GL.glUniform1f(uLineScale, pixel / width);
					}

					if (ll.roundCap) {
						if (lineMode != 1) {
							lineMode = 1;
							GL.glUniform1f(uLineMode, lineMode);
						}
					} else if (lineMode != 0) {
						lineMode = 0;
						GL.glUniform1f(uLineMode, lineMode);
					}

					GL.glDrawArrays(GL20.GL_TRIANGLE_STRIP, l.offset, l.verticesCnt);
				}
			}

			return l;
		}

		private final static String lineVertexShader = ""
				+ "precision mediump float;"
				+ "uniform mat4 u_mvp;"
				// factor to increase line width relative to scale
				+ "uniform float u_width;"
				// xy hold position, zw extrusion vector
				+ "attribute vec4 a_pos;"
				+ "uniform float u_mode;"
				+ "varying vec2 v_st;"
				+ "void main() {"
				// scale extrusion to u_width pixel
				// just ignore the two most insignificant bits of a_st :)
				+ "  vec2 dir = a_pos.zw;"
				+ "  gl_Position = u_mvp * vec4(a_pos.xy + (u_width * dir), 0.0, 1.0);"
				// last two bits of a_st hold the texture coordinates
				// ..maybe one could wrap texture so that `abs` is not required
				+ "  v_st = abs(mod(dir, 4.0)) - 1.0;"
				+ "}";

		private final static String lineSimpleFragmentShader = ""
				+ "precision mediump float;"
				+ "uniform sampler2D tex;"
				+ "uniform float u_wscale;"
				+ "uniform float u_mode;"
				+ "uniform vec4 u_color;"
				+ "varying vec2 v_st;"
				+ "void main() {"
				//+ "  float len;"
				// (currently required as overlay line renderers dont load the texture)
				//+ "  if (u_mode == 0)"
				//+ "    len = abs(v_st.s);"
				//+ "  else"
				//+ "    len = texture2D(tex, v_st).a;"
				//+ "    len = u_mode * length(v_st);"
				// this avoids branching, need to check performance
				+ (GLAdapter.GDX_DESKTOP_QUIRKS
						? " float len = max((1.0 - u_mode) * abs(v_st.s), u_mode * length(v_st));"
						: " float len = max((1.0 - u_mode) * abs(v_st.s), u_mode * texture2D(tex, v_st).a);")
				// interpolate alpha between: 0.0 < 1.0 - len < u_wscale
				// where wscale is 'filter width' / 'line width' and 0 <= len <= sqrt(2)
				//+ "  gl_FragColor = u_color * smoothstep(0.0, u_wscale, 1.0 - len);"
				//+ "  gl_FragColor = mix(vec4(1.0,0.0,0.0,1.0), u_color, smoothstep(0.0, u_wscale, 1.0 - len));"
				+ "  float alpha = min(1.0, (1.0 - len) / u_wscale);"
				+ "  if (alpha > 0.1)"
				+ "    gl_FragColor = u_color * alpha;"
				+ "  else"
				+ "    discard;"
				//			+ "gl_FragColor = vec4(texture2D(tex, v_st).a);"
				+ "}";

		private final static String lineFragmentShader = ""
				+ "#extension GL_OES_standard_derivatives : enable\n"
				+ "precision mediump float;"
				+ "uniform sampler2D tex;"
				+ "uniform float u_mode;"
				+ "uniform vec4 u_color;"
				+ "uniform float u_wscale;"
				+ "varying vec2 v_st;"
				+ "void main() {"
				+ "  float len;"
				+ "  float fuzz;"
				+ "  if (u_mode == 0.0){"
				+ "    len = abs(v_st.s);"
				+ "    fuzz = fwidth(v_st.s);"
				+ "  } else {"
				+ (GLAdapter.GDX_DESKTOP_QUIRKS
						? "    len = length(v_st);"
						: "    len = texture2D(tex, v_st).a;")
				+ "    vec2 st_width = fwidth(v_st);"
				+ "    fuzz = max(st_width.s, st_width.t);"
				+ "  }"
				//+ "  gl_FragColor = u_color * smoothstep(0.0, fuzz + u_wscale, 1.0 - len);"
				// smoothstep is too sharp, guess one could increase extrusion with z..
				// this looks ok:
				//+ "  gl_FragColor = u_color * min(1.0, (1.0 - len) / (u_wscale + fuzz));"
				// can be faster according to nvidia docs 'Optimize OpenGL ES 2.0 Performace'
				+ "  gl_FragColor = u_color * clamp((1.0 - len) / (u_wscale + fuzz), 0.0, 1.0);"
				//+ "  gl_FragColor = mix(vec4(0.0,1.0,0.0,1.0), u_color, clamp((1.0 - len) / (u_wscale + fuzz), 0.0, 1.0));"
				+ "}";

		//	private final static String lineVertexShader = ""
		//			+ "precision mediump float;"
		//			+ "uniform mat4 u_mvp;"
		//			+ "uniform float u_width;"
		//			+ "attribute vec4 a_pos;"
		//			+ "uniform int u_mode;"
		//			//+ "attribute vec2 a_st;"
		//			+ "varying vec2 v_st;"
		//			+ "const float dscale = 8.0/2048.0;"
		//			+ "void main() {"
		//			// scale extrusion to u_width pixel
		//			// just ignore the two most insignificant bits of a_st :)
		//			+ "  vec2 dir = a_pos.zw;"
		//			+ "  gl_Position = u_mvp * vec4(a_pos.xy + (dscale * u_width * dir), 0.0, 1.0);"
		//			// last two bits of a_st hold the texture coordinates
		//			+ "  v_st = u_width * (abs(mod(dir, 4.0)) - 1.0);"
		//			// use bit operations when available (gles 1.3)
		//			// + "  v_st = u_width * vec2(a_st.x & 3 - 1, a_st.y & 3 - 1);"
		//			+ "}";
		//
		//	private final static String lineSimpleFragmentShader = ""
		//			+ "precision mediump float;"
		//			+ "uniform float u_wscale;"
		//			+ "uniform float u_width;"
		//			+ "uniform int u_mode;"
		//			+ "uniform vec4 u_color;"
		//			+ "varying vec2 v_st;"
		//			+ "void main() {"
		//			+ "  float len;"
		//			+ "  if (u_mode == 0)"
		//			+ "    len = abs(v_st.s);"
		//			+ "  else "
		//			+ "    len = length(v_st);"
		//			// fade to alpha. u_wscale is the width in pixel which should be
		//			// faded, u_width - len the position of this fragment on the
		//			// perpendicular to this line segment. this only works with no
		//			// perspective
		//			//+ "  gl_FragColor = min(1.0, (u_width - len) / u_wscale) * u_color;"
		//			+ "  gl_FragColor = u_color * smoothstep(0.0, u_wscale, (u_width - len));"
		//			+ "}";
		//
		//	private final static String lineFragmentShader = ""
		//			+ "#extension GL_OES_standard_derivatives : enable\n"
		//			+ "precision mediump float;"
		//			+ "uniform float u_wscale;"
		//			+ "uniform float u_width;"
		//			+ "uniform int u_mode;"
		//			+ "uniform vec4 u_color;"
		//			+ "varying vec2 v_st;"
		//			+ "void main() {"
		//			+ "  float len;"
		//			+ "  float fuzz;"
		//			+ "  if (u_mode == 0){"
		//			+ "    len = abs(v_st.s);"
		//			+ "    fuzz = u_wscale + fwidth(v_st.s);"
		//			+ "  } else {"
		//			+ "    len = length(v_st);"
		//			+ "    vec2 st_width = fwidth(v_st);"
		//			+ "    fuzz = u_wscale + max(st_width.s, st_width.t);"
		//			+ "  }"
		//			+ "  gl_FragColor = u_color * min(1.0, (u_width - len) / fuzz);"
		//			+ "}";
	}

}
