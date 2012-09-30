/*
 * Copyright 2010, 2011, 2012 mapsforge.org, 2012 Hannes Janetzek
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
import org.oscim.core.MercatorProjection;
import org.oscim.utils.FastMath;

import android.opengl.Matrix;
import android.util.FloatMath;
import android.util.Log;

/**
 * A MapPosition stores the latitude and longitude coordinate of a MapView
 * together with its zoom level.
 */
public class MapViewPosition {
	private static final String TAG = "MapViewPosition";

	public final static int MAX_ZOOMLEVEL = 17;
	public final static int MIN_ZOOMLEVEL = 2;

	private final static float MAX_ANGLE = 20;

	private final MapView mMapView;

	private double mLatitude;
	private double mLongitude;
	private byte mZoomLevel;
	// 1.0 - 2.0 scale per level
	private float mScale;
	// 2^mZoomLevel * mScale;
	private float mMapScale;

	private float mRotation;
	public float mTilt;

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

	private float[] mProjMatrix = new float[16];
	private float[] mProjMatrixI = new float[16];
	private float[] mUnprojMatrix = new float[16];
	private float[] mRotateMatrix = new float[16];
	private float[] mTmpMatrix = new float[16];

	private int mHeight, mWidth;

	void setViewport(int width, int height) {
		Matrix.frustumM(mProjMatrix, 0, -0.5f * width, 0.5f * width,
				0.5f * height, -0.5f * height, 1, 2);

		Matrix.translateM(mProjMatrix, 0, 0, 0, -1);

		Matrix.invertM(mProjMatrixI, 0, mProjMatrix, 0);
		Matrix.invertM(mUnprojMatrix, 0, mProjMatrix, 0);

		Matrix.setIdentityM(mRotateMatrix, 0);

		mHeight = height;
		mWidth = width;
	}

	public synchronized boolean getMapPosition(final MapPosition mapPosition,
			final float[] coords) {
		// if (!isValid())
		// return false;

		// if (mapPosition.lat == mLatitude
		// && mapPosition.lon == mLongitude
		// && mapPosition.zoomLevel == mZoomLevel
		// && mapPosition.scale == mScale
		// && mapPosition.angle == mRotation)
		// return false;
		byte z = mZoomLevel;

		mapPosition.lat = mLatitude;
		mapPosition.lon = mLongitude;
		mapPosition.angle = mRotation;
		mapPosition.scale = mScale;
		mapPosition.zoomLevel = z;

		mapPosition.x = MercatorProjection.longitudeToPixelX(mLongitude, z);
		mapPosition.y = MercatorProjection.latitudeToPixelY(mLatitude, z);

		if (mapPosition.rotation != null) {
			// updateMatrix();
			System.arraycopy(mRotateMatrix, 0, mapPosition.rotation, 0, 16);
		}

		if (coords == null)
			return true;

		// if (mapPosition.rotation == null)
		// updateMatrix();

		// not so sure about this, but works...
		float tilt = (float) Math.sin(Math.toRadians(mTilt)) * 4;

		unproject(-1, 1, tilt, coords, 0); // top-left
		unproject(1, 1, tilt, coords, 2); // top-right
		unproject(1, -1, -tilt, coords, 4); // bottom-right
		unproject(-1, -1, -tilt, coords, 6); // bottom-left

		return true;
	}

	private float[] mv = { 0, 0, 0, 1 };
	private float[] mBBoxCoords = new float[8];

	private void unproject(float x, float y, float z, float[] coords, int position) {
		mv[0] = x;
		mv[1] = y;
		mv[2] = z - 1;
		mv[3] = 1;

		Matrix.multiplyMV(mv, 0, mUnprojMatrix, 0, mv, 0);

		if (mv[3] != 0) {
			float w = 1 / mv[3];
			coords[position] = mv[0] * w;
			coords[position + 1] = mv[1] * w;
		} else {
			// else what?
			Log.d(TAG, "... what?");
		}
	}

	private void updateMatrix() {
		Matrix.setRotateM(mRotateMatrix, 0, mRotation, 0, 0, 1);

		// tilt map
		float tilt = mTilt;
		Matrix.setRotateM(mTmpMatrix, 0, tilt / (mHeight / 2), 1, 0, 0);

		// apply first rotation, then tilt
		Matrix.multiplyMM(mRotateMatrix, 0, mTmpMatrix, 0, mRotateMatrix, 0);

		// get unproject matrix:
		// (transpose of rotation is its inverse)
		Matrix.transposeM(mTmpMatrix, 0, mRotateMatrix, 0);
		// (AB)^-1 = B^-1*A^-1
		Matrix.multiplyMM(mUnprojMatrix, 0, mTmpMatrix, 0, mProjMatrixI, 0);

	}

	/**
	 * sets viewBox to visible bounding box, (left,top,right,bottom)
	 * 
	 * @param viewBox
	 *            ...
	 */
	public synchronized void getViewBox(final float[] viewBox) {

		updateMatrix();

		float tilt = FloatMath.sin((float) Math.toRadians(mTilt)) * 4;

		unproject(-1, 1, -tilt, mBBoxCoords, 0); // top-left
		unproject(1, 1, -tilt, mBBoxCoords, 2); // top-right
		unproject(1, -1, tilt, mBBoxCoords, 4); // bottom-right
		unproject(-1, -1, tilt, mBBoxCoords, 6); // bottom-left

		byte z = mZoomLevel;
		double pixelX = MercatorProjection.longitudeToPixelX(mLongitude, z);
		double pixelY = MercatorProjection.latitudeToPixelY(mLatitude, z);

		double dx = pixelX - mBBoxCoords[0] / mScale;
		double dy = pixelY - mBBoxCoords[1] / mScale;
		double lon = MercatorProjection.pixelXToLongitude(dx, z);
		double lat = MercatorProjection.pixelYToLatitude(dy, z);
		Log.d(">>>", "bl:" + lon + " " + lat);

		dx = pixelX - mBBoxCoords[2] / mScale;
		dy = pixelY - mBBoxCoords[3] / mScale;
		lon = MercatorProjection.pixelXToLongitude(dx, z);
		lat = MercatorProjection.pixelYToLatitude(dy, z);
		Log.d("...", "br:" + lon + " " + lat);

		dx = pixelX - mBBoxCoords[4] / mScale;
		dy = pixelY - mBBoxCoords[5] / mScale;
		lon = MercatorProjection.pixelXToLongitude(dx, z);
		lat = MercatorProjection.pixelYToLatitude(dy, z);
		Log.d("...", "tl:" + lon + " " + lat);

		dx = pixelX - mBBoxCoords[6] / mScale;
		dy = pixelY - mBBoxCoords[7] / mScale;
		lon = MercatorProjection.pixelXToLongitude(dx, z);
		lat = MercatorProjection.pixelYToLatitude(dy, z);
		Log.d("...", "tr:" + lon + " " + lat);

	}

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

	// public static double pixelXToLongitude(double pixelX, byte zoomLevel) {
	// return 360 * ((pixelX / ((long) Tile.TILE_SIZE << zoomLevel)) - 0.5);
	// }
	//
	// public static double pixelYToLatitude(double pixelY, byte zoomLevel) {
	// double y = 0.5 - (pixelY / ((long) Tile.TILE_SIZE << zoomLevel));
	// return 90 - 360 * Math.atan(Math.exp(-y * (2 * Math.PI))) / Math.PI;
	// }

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
			double x = dx * Math.cos(rad) + dy * Math.sin(rad);
			double y = dx * -Math.sin(rad) + dy * Math.cos(rad);
			dx = x;
			dy = y;
		}

		dx = pixelX - dx;
		dy = pixelY - dy;

		mLatitude = MercatorProjection.pixelYToLatitude(dy, mZoomLevel);
		mLatitude = MercatorProjection.limitLatitude(mLatitude);

		mLongitude = MercatorProjection.pixelXToLongitude(dx, mZoomLevel);

		mLongitude = MercatorProjection.wrapLongitude(mLongitude);
		// mLongitude = MercatorProjection.limitLongitude(mLongitude);

		// getViewBox(null);
	}

	public synchronized void rotateMap(float angle, float cx, float cy) {
		moveMap(cx, cy);
		// Log.d("MapViewPosition", "rotate:" + angle + " " + (mRotation -
		// angle));
		mRotation += angle;
		updateMatrix();
	}

	public void setRotation(float f) {
		mRotation = f;
		updateMatrix();
	}

	public boolean tilt(float move) {
		float tilt = mTilt + move;
		if (tilt > MAX_ANGLE)
			tilt = MAX_ANGLE;
		else if (tilt < 0)
			tilt = 0;

		if (mTilt == tilt)
			return false;

		mTilt = tilt;
		updateMatrix();
		return true;
	}

	public void setTilt(float f) {
		mTilt = f;
		updateMatrix();
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
	 * @return true if scale was changed
	 */
	public synchronized boolean scaleMap(float scale, float pivotX, float pivotY) {

		float newScale = mMapScale * scale;

		int z = FastMath.log2((int) newScale);

		if (z < MIN_ZOOMLEVEL || (z >= MAX_ZOOMLEVEL && mScale >= 8))
			return false;

		if (z > MAX_ZOOMLEVEL) {
			// z17 shows everything, just increase scaling
			// need to fix this for ScanBox
			if (mScale * scale > 2) // 8)
				return false;

			mScale *= scale;
			mMapScale = newScale;
		} else {
			mZoomLevel = (byte) z;
			mScale = newScale / (1 << z);
			mMapScale = newScale;
		}

		if (pivotY != 0 || pivotY != 0)
			moveMap(pivotX * (1.0f - scale),
					pivotY * (1.0f - scale));

		return true;
	}

}
