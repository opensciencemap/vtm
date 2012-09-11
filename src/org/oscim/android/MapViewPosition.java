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
package org.oscim.android;

import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;

import android.util.FloatMath;
import android.util.Log;

/**
 * A MapPosition stores the latitude and longitude coordinate of a MapView together with its zoom level.
 */
public class MapViewPosition {
	private static float MAX_SCALE = 2.0f;
	private static float MIN_SCALE = 1.0f;
	private static int MAX_ZOOMLEVEL = 16;

	private double mLatitude;
	private double mLongitude;
	private final MapView mMapView;
	private byte mZoomLevel;
	private float mScale;
	private float mRotation;

	MapViewPosition(MapView mapView) {
		mMapView = mapView;

		mLatitude = Double.NaN;
		mLongitude = Double.NaN;
		mZoomLevel = -1;
		mScale = 1;
		mRotation = 0.0f;
	}

	/**
	 * @return the current center point of the MapView.
	 */
	public synchronized GeoPoint getMapCenter() {
		return new GeoPoint(mLatitude, mLongitude);
	}

	/**
	 * @return an immutable MapPosition or null, if this map position is not valid.
	 * @see #isValid()
	 */
	public synchronized MapPosition getMapPosition() {
		if (!isValid()) {
			return null;
		}
		// Log.d("MapViewPosition", "lat: " + mLatitude + " lon: " + mLongitude);
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
		double dx, dy;

		if (mMapView.enableRotation || mMapView.enableCompass) {
			float rad = (float) Math.toRadians(mRotation);
			dx = mx / mScale;
			dy = my / mScale;

			double x = dx * FloatMath.cos(rad) + dy * -FloatMath.sin(rad);
			double y = dx * FloatMath.sin(rad) + dy * FloatMath.cos(rad);

			dx = pixelX - x;
			dy = pixelY - y;
		}
		else {
			dx = pixelX - mx / mScale;
			dy = pixelY - my / mScale;
		}
		mLatitude = MercatorProjection.pixelYToLatitude(dy, mZoomLevel);
		mLatitude = MercatorProjection.limitLatitude(mLatitude);

		mLongitude = MercatorProjection.pixelXToLongitude(dx, mZoomLevel);

		mLongitude = MercatorProjection.wrapLongitude(mLongitude);
		// mLongitude = MercatorProjection.limitLongitude(mLongitude);
	}

	public synchronized void rotateMap(float angle, float cx, float cy) {
		moveMap(cx, cy);
		Log.d("MapViewPosition", "rotate:" + angle + " " + (mRotation - angle));
		mRotation -= angle;
	}

	public void setRotation(float f) {
		mRotation = f;
	}

	synchronized void setMapCenter(GeoPoint geoPoint) {
		mLatitude = MercatorProjection.limitLatitude(geoPoint.getLatitude());
		mLongitude = MercatorProjection.limitLongitude(geoPoint.getLongitude());
	}

	synchronized void setMapCenter(MapPosition mapPosition) {
		mLatitude = MercatorProjection.limitLatitude(mapPosition.lat);
		mLongitude = MercatorProjection.limitLongitude(mapPosition.lon);
		mZoomLevel = mMapView.limitZoomLevel(mapPosition.zoomLevel);
	}

	synchronized void setZoomLevel(byte zoomLevel) {
		mZoomLevel = mMapView.limitZoomLevel(zoomLevel);
	}

	synchronized void setScale(float scale) {
		mScale = scale;
	}

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

		float s = mScale * scale;

		if (s >= MAX_SCALE) {
			if (s > 8)
				return;

			if (mZoomLevel <= MAX_ZOOMLEVEL) {
				byte z = (byte) FloatMath.sqrt(s);
				mZoomLevel += z;
				s *= 1.0f / (1 << z);
			}
		} else if (s < MIN_SCALE) {
			byte z = (byte) FloatMath.sqrt(1 / s);
			if (z != 0 && mZoomLevel == 1)
				return;
			mZoomLevel -= z;
			s *= 1 << z;
		}

		mScale = s;
	}

	/**
	 * Zooms in or out by the given amount of zoom levels.
	 * 
	 * @param zoomLevelDiff
	 *            the difference to the current zoom level.
	 * @param s
	 *            scale between min/max zoom
	 * @return true if the zoom level was changed, false otherwise.
	 */
	// public boolean zoom(byte zoomLevelDiff, float s) {
	// float scale = s;
	//
	// if (zoomLevelDiff > 0) {
	// // check if zoom in is possible
	// if (mMapViewPosition.getZoomLevel() + zoomLevelDiff > getMaximumPossibleZoomLevel()) {
	// return false;
	// }
	//
	// scale *= 1.0f / (1 << zoomLevelDiff);
	// } else if (zoomLevelDiff < 0) {
	// // check if zoom out is possible
	// if (mMapViewPosition.getZoomLevel() + zoomLevelDiff < mMapZoomControls.getZoomLevelMin()) {
	// return false;
	// }
	//
	// scale *= 1 << -zoomLevelDiff;
	// }
	//
	// if (scale == 0)
	// scale = 1;
	// // else
	// // scale = Math.round(256.0f * scale) / 256.0f;
	//
	// mMapViewPosition.setZoomLevel((byte) (mMapViewPosition.getZoomLevel() + zoomLevelDiff));
	//
	// // mapZoomControls.onZoomLevelChange(mapViewPosition.getZoomLevel());
	//
	// // zoomAnimator.setParameters(zoomStart, matrixScaleFactor,
	// // getWidth() >> 1, getHeight() >> 1);
	// // zoomAnimator.startAnimation();
	//
	// // if (scale > MAX_ZOOM) {
	// // scale = MAX_ZOOM;
	// // }
	//
	// if (zoomLevelDiff != 0 || mZoomFactor != scale) {
	// mZoomFactor = scale;
	// redrawTiles();
	// }
	//
	// return true;
	// }

}
