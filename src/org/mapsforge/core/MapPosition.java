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
 * A MapPosition represents an immutable pair of {@link GeoPoint} and zoom level.
 */
public class MapPosition implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * The map position.
	 */
	public final GeoPoint geoPoint;

	/**
	 * The zoom level.
	 */
	public final byte zoomLevel;

	/**
	 * 1.0 - 2.0 scale of current zoomlevel
	 */
	public final float scale;
	/**
	 * The hash code of this object.
	 */
	private transient int hashCodeValue;

	/**
	 * @param geoPoint
	 *            the map position.
	 * @param zoomLevel
	 *            the zoom level.
	 * @param scale
	 *            ...
	 */
	public MapPosition(GeoPoint geoPoint, byte zoomLevel, float scale) {
		this.geoPoint = geoPoint;
		this.zoomLevel = zoomLevel;
		this.scale = scale;
		this.hashCodeValue = calculateHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof MapPosition)) {
			return false;
		}
		MapPosition other = (MapPosition) obj;
		if (this.geoPoint == null) {
			if (other.geoPoint != null) {
				return false;
			}
		} else if (!this.geoPoint.equals(other.geoPoint)) {
			return false;
		}
		if (this.zoomLevel != other.zoomLevel) {
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
		StringBuilder builder = new StringBuilder();
		builder.append("MapPosition [geoPoint=");
		builder.append(this.geoPoint);
		builder.append(", zoomLevel=");
		builder.append(this.zoomLevel);
		builder.append("]");
		return builder.toString();
	}

	/**
	 * @return the hash code of this object.
	 */
	private int calculateHashCode() {
		int result = 7;
		result = 31 * result + ((this.geoPoint == null) ? 0 : this.geoPoint.hashCode());
		result = 31 * result + this.zoomLevel;
		return result;
	}

	private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
		objectInputStream.defaultReadObject();
		this.hashCodeValue = calculateHashCode();
	}
}
