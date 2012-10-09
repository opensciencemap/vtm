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

/* ported from Polymaps: Layer.js Copyright (c) 2010, SimpleGeo and Stamen Design */

package org.oscim.renderer;

import android.util.FloatMath;
import android.util.Log;

public abstract class ScanBox {

	static class Edge {
		float x0, y0, x1, y1, dx, dy;

		void set(float x0, float y0, float x1, float y1) {
			if (y0 <= y1) {
				this.x0 = x0;
				this.y0 = y0;
				this.x1 = x1;
				this.y1 = y1;
				dx = x1 - x0;
				dy = y1 - y0;
			} else {
				this.x0 = x1;
				this.y0 = y1;
				this.x1 = x0;
				this.y1 = y0;
				dx = x0 - x1;
				dy = y0 - y1;
			}
		}
	}

	private Edge ab = new Edge();
	private Edge bc = new Edge();
	private Edge ca = new Edge();
	protected byte mZoom;

	void scan(float[] coords, byte zoom) {
		mZoom = zoom;

		ab.set(coords[0], coords[1], coords[2], coords[3]);
		bc.set(coords[2], coords[3], coords[4], coords[5]);
		ca.set(coords[4], coords[5], coords[0], coords[1]);
		scanTriangle();

		ab.set(coords[4], coords[5], coords[6], coords[7]);
		bc.set(coords[6], coords[7], coords[0], coords[1]);
		ca.set(coords[0], coords[1], coords[4], coords[5]);
		scanTriangle();
	}

	/**
	 * @param y
	 *            ...
	 * @param x1
	 *            ...
	 * @param x2
	 *            ...
	 */
	void setVisible(int y, int x1, int x2) {
	}

	private void scanTriangle() {

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
		// ca.dy > bc.dy > ab.dy

		if (ca.dy == 0)
			return;

		if (ab.dy != 0)
			scanSpans(ca, ab);

		if (bc.dy != 0)
			scanSpans(ca, bc);
	}

	// FIXME
	private static final int MAX_SLOPE = 4;

	private void scanSpans(Edge e0, Edge e1) {

		int y0 = (int) Math.max(0, FloatMath.floor(e1.y0));
		int y1 = (int) Math.min((1 << mZoom), FloatMath.ceil(e1.y1));

		// sort edge by x-coordinate
		if (e0.x0 == e1.x0 && e0.y0 == e1.y0) {
			// bottom-flat
			if (e0.x0 + e1.dy / e0.dy * e0.dx < e1.x1) {
				Edge t = e0;
				e0 = e1;
				e1 = t;
			}
		} else {
			// top-flat
			if (e0.x1 - e1.dy / e0.dy * e0.dx < e1.x0) {
				Edge t = e0;
				e0 = e1;
				e1 = t;
			}
		}

		float m0 = e0.dx / e0.dy;
		float m1 = e1.dx / e1.dy;

		// still needed?
		if (m0 > MAX_SLOPE)
			m0 = MAX_SLOPE;
		else if (m0 < -MAX_SLOPE)
			m0 = -MAX_SLOPE;

		if (m1 > MAX_SLOPE)
			m1 = MAX_SLOPE;
		else if (m1 < -MAX_SLOPE)
			m1 = -MAX_SLOPE;

		int d0 = e0.dx > 0 ? 1 : 0; // use y + 1 to compute x0
		int d1 = e1.dx < 0 ? 1 : 0; // use y + 1 to compute x1

		float x0, x1, dy;

		for (int y = y0; y < y1; y++) {
			dy = y + d0 - e0.y0;
			if (e0.dy < dy)
				dy = e0.dy;

			x0 = e0.x0 + m0 * dy;
			x0 = FloatMath.ceil(x0);

			dy = y + d1 - e1.y0;
			if (e1.dy < dy)
				dy = e1.dy;

			x1 = e1.x0 + m1 * dy;
			x1 = FloatMath.floor(x1);

			if (x1 > x0)
				Log.d("...", "X set visible" + y + " " + x1 + "/" + x0);

			setVisible(y, (int) x1, (int) x0);
		}
	}
}
