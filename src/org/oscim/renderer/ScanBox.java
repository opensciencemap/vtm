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


public abstract class ScanBox {
	/*
	 * ported from Polymaps: Layer.js
	 */

	static class Edge {
		float x0, y0, x1, y1, dx, dy;

		void set(float x0, float y0, float x1, float y1) {
			if (y0 <= y1) {
				this.x0 = x0;
				this.y0 = y0;
				this.x1 = x1;
				this.y1 = y1;
			} else {
				this.x0 = x1;
				this.y0 = y1;
				this.x1 = x0;
				this.y1 = y0;
			}
			this.dx = this.x1 - this.x0;
			this.dy = this.y1 - this.y0;
		}
	}

	private Edge ab = new Edge();
	private Edge bc = new Edge();
	private Edge ca = new Edge();
	private float minX, maxX;

	protected byte mZoom;

	abstract void setVisible(int y, int x1, int x2);

	public void scan(float[] coords, byte zoom) {
		mZoom = zoom;

		maxX = Float.MIN_VALUE;
		minX = Float.MAX_VALUE;

		for(int i = 0; i < 8; i += 2){
			float x = coords[i];
			if (x > maxX)
				maxX = x;
			if (x < minX)
				minX = x;
		}
		maxX = (float)Math.ceil(maxX);
		minX = (float)Math.floor(minX);

		// top-left -> top-right
		ab.set(coords[0], coords[1], coords[2], coords[3]);
		// top-right ->  bottom-right
		bc.set(coords[2], coords[3], coords[4], coords[5]);
		// bottom-right -> bottom-left
		ca.set(coords[4], coords[5], coords[0], coords[1]);

		scanTriangle();

		// top-left -> bottom-right
		ab.set(coords[0], coords[1], coords[4], coords[5]);
		// bottom-right -> bottom-left
		bc.set(coords[4], coords[5], coords[6], coords[7]);
		// bottom-left -> top-left
		ca.set(coords[6], coords[7], coords[0], coords[1]);

		scanTriangle();
	}

	private void scanTriangle() {

		// sort so that ca.dy > bc.dy > ab.dy
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

		// shouldnt be possible, anyway
		if (ca.dy == 0)
			return;

		if (ab.dy > 0.1)
			scanSpans(ca, ab);

		if (bc.dy > 0.1)
			scanSpans(ca, bc);
	}

	private void scanSpans(Edge e0, Edge e1) {

		// scan the y-range of the edge with less dy
		int y0 = (int) Math.max(0, Math.floor(e1.y0));
		int y1 = (int) Math.min((1 << mZoom), Math.ceil(e1.y1));

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

		// e0 goes to the right, e1 to the left
		int d0 = e0.dx > 0 ? 1 : 0; // use y + 1 to compute x0
		int d1 = e1.dx < 0 ? 1 : 0; // use y + 1 to compute x1
		float dy;

		for (int y = y0; y < y1; y++) {

			dy = d0 + y - e0.y0;
			if (dy > e0.dy)
				dy = e0.dy;

			float x0 = (float)Math.ceil(e0.x0 + m0 * dy);

			dy = d1 + y - e1.y0;
			if (dy > e1.dy)
				dy = e1.dy;

			float x1 = (float)Math.floor(e1.x0 + m1 * dy);

			if (x1 < minX)
				x1 = minX;

			if (x0 > maxX)
				x0 = maxX;

			if (x1 < x0)
				setVisible(y, (int) x1, (int) x0);
		}
	}
}
