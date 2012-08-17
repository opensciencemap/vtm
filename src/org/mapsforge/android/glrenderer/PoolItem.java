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

package org.mapsforge.android.glrenderer;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

// TODO use byte[] for half-float, not converting on compilation (in glThread) 

class PoolItem {
	final float[] vertices;
	int used;
	PoolItem next;

	PoolItem() {
		vertices = new float[SIZE];
		used = 0;
	}

	static int SIZE = 128;

	private static final float FLOAT_HALF_PREC = 5.96046E-8f;
	private static final float FLOAT_HALF_MAX = 65504f;

	private static ByteBuffer byteBuffer = ByteBuffer.allocate(SIZE * 4);
	private static IntBuffer intBuffer = byteBuffer.asIntBuffer();
	private static FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
	private static int[] intArray = new int[SIZE];

	static void toHalfFloat(PoolItem item, short[] data) {
		floatBuffer.position(0);
		floatBuffer.put(item.vertices, 0, item.used);
		intBuffer.position(0);
		intBuffer.get(intArray, 0, item.used);

		int out = 0;
		for (int j = 0; j < item.used; j++) {
			float flt = item.vertices[j];
			int f = intArray[j];

			if (f == 0x0000000) {
				// == 0
				data[out++] = (short) 0x0000;
			} else if (f == 0x80000000) {
				// == -0
				data[out++] = (short) 0x8000;
			} else if (f == 0x3f800000) {
				// == 1
				data[out++] = (short) 0x3c00;
			} else if (f == 0xbf800000) {
				// == -1
				data[out++] = (short) 0xbc00;
			} else if (flt > FLOAT_HALF_MAX) {
				if (flt == Float.POSITIVE_INFINITY) {
					data[out++] = (short) 0x7c00;
				} else {
					data[out++] = (short) 0x7bff;
				}
			} else if (flt < -FLOAT_HALF_MAX) {
				if (flt == Float.NEGATIVE_INFINITY) {
					data[out++] = (short) 0xfc00;
				} else {
					data[out++] = (short) 0xfbff;
				}
			} else if (flt > 0f && flt < FLOAT_HALF_PREC) {
				data[out++] = (short) 0x0001;
			} else if (flt < 0f && flt > -FLOAT_HALF_PREC) {
				data[out++] = (short) 0x8001;
			} else {
				// maybe just ignore and set 0? -- we'll see. when this happens
				if (f == 0x7fc00000)
					throw new UnsupportedOperationException(
							"NaN to half conversion not supported!");

				data[out++] = (short) (((f >> 16) & 0x8000)
						| ((((f & 0x7f800000) - 0x38000000) >> 13) & 0x7c00)
						| ((f >> 13) & 0x03ff));
			}
		}
	}
}
