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
package org.mapsforge.core;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/**
 * A GeoPoint represents an immutable pair of latitude and longitude coordinates.
 */
public class GeoPoint implements Comparable<GeoPoint>, Serializable {
	/**
	 * Conversion factor from degrees to microdegrees.
	 */
	private static final double CONVERSION_FACTOR = 1000000d;

	private static final long serialVersionUID = 1L;

	/**
	 * The latitude value of this GeoPoint in microdegrees (degrees * 10^6).
	 */
	public final int latitudeE6;

	/**
	 * The longitude value of this GeoPoint in microdegrees (degrees * 10^6).
	 */
	public final int longitudeE6;

	/**
	 * The hash code of this object.
	 */
	private transient int hashCodeValue;

	/**
	 * @param latitude
	 *            the latitude in degrees, will be limited to the possible latitude range.
	 * @param longitude
	 *            the longitude in degrees, will be limited to the possible longitude range.
	 */
	public GeoPoint(double latitude, double longitude) {
		double limitLatitude = MercatorProjection.limitLatitude(latitude);
		this.latitudeE6 = (int) (limitLatitude * CONVERSION_FACTOR);

		double limitLongitude = MercatorProjection.limitLongitude(longitude);
		this.longitudeE6 = (int) (limitLongitude * CONVERSION_FACTOR);

		this.hashCodeValue = calculateHashCode();
	}

	/**
	 * @param latitudeE6
	 *            the latitude in microdegrees (degrees * 10^6), will be limited to the possible latitude range.
	 * @param longitudeE6
	 *            the longitude in microdegrees (degrees * 10^6), will be limited to the possible longitude range.
	 */
	public GeoPoint(int latitudeE6, int longitudeE6) {
		this(latitudeE6 / CONVERSION_FACTOR, longitudeE6 / CONVERSION_FACTOR);
	}

	@Override
	public int compareTo(GeoPoint geoPoint) {
		if (this.longitudeE6 > geoPoint.longitudeE6) {
			return 1;
		} else if (this.longitudeE6 < geoPoint.longitudeE6) {
			return -1;
		} else if (this.latitudeE6 > geoPoint.latitudeE6) {
			return 1;
		} else if (this.latitudeE6 < geoPoint.latitudeE6) {
			return -1;
		}
		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof GeoPoint)) {
			return false;
		}
		GeoPoint other = (GeoPoint) obj;
		if (this.latitudeE6 != other.latitudeE6) {
			return false;
		} else if (this.longitudeE6 != other.longitudeE6) {
			return false;
		}
		return true;
	}

	/**
	 * @return the latitude value of this GeoPoint in degrees.
	 */
	public double getLatitude() {
		return this.latitudeE6 / CONVERSION_FACTOR;
	}

	/**
	 * @return the longitude value of this GeoPoint in degrees.
	 */
	public double getLongitude() {
		return this.longitudeE6 / CONVERSION_FACTOR;
	}

	@Override
	public int hashCode() {
		return this.hashCodeValue;
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("GeoPoint [latitudeE6=");
		stringBuilder.append(this.latitudeE6);
		stringBuilder.append(", longitudeE6=");
		stringBuilder.append(this.longitudeE6);
		stringBuilder.append("]");
		return stringBuilder.toString();
	}

	/**
	 * @return the hash code of this object.
	 */
	private int calculateHashCode() {
		int result = 7;
		result = 31 * result + this.latitudeE6;
		result = 31 * result + this.longitudeE6;
		return result;
	}

	private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
		objectInputStream.defaultReadObject();
		this.hashCodeValue = calculateHashCode();
	}
}
