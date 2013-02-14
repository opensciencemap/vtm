/*
 * Copyright 2012, 2013 OpenScienceMap
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

import android.graphics.Point;

/**
 * @author Hannes Janetzek
 */
public final class GeometryUtils {
	/**
	 * Calculates the center of the minimum bounding rectangle for the given
	 * coordinates.
	 *
	 * @param coordinates
	 *            the coordinates for which calculation should be done.
	 * @return the center coordinates of the minimum bounding rectangle.
	 */
	static float[] calculateCenterOfBoundingBox(float[] coordinates) {
		float longitudeMin = coordinates[0];
		float longitudeMax = coordinates[0];
		float latitudeMax = coordinates[1];
		float latitudeMin = coordinates[1];

		for (int i = 2; i < coordinates.length; i += 2) {
			if (coordinates[i] < longitudeMin) {
				longitudeMin = coordinates[i];
			} else if (coordinates[i] > longitudeMax) {
				longitudeMax = coordinates[i];
			}

			if (coordinates[i + 1] < latitudeMin) {
				latitudeMin = coordinates[i + 1];
			} else if (coordinates[i + 1] > latitudeMax) {
				latitudeMax = coordinates[i + 1];
			}
		}

		return new float[] { (longitudeMin + longitudeMax) / 2, (latitudeMax + latitudeMin) / 2 };
	}

	/**
	 * @param way
	 *            the coordinates of the way.
	 * @return true if the given way is closed, false otherwise.
	 */
	static boolean isClosedWay(float[] way) {
		return Float.compare(way[0], way[way.length - 2]) == 0
				&& Float.compare(way[1], way[way.length - 1]) == 0;
	}

	private GeometryUtils() {
		throw new IllegalStateException();
	}

	public static byte linesIntersect(
			double x1, double y1, double x2, double y2,
			double x3, double y3, double x4, double y4) {
		// Return false if either of the lines have zero length
		if (x1 == x2 && y1 == y2 || x3 == x4 && y3 == y4) {
			return 0;
		}
		// Fastest method, based on Franklin Antonio's
		// "Faster Line Segment Intersection" topic "in Graphics Gems III" book
		// (http://www.graphicsgems.org/)
		double ax = x2 - x1;
		double ay = y2 - y1;
		double bx = x3 - x4;
		double by = y3 - y4;
		double cx = x1 - x3;
		double cy = y1 - y3;

		double alphaNumerator = by * cx - bx * cy;
		double commonDenominator = ay * bx - ax * by;

		if (commonDenominator > 0) {
			if (alphaNumerator < 0 || alphaNumerator > commonDenominator) {
				return 0;
			}
		} else if (commonDenominator < 0) {
			if (alphaNumerator > 0 || alphaNumerator < commonDenominator) {
				return 0;
			}
		}
		double betaNumerator = ax * cy - ay * cx;
		if (commonDenominator > 0) {
			if (betaNumerator < 0 || betaNumerator > commonDenominator) {
				return 0;
			}
		} else if (commonDenominator < 0) {
			if (betaNumerator > 0 || betaNumerator < commonDenominator) {
				return 0;
			}
		}
		if (commonDenominator == 0) {
			// This code wasn't in Franklin Antonio's method. It was added by Keith
			// Woodward.
			// The lines are parallel.
			// Check if they're collinear.
			double y3LessY1 = y3 - y1;
			double collinearityTestForP3 = x1 * (y2 - y3) + x2 * (y3LessY1) + x3 * (y1 - y2); // see
																								// http://mathworld.wolfram.com/Collinear.html
			// If p3 is collinear with p1 and p2 then p4 will also be collinear,
			// since p1-p2 is parallel with p3-p4
			if (collinearityTestForP3 == 0) {
				// The lines are collinear. Now check if they overlap.
				if (x1 >= x3 && x1 <= x4 || x1 <= x3 && x1 >= x4 || x2 >= x3 && x2 <= x4
						|| x2 <= x3 && x2 >= x4
						|| x3 >= x1 && x3 <= x2 || x3 <= x1 && x3 >= x2) {
					if (y1 >= y3 && y1 <= y4 || y1 <= y3 && y1 >= y4 || y2 >= y3 && y2 <= y4
							|| y2 <= y3 && y2 >= y4
							|| y3 >= y1 && y3 <= y2 || y3 <= y1 && y3 >= y2) {
						return 2;
					}
				}
			}
			return 0;
		}
		return 1;
	}

	static boolean doesIntersect(double l1x1, double l1y1, double l1x2, double l1y2, double l2x1,
			double l2y1,
			double l2x2, double l2y2) {
		double denom = ((l2y2 - l2y1) * (l1x2 - l1x1)) - ((l2x2 - l2x1) * (l1y2 - l1y1));

		if (denom == 0.0f) {
			return false;
		}

		double ua = (((l2x2 - l2x1) * (l1y1 - l2y1)) - ((l2y2 - l2y1) * (l1x1 - l2x1))) / denom;
		double ub = (((l1x2 - l1x1) * (l1y1 - l2y1)) - ((l1y2 - l1y1) * (l1x1 - l2x1))) / denom;

		return ((ua >= 0.0d) && (ua <= 1.0d) && (ub >= 0.0d) && (ub <= 1.0d));
	}

	public static boolean lineIntersect(
			int x1, int y1, int x2, int y2,
			int x3, int y3, int x4, int y4) {

		float denom = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
		if (denom == 0.0) { // Lines are parallel.
			return false;
		}
		float ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / denom;
		float ub = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / denom;
		if (ua >= 0.0f && ua <= 1.0f && ub >= 0.0f && ub <= 1.0f) {
			return true;
		}

		return false;
	}

	// http://en.wikipedia.org/wiki/Distance_from_a_point_to_a_line
	public static double pointToLineDistance(Point A, Point B, Point P) {
		double normalLength = Math.hypot(B.x - A.x, B.y - A.y);
		return Math.abs((P.x - A.x) * (B.y - A.y) - (P.y - A.y) * (B.x - A.x)) / normalLength;
	}

	public static final class Point2D {
		public double x;
		public double y;
	}

	// from libosmscout-render
	public static byte calcLinesIntersect(
			double ax1, double ay1,
			double ax2, double ay2,
			double bx1, double by1,
			double bx2, double by2,
			GeometryUtils.Point2D point)
	{
		double ua_numr = (bx2 - bx1) * (ay1 - by1) - (by2 - by1) * (ax1 - bx1);
		double ub_numr = (ax2 - ax1) * (ay1 - by1) - (ay2 - ay1) * (ax1 - bx1);
		double denr = (by2 - by1) * (ax2 - ax1) - (bx2 - bx1) * (ay2 - ay1);

		if (denr == 0.0)
		{
			// lines are coincident
			if (ua_numr == 0.0 && ub_numr == 0.0)
				return 2; //XSEC_COINCIDENT;

			// lines are parallel
			return 3; //XSEC_PARALLEL;
		}

		double ua = ua_numr / denr;
		double ub = ub_numr / denr;

		//if (ua >= 0.0 && ua <= 1.0 && ub >= 0.0 && ub <= 1.0)
		{
			point.x = ax1 + ua * (ax2 - ax1);
			point.y = ay1 + ua * (ay2 - ay1);
			return 0; //XSEC_TRUE;
		}

		//return 1; //XSEC_FALSE;
	}

	public static float dist(float x1, float y1, float x2, float y2) {
		return (float) Math.sqrt((x1 - x2) * (x1 - x2) + (y2 - y1) * (y2 - y1));
	}

	public static float pointLineDistance(float x1, float y1, float x2, float y2, float x3, float y3) {
		// taken from kartograph/simplify/douglas_peucker.py:
		// the perpendicular distance from a point (x3,y3) to the line from (x1,y1) to (x2,y2)
		// taken from http://local.wasp.uwa.edu.au/~pbourke/geometry/pointline/
		float d = dist(x1, y1, x2, y2);
		float u = (x3 - x1) * (x2 - x1) + (y3 - y1) * (y2 - y1) / (d * d);
		float x = x1 + u * (x2 - x1);
		float y = y1 + u * (y2 - y1);
		return dist(x, y, x3, y3);
	}

	/*
	 * from World Wind Android:
	 * Copyright (C) 2012 United States Government as represented by the
	 * Administrator of the National Aeronautics and Space Administration.
	 * All Rights Reserved.
	 * .
	 * given the current and previous locations of two points, compute the angle
	 * of the rotation they trace out
	 */
	public static double computeRotationAngle(float x, float y, float x2, float y2,
			float xPrev, float yPrev, float xPrev2, float yPrev2)
	{
		// can't compute if no previous points
		if (xPrev < 0 || yPrev < 0 || xPrev2 < 0 || yPrev2 < 0)
			return 0;

		if ((x - x2) == 0 || (xPrev - xPrev2) == 0)
			return 0;

		// 1. compute lines connecting pt1 to pt2, and pt1' to pt2'
		float slope = (y - y2) / (x - x2);
		float slopePrev = (yPrev - yPrev2) / (xPrev - xPrev2);

		// b = y - mx
		float b = y - slope * x;
		float bPrev = yPrev - slopePrev * xPrev;

		// 2. use Cramer's Rule to find the intersection of the two lines
		float det1 = -slope * 1 + slopePrev * 1;
		float det2 = b * 1 - bPrev * 1;
		float det3 = (-slope * bPrev) - (-slopePrev * b);

		// check for case where lines are parallel
		if (det1 == 0)
			return 0;

		// compute the intersection point
		float isectX = det2 / det1;
		float isectY = det3 / det1;

		// 3. use the law of Cosines to determine the angle covered

		// compute lengths of sides of triangle created by pt1, pt1Prev and the intersection pt
		double BC = Math.sqrt(Math.pow(x - isectX, 2) + Math.pow(y - isectY, 2));
		double AC = Math.sqrt(Math.pow(xPrev - isectX, 2) + Math.pow(yPrev - isectY, 2));
		double AB = Math.sqrt(Math.pow(x - xPrev, 2) + Math.pow(y - yPrev, 2));

		double dpx, dpy, dcx, dcy;

		//this.point1.set(xPrev - isectX, yPrev - isectY);
		//this.point2.set(x - isectX, y - isectY);

		// if one finger stayed fixed, may have degenerate triangle, so use other triangle instead
		if (BC == 0 || AC == 0 || AB == 0)
		{
			BC = Math.sqrt(Math.pow(x2 - isectX, 2) + Math.pow(y2 - isectY, 2));
			AC = Math.sqrt(Math.pow(xPrev2 - isectX, 2) + Math.pow(yPrev2 - isectY, 2));
			AB = Math.sqrt(Math.pow(x2 - xPrev2, 2) + Math.pow(y2 - yPrev2, 2));

			//this.point1.set(xPrev2 - isectX, yPrev2 - isectY);
			//this.point2.set(x2 - isectX, y2 - isectY);

			if (BC == 0 || AC == 0 || AB == 0)
				return 0;

			dpx = xPrev2 - isectX;
			dpy = yPrev2 - isectY;

			dcx = x2 - isectX;
			dcy = y2 - isectY;
		} else {
			dpx = xPrev - isectX;
			dpy = yPrev - isectY;

			dcx = x - isectX;
			dcy = y - isectY;
		}

		// Law of Cosines
		double num = (Math.pow(BC, 2) + Math.pow(AC, 2) - Math.pow(AB, 2));
		double denom = (2 * BC * AC);
		double BCA = Math.acos(num / denom);

		// use cross product to determine if rotation is positive or negative
		//if (this.point1.cross3(this.point2).z < 0)
		if (dpx * dcy - dpy * dcx < 0)
			BCA = 2 * Math.PI - BCA;

		return Math.toDegrees(BCA);
	}

	// from www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html
	public static boolean pointInPoly(int nvert, float[] vert, float testx, float testy) {
		int i, j;
		boolean inside = false;
		for (i = 0, j = (nvert - 1) * 2; i < nvert * 2; j = i++) {
			if (((vert[i * 2 + 1] > testy) != (vert[j * j + 1] > testy)) &&
					(testx < (vert[j * 2] - vert[i * 2])
							* (testy - vert[i * 2 + 1])
							/ (vert[j * 2 + 1] - vert[i * 2 + 1]) + vert[i * 2]))
				inside = !inside;
		}
		return inside;
	}


}
