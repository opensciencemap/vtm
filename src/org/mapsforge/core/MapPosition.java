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

/**
 * A MapPosition represents an immutable pair of {@link GeoPoint} and zoom level.
 */
public class MapPosition {

	/**
	 * The map position.
	 */
	// public final GeoPoint geoPoint;
	public final double lon;
	public final double lat;

	/**
	 * The zoom level.
	 */
	public final byte zoomLevel;

	/**
	 * 1.0 - 2.0 scale of current zoomlevel
	 */
	public final float scale;

	public final float angle;

	public final double x;
	public final double y;

	public MapPosition() {
		this.zoomLevel = (byte) 1;
		this.scale = 1;
		this.lat = 0;
		this.lon = 0;
		this.angle = 0;
		this.x = MercatorProjection.longitudeToPixelX(this.lon, zoomLevel);
		this.y = MercatorProjection.latitudeToPixelY(this.lat, zoomLevel);
	}

	/**
	 * @param geoPoint
	 *            the map position.
	 * @param zoomLevel
	 *            the zoom level.
	 * @param scale
	 *            ...
	 */
	public MapPosition(GeoPoint geoPoint, byte zoomLevel, float scale) {
		// this.geoPoint = geoPoint;
		this.zoomLevel = zoomLevel;
		this.scale = scale;
		this.lat = geoPoint.getLatitude();
		this.lon = geoPoint.getLongitude();
		this.angle = 0;
		this.x = MercatorProjection.longitudeToPixelX(this.lon, zoomLevel);
		this.y = MercatorProjection.latitudeToPixelY(this.lat, zoomLevel);
	}

	public MapPosition(double latitude, double longitude, byte zoomLevel, float scale,
			float angle) {
		this.zoomLevel = zoomLevel;
		this.scale = scale;
		this.lat = latitude;
		this.lon = longitude;
		this.angle = angle;
		this.x = MercatorProjection.longitudeToPixelX(longitude, zoomLevel);
		this.y = MercatorProjection.latitudeToPixelY(latitude, zoomLevel);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MapPosition [geoPoint=");
		builder.append("lat");
		builder.append(this.lat);
		builder.append("lon");
		builder.append(this.lon);
		builder.append(", zoomLevel=");
		builder.append(this.zoomLevel);
		builder.append("]");
		return builder.toString();
	}
}
