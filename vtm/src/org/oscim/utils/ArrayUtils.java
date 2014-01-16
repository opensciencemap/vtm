/*
 * Copyright 2013 Hannes Janetzek
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

public class ArrayUtils {

	public static <T> void reverse(T[] data) {
		reverse(data, 0, data.length);
	}

	public static <T> void reverse(T[] data, int left, int right) {
		right--;

		while (left < right) {
			T tmp = data[left];
			data[left] = data[right];
			data[right] = tmp;

			left++;
			right--;
		}
	}

	public static void reverse(short[] data, int left, int right, int stride) {
		right -= stride;

		while (left < right) {
			for (int i = 0; i < stride; i++) {
				short tmp = data[left + i];
				data[left + i] = data[right + i];
				data[right + i] = tmp;
			}
			left += stride;
			right -= stride;
		}
	}

	public static void reverse(byte[] data, int left, int right) {
		right -= 1;

		while (left < right) {
			byte tmp = data[left];
			data[left] = data[right];
			data[right] = tmp;

			left++;
			right--;
		}
	}
}
