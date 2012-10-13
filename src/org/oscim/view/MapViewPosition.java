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
import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.utils.FastMath;

import android.opengl.Matrix;
import android.util.FloatMath;
import android.util.Log;

/**
 * A MapPosition stores the latitude and longitude coordinate of a MapView
 * together with its zoom level.
 */

// TODO use global coordinates that directly scale to pixel

public class MapViewPosition {

	private static final String TAG = "MapViewPosition";

	public final static int MAX_ZOOMLEVEL = 17;
	public final static int MIN_ZOOMLEVEL = 2;

	private final static float MAX_ANGLE = 35;

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
	private float[] mViewMatrix = new float[16];
	private float[] mRotMatrix = new float[16];
	private float[] mTmpMatrix = new float[16];

	private int mHeight, mWidth;
	public final static float VIEW_SCALE = 1f / 2;
	public final static float VIEW_DISTANCE = 2;
	public final static float VIEW_NEAR = VIEW_DISTANCE;
	public final static float VIEW_FAR = VIEW_DISTANCE * 2;

	public final static float DIST = 2;

	// public final static float VIEW_SCALE = 1f / 2;
	// public final static float VIEW_DISTANCE = 1;
	// public final static float VIEW_NEAR = 1;
	// public final static float VIEW_FAR = 2;

	void setViewport(int width, int height) {
		float sw = VIEW_SCALE;
		float sh = VIEW_SCALE;
		float aspect = height / (float) width;

		Matrix.frustumM(mProjMatrix, 0, -1 * sw, 1 * sw,
				aspect * sh, -aspect * sh, VIEW_NEAR, VIEW_FAR);

		Matrix.setIdentityM(mTmpMatrix, 0);
		Matrix.translateM(mTmpMatrix, 0, 0, 0, -VIEW_DISTANCE);
		Matrix.multiplyMM(mProjMatrix, 0, mProjMatrix, 0, mTmpMatrix, 0);

		Matrix.invertM(mProjMatrixI, 0, mProjMatrix, 0);

		mHeight = height;
		mWidth = width;

		updateMatrix();
	}

	public synchronized boolean getMapPosition(final MapPosition mapPosition,
			final float[] coords) {
		// if (!isValid())
		// return false;

		if (mapPosition.lat == mLatitude
				&& mapPosition.lon == mLongitude
				&& mapPosition.zoomLevel == mZoomLevel
				&& mapPosition.scale == mScale
				&& mapPosition.angle == mRotation
				&& mapPosition.tilt == mTilt)
			return false;

		byte z = mZoomLevel;

		mapPosition.lat = mLatitude;
		mapPosition.lon = mLongitude;
		mapPosition.angle = mRotation;
		mapPosition.tilt = mTilt;
		mapPosition.scale = mScale;
		mapPosition.zoomLevel = z;

		mapPosition.x = MercatorProjection.longitudeToPixelX(mLongitude, z);
		mapPosition.y = MercatorProjection.latitudeToPixelY(mLatitude, z);

		if (mapPosition.viewMatrix != null)
			System.arraycopy(mViewMatrix, 0, mapPosition.viewMatrix, 0, 16);

		if (mapPosition.rotateMatrix != null)
			System.arraycopy(mRotMatrix, 0, mapPosition.rotateMatrix, 0, 16);

		if (coords == null)
			return true;

		// not so sure about this, but somehow works. weird z-values...
		float tilt = FloatMath.sin((float) Math.toRadians(mTilt
				// * 2.2f for dist = 1
				* 1.4f // for dist = 2
				// * 0.8f for dist = 4
				* ((float) mHeight / mWidth)));

		float d = 1f;
		unproject(-d, d, tilt, coords, 0); // bottom-left
		unproject(d, d, tilt, coords, 2); // bottom-right
		unproject(d, -d, -tilt, coords, 4); // top-right
		unproject(-d, -d, -tilt, coords, 6); // top-left

		return true;
	}

	private float[] mv = { 0, 0, 0, 1 };
	// private float[] mu = { 0, 0, 0, 1 };
	private float[] mBBoxCoords = new float[8];

	private void unproject(float x, float y, float z, float[] coords, int position) {
		// mv[0] = x;
		// mv[1] = y;
		// mv[2] = z - 2f;
		// // mv[2] = 1f / (z - 2f);
		// mv[3] = 1;
		// Matrix.multiplyMV(mu, 0, mProjMatrix, 0, mv, 0);

		mv[0] = x;
		mv[1] = y;
		mv[2] = z - 1f;
		// mv[2] = -mu[2] / mu[3];
		mv[3] = 1;

		Matrix.multiplyMV(mv, 0, mUnprojMatrix, 0, mv, 0);

		if (mv[3] != 0) {
			coords[position] = mv[0] / mv[3];
			coords[position + 1] = mv[1] / mv[3];
			// Log.d(TAG, (z * 1.4f - 1) + " " + mu[2] / mu[3] + " - " + x + ":"
			// + y + "  -  "
			// + coords[position] + ":" + coords[position + 1] + " - " + mTilt);
		} else {
			// else what?
			Log.d(TAG, "... what?");
		}
	}

	private void updateMatrix() {
		Matrix.setRotateM(mRotMatrix, 0, mRotation, 0, 0, 1);
		// - view matrix
		// 1. scale to window coordinates
		// 2. rotate
		// 3. tilt

		// - projection matrix
		// 4. translate to near-plane
		// 5. apply projection

		// tilt map
		float tilt = mTilt;
		Matrix.setRotateM(mTmpMatrix, 0, tilt, 1, 0, 0);

		// apply first viewMatrix, then tilt
		Matrix.multiplyMM(mRotMatrix, 0, mTmpMatrix, 0, mRotMatrix, 0);

		// scale to window coordinates
		Matrix.setIdentityM(mTmpMatrix, 0);
		Matrix.scaleM(mTmpMatrix, 0, 1f / mWidth, 1f / mWidth, 1);

		Matrix.multiplyMM(mViewMatrix, 0, mRotMatrix, 0, mTmpMatrix, 0);

		// // move to near plane
		// Matrix.setIdentityM(mTmpMatrix, 0);
		// Matrix.translateM(mTmpMatrix, 0, 0, 0, -VIEW_DISTANCE);
		// Matrix.multiplyMM(mViewMatrix, 0, mTmpMatrix, 0, mViewMatrix, 0);

		// get unproject matrix:
		// Matrix.invertM(mTmpMatrix, 0, mViewMatrix, 0);
		Matrix.setIdentityM(mUnprojMatrix, 0);

		// inverse scale
		Matrix.scaleM(mUnprojMatrix, 0, mWidth, mWidth, 1);

		// inverse rotation
		Matrix.transposeM(mTmpMatrix, 0, mRotMatrix, 0);

		// (AB)^-1 = B^-1*A^-1
		Matrix.multiplyMM(mTmpMatrix, 0, mUnprojMatrix, 0, mTmpMatrix, 0);

		// unapply projection, tilt, rotate and scale
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

		// updateMatrix();

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

		setTilt(tilt);
		// mTilt = tilt;
		// updateMatrix();
		return true;
	}

	public void setTilt(float f) {
		mTilt = f;

		// float sw = VIEW_SCALE;
		// float sh = VIEW_SCALE;
		// sh += (mTilt / 250);

		// Matrix.frustumM(mProjMatrix, 0, -sw * mWidth, sw * mWidth,
		// sh * mHeight, -sh * mHeight, 1, 2);
		//
		// Matrix.translateM(mProjMatrix, 0, 0, 0, -VIEW_DISTANCE);
		//
		// Matrix.invertM(mProjMatrixI, 0, mProjMatrix, 0);
		// Matrix.invertM(mUnprojMatrix, 0, mProjMatrix, 0);

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

		if (pivotX != 0 || pivotY != 0)
			moveMap(pivotX * (1.0f - scale),
					pivotY * (1.0f - scale));

		return true;
	}

}
