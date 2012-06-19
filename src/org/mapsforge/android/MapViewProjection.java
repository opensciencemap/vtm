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
package org.mapsforge.android;

import org.mapsforge.core.GeoPoint;
import org.mapsforge.core.MapPosition;
import org.mapsforge.core.MercatorProjection;

import android.graphics.Point;

class MapViewProjection implements Projection {
	private static final String INVALID_MAP_VIEW_DIMENSIONS = "invalid MapView dimensions";

	private final MapView mMapView;

	MapViewProjection(MapView mapView) {
		mMapView = mapView;
	}

	@Override
	public GeoPoint fromPixels(int x, int y) {
		if (mMapView.getWidth() <= 0 || mMapView.getHeight() <= 0) {
			return null;
		}

		MapPosition mapPosition = mMapView.getMapPosition().getMapPosition();

		// calculate the pixel coordinates of the top left corner
		GeoPoint geoPoint = mapPosition.geoPoint;
		double pixelX = MercatorProjection.longitudeToPixelX(geoPoint.getLongitude(), mapPosition.zoomLevel);
		double pixelY = MercatorProjection.latitudeToPixelY(geoPoint.getLatitude(), mapPosition.zoomLevel);
		pixelX -= mMapView.getWidth() >> 1;
		pixelY -= mMapView.getHeight() >> 1;

		// convert the pixel coordinates to a GeoPoint and return it
		return new GeoPoint(MercatorProjection.pixelYToLatitude(pixelY + y, mapPosition.zoomLevel),
				MercatorProjection.pixelXToLongitude(pixelX + x, mapPosition.zoomLevel));
	}

	@Override
	public int getLatitudeSpan() {
		if (mMapView.getWidth() > 0 && mMapView.getWidth() > 0) {
			GeoPoint top = fromPixels(0, 0);
			GeoPoint bottom = fromPixels(0, mMapView.getHeight());
			return Math.abs(top.latitudeE6 - bottom.latitudeE6);
		}
		throw new IllegalStateException(INVALID_MAP_VIEW_DIMENSIONS);
	}

	@Override
	public int getLongitudeSpan() {
		if (mMapView.getWidth() > 0 && mMapView.getWidth() > 0) {
			GeoPoint left = fromPixels(0, 0);
			GeoPoint right = fromPixels(mMapView.getWidth(), 0);
			return Math.abs(left.longitudeE6 - right.longitudeE6);
		}
		throw new IllegalStateException(INVALID_MAP_VIEW_DIMENSIONS);
	}

	@Override
	public float metersToPixels(float meters, byte zoom) {
		double latitude = mMapView.getMapPosition().getMapCenter().getLatitude();
		double groundResolution = MercatorProjection.calculateGroundResolution(latitude, zoom);
		return (float) (meters * (1 / groundResolution));
	}

	@Override
	public Point toPixels(GeoPoint in, Point out) {
		if (mMapView.getWidth() <= 0 || mMapView.getHeight() <= 0) {
			return null;
		}

		MapPosition mapPosition = mMapView.getMapPosition().getMapPosition();

		// calculate the pixel coordinates of the top left corner
		GeoPoint geoPoint = mapPosition.geoPoint;
		double pixelX = MercatorProjection.longitudeToPixelX(geoPoint.getLongitude(), mapPosition.zoomLevel);
		double pixelY = MercatorProjection.latitudeToPixelY(geoPoint.getLatitude(), mapPosition.zoomLevel);
		pixelX -= mMapView.getWidth() >> 1;
		pixelY -= mMapView.getHeight() >> 1;

		if (out == null) {
			// create a new point and return it
			return new Point(
					(int) (MercatorProjection.longitudeToPixelX(in.getLongitude(), mapPosition.zoomLevel) - pixelX),
					(int) (MercatorProjection.latitudeToPixelY(in.getLatitude(), mapPosition.zoomLevel) - pixelY));
		}

		// reuse the existing point
		out.x = (int) (MercatorProjection.longitudeToPixelX(in.getLongitude(), mapPosition.zoomLevel) - pixelX);
		out.y = (int) (MercatorProjection.latitudeToPixelY(in.getLatitude(), mapPosition.zoomLevel) - pixelY);
		return out;
	}

	@Override
	public Point toPoint(GeoPoint in, Point out, byte zoom) {
		if (out == null) {
			// create a new point and return it
			return new Point((int) MercatorProjection.longitudeToPixelX(in.getLongitude(), zoom),
					(int) MercatorProjection.latitudeToPixelY(in.getLatitude(), zoom));
		}

		// reuse the existing point
		out.x = (int) MercatorProjection.longitudeToPixelX(in.getLongitude(), zoom);
		out.y = (int) MercatorProjection.latitudeToPixelY(in.getLatitude(), zoom);
		return out;
	}
}
