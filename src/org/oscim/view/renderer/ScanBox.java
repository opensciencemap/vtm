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

/* ported from Polymaps: Layer.js */

package org.oscim.view.renderer;

import org.oscim.view.renderer.MapRenderer.TilesData;

import android.util.FloatMath;

public class ScanBox {

	interface Callback {
		void call(MapTile tile);
	}

	class SetVisible implements Callback {

		@Override
		public void call(MapTile tile) {
			tile.isVisible = true;
		}
	}

	static class Edge {
		float x0, y0, x1, y1, dx, dy;

		void set(float x0, float y0, float x1, float y1) {
			if (y0 <= y1) {
				this.x0 = x0;
				this.y0 = y0;
				this.x1 = x1;
				this.y1 = y1;
				this.dx = x1 - x0;
				this.dy = y1 - y0;
			} else {
				this.x0 = x1;
				this.y0 = y1;
				this.x1 = x0;
				this.y1 = y0;
				this.dx = x0 - x1;
				this.dy = y0 - y1;
			}
		}
	}

	static Edge ab = new Edge();
	static Edge bc = new Edge();
	static Edge ca = new Edge();

	static void scanSpans(Edge e0, Edge e1, float ymin, float ymax) {

		// sort edge by x-coordinate
		if (e0.x0 == e1.x0 && e0.y0 == e1.y0) {
			if (e0.x0 + e1.dy / e0.dy * e0.dx < e1.x1) {
				Edge t = e0;
				e0 = e1;
				e1 = t;
			}
		} else {
			if (e0.x1 - e1.dy / e0.dy * e0.dx < e1.x0) {
				Edge t = e0;
				e0 = e1;
				e1 = t;
			}
		}

		float m0 = e0.dx / e0.dy;
		float m1 = e1.dx / e1.dy;

		int d0 = e0.dx > 0 ? 1 : 0;// use y + 1 to compute x0
		int d1 = e1.dx < 0 ? 1 : 0; // use y + 1 to compute x1

		float x0, x1;

		int y = (int) Math.max(ymin, FloatMath.floor(e1.y0));
		int bottom = (int) Math.min(ymax, FloatMath.ceil(e1.y1));

		for (; y < bottom; y++) {
			// float x0 = (m0 * Math.min(e0.dy, y + d0 - e0.y0) + e0.x0);
			// float x1 = (m1 * Math.min(e1.dy, y + d1 - e1.y0) + e1.x0);

			x0 = y + d0 - e0.y0;
			if (e0.dy < x0)
				x0 = e0.dy;

			x0 = m0 * x0 + e0.x0;

			if (x0 < 0)
				x0 = 0;
			else
				x0 = FloatMath.ceil(x0);

			x1 = y + d1 - e1.y0;
			if (e1.dy < x1)
				x1 = e1.dy;

			x1 = m1 * x1 + e1.x0;

			if (x1 < 0)
				x1 = 0;
			else
				x1 = FloatMath.floor(x1);

			setVisible(y, (int) x1, (int) x0);

			// setVisible(y, (int) (x1 - 0.5f), (int) (x0 + 0.5f));
		}
	}

	static void scanTriangle(float ymin, float ymax) {

		if (ab.dy > bc.dy) {
			Edge t = ab;
			ab = bc;
			bc = t;
		}
		if (ab.dy > ca.dy) {
			Edge t = ab;
			ab = ca;
			ca = t;
		}
		if (bc.dy > ca.dy) {
			Edge t = bc;
			bc = ca;
			ca = t;
		}

		if (ab.dy != 0)
			scanSpans(ca, ab, ymin, ymax);
		if (bc.dy != 0)
			scanSpans(ca, bc, ymin, ymax);
	}

	public static void scan(float[] coords, TilesData tiles, int max) {
		sTiles = tiles;
		cntDoubles = 0;
		ab.set(coords[0], coords[1], coords[2], coords[3]);
		bc.set(coords[2], coords[3], coords[4], coords[5]);
		ca.set(coords[4], coords[5], coords[0], coords[1]);

		scanTriangle(0, max);
		// Log.d("..", ">doubles " + cntDoubles);
		ab.set(coords[4], coords[5], coords[6], coords[7]);
		bc.set(coords[6], coords[7], coords[0], coords[1]);
		ca.set(coords[0], coords[1], coords[4], coords[5]);

		scanTriangle(0, max);

		// Log.d("..", "<doubles " + cntDoubles);
	}

	private static TilesData sTiles;
	private static int cntDoubles;

	private static void setVisible(int y, int x1, int x2) {

		MapTile[] tiles = sTiles.tiles;
		for (int i = 0, n = sTiles.cnt; i < n; i++) {
			if (tiles[i].tileY == y) {
				if (tiles[i].tileX >= x1 && tiles[i].tileX < x2) {
					// if (tiles[i].isVisible) {
					// Log.d("..", ">>>" + y + " " + tiles[i].tileX);
					// cntDoubles++;
					// }
					tiles[i].isVisible = true;
				}
			}
		}
	}
}
