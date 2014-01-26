/*
 * Copyright 2013 Hannes Janetzek
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

import java.nio.ShortBuffer;

import org.oscim.backend.GL20;
import org.oscim.backend.GLAdapter;
import org.oscim.backend.canvas.Paint.Cap;
import org.oscim.core.GeometryBuffer;
import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLUtils;
import org.oscim.renderer.MapRenderer;
import org.oscim.renderer.MapRenderer.Matrices;
import org.oscim.theme.styles.Line;
import org.oscim.utils.FastMath;
import org.oscim.utils.pool.Inlist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public final class LineLayer extends RenderElement {
	static final Logger log = LoggerFactory.getLogger(LineLayer.class);

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
		super(RenderElement.LINE);
		this.level = layer;
	}

	public void addOutline(LineLayer link) {
		for (LineLayer l = outlines; l != null; l = l.outlines)
			if (link == l)
				return;

		link.outlines = outlines;
		outlines = link;
	}

	public void addLine(GeometryBuffer geom) {
		if (geom.isPoly())
			addLine(geom.points, geom.index, -1, true);
		else if (geom.isLine())
			addLine(geom.points, geom.index, -1, false);
		else
			log.debug("geometry must be LINE or POLYGON");
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
			vertexItems = VertexItem.pool.get();

		VertexItem si = Inlist.last(vertexItems);
		short v[] = si.vertices;
		int opos = si.used;

		// FIXME: remove this when switching to oscimap MapDatabase
		//if (!MapView.enableClosePolygons)
		//	closed = false;

		// Note: just a hack to save some vertices, when there are more than 200 lines
		// per type
		if (rounded && index != null) {
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
			numVertices += length + (rounded ? 6 : 2) + (closed ? 2 : 0);

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
				si = VertexItem.pool.getNext(si);
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
					si = VertexItem.pool.getNext(si);
					v = si.vertices;
					opos = 0;
				}

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = dx;
				v[opos++] = dy;

				if (opos == VertexItem.SIZE) {
					si = VertexItem.pool.getNext(si);
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
					si = VertexItem.pool.getNext(si);
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
					si = VertexItem.pool.getNext(si);
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
					numVertices -= 2;

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
					si = VertexItem.pool.getNext(si);
					v = si.vertices;
					opos = 0;
				}

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = dx;
				v[opos++] = dy;

				if (opos == VertexItem.SIZE) {
					si = VertexItem.pool.getNext(si);
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
				// skip too short segmets
				if (a < 1) {
					numVertices -= 2;
					continue;
				}
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
					si = VertexItem.pool.getNext(si);
					v = si.vertices;
					opos = 0;
				}

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = (short) (0 | ddx & DIR_MASK);
				v[opos++] = (short) (1 | ddy & DIR_MASK);

				if (opos == VertexItem.SIZE) {
					si = VertexItem.pool.getNext(si);
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
				si = VertexItem.pool.getNext(si);
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
					si = VertexItem.pool.getNext(si);
					v = si.vertices;
					opos = 0;
				}

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = (short) (2 | -ddx & DIR_MASK);
				v[opos++] = (short) (1 | -ddy & DIR_MASK);

				if (opos == VertexItem.SIZE) {
					si = VertexItem.pool.getNext(si);
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
					si = VertexItem.pool.getNext(si);
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
					si = VertexItem.pool.getNext(si);
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
					numVertices -= 2;

				ddx = (int) ((ux - vx) * DIR_SCALE);
				ddy = (int) ((uy - vy) * DIR_SCALE);

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = (short) (0 | (flip ? -ddx : ddx) & DIR_MASK);
				v[opos++] = (short) (1 | (flip ? -ddy : ddy) & DIR_MASK);

				if (opos == VertexItem.SIZE) {
					si = VertexItem.pool.getNext(si);
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
					si = VertexItem.pool.getNext(si);
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
	}

	@Override
	public void clear() {
		vertexItems = VertexItem.pool.releaseAll(vertexItems);
		numVertices = 0;
	}

	@Override
	protected void compile(ShortBuffer sbuf) {
	}

	public static final class Renderer {
		// TODO: http://http.developer.nvidia.com/GPUGems2/gpugems2_chapter22.html

		// factor to normalize extrusion vector and scale to coord scale
		private final static float COORD_SCALE_BY_DIR_SCALE =
		        MapRenderer.COORD_SCALE / LineLayer.DIR_SCALE;

		private final static int CAP_THIN = 0;
		private final static int CAP_BUTT = 1;
		private final static int CAP_ROUND = 2;

		private final static int SHADER_FLAT = 1;
		private final static int SHADER_PROJ = 0;

		// shader handles
		private static int[] lineProgram = new int[2];
		private static int[] hLineVertexPosition = new int[2];
		private static int[] hLineColor = new int[2];
		private static int[] hLineMatrix = new int[2];
		private static int[] hLineFade = new int[2];
		private static int[] hLineWidth = new int[2];
		private static int[] hLineMode = new int[2];
		public static int mTexID;

		static boolean init() {

			lineProgram[0] = GLUtils.createProgram(lineVertexShader,
			                                       lineFragmentShader);
			if (lineProgram[0] == 0) {
				log.error("Could not create line program.");
				//return false;
			}

			lineProgram[1] = GLUtils.createProgram(lineVertexShader,
			                                       lineSimpleFragmentShader);
			if (lineProgram[1] == 0) {
				log.error("Could not create simple line program.");
				return false;
			}

			for (int i = 0; i < 2; i++) {
				if (lineProgram[i] == 0)
					continue;

				hLineMatrix[i] = GL.glGetUniformLocation(lineProgram[i], "u_mvp");
				hLineFade[i] = GL.glGetUniformLocation(lineProgram[i], "u_fade");
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

			mTexID = GLUtils.loadTexture(pixel, 128, 128, GL20.GL_ALPHA,
			                             GL20.GL_NEAREST, GL20.GL_NEAREST,
			                             GL20.GL_MIRRORED_REPEAT,
			                             GL20.GL_MIRRORED_REPEAT);
			return true;
		}

		public static RenderElement draw(ElementLayers layers, RenderElement curLayer,
		        MapPosition pos, Matrices m, float scale) {

			if (curLayer == null)
				return null;

			// simple line shader does not take forward shortening into
			// account. only used when tilt is 0.
			int mode = pos.tilt < 1 ? 1 : 0;

			GLState.useProgram(lineProgram[mode]);
			GLState.blend(true);

			// Somehow we loose the texture after an indefinite
			// time, when label/symbol textures are used.
			// Debugging gl on Desktop is most fun imaginable,
			// so for now:
			if (!GLAdapter.GDX_DESKTOP_QUIRKS)
				GLState.bindTex2D(mTexID);

			int uLineFade = hLineFade[mode];
			int uLineMode = hLineMode[mode];
			int uLineColor = hLineColor[mode];
			int uLineWidth = hLineWidth[mode];

			GLState.enableVertexArrays(hLineVertexPosition[mode], -1);

			GL.glVertexAttribPointer(hLineVertexPosition[mode], 4, GL20.GL_SHORT,
			                         false, 0, layers.lineOffset);

			m.mvp.setAsUniform(hLineMatrix[mode]);

			// Line scale factor for non fixed lines: Within a zoom-
			// level lines would be scaled by the factor 2 by view-matrix.
			// Though lines should only scale by sqrt(2). This is achieved
			// by inverting scaling of extrusion vector with: width/sqrt(s).
			double variableScale = Math.sqrt(scale);

			// scale factor to map one pixel on tile to one pixel on screen:
			// used with orthographic projection, (shader mode == 1)
			double pixel = (mode == SHADER_PROJ) ? 0 : 1.5 / scale;

			GL.glUniform1f(uLineFade, (float) pixel);

			int capMode = 0;
			GL.glUniform1f(uLineMode, capMode);

			boolean blur = false;
			double width;

			RenderElement l = curLayer;
			for (; l != null && l.type == RenderElement.LINE; l = l.next) {
				LineLayer ll = (LineLayer) l;
				Line line = ll.line;

				if (line.fade < pos.zoomLevel) {
					GLUtils.setColor(uLineColor, line.color, 1);
				} else if (line.fade > pos.zoomLevel) {
					continue;
				} else {
					float alpha = (float) (scale > 1.2 ? scale : 1.2) - 1;
					GLUtils.setColor(uLineColor, line.color, alpha);
				}

				if (mode == SHADER_PROJ && blur && line.blur == 0) {
					GL.glUniform1f(uLineFade, 0);
					blur = false;
				}

				// draw LineLayer
				if (!line.outline) {

					// invert scaling of extrusion vectors so that line
					// width stays the same.
					width = ll.width / (line.fixed ? scale : variableScale);

					GL.glUniform1f(uLineWidth,
					               (float) (width * COORD_SCALE_BY_DIR_SCALE));

					// Line-edge fade
					if (line.blur > 0) {
						GL.glUniform1f(uLineFade, line.blur);
						blur = true;
					} else if (mode == SHADER_FLAT) {
						GL.glUniform1f(uLineFade, (float) (pixel / width));
						//GL.glUniform1f(uLineScale, (float)(pixel / (ll.width / s)));
					}

					// Cap mode
					if (ll.width < 1.5 /* || ll.line.fixed */) {
						if (capMode != CAP_THIN) {
							capMode = CAP_THIN;
							GL.glUniform1f(uLineMode, capMode);
						}
					} else if (ll.roundCap) {
						if (capMode != CAP_ROUND) {
							capMode = CAP_ROUND;
							GL.glUniform1f(uLineMode, capMode);
						}
					} else if (capMode != CAP_BUTT) {
						capMode = CAP_BUTT;
						GL.glUniform1f(uLineMode, capMode);
					}

					GL.glDrawArrays(GL20.GL_TRIANGLE_STRIP,
					                l.getOffset(), l.numVertices);

					continue;
				}

				// draw LineLayers references by this outline
				for (LineLayer o = ll.outlines; o != null; o = o.outlines) {

					if (o.line.fixed)
						width = (ll.width + o.width) / scale;
					else
						width = ll.width / scale + o.width / variableScale;

					GL.glUniform1f(uLineWidth,
					               (float) (width * COORD_SCALE_BY_DIR_SCALE));

					// Line-edge fade
					if (line.blur > 0) {
						GL.glUniform1f(uLineFade, line.blur);
						blur = true;
					} else if (mode == SHADER_FLAT) {
						GL.glUniform1f(uLineFade, (float) (pixel / width));
					}

					// Cap mode
					if (o.roundCap) {
						if (capMode != CAP_ROUND) {
							capMode = CAP_ROUND;
							GL.glUniform1f(uLineMode, capMode);
						}
					} else if (capMode != CAP_BUTT) {
						capMode = CAP_BUTT;
						GL.glUniform1f(uLineMode, capMode);
					}

					GL.glDrawArrays(GL20.GL_TRIANGLE_STRIP,
					                o.getOffset(), o.numVertices);
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
		        // just ignore the two most insignificant bits.
		        + "  vec2 dir = a_pos.zw;"
		        + "  gl_Position = u_mvp * vec4(a_pos.xy + (u_width * dir), 0.0, 1.0);"

		        // last two bits hold the texture coordinates.
		        + "  v_st = abs(mod(dir, 4.0)) - 1.0;"
		        + "}";

		/** Antialising for orthonogonal projection */
		private final static String lineSimpleFragmentShader = ""
		        + "precision mediump float;"
		        + "uniform sampler2D tex;"
		        + "uniform float u_fade;"
		        + "uniform float u_mode;"
		        + "uniform vec4 u_color;"
		        + "varying vec2 v_st;"
		        + "void main() {"
		        + "float len;"
		        + "  if (u_mode == 2.0){"
		        //   round cap line
		        + (GLAdapter.GDX_DESKTOP_QUIRKS
		                ? "    len = length(v_st);"
		                : "    len = texture2D(tex, v_st).a;")
		        + "  } else {"
		        //     flat cap line
		        + "    len = abs(v_st.s);"
		        + "  }"
		        //   u_mode == 0.0 -> thin line
		        //+ "  len = len * clamp(u_mode, len, 1.0);"

		        // use 'max' to avoid branching, need to check performance
		        //+ (GLAdapter.GDX_DESKTOP_QUIRKS
		        //        ? " float len = max((1.0 - u_mode) * abs(v_st.s), u_mode * length(v_st));"
		        //        : " float len = max((1.0 - u_mode) * abs(v_st.s), u_mode * texture2D(tex, v_st).a);")

		        // Antialias line-edges:
		        // - 'len' is 0 at center of line. -> (1.0 - len) is 0 at the edges
		        // - 'u_fade' is 'pixel' / 'width', i.e. the inverse width of the
		        //   line in pixel on screen.
		        // - 'pixel' is 1.5 / relativeScale
		        // - '(1.0 - len) / u_fade' interpolates the 'pixel' on line-edge
		        //   between 0 and 1 (it is greater 1 for all inner pixel).
		        + "  gl_FragColor = u_color * clamp((1.0 - len) / u_fade, 0.0, 1.0);"
		        // -> nicer for thin lines
		        //+ "  gl_FragColor = u_color * clamp((1.0 - (len * len)) / u_fade, 0.0, 1.0);"
		        + "}";

		private final static String lineFragmentShader = ""
		        + "#extension GL_OES_standard_derivatives : enable\n"
		        + "precision mediump float;"
		        + "uniform sampler2D tex;"
		        + "uniform float u_mode;"
		        + "uniform vec4 u_color;"
		        + "uniform float u_fade;"
		        + "varying vec2 v_st;"
		        + "void main() {"
		        + "  float len;"
		        + "  float fuzz;"
		        + "  if (u_mode == 2.0){"
		        //   round cap line
		        + (GLAdapter.GDX_DESKTOP_QUIRKS
		                ? "    len = length(v_st);"
		                : "    len = texture2D(tex, v_st).a;")
		        + "    vec2 st_width = fwidth(v_st);"
		        + "    fuzz = max(st_width.s, st_width.t);"
		        + "  } else {"
		        //     flat cap line
		        + "    len = abs(v_st.s);"
		        + "    fuzz = fwidth(v_st.s);"
		        + "  }"
		        //   u_mode == 0.0 -> thin line
		        //+ "  len = len * clamp(u_mode, len, 1.0);"

		        + "  if (fuzz > 2.0)"
		        + "  gl_FragColor = u_color * 0.5;" //vec4(1.0, 1.0, 1.0, 1.0);"
		        + "  else"
		        + "  gl_FragColor = u_color * clamp((1.0 - len) / max(u_fade, fuzz), 0.0, 1.0);"
		        //+ "  gl_FragColor = u_color * clamp((1.0 - len), 0.0, 1.0);"
		        + "}";

	}

}
