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
package org.oscim.view;

import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.utils.FastMath;

/**
 * A MapPosition stores the latitude and longitude coordinate of a MapView
 * together with its zoom level.
 */
public class MapViewPosition {
	private static final String TAG = "MapViewPosition";

	public final static int MAX_ZOOMLEVEL = 16;

	private final static float MAX_SCALE = 2.0f;
	private final static float MIN_SCALE = 1.0f;

	private final MapView mMapView;

	private double mLatitude;
	private double mLongitude;
	private byte mZoomLevel;
	private float mScale;
	private float mRotation;
	public float mTilt;

	// 2^mZoomLevel * mScale;
	private float mMapScale;

	// private final static float MAP_SIZE = 1000000;
	// private final static float MAP_SIZE2 = 1000000 >> 1;

	MapViewPosition(MapView mapView) {
		mMapView = mapView;

		mLatitude = Double.NaN;
		mLongitude = Double.NaN;
		mZoomLevel = -1;
		mScale = 1;
		mRotation = 0.0f;
		mTilt = 0;
		mMapScale = 1;
	}

	// private static double latitudeToMapView(double latitude) {
	// double sinLatitude = Math.sin(latitude * (Math.PI / 180));
	// return (0.5 - Math.log((1 + sinLatitude) / (1 - sinLatitude)) / (4 *
	// Math.PI))
	// * MAP_SIZE;
	// }
	//
	// public static double longitudeToMapView(double longitude) {
	// return (longitude + 180) / 360 * MAP_SIZE;
	// }
	//
	// private static double pixelXToLongitude(double pixelX, byte zoomLevel) {
	// return 360 * ((pixelX / ((long) Tile.TILE_SIZE << zoomLevel)) - 0.5);
	// }

	/**
	 * @return the current center point of the MapView.
	 */
	public synchronized GeoPoint getMapCenter() {
		return new GeoPoint(mLatitude, mLongitude);
	}

	/**
	 * @return an immutable MapPosition or null, if this map position is not
	 *         valid.
	 * @see #isValid()
	 */
	public synchronized MapPosition getMapPosition() {
		if (!isValid()) {
			return null;
		}
		// Log.d("MapViewPosition", "lat: " + mLatitude + " lon: " +
		// mLongitude);
		return new MapPosition(mLatitude, mLongitude, mZoomLevel, mScale, mRotation);
	}

	/**
	 * @return the current zoom level of the MapView.
	 */
	public synchronized byte getZoomLevel() {
		return mZoomLevel;
	}

	/**
	 * @return the current scale of the MapView.
	 */
	public synchronized float getScale() {
		return mScale;
	}

	/**
	 * @return true if this MapViewPosition is valid, false otherwise.
	 */
	public synchronized boolean isValid() {
		if (Double.isNaN(mLatitude)) {
			return false;
		} else if (mLatitude < MercatorProjection.LATITUDE_MIN) {
			return false;
		} else if (mLatitude > MercatorProjection.LATITUDE_MAX) {
			return false;
		}

		if (Double.isNaN(mLongitude)) {
			return false;
		} else if (mLongitude < MercatorProjection.LONGITUDE_MIN) {
			return false;
		} else if (mLongitude > MercatorProjection.LONGITUDE_MAX) {
			return false;
		}

		return true;
	}

	/**
	 * Get GeoPoint for a pixel on screen
	 * 
	 * @param x
	 *            ...
	 * @param y
	 *            ...
	 * @return the GeoPoint
	 */
	public GeoPoint getOffsetPoint(float x, float y) {
		double pixelX = MercatorProjection.longitudeToPixelX(mLongitude, mZoomLevel);
		double pixelY = MercatorProjection.latitudeToPixelY(mLatitude, mZoomLevel);

		double dx = ((mMapView.getWidth() >> 1) - x) / mScale;
		double dy = ((mMapView.getHeight() >> 1) - y) / mScale;

		if (mMapView.enableRotation || mMapView.enableCompass) {
			double rad = Math.toRadians(mRotation);
			double xx = dx * Math.cos(rad) + dy * -Math.sin(rad);
			double yy = dx * Math.sin(rad) + dy * Math.cos(rad);

			dx = pixelX - xx;
			dy = pixelY - yy;
		} else {
			dx = pixelX - dx;
			dy = pixelY - dy;
		}

		double latitude = MercatorProjection.pixelYToLatitude(dy, mZoomLevel);
		latitude = MercatorProjection.limitLatitude(latitude);

		double longitude = MercatorProjection.pixelXToLongitude(dx, mZoomLevel);
		longitude = MercatorProjection.limitLongitude(longitude);

		return new GeoPoint(latitude, longitude);
	}

	/**
	 * Moves this MapViewPosition by the given amount of pixels.
	 * 
	 * @param mx
	 *            the amount of pixels to move the map horizontally.
	 * @param my
	 *            the amount of pixels to move the map vertically.
	 */
	public synchronized void moveMap(float mx, float my) {
		double pixelX = MercatorProjection.longitudeToPixelX(mLongitude, mZoomLevel);
		double pixelY = MercatorProjection.latitudeToPixelY(mLatitude, mZoomLevel);

		double dx = mx / mScale;
		double dy = my / mScale;

		if (mMapView.enableRotation || mMapView.enableCompass) {
			double rad = Math.toRadians(mRotation);
			double x = dx * Math.cos(rad) + dy * -Math.sin(rad);
			double y = dx * Math.sin(rad) + dy * Math.cos(rad);

			dx = pixelX - x;
			dy = pixelY - y;
		}
		else {
			dx = pixelX - dx;
			dy = pixelY - dy;
		}
		mLatitude = MercatorProjection.pixelYToLatitude(dy, mZoomLevel);
		mLatitude = MercatorProjection.limitLatitude(mLatitude);

		mLongitude = MercatorProjection.pixelXToLongitude(dx, mZoomLevel);

		mLongitude = MercatorProjection.wrapLongitude(mLongitude);
		// mLongitude = MercatorProjection.limitLongitude(mLongitude);
	}

	public synchronized void rotateMap(float angle, float cx, float cy) {
		moveMap(cx, cy);
		// Log.d("MapViewPosition", "rotate:" + angle + " " + (mRotation -
		// angle));
		mRotation -= angle;
	}

	public void setRotation(float f) {
		mRotation = f;
	}

	public void setTilt(float f) {
		mTilt = f;
	}

	synchronized void setMapCenter(GeoPoint geoPoint) {
		mLatitude = MercatorProjection.limitLatitude(geoPoint.getLatitude());
		mLongitude = MercatorProjection.limitLongitude(geoPoint.getLongitude());
	}

	synchronized void setMapCenter(MapPosition mapPosition) {
		mLatitude = MercatorProjection.limitLatitude(mapPosition.lat);
		mLongitude = MercatorProjection.limitLongitude(mapPosition.lon);
		mZoomLevel = mMapView.limitZoomLevel(mapPosition.zoomLevel);
		mMapScale = 1 << mZoomLevel;
	}

	synchronized void setZoomLevel(byte zoomLevel) {
		mZoomLevel = mMapView.limitZoomLevel(zoomLevel);
		mMapScale = 1 << mZoomLevel;
	}

	synchronized void setScale(float scale) {
		mScale = scale;
	}

	// synchronized void zoomBoundingBox(GeoPoint p1, GeoPoint p2) {
	//
	// }

	/**
	 * @param scale
	 *            ...
	 * @param pivotX
	 *            ...
	 * @param pivotY
	 *            ...
	 */
	public synchronized void scaleMap(float scale, float pivotX, float pivotY) {
		if (pivotY != 0 || pivotY != 0)
			moveMap(pivotX * (1.0f - scale),
					pivotY * (1.0f - scale));

		float newScale = mMapScale * scale;

		int z = FastMath.log2((int) newScale);

		if (z <= 0 || (z >= MAX_ZOOMLEVEL && mScale >= 8))
			return;

		if (z > MAX_ZOOMLEVEL) {
			// z16 shows everything, just increase scaling
			if (mScale * scale > 8)
				return;

			mScale *= scale;
			mMapScale = newScale;
			return;
		}

		mZoomLevel = (byte) z;
		mScale = newScale / (1 << z);
		mMapScale = newScale;
	}

	public boolean tilt(float moveX) {
		float tilt = mTilt + moveX;
		if (tilt > 25)
			tilt = 25;
		else if (tilt < 0)
			tilt = 0;
		if (mTilt == tilt)
			return false;

		mTilt = tilt;
		return true;
	}
}
