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
package org.mapsforge.android.mapgenerator;

/**
 * @author jeff
 */
public class TileCacheKey {
	long x, y;
	byte z;
	int hash;

	/**
	 * 
	 */
	public TileCacheKey() {
	}

	/**
	 * @param key
	 *            create new TileCacheKey for key
	 */
	public TileCacheKey(TileCacheKey key) {
		this.x = key.x;
		this.y = key.y;
		this.z = key.z;
		this.hash = key.hash;
	}

	/**
	 * @param x
	 *            Position
	 * @param y
	 *            Position
	 * @param z
	 *            Position
	 */
	public TileCacheKey(long x, long y, byte z) {
		this.x = x;
		this.y = y;
		this.z = z;
		hash = 7 * z + 31 * ((int) (x ^ (x >>> 32)) + 31 * (int) (y ^ (y >>> 32)));
	}

	/**
	 * @param x
	 *            Position
	 * @param y
	 *            Position
	 * @param z
	 *            Position
	 * @return self
	 */
	public TileCacheKey set(long x, long y, byte z) {
		this.x = x;
		this.y = y;
		this.z = z;
		hash = 7 * z + 31 * ((int) (x ^ (x >>> 32)) + 31 * (int) (y ^ (y >>> 32)));
		return this;
	}

	@Override
	public boolean equals(Object obj) {
		TileCacheKey other = (TileCacheKey) obj;
		return (x == other.x && y == other.y && z == other.z);
	}

	@Override
	public int hashCode() {
		return hash;
	}
}
