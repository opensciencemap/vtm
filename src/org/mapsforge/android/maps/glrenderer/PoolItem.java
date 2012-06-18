/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package org.mapsforge.android.maps.glrenderer;

// TODO use byte[] for half-float, not converting on compilation (in glThread) 

class PoolItem {
	final float[] vertices;
	// final byte[] vertices;
	int used;

	PoolItem() {
		vertices = new float[SIZE];
		// vertices = new byte[SIZE];
		used = 0;
	}

	static int SIZE = 256;

	private static final byte b0x7c = (byte) 0x7c;
	private static final byte b0x00 = (byte) 0x00;
	private static final byte b0x01 = (byte) 0x01;
	private static final byte b0xfc = (byte) 0xfc;
	private static final byte b0x80 = (byte) 0x80;
	private static final byte b0x7b = (byte) 0x7b;
	private static final byte b0xff = (byte) 0xff;
	private static final byte b0xfb = (byte) 0xfb;
	private static final float FLOAT_HALF_PREC = 5.96046E-8f;
	private static final float FLOAT_HALF_MAX = 65504f;

	static void toHalfFloat(PoolItem item, byte[] data) {
		int out = 0;
		for (int j = 0; j < item.used; j++) {
			float flt = item.vertices[j];

			if (flt == 0f) {
				data[out++] = b0x00;
				data[out++] = b0x00;
			} else if (flt == -0f) {
				data[out++] = b0x00;
				data[out++] = b0x80;
			} else if (flt > FLOAT_HALF_MAX) {
				if (flt == Float.POSITIVE_INFINITY) {
					data[out++] = b0x00;
					data[out++] = b0x7c;
				} else {
					data[out++] = b0xff;
					data[out++] = b0x7b;
				}
			} else if (flt < -FLOAT_HALF_MAX) {
				if (flt == Float.NEGATIVE_INFINITY) {
					data[out++] = b0x00;
					data[out++] = b0xfc;
				} else {
					data[out++] = b0xff;
					data[out++] = b0xfb;
				}
			} else if (flt > 0f && flt < FLOAT_HALF_PREC) {
				data[out++] = b0x01;
				data[out++] = b0x00;
			} else if (flt < 0f && flt > -FLOAT_HALF_PREC) {
				data[out++] = b0x01;
				data[out++] = b0x80;
			} else {
				int f = Float.floatToIntBits(flt);

				if (f == 0x7fc00000)
					throw new UnsupportedOperationException("NaN to half conversion not supported!");

				data[out++] = (byte) ((f >> 13) & 0xff);

				data[out++] = (byte) (((f >> 24) & 0x80)
						| ((((f & 0x7f800000) - 0x38000000) >> 21) & 0x7c)
						| ((f >> 21) & 0x03));
			}
		}
	}
}
