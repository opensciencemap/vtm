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

import static org.oscim.renderer.elements.VertexItem.SIZE;
import static org.oscim.renderer.elements.VertexItem.pool;

import org.oscim.backend.GL20;
import org.oscim.backend.GLAdapter;
import org.oscim.backend.canvas.Paint.Cap;
import org.oscim.core.GeometryBuffer;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Tile;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLUtils;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.MapRenderer;
import org.oscim.theme.styles.Line;
import org.oscim.utils.pool.Inlist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Note:
 * Coordinates must be in range [-4096..4096] and the maximum
 * resolution for coordinates is 0.25 as points will be converted
 * to fixed point values.
 */
public final class LineLayer extends RenderElement {
	static final Logger log = LoggerFactory.getLogger(LineLayer.class);

	private static final float COORD_SCALE = MapRenderer.COORD_SCALE;
	/** scale factor mapping extrusion vector to short values */
	public static final float DIR_SCALE = 2048;

	/** maximal resoultion */
	private static final float MIN_DIST = 1 / 8f;

	/**
	 * not quite right.. need to go back so that additional
	 * bevel vertices are at least MIN_DIST apart
	 */
	private static final float BEVEL_MIN = MIN_DIST * 4;

	/**
	 * mask for packing last two bits of extrusion vector with texture
	 * coordinates
	 */
	private static final int DIR_MASK = 0xFFFFFFFC;

	/* lines referenced by this outline layer */
	public LineLayer outlines;
	public Line line;
	public float scale = 1;

	public boolean roundCap;
	private float mMinDist = MIN_DIST;

	public float heightOffset;

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

	/**
	 * For point reduction by minimal distance. Default is 1/8.
	 */
	public void setDropDistance(float minDist) {
		mMinDist = Math.max(minDist, MIN_DIST);
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

	private static int tmax = Tile.SIZE + 4;
	private static int tmin = -4;

	private void addLine(float[] points, short[] index, int numPoints, boolean closed) {

		boolean rounded = false;
		boolean squared = false;

		if (line.cap == Cap.ROUND)
			rounded = true;
		else if (line.cap == Cap.SQUARE)
			squared = true;

		if (vertexItems == null)
			vertexItems = pool.get();

		VertexItem vertexItem = Inlist.last(vertexItems);

		/* Note: just a hack to save some vertices, when there are
		 * more than 200 lines per type. FIXME make optional! */
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

			/* check end-marker in indices */
			if (length < 0)
				break;

			int ipos = pos;
			pos += length;

			/* need at least two points */
			if (length < 4)
				continue;



			vertexItem = addLine(vertexItem, points, ipos, length, rounded, squared, closed);

		}
	}

	private void addVertex(short[] v, int pos, short x, short y, short dx, short dy) {
		v[pos + 0] = x;
		v[pos + 1] = y;
		v[pos + 2] = dx;
		v[pos + 3] = dy;
	}

	private VertexItem addVertex(VertexItem vertexItem,
	        float x, float y,
	        float vNextX, float vNextY,
	        float vPrevX, float vPrevY) {

		float ux = vNextX + vPrevX;
		float uy = vNextY + vPrevY;

		/* vPrev times perpendicular of sum(vNext, vPrev) */
		double a = uy * vPrevX - ux * vPrevY;

		if (a < 0.01 && a > -0.01) {
			ux = -vPrevY;
			uy = vPrevX;
		} else {
			ux /= a;
			uy /= a;
		}

		short ox = (short) (x * COORD_SCALE);
		short oy = (short) (y * COORD_SCALE);

		int ddx = (int) (ux * DIR_SCALE);
		int ddy = (int) (uy * DIR_SCALE);

		int opos = vertexItem.used;
		short[] v = vertexItem.vertices;

		if (opos == SIZE) {
			vertexItem = pool.getNext(vertexItem);
			v = vertexItem.vertices;
			opos = 0;
		}

		v[opos + 0] = ox;
		v[opos + 1] = oy;
		v[opos + 2] = (short) (0 | ddx & DIR_MASK);
		v[opos + 3] = (short) (1 | ddy & DIR_MASK);

		if ((opos += 4) == SIZE) {
			vertexItem = pool.getNext(vertexItem);
			v = vertexItem.vertices;
			opos = 0;
		}

		v[opos + 0] = ox;
		v[opos + 1] = oy;
		v[opos + 2] = (short) (2 | -ddx & DIR_MASK);
		v[opos + 3] = (short) (1 | -ddy & DIR_MASK);

		vertexItem.used = opos + 4;
		return vertexItem;

	}

	private VertexItem addLine(VertexItem vertexItem, float[] points, int start, int length,
	        boolean rounded, boolean squared, boolean closed) {

		float ux, uy;
		float vPrevX, vPrevY;
		float vNextX, vNextY;
		float curX, curY;
		float nextX, nextY;
		double a;

		short v[] = vertexItem.vertices;
		int opos = vertexItem.used;

		/* amount of vertices used
		 * + 2 for drawing triangle-strip
		 * + 4 for round caps
		 * + 2 for closing polygons */
		numVertices += length + (rounded ? 6 : 2) + (closed ? 2 : 0);

		int ipos = start;

		curX = points[ipos++];
		curY = points[ipos++];

		nextX = points[ipos++];
		nextY = points[ipos++];

		/* Unit vector to next node */
		vPrevX = nextX - curX;
		vPrevY = nextY - curY;
		a = (float) Math.sqrt(vPrevX * vPrevX + vPrevY * vPrevY);
		vPrevX /= a;
		vPrevY /= a;

		/* perpendicular on the first segment */
		ux = -vPrevY;
		uy = vPrevX;

		int ddx, ddy;

		/* vertex point coordinate */
		short ox = (short) (curX * COORD_SCALE);
		short oy = (short) (curY * COORD_SCALE);

		/* vertex extrusion vector, last two bit
		 * encode texture coord. */
		short dx, dy;

		/* when the endpoint is outside the tile region omit round caps. */
		boolean outside = (curX < tmin || curX > tmax || curY < tmin || curY > tmax);

		if (opos == SIZE) {
			vertexItem = pool.getNext(vertexItem);
			v = vertexItem.vertices;
			opos = 0;
		}

		if (rounded && !outside) {
			ddx = (int) ((ux - vPrevX) * DIR_SCALE);
			ddy = (int) ((uy - vPrevY) * DIR_SCALE);
			dx = (short) (0 | ddx & DIR_MASK);
			dy = (short) (2 | ddy & DIR_MASK);

			addVertex(v, opos, ox, oy, dx, dy);

			if ((opos += 4) == SIZE) {
				vertexItem = pool.getNext(vertexItem);
				v = vertexItem.vertices;
				opos = 0;
			}

			addVertex(v, opos, ox, oy, dx, dy);

			if ((opos += 4) == SIZE) {
				vertexItem = pool.getNext(vertexItem);
				v = vertexItem.vertices;
				opos = 0;
			}

			ddx = (int) (-(ux + vPrevX) * DIR_SCALE);
			ddy = (int) (-(uy + vPrevY) * DIR_SCALE);

			addVertex(v, opos, ox, oy,
			          (short) (2 | ddx & DIR_MASK),
			          (short) (2 | ddy & DIR_MASK));

			if ((opos += 4) == SIZE) {
				vertexItem = pool.getNext(vertexItem);
				v = vertexItem.vertices;
				opos = 0;
			}

			/* Start of line */
			ddx = (int) (ux * DIR_SCALE);
			ddy = (int) (uy * DIR_SCALE);

			addVertex(v, opos, ox, oy,
			          (short) (0 | ddx & DIR_MASK),
			          (short) (1 | ddy & DIR_MASK));

			if ((opos += 4) == SIZE) {
				vertexItem = pool.getNext(vertexItem);
				v = vertexItem.vertices;
				opos = 0;
			}

			addVertex(v, opos, ox, oy,
			          (short) (2 | -ddx & DIR_MASK),
			          (short) (1 | -ddy & DIR_MASK));

		} else {
			/* outside means line is probably clipped
			 * TODO should align ending with tile boundary
			 * for now, just extend the line a little */
			float tx = vPrevX;
			float ty = vPrevY;

			if (squared) {
				tx = 0;
				ty = 0;
			} else if (!outside) {
				tx *= 0.5;
				ty *= 0.5;
			}

			if (rounded)
				numVertices -= 2;

			/* add first vertex twice */
			ddx = (int) ((ux - tx) * DIR_SCALE);
			ddy = (int) ((uy - ty) * DIR_SCALE);
			dx = (short) (0 | ddx & DIR_MASK);
			dy = (short) (1 | ddy & DIR_MASK);

			addVertex(v, opos, ox, oy, dx, dy);

			if ((opos += 4) == SIZE) {
				vertexItem = pool.getNext(vertexItem);
				v = vertexItem.vertices;
				opos = 0;
			}

			addVertex(v, opos, ox, oy, dx, dy);

			if ((opos += 4) == SIZE) {
				vertexItem = pool.getNext(vertexItem);
				v = vertexItem.vertices;
				opos = 0;
			}

			ddx = (int) (-(ux + tx) * DIR_SCALE);
			ddy = (int) (-(uy + ty) * DIR_SCALE);

			addVertex(v, opos, ox, oy,
			          (short) (2 | ddx & DIR_MASK),
			          (short) (1 | ddy & DIR_MASK));
		}

		curX = nextX;
		curY = nextY;

		/* Unit vector pointing back to previous node */
		vPrevX *= -1;
		vPrevY *= -1;

		vertexItem.used = opos + 4;

		for (int end = start + length;;) {

			if (ipos < end) {
				nextX = points[ipos++];
				nextY = points[ipos++];
			} else if (closed && ipos < end + 2) {
				/* add startpoint == endpoint */
				nextX = points[start];
				nextY = points[start + 1];
				ipos += 2;
			} else
				break;

			/* unit vector pointing forward to next node */
			vNextX = nextX - curX;
			vNextY = nextY - curY;
			a = Math.sqrt(vNextX * vNextX + vNextY * vNextY);
			/* skip too short segmets */
			if (a < mMinDist) {
				numVertices -= 2;
				continue;
			}
			vNextX /= a;
			vNextY /= a;

			double dotp = (vNextX * vPrevX + vNextY * vPrevY);

			//log.debug("acos " + dotp);
			if (dotp > 0.65) {
				/* add bevel join to avoid miter going to infinity */
				numVertices += 2;

				//dotp = FastMath.clamp(dotp, -1, 1);
				//double cos = Math.acos(dotp);
				//log.debug("cos " + Math.toDegrees(cos));
				//log.debug("back " + (mMinDist * 2 / Math.sin(cos + Math.PI / 2)));

				float px, py;
				if (dotp > 0.999) {
					/* 360 degree angle, set points aside */
					ux = vPrevX + vNextX;
					uy = vPrevY + vNextY;
					a = vNextX * uy - vNextY * ux;
					if (a < 0.1 && a > -0.1) {
						/* Almost straight */
						ux = -vNextY;
						uy = vNextX;
					} else {
						ux /= a;
						uy /= a;
					}
					//log.debug("aside " + a + " " + ux + " " + uy);
					px = curX - ux * BEVEL_MIN;
					py = curY - uy * BEVEL_MIN;
					curX = curX + ux * BEVEL_MIN;
					curY = curY + uy * BEVEL_MIN;
				} else {
					//log.debug("back");
					/* go back by min dist */
					px = curX + vPrevX * BEVEL_MIN;
					py = curY + vPrevY * BEVEL_MIN;
					/* go forward by min dist */
					curX = curX + vNextX * BEVEL_MIN;
					curY = curY + vNextY * BEVEL_MIN;
				}

				/* unit vector pointing forward to next node */
				vNextX = curX - px;
				vNextY = curY - py;
				a = Math.sqrt(vNextX * vNextX + vNextY * vNextY);
				vNextX /= a;
				vNextY /= a;

				vertexItem = addVertex(vertexItem, px, py, vPrevX, vPrevY, vNextX, vNextY);

				/* flip unit vector to point back */
				vPrevX = -vNextX;
				vPrevY = -vNextY;

				/* unit vector pointing forward to next node */
				vNextX = nextX - curX;
				vNextY = nextY - curY;
				a = Math.sqrt(vNextX * vNextX + vNextY * vNextY);
				vNextX /= a;
				vNextY /= a;
			}

			vertexItem = addVertex(vertexItem, curX, curY, vPrevX, vPrevY, vNextX, vNextY);

			curX = nextX;
			curY = nextY;

			/* flip vector to point back */
			vPrevX = -vNextX;
			vPrevY = -vNextY;
		}

		opos = vertexItem.used;
		v = vertexItem.vertices;

		ux = vPrevY;
		uy = -vPrevX;

		outside = (curX < tmin || curX > tmax || curY < tmin || curY > tmax);

		if (opos == SIZE) {
			vertexItem = pool.getNext(vertexItem);
			v = vertexItem.vertices;
			opos = 0;
		}

		ox = (short) (curX * COORD_SCALE);
		oy = (short) (curY * COORD_SCALE);

		if (rounded && !outside) {
			ddx = (int) (ux * DIR_SCALE);
			ddy = (int) (uy * DIR_SCALE);

			addVertex(v, opos, ox, oy,
			          (short) (0 | ddx & DIR_MASK),
			          (short) (1 | ddy & DIR_MASK));

			if ((opos += 4) == SIZE) {
				vertexItem = pool.getNext(vertexItem);
				v = vertexItem.vertices;
				opos = 0;
			}

			addVertex(v, opos, ox, oy,
			          (short) (2 | -ddx & DIR_MASK),
			          (short) (1 | -ddy & DIR_MASK));

			if ((opos += 4) == SIZE) {
				vertexItem = pool.getNext(vertexItem);
				v = vertexItem.vertices;
				opos = 0;
			}

			/* For rounded line edges */
			ddx = (int) ((ux - vPrevX) * DIR_SCALE);
			ddy = (int) ((uy - vPrevY) * DIR_SCALE);

			addVertex(v, opos, ox, oy,
			          (short) (0 | ddx & DIR_MASK),
			          (short) (0 | ddy & DIR_MASK));

			/* last vertex */
			ddx = (int) (-(ux + vPrevX) * DIR_SCALE);
			ddy = (int) (-(uy + vPrevY) * DIR_SCALE);
			dx = (short) (2 | ddx & DIR_MASK);
			dy = (short) (0 | ddy & DIR_MASK);

		} else {
			if (squared) {
				vPrevX = 0;
				vPrevY = 0;
			} else if (!outside) {
				vPrevX *= 0.5;
				vPrevY *= 0.5;
			}

			if (rounded)
				numVertices -= 2;

			ddx = (int) ((ux - vPrevX) * DIR_SCALE);
			ddy = (int) ((uy - vPrevY) * DIR_SCALE);

			addVertex(v, opos, ox, oy,
			          (short) (0 | ddx & DIR_MASK),
			          (short) (1 | ddy & DIR_MASK));

			/* last vertex */
			ddx = (int) (-(ux + vPrevX) * DIR_SCALE);
			ddy = (int) (-(uy + vPrevY) * DIR_SCALE);
			dx = (short) (2 | ddx & DIR_MASK);
			dy = (short) (1 | ddy & DIR_MASK);
		}

		/* add last vertex twice */
		if ((opos += 4) == SIZE) {
			vertexItem = pool.getNext(vertexItem);
			v = vertexItem.vertices;
			opos = 0;
		}

		addVertex(v, opos, ox, oy, dx, dy);

		if ((opos += 4) == SIZE) {
			vertexItem = pool.getNext(vertexItem);
			v = vertexItem.vertices;
			opos = 0;
		}
		addVertex(v, opos, ox, oy, dx, dy);

		vertexItem.used = opos + 4;

		return vertexItem;
	}

	public static final class Renderer {
		/* TODO:
		 * http://http.developer.nvidia.com/GPUGems2/gpugems2_chapter22.html */

		/* factor to normalize extrusion vector and scale to coord scale */
		private final static float COORD_SCALE_BY_DIR_SCALE =
		        MapRenderer.COORD_SCALE / LineLayer.DIR_SCALE;

		private final static int CAP_THIN = 0;
		private final static int CAP_BUTT = 1;
		private final static int CAP_ROUND = 2;

		private final static int SHADER_FLAT = 1;
		private final static int SHADER_PROJ = 0;

		/* shader handles */
		private static int[] lineProgram = new int[2];
		private static int[] hLineVertexPosition = new int[2];
		private static int[] hLineColor = new int[2];
		private static int[] hLineMatrix = new int[2];
		private static int[] hLineFade = new int[2];
		private static int[] hLineWidth = new int[2];
		private static int[] hLineMode = new int[2];
		private static int[] hLineHeight = new int[2];

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
				hLineHeight[i] = GL.glGetUniformLocation(lineProgram[i], "u_height");
				hLineVertexPosition[i] = GL.glGetAttribLocation(lineProgram[i], "a_pos");
			}

			/* create lookup table as texture for 'length(0..1,0..1)'
			 * using mirrored wrap mode for 'length(-1..1,-1..1)' */
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

		public static RenderElement draw(RenderElement curLayer, GLViewport v,
		        float scale, ElementLayers layers) {

			if (curLayer == null)
				return null;

			/* simple line shader does not take forward shortening into
			 * account. only used when tilt is 0. */
			int mode = v.pos.tilt < 1 ? 1 : 0;

			GLState.useProgram(lineProgram[mode]);
			GLState.blend(true);

			/* Somehow we loose the texture after an indefinite
			 * time, when label/symbol textures are used.
			 * Debugging gl on Desktop is most fun imaginable,
			 * so for now: */
			if (!GLAdapter.GDX_DESKTOP_QUIRKS)
				GLState.bindTex2D(mTexID);

			int uLineFade = hLineFade[mode];
			int uLineMode = hLineMode[mode];
			int uLineColor = hLineColor[mode];
			int uLineWidth = hLineWidth[mode];
			int uLineHeight = hLineHeight[mode];

			GLState.enableVertexArrays(hLineVertexPosition[mode], -1);

			GL.glVertexAttribPointer(hLineVertexPosition[mode], 4, GL20.GL_SHORT,
			                         false, 0, layers.offset[LINE]);

			v.mvp.setAsUniform(hLineMatrix[mode]);

			/* Line scale factor for non fixed lines: Within a zoom-
			 * level lines would be scaled by the factor 2 by view-matrix.
			 * Though lines should only scale by sqrt(2). This is achieved
			 * by inverting scaling of extrusion vector with: width/sqrt(s). */
			double variableScale = Math.sqrt(scale);

			/* scale factor to map one pixel on tile to one pixel on screen:
			 * used with orthographic projection, (shader mode == 1) */
			double pixel = (mode == SHADER_PROJ) ? 0.0001 : 1.5 / scale;

			GL.glUniform1f(uLineFade, (float) pixel);

			int capMode = 0;
			GL.glUniform1f(uLineMode, capMode);

			boolean blur = false;
			double width;

			float heightOffset = 0;
			GL.glUniform1f(uLineHeight, heightOffset);

			RenderElement l = curLayer;
			for (; l != null && l.type == RenderElement.LINE; l = l.next) {
				LineLayer ll = (LineLayer) l;
				Line line = (Line) ll.line.getCurrent();

				if (ll.heightOffset != heightOffset) {
					heightOffset = ll.heightOffset;

					GL.glUniform1f(uLineHeight, heightOffset /
					        MercatorProjection.groundResolution(v.pos));
				}

				if (line.fade < v.pos.zoomLevel) {
					GLUtils.setColor(uLineColor, line.color, 1);
				} else if (line.fade > v.pos.zoomLevel) {
					continue;
				} else {
					float alpha = (float) (scale > 1.2 ? scale : 1.2) - 1;
					GLUtils.setColor(uLineColor, line.color, alpha);
				}

				if (mode == SHADER_PROJ && blur && line.blur == 0) {
					GL.glUniform1f(uLineFade, (float) pixel);
					blur = false;
				}

				/* draw LineLayer */
				if (!line.outline) {
					/* invert scaling of extrusion vectors so that line
					 * width stays the same. */
					if (line.fixed) {
						width = Math.max(line.width, 1) / scale;
					} else {
						width = ll.scale * line.width / variableScale;
					}

					GL.glUniform1f(uLineWidth,
					               (float) (width * COORD_SCALE_BY_DIR_SCALE));

					/* Line-edge fade */
					if (line.blur > 0) {
						GL.glUniform1f(uLineFade, line.blur);
						blur = true;
					} else if (mode == SHADER_FLAT) {
						GL.glUniform1f(uLineFade, (float) (pixel / width));
						//GL.glUniform1f(uLineScale, (float)(pixel / (ll.width / s)));
					}

					/* Cap mode */
					if (ll.scale < 1.5 /* || ll.line.fixed */) {

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
					                l.offset, l.numVertices);

					continue;
				}

				/* draw LineLayers references by this outline */

				for (LineLayer ref = ll.outlines; ref != null; ref = ref.outlines) {
					Line core = (Line) ref.line.getCurrent();

					// core width
					if (core.fixed) {
						width = Math.max(core.width, 1) / scale;
					} else {
						width = ref.scale * core.width / variableScale;
					}
					// add outline width
					if (line.fixed) {
						width += line.width / scale;
					} else {
						width += ll.scale * line.width / variableScale;
					}

					GL.glUniform1f(uLineWidth,
					               (float) (width * COORD_SCALE_BY_DIR_SCALE));

					/* Line-edge fade */
					if (line.blur > 0) {
						GL.glUniform1f(uLineFade, line.blur);
						blur = true;
					} else if (mode == SHADER_FLAT) {
						GL.glUniform1f(uLineFade, (float) (pixel / width));
					}

					/* Cap mode */
					if (ref.roundCap) {

						if (capMode != CAP_ROUND) {
							capMode = CAP_ROUND;
							GL.glUniform1f(uLineMode, capMode);
						}
					} else if (capMode != CAP_BUTT) {
						capMode = CAP_BUTT;
						GL.glUniform1f(uLineMode, capMode);
					}

					GL.glDrawArrays(GL20.GL_TRIANGLE_STRIP,
					                ref.offset, ref.numVertices);
				}
			}

			return l;
		}

		private final static String lineVertexShader = ""
		        + "precision mediump float;"
		        + "uniform mat4 u_mvp;"
		        //+ "uniform mat4 u_vp;"
		        /* factor to increase line width relative to scale */
		        + "uniform float u_width;"
		        /* xy hold position, zw extrusion vector */
		        + "attribute vec4 a_pos;"
		        + "uniform float u_mode;"
		        + "uniform float u_height;"
		        + "varying vec2 v_st;"
		        + "void main() {"

		        /* scale extrusion to u_width pixel
				 * just ignore the two most insignificant bits. */
		        + "  vec2 dir = a_pos.zw;"
		        + "  gl_Position = u_mvp * vec4(a_pos.xy + (u_width * dir), u_height, 1.0);"

		        /* last two bits hold the texture coordinates. */
		        + "  v_st = abs(mod(dir, 4.0)) - 1.0;"
		        + "}";

		private final static String lineVertexShader2 = ""
		        + "precision highp float;"
		        + "uniform mat4 u_mvp;"
		        + "uniform mat4 u_vp;"
		        /* factor to increase line width relative to scale */
		        + "uniform float u_width;"
		        /* xy hold position, zw extrusion vector */
		        + "attribute vec4 a_pos;"
		        + "uniform float u_mode;"
		        + "varying vec2 v_st;"
		        + "void main() {"

		        /* scale extrusion to u_width pixel */
		        /* just ignore the two most insignificant bits. */
		        + "  vec2 dir = a_pos.zw;"
		        + "  vec4 pos = u_vp * vec4(a_pos.xy + (u_width * dir), 0.0, 0.0);"
		        + "  vec4 orig = u_vp * vec4(a_pos.xy, 0.0, 0.0);"
		        + "  float len = length(orig - pos);"
		        //+ "  if (len < 0.0625){"
		        + "     pos = u_mvp * vec4(a_pos.xy + (u_width * dir) / (len * 4.0), 0.0, 1.0);"
		        //+ "   }"
		        //+ "   else  pos = u_mvp * vec4(a_pos.xy + (u_width * dir), 0.0, 1.0);"
		        + " gl_Position = pos;"
		        /* last two bits hold the texture coordinates. */
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
		        /* round cap line */
		        + (GLAdapter.GDX_DESKTOP_QUIRKS
		                ? "    len = length(v_st);"
		                : "    len = texture2D(tex, v_st).a;")
		        + "  } else {"
		        /* flat cap line */
		        + "    len = abs(v_st.s);"
		        + "  }"
		        /* u_mode == 0.0 -> thin line */
		        //+ "  len = len * clamp(u_mode, len, 1.0);"

		        /* use 'max' to avoid branching, need to check performance */
		        //+ (GLAdapter.GDX_DESKTOP_QUIRKS
		        //        ? " float len = max((1.0 - u_mode) * abs(v_st.s), u_mode * length(v_st));"
		        //        : " float len = max((1.0 - u_mode) * abs(v_st.s), u_mode * texture2D(tex, v_st).a);")

		        /* Antialias line-edges:
				 * - 'len' is 0 at center of line. -> (1.0 - len) is 0 at the
				 * edges
				 * - 'u_fade' is 'pixel' / 'width', i.e. the inverse width of
				 * the
				 * line in pixel on screen.
				 * - 'pixel' is 1.5 / relativeScale
				 * - '(1.0 - len) / u_fade' interpolates the 'pixel' on
				 * line-edge
				 * between 0 and 1 (it is greater 1 for all inner pixel). */
		        + "  gl_FragColor = u_color * clamp((1.0 - len) / u_fade, 0.0, 1.0);"
		        /* -> nicer for thin lines */
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
		        /* round cap line */
		        + (GLAdapter.GDX_DESKTOP_QUIRKS
		                ? "    len = length(v_st);"
		                : "    len = texture2D(tex, v_st).a;")
		        + "    vec2 st_width = fwidth(v_st);"
		        + "    fuzz = max(st_width.s, st_width.t);"
		        + "  } else {"
		        /* flat cap line */
		        + "    len = abs(v_st.s);"
		        + "    fuzz = fwidth(v_st.s);"
		        + "  }"
		        /* u_mode == 0.0 -> thin line */
		        //+ "  len = len * clamp(u_mode, len, 1.0);"

		        + "  if (fuzz > 2.0)"
		        + "  gl_FragColor = u_color * 0.5;" //vec4(1.0, 1.0, 1.0, 1.0);"
		        + "  else"
		        + "  gl_FragColor = u_color * clamp((1.0 - len) / max(u_fade, fuzz), 0.0, 1.0);"
		        //+ "  gl_FragColor = u_color * clamp((1.0 - len), 0.0, 1.0);"
		        + "}";

	}
}
