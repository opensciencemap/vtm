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
package org.oscim.generator;

/**
 * based on ComparableTimSort:
 * everything below is Copyright OpenJDK, Oracle
 */

public class TileDistanceSort {
	public static void sort(JobTile[] a, int lo, int hi) {
		int nRemaining = hi - lo;
		if (nRemaining < 2) {
			return;
		}

		int initRunLen = countRunAndMakeAscending(a, lo, hi);
		binarySort(a, lo, hi, lo + initRunLen);

	}

	static int compareTo(JobTile a, JobTile b) {
		if (a == null && b == null)
			return 0;

		if (a == null)
			return 1;

		if (b == null)
			return -1;

		if (a.distance < b.distance) {
			return -1;
		}
		if (a.distance > b.distance) {
			return 1;
		}
		return 0;
	}

	private static void binarySort(JobTile[] a, int lo, int hi, int start)
	{
		assert ((lo <= start) && (start <= hi));
		if (start == lo)
			++start;
		for (; start < hi; ++start)
		{
			JobTile pivot = a[start];

			int left = lo;
			int right = start;
			assert (left <= right);

			while (left < right) {
				int mid = left + right >>> 1;
				//if (pivot.compareTo(a[mid]) < 0)
				if (compareTo(pivot, a[mid]) < 0)
					right = mid;
				else
					left = mid + 1;
			}
			assert (left == right);

			int n = start - left;

			switch (n)
			{
				case 2:
					a[(left + 2)] = a[(left + 1)];
					//$FALL-THROUGH$
				case 1:
					a[(left + 1)] = a[left];
					break;
				default:
					System.arraycopy(a, left, a, left + 1, n);
			}
			a[left] = pivot;
		}
	}

	private static int countRunAndMakeAscending(JobTile[] a, int lo, int hi)
	{
		assert (lo < hi);
		int runHi = lo + 1;
		if (runHi == hi) {
			return 1;
		}

		if (compareTo((a[(runHi++)]), a[lo]) < 0) {
			while ((runHi < hi) && (compareTo((a[runHi]), a[(runHi - 1)]) < 0))
				++runHi;
			reverseRange(a, lo, runHi);
		} else {
			while ((runHi < hi) && (compareTo((a[runHi]), a[(runHi - 1)]) >= 0)) {
				++runHi;
			}
		}
		return (runHi - lo);
	}

	private static void reverseRange(JobTile[] a, int lo, int hi)
	{
		--hi;
		while (lo < hi) {
			JobTile t = a[lo];
			a[(lo++)] = a[hi];
			a[(hi--)] = t;
		}
	}
}
