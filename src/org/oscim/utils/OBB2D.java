/*
 * Copyright 2013 OpenScienceMap
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.oscim.utils;

/**
 * from http://www.flipcode.com/archives/2D_OBB_Intersection.shtml
 *
 * @author Morgan McGuire morgan@cs.brown.edu
 * @author Hannes Janetzek
 */

public class OBB2D {
	// Corners of the box, where 0 is the lower left.
	public final float[] corner = new float[4 * 2];

	// Two edges of the box extended away from corner[0].
	public final float[] axis = new float[2 * 2];

	// origin[a] = corner[0].dot(axis[a]);
	public final float[] origin = new float[2];

	// Returns true if other overlaps one dimension of this.
	private boolean overlaps1Way(OBB2D other) {
		for (int a = 0; a < 2; a++) {

			double t = Vec2.dot(other.corner, 0, axis, a);

			// Find the extent of box 2 on axis a
			double tMin = t;
			double tMax = t;

			for (int c = 1; c < 4; c++) {
				t = Vec2.dot(other.corner, c, axis, a);

				if (t < tMin) {
					tMin = t;
				} else if (t > tMax) {
					tMax = t;
				}
			}

			// We have to subtract off the origin

			// See if [tMin, tMax] intersects [0, 1]
			if ((tMin > 1 + origin[a]) || (tMax < origin[a])) {
				// There was no intersection along this dimension;
				// the boxes cannot possibly overlap.
				return false;
			}
		}

		// There was no dimension along which there is no intersection.
		// Therefore the boxes overlap.
		return true;
	}

	// Updates the axes after the corners move.  Assumes the
	// corners actually form a rectangle.
	private void computeAxes() {
		Vec2.sub(axis, 0, corner, 1, corner, 0);
		Vec2.sub(axis, 1, corner, 3, corner, 0);

		// Make the length of each axis 1/edge length so we know any
		// dot product must be less than 1 to fall within the edge.
		Vec2.normalizeSquared(axis, 0);
		origin[0] = Vec2.dot(corner, 0, axis, 0);

		Vec2.normalizeSquared(axis, 1);
		origin[1] = Vec2.dot(corner, 0, axis, 1);
	}

	public OBB2D(float cx, float cy, float w, float h, float angle) {
		float rcos = (float) Math.cos(angle);
		float rsin = (float) Math.sin(angle);

		float[] tmp = new float[4 * 2];
		Vec2.set(tmp, 0, rcos, rsin);
		Vec2.set(tmp, 1, -rsin, rcos);

		Vec2.mul(tmp, 0, w / 2);
		Vec2.mul(tmp, 1, h / 2);

		Vec2.add(tmp, 2, tmp, 0, tmp, 1);
		Vec2.sub(tmp, 3, tmp, 0, tmp, 1);

		Vec2.set(tmp, 0, cx, cy);

		Vec2.sub(corner, 0, tmp, 0, tmp, 3);
		Vec2.add(corner, 1, tmp, 0, tmp, 3);
		Vec2.add(corner, 2, tmp, 0, tmp, 2);
		Vec2.sub(corner, 3, tmp, 0, tmp, 2);

		computeAxes();
	}

	public OBB2D(float cx, float cy, float width, float height, double acos, double asin) {

		float vx = (float) acos * width / 2;
		float vy = (float) asin * width / 2;

		float ux = (float) -asin * height / 2;
		float uy = (float) acos * height / 2;

		corner[0] = cx + (vx - ux);
		corner[1] = cy + (vy - uy);

		corner[2] = cx + (-vx - ux);
		corner[3] = cy + (-vy - uy);

		corner[4] = cx + (-vx + ux);
		corner[5] = cy + (-vy + uy);

		corner[6] = cx + (vx + ux);
		corner[7] = cy + (vy + uy);

		computeAxes();
	}

	public OBB2D(float cx, float cy, float vx, float vy, float width, float height,
			boolean normalized) {

		float ux = -vy;
		float uy = vx;

		float hw = width / 2;
		float hh = height / 2;

		vx *= hw;
		vy *= hw;

		ux *= hh;
		uy *= hh;

		corner[0] = cx - (vx - ux);
		corner[1] = cy - (vy - uy);

		corner[2] = cx + (vx - ux);
		corner[3] = cy + (vy - uy);

		corner[4] = cx + (vx + ux);
		corner[5] = cy + (vy + uy);

		corner[6] = cx - (vx + ux);
		corner[7] = cy - (vy + uy);

		computeAxes();
	}

	public OBB2D(float cx, float cy, float dx, float dy, float width, float height) {

		float vx = cx - dx;
		float vy = cy - dy;

		float a = (float) Math.sqrt(vx * vx + vy * vy);
		vx /= a;
		vy /= a;

		float hw = width / 2;
		float hh = height / 2;

		float ux = vy * hh;
		float uy = -vx * hh;

		vx *= hw;
		vy *= hw;

		corner[0] = cx - vx - ux;
		corner[1] = cy - vy - uy;

		corner[2] = cx + vx - ux;
		corner[3] = cy + vy - uy;

		corner[4] = cx + vx + ux;
		corner[5] = cy + vy + uy;

		corner[6] = cx - vx + ux;
		corner[7] = cy - vy + uy;

		computeAxes();
	}

	// width and height must be > 1 I guess
	public OBB2D(float cx, float cy, float width, float height) {

		float hw = width / 2;
		float hh = height / 2;

		corner[0] = cx - hw;
		corner[1] = cy - hh;

		corner[2] = cx - hw;
		corner[3] = cy + hh;

		corner[4] = cx + hw;
		corner[5] = cy + hh;

		corner[6] = cx + hw;
		corner[7] = cy - hh;

		axis[0] = 0;
		axis[1] = 1 / height;

		axis[2] = 1 / width;
		axis[3] = 0;

		origin[0] = corner[1] * axis[1];
		origin[1] = corner[2] * axis[2];
	}

	// Returns true if the intersection of the boxes is non-empty.
	public boolean overlaps(OBB2D other) {
		return overlaps1Way(other) && other.overlaps1Way(this);
	}

	//    // For testing purposes.
	//    void moveTo(Vec2 center) {
	//        Vec2 centroid = (corner[0] + corner[1] + corner[2] + corner[3]) / 4;
	//
	//        Vec2 translation = center - centroid;
	//
	//        for (int c = 0; c < 4; ++c) {
	//            corner[c] += translation;
	//        }
	//
	//        computeAxes();
	//    }

	//    void render() {
	//        glBegin(GL_LINES);
	//            for (int c = 0; c < 5; ++c) {
	//              glVertex2fv(corner[c & 3]);
	//            }
	//        glEnd();
	//    }

}
