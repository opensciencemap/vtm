/*
 * Copyright 2013 OpenScienceMap
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
package org.oscim.renderer.layer;

import org.oscim.core.Tile;
import org.oscim.renderer.GLRenderer;
import org.oscim.theme.renderinstruction.Line;
import org.oscim.utils.FastMath;
import org.oscim.view.MapView;

import android.graphics.Paint.Cap;

/**
 * @author Hannes Janetzek
 */
public final class LineLayer extends Layer {

	private static final float COORD_SCALE = GLRenderer.COORD_MULTIPLIER;
	// scale factor mapping extrusion vector to short values
	private static final float DIR_SCALE = 2048;
	// mask for packing last two bits of extrusion vector with texture
	// coordinates
	private static final int DIR_MASK = 0xFFFFFFFC;

	// lines referenced by this outline layer
	public LineLayer outlines;
	public Line line;
	public float width;

	public boolean roundCap;

	LineLayer(int layer) {
		this.layer = layer;
		this.type = Layer.LINE;
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
	 *            array of points as x,y pairs
	 * @param index
	 *            array of indices holding the length of the individual
	 *            lines
	 * @param closed
	 *            whether to connect start- and end-point
	 */
	public void addLine(float[] points, short[] index, boolean closed) {
		float x, y, nextX, nextY;
		float a, ux, uy, vx, vy, wx, wy;

		int tmax = Tile.TILE_SIZE + 4;
		int tmin = -4;

		boolean rounded = false;
		boolean squared = false;

		if (line.cap == Cap.ROUND)
			rounded = true;
		else if (line.cap == Cap.SQUARE)
			squared = true;

		if (pool == null)
			curItem = pool = VertexPool.get();

		VertexPoolItem si = curItem;
		short v[] = si.vertices;
		int opos = si.used;

		// FIXME: remove this when switching to oscimap MapDatabase
		if (!MapView.enableClosePolygons)
			closed = false;

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

		for (int i = 0, pos = 0, n = index.length; i < n; i++) {
			int length = index[i];

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

			if (opos == VertexPoolItem.SIZE) {
				si = si.next = VertexPool.get();
				v = si.vertices;
				opos = 0;
			}

			int ddx, ddy;

			// vertex point coordinate
			short ox = (short) (x * COORD_SCALE);
			short oy = (short) (y * COORD_SCALE);

			// vertex extrusion vector, last two bit
			// encode texture coord.
			short dx, dy;

			// when the endpoint is outside the tile region omit round caps.
			boolean outside = (x < tmin || x > tmax || y < tmin || y > tmax);

			if (opos == VertexPoolItem.SIZE) {
				si = si.next = VertexPool.get();
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

				if (opos == VertexPoolItem.SIZE) {
					si = si.next = VertexPool.get();
					v = si.vertices;
					opos = 0;
				}

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = dx;
				v[opos++] = dy;

				if (opos == VertexPoolItem.SIZE) {
					si = si.next = VertexPool.get();
					v = si.vertices;
					opos = 0;
				}

				ddx = (int) (-(ux + vx) * DIR_SCALE);
				ddy = (int) (-(uy + vy) * DIR_SCALE);

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = (short) (2 | ddx & DIR_MASK);
				v[opos++] = (short) (2 | ddy & DIR_MASK);

				if (opos == VertexPoolItem.SIZE) {
					si = si.next = VertexPool.get();
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

				if (opos == VertexPoolItem.SIZE) {
					si = si.next = VertexPool.get();
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

				if (opos == VertexPoolItem.SIZE) {
					si = si.next = VertexPool.get();
					v = si.vertices;
					opos = 0;
				}

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = dx;
				v[opos++] = dy;

				if (opos == VertexPoolItem.SIZE) {
					si = si.next = VertexPool.get();
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

			for (;;) {
				if (ipos < pos + length) {
					nextX = points[ipos++];
					nextY = points[ipos++];
				} else if (closed && ipos < pos + length + 2) {
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
				if (opos == VertexPoolItem.SIZE) {
					si = si.next = VertexPool.get();
					v = si.vertices;
					opos = 0;
				}

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = (short) (0 | ddx & DIR_MASK);
				v[opos++] = (short) (1 | ddy & DIR_MASK);

				if (opos == VertexPoolItem.SIZE) {
					si = si.next = VertexPool.get();
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

			if (opos == VertexPoolItem.SIZE) {
				si.next = VertexPool.get();
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

				if (opos == VertexPoolItem.SIZE) {
					si = si.next = VertexPool.get();
					v = si.vertices;
					opos = 0;
				}

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = (short) (2 | -ddx & DIR_MASK);
				v[opos++] = (short) (1 | -ddy & DIR_MASK);

				if (opos == VertexPoolItem.SIZE) {
					si = si.next = VertexPool.get();
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

				if (opos == VertexPoolItem.SIZE) {
					si = si.next = VertexPool.get();
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

				if (opos == VertexPoolItem.SIZE) {
					si = si.next = VertexPool.get();
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

				if (opos == VertexPoolItem.SIZE) {
					si = si.next = VertexPool.get();
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

				if (opos == VertexPoolItem.SIZE) {
					si = si.next = VertexPool.get();
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
	protected void clear() {
	}
}
