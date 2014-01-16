/*
 * Copyright 2012, 2013 Hannes Janetzek
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
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

public final class Vec2 {

	public static void set(float[] v, int pos, float x, float y) {
		v[(pos << 1) + 0] = x;
		v[(pos << 1) + 1] = y;
	}

	public static float dot(float[] a, int apos, float[] b, int bpos) {
		return a[apos << 1] * b[bpos << 1] + a[(apos << 1) + 1] * b[(bpos << 1) + 1];
	}

	public final static float lengthSquared(float[] v, int pos) {
		float x = v[(pos << 1) + 0];
		float y = v[(pos << 1) + 1];

		return x * x + y * y;
	}

	public final static void normalizeSquared(float[] v, int pos) {
		float x = v[(pos << 1) + 0];
		float y = v[(pos << 1) + 1];

		float length = x * x + y * y;

		v[(pos << 1) + 0] = x / length;
		v[(pos << 1) + 1] = y / length;
	}

	public final static void normalize(float[] v, int pos) {
		float x = v[(pos << 1) + 0];
		float y = v[(pos << 1) + 1];

		double length = Math.sqrt(x * x + y * y);

		v[(pos << 1) + 0] = (float) (x / length);
		v[(pos << 1) + 1] = (float) (y / length);
	}

	public final static float length(float[] v, int pos) {
		float x = v[(pos << 1) + 0];
		float y = v[(pos << 1) + 1];

		return (float) Math.sqrt(x * x + y * y);
	}

	public final static void add(float[] result, int rpos, float[] a, int apos, float[] b, int bpos) {
		result[(rpos << 1) + 0] = a[(apos << 1) + 0] + b[(bpos << 1) + 0];
		result[(rpos << 1) + 1] = a[(apos << 1) + 1] + b[(bpos << 1) + 1];
	}

	public final static void sub(float[] result, int rpos, float[] a, int apos, float[] b, int bpos) {
		result[(rpos << 1) + 0] = a[(apos << 1) + 0] - b[(bpos << 1) + 0];
		result[(rpos << 1) + 1] = a[(apos << 1) + 1] - b[(bpos << 1) + 1];
	}

	public final static void mul(float[] v, int pos, float a) {
		v[(pos << 1) + 0] *= a;
		v[(pos << 1) + 1] *= a;
	}

}
