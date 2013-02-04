/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 * Copyright 2012 Hannes Janetzek
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
package org.oscim.core;

import android.opengl.Matrix;

/** A MapPosition Container. */
public class MapPosition {

	public double lon;
	public double lat;

	public byte zoomLevel;
	public float scale;
	public float angle;
	public float tilt;

	public double x;
	public double y;

	public float[] viewMatrix;

	//	// DO NOT MODIFY! shared with MapViewPosition
	//	public float[] projMatrix;

	public MapPosition() {
		this.zoomLevel = (byte) 1;
		this.scale = 1;
		this.lat = 0;
		this.lon = 0;
		this.angle = 0;
		this.x = MercatorProjection.longitudeToPixelX(this.lon, zoomLevel);
		this.y = MercatorProjection.latitudeToPixelY(this.lat, zoomLevel);
	}

	// FIXME remove this here
	public void init() {
		viewMatrix = new float[16];
		Matrix.setIdentityM(viewMatrix, 0);
		//
		//		rotateMatrix = new float[16];
		//		Matrix.setIdentityM(rotateMatrix, 0);
	}

	//	public Point geopointToMap(GeoPoint in, Point reuse) {
	//		Point out = reuse == null ? new Point() : reuse;
	//		out.x = (int) (MercatorProjection.longitudeToPixelX(in.getLongitude(), zoomLevel) - x);
	//		out.y = (int) (MercatorProjection.latitudeToPixelY(in.getLatitude(), zoomLevel) - y);
	//
	//		return out;
	//	}

	//	public void geopointToMap(GeoPoint in, float[] out, int pos) {
	//		out[pos * 2 + 0] =
	//				(float) (MercatorProjection.longitudeToPixelX(in.getLongitude(), zoomLevel) - x);
	//		out[pos * 2 + 1] =
	//				(float) (MercatorProjection.latitudeToPixelY(in.getLatitude(), zoomLevel) - y);
	//	}

	/**
	 * @param geoPoint
	 *            the map position.
	 * @param zoomLevel
	 *            the zoom level.
	 * @param scale
	 *            ...
	 */
	public MapPosition(GeoPoint geoPoint, byte zoomLevel, float scale) {
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
		builder.append("MapPosition [");
		builder.append("lat=");
		builder.append(this.lat);
		builder.append(", lon=");
		builder.append(this.lon);
		builder.append(", zoomLevel=");
		builder.append(this.zoomLevel);
		builder.append("]");
		return builder.toString();
	}
}
