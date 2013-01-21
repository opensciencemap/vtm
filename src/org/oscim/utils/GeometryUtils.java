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
}
