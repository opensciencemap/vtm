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
package org.oscim.android.swrenderer;

/**
 * An ImmutablePoint represents an fixed pair of float coordinates.
 */
class ImmutablePoint implements Comparable<ImmutablePoint> {
	/**
	 * Subtracts the x and y coordinates of one point from another point.
	 * 
	 * @param minuend
	 *            the minuend.
	 * @param subtrahend
	 *            the subtrahend.
	 * @return a new Point object.
	 */
	static ImmutablePoint substract(ImmutablePoint minuend, ImmutablePoint subtrahend) {
		return new ImmutablePoint(minuend.pointX - subtrahend.pointX, minuend.pointY - subtrahend.pointY);
	}

	/**
	 * Stores the hash code of this object.
	 */
	private final int hashCodeValue;

	/**
	 * X coordinate of this point.
	 */
	final float pointX;

	/**
	 * Y coordinate of this point.
	 */
	final float pointY;

	/**
	 * @param x
	 *            the x coordinate of the point.
	 * @param y
	 *            the y coordinate of the point.
	 */
	ImmutablePoint(float x, float y) {
		this.pointX = x;
		this.pointY = y;
		this.hashCodeValue = calculateHashCode();
	}

	@Override
	public int compareTo(ImmutablePoint point) {
		if (this.pointX > point.pointX) {
			return 1;
		} else if (this.pointX < point.pointX) {
			return -1;
		} else if (this.pointY > point.pointY) {
			return 1;
		} else if (this.pointY < point.pointY) {
			return -1;
		}
		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof ImmutablePoint)) {
			return false;
		}
		ImmutablePoint other = (ImmutablePoint) obj;
		if (this.pointX != other.pointX) {
			return false;
		} else if (this.pointY != other.pointY) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return this.hashCodeValue;
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("ImmutablePoint [x=");
		stringBuilder.append(this.pointX);
		stringBuilder.append(", y=");
		stringBuilder.append(this.pointY);
		stringBuilder.append("]");
		return stringBuilder.toString();
	}

	/**
	 * @return the hash code of this object.
	 */
	private int calculateHashCode() {
		int result = 7;
		result = 31 * result + Float.floatToIntBits(this.pointX);
		result = 31 * result + Float.floatToIntBits(this.pointY);
		return result;
	}
}
