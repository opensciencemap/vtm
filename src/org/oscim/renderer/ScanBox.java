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

import android.util.FloatMath;

public abstract class ScanBox {

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
	protected byte mZoom;

	abstract void setVisible(int y, int x1, int x2);

	public void scan(float[] coords, byte zoom) {
		mZoom = zoom;
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

		// FIXME, something is wrong here, with a steep angle
		// 'fill' can shoot far over the area in one direction
		int maxSlope = 8;
		if (m0 > maxSlope)
			m0 = maxSlope;
		else if (m0 < -maxSlope)
			m0 = -maxSlope;

		if (m1 > maxSlope)
			m1 = maxSlope;
		else if (m1 < -maxSlope)
			m1 = -maxSlope;

		// e0 goes to the right, e1 to the left
		int d0 = e0.dx > 0 ? 1 : 0; // use y + 1 to compute x0
		int d1 = e1.dx < 0 ? 1 : 0; // use y + 1 to compute x1
		float dy;

		for (int y = y0; y < y1; y++) {

			dy = d0 + y - e0.y0;
			if (dy > e0.dy)
				dy = e0.dy;

			float x0 = FloatMath.ceil(e0.x0 + m0 * dy);

			dy = d1 + y - e1.y0;
			if (dy > e1.dy)
				dy = e1.dy;

			float x1 = FloatMath.floor(e1.x0 + m1 * dy);

			if (x1 < x0)
				setVisible(y, (int) x1, (int) x0);
		}
	}

	/*
	 * ported from Polymaps: Layer.js Copyright (c) 2010, SimpleGeo and Stamen
	 * Design
	 */

	//	// scan-line conversion
	//	function edge(a, b) {
	//	  if (a.row > b.row) { var t = a; a = b; b = t; }
	//	  return {
	//	    x0: a.column,
	//	    y0: a.row,
	//	    x1: b.column,
	//	    y1: b.row,
	//	    dx: b.column - a.column,
	//	    dy: b.row - a.row
	//	  };
	//	}
	//
	//	// scan-line conversion
	//	function scanSpans(e0, e1, ymin, ymax, scanLine) {
	//	  var y0 = Math.max(ymin, Math.floor(e1.y0)),
	//	      y1 = Math.min(ymax, Math.ceil(e1.y1));
	//
	//	  // sort edges by x-coordinate
	//	  if ((e0.x0 == e1.x0 && e0.y0 == e1.y0)
	//	      ? (e0.x0 + e1.dy / e0.dy * e0.dx < e1.x1)
	//	      : (e0.x1 - e1.dy / e0.dy * e0.dx < e1.x0)) {
	//	    var t = e0; e0 = e1; e1 = t;
	//	  }
	//
	//	  // scan lines!
	//	  var m0 = e0.dx / e0.dy,
	//	      m1 = e1.dx / e1.dy,
	//	      d0 = e0.dx > 0, // use y + 1 to compute x0
	//	      d1 = e1.dx < 0; // use y + 1 to compute x1
	//	  for (var y = y0; y < y1; y++) {
	//	    var x0 = m0 * Math.max(0, Math.min(e0.dy, y + d0 - e0.y0)) + e0.x0,
	//	        x1 = m1 * Math.max(0, Math.min(e1.dy, y + d1 - e1.y0)) + e1.x0;
	//	    scanLine(Math.floor(x1), Math.ceil(x0), y);
	//	  }
	//	}
	//
	//	// scan-line conversion
	//	function scanTriangle(a, b, c, ymin, ymax, scanLine) {
	//	  var ab = edge(a, b),
	//	      bc = edge(b, c),
	//	      ca = edge(c, a);
	//
	//	  // sort edges by y-length
	//	  if (ab.dy > bc.dy) { var t = ab; ab = bc; bc = t; }
	//	  if (ab.dy > ca.dy) { var t = ab; ab = ca; ca = t; }
	//	  if (bc.dy > ca.dy) { var t = bc; bc = ca; ca = t; }
	//
	//	  // scan span! scan span!
	//	  if (ab.dy) scanSpans(ca, ab, ymin, ymax, scanLine);
	//	  if (bc.dy) scanSpans(ca, bc, ymin, ymax, scanLine);
	//	}

}
