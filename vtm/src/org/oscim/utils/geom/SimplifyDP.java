package org.oscim.utils.geom;

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

import static org.oscim.utils.geom.GeometryUtils.squareSegmentDistance;

import org.oscim.core.GeometryBuffer;

/**
 * Douglas-Peucker line simplification with stack and some bit twiddling.
 * 
 * based on: https://github.com/mourner/simplify-js,
 * https://github.com/ekeneijeoma/simplify-java
 */
public class SimplifyDP {

	int[] markers = new int[1];
	int[] stack = new int[32];

	public void simplify(GeometryBuffer geom, float sqTolerance) {
		short[] idx = geom.index;

		int inPos = 0;
		int outPos = 0;

		for (int i = 0, n = idx.length; i < n; i++) {
			int len = idx[i];
			if (len < 0)
				break;
			if (len < 6) {
				inPos += len;
				outPos += len;
				continue;
			}

			int cnt = simplify(geom.points, inPos, len, outPos, sqTolerance);
			idx[i] = (short) cnt;
			outPos += cnt;
			inPos += len;
		}
	}

	public int simplify(float[] points, int inPos, int length, int out, float sqTolerance) {

		/* cheap int bitset (use length / 32 ints)
		 * might should use boolean, or BitSet,
		 * as this is not really a memory hog :) */
		int n = (length >> 5) + 1;
		if (markers.length < n) {
			markers = new int[n];
		} else {
			for (int i = 0; i < n; i++)
				markers[i] = 0;
		}

		int first = 0;
		int last = inPos + length - 2;
		int index = 0;

		float maxSqDist;
		float sqDist;
		int sp = 0;

		while (true) {
			maxSqDist = 0;

			for (int i = first + 2; i < last; i += 2) {
				sqDist = squareSegmentDistance(points, i, first, last);
				if (sqDist > maxSqDist) {
					index = i;
					maxSqDist = sqDist;
				}
			}

			if (maxSqDist > sqTolerance) {
				/* get marker for 'pos' and shift marker relative
				 * position into it;
				 * (pos & 0x1F == pos % 32) */
				int pos = index >> 1;
				markers[pos >> 5] |= 1 << (pos & 0x1F);

				if (sp + 4 == stack.length) {
					int tmp[] = new int[stack.length + 64];
					System.arraycopy(stack, 0, tmp, 0, stack.length);
					stack = tmp;
				}

				stack[sp++] = first;
				stack[sp++] = index;

				stack[sp++] = index;
				stack[sp++] = last;
			}

			if (sp == 0)
				break;

			last = stack[--sp];
			first = stack[--sp];
		}

		points[out++] = points[inPos];
		points[out++] = points[inPos + 1];

		last = inPos + length - 2;

		O: for (int i = 0; i < markers.length; i++) {
			int marker = markers[i];
			/* point position of this marker */
			int mPos = (i << 6);

			for (int j = 0; j < 32; j++) {
				/* check if marker is set */
				if ((marker & (1 << j)) != 0) {
					int pos = mPos + (j << 1);
					if (pos >= last)
						break O;

					points[out++] = points[pos];
					points[out++] = points[pos + 1];
				}
			}
		}
		points[out++] = points[last];
		points[out++] = points[last + 1];

		return out;
	}
}
