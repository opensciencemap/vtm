/*
 * Copyright 2012 Hannes Janetzek
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

public class FastMath {
	/**
	 * from http://graphics.stanford.edu/~seander/bithacks.html#IntegerLog
	 *
	 * @param v
	 *            ...
	 * @return ...
	 */
	public static int log2(int v) {

		int r = 0; // result of log2(v) will go here

		if ((v & 0xFFFF0000) != 0) {
			v >>= 16;
			r |= 16;
		}
		if ((v & 0xFF00) != 0) {
			v >>= 8;
			r |= 8;
		}
		if ((v & 0xF0) != 0) {
			v >>= 4;
			r |= 4;
		}
		if ((v & 0xC) != 0) {
			v >>= 2;
			r |= 2;
		}
		if ((v & 0x2) != 0) {
			r |= 1;
		}
		return r;
	}

	public static float pow(int pow) {
		if (pow == 0)
			return 1;

		return (pow > 0 ? (1 << pow) : (1.0f / (1 << -pow)));
	}

	public static float abs(float value){
		return value < 0 ? -value : value;
	}

	public static float absMax(float value1, float value2){
		float a1 = value1 < 0 ? -value1 : value1;
		float a2 = value2 < 0 ? -value2 : value2;
		return a2 < a1 ? a1 : a2;
	}

	public static boolean absMaxCmp(float value1, float value2, float cmp){
		return value1 < -cmp || value1 > cmp || value2 < -cmp || value2 > cmp;
	}
}
