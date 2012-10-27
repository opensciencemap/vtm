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
package org.oscim.view;

import java.lang.ref.WeakReference;

import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.utils.FastMath;

import android.graphics.Point;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.FloatMath;
import android.util.Log;

/**
 * A MapPosition stores the latitude and longitude coordinate of a MapView
 * together with its zoom level, rotation and tilt
 */

public class MapViewPosition {

	private static final String TAG = MapViewPosition.class.getSimpleName();

	public final static int MAX_ZOOMLEVEL = 17;
	public final static int MIN_ZOOMLEVEL = 2;

	private final static float MAX_ANGLE = 40;

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

	//	private static final int REF_ZOOM = 20;
	private double mPosX;
	private double mPosY;

	private AnimationHandler mHandler;

	MapViewPosition(MapView mapView) {
		mMapView = mapView;
		mLatitude = Double.NaN;
		mLongitude = Double.NaN;
		mZoomLevel = -1;
		mScale = 1;
		mRotation = 0.0f;
		mTilt = 0;
		mMapScale = 1;

		mHandler = new AnimationHandler(this);
	}

	private float[] mProjMatrix = new float[16];
	private float[] mProjMatrixI = new float[16];
	private float[] mUnprojMatrix = new float[16];
	private float[] mViewMatrix = new float[16];
	private float[] mRotMatrix = new float[16];
	private float[] mTmpMatrix = new float[16];

	private static int mHeight, mWidth;
	public final static float VIEW_SCALE = 1f / 2;
	public final static float VIEW_DISTANCE = 2;
	public final static float VIEW_NEAR = VIEW_DISTANCE;
	public final static float VIEW_FAR = VIEW_DISTANCE * 2;

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

		mapPosition.x = mPosX;
		mapPosition.y = mPosY;

		if (mapPosition.viewMatrix != null)
			System.arraycopy(mViewMatrix, 0, mapPosition.viewMatrix, 0, 16);

		if (coords == null)
			return true;

		float tilt = getZ(1);

		unproject(-1, 1, tilt, coords, 0); // bottom-left
		unproject(1, 1, tilt, coords, 2); // bottom-right
		unproject(1, -1, -tilt, coords, 4); // top-right
		unproject(-1, -1, -tilt, coords, 6); // top-left

		return true;
	}

	/** @return the current center point of the MapView. */
	public synchronized GeoPoint getMapCenter() {
		return new GeoPoint(mLatitude, mLongitude);
	}

	/**
	 * @return a MapPosition or null, if this map position is not valid.
	 * @see #isValid()
	 */
	public synchronized MapPosition getMapPosition() {
		if (!isValid()) {
			return null;
		}

		return new MapPosition(mLatitude, mLongitude, mZoomLevel, mScale, mRotation);
	}

	/** @return the current zoom level of the MapView. */
	public synchronized byte getZoomLevel() {
		return mZoomLevel;
	}

	/** @return the current scale of the MapView. */
	public synchronized float getScale() {
		return mScale;
	}

	/**
	 * ...
	 * @return BoundingBox containing view
	 */
	public synchronized BoundingBox getViewBox() {

		float[] coords = mBBoxCoords;

		float tilt = getZ(1);
		unproject(-1, 1, -tilt, coords, 0); // top-left
		unproject(1, 1, -tilt, coords, 2); 	// top-right
		unproject(1, -1, tilt, coords, 4); 	// bottom-right
		unproject(-1, -1, tilt, coords, 6); // bottom-left

		byte z = mZoomLevel;
		double dx, dy;
		double minLat = 0, minLon = 0, maxLat = 0, maxLon = 0, lon, lat;

		for (int i = 0; i < 8; i += 2) {

			dx = mPosX - coords[i + 0] / mScale;
			dy = mPosY - coords[i + 1] / mScale;

			lon = MercatorProjection.pixelXToLongitude(dx, z);
			lat = MercatorProjection.pixelYToLatitude(dy, z);
			if (i == 0) {
				minLon = maxLon = lon;
				minLat = maxLat = lat;
			} else {
				if (lat > maxLat)
					maxLat = lat;
				else if (lat < minLat)
					minLat = lat;

				if (lon > maxLon)
					maxLon = lon;
				else if (lon < minLon)
					minLon = lon;
			}
		}

		return new BoundingBox(minLat, minLon, maxLat, maxLon);
	}

	private float[] mv = { 0, 0, 0, 1 };
	private float[] mu = { 0, 0, 0, 1 };
	private float[] mBBoxCoords = new float[8];

	/* get the depth-value of the map for the current tilt, approximately.
	 * needed to un-project a point on screen to the position on the map. not
	 * so sure about this, but at least somehow works. */
	private float getZ(float y) {
		return FloatMath.sin((float) Math.toRadians(mTilt))
				// * 2.2f // for dist = 1
				* 1.3f // for dist = 2
				// * 0.8f // for dist = 4
				* ((float) mHeight / mWidth) * y;
	}

	/**
	 * for x,y in screen coordinates get the point on the map in map-tile
	 * coordinates
	 * @param x ...
	 * @param y ...
	 * @param reuse ...
	 * @return ...
	 */
	public synchronized Point getScreenPointOnMap(float x, float y, Point reuse) {
		Point out = reuse == null ? new Point() : reuse;

		float mx = ((mWidth / 2) - x) / (mWidth / 2);
		float my = ((mHeight / 2) - y) / (mHeight / 2);

		unproject(-mx, my, getZ(my), mu, 0);

		out.x = (int) (mPosX + mu[0] / mScale);
		out.y = (int) (mPosY + mu[1] / mScale);

		return out;
	}

	/**
	 * get the GeoPoint for x,y in screen coordinates
	 * @param x screen pixel x
	 * @param y screen pixel y
	 * @return the corresponding GeoPoint
	 */
	public synchronized GeoPoint fromScreenPixels(float x, float y) {
		float mx = ((mWidth / 2) - x) / (mWidth / 2);
		float my = ((mHeight / 2) - y) / (mHeight / 2);

		unproject(-mx, my, getZ(my), mu, 0);

		double dx = mPosX + mu[0] / mScale;
		double dy = mPosY + mu[1] / mScale;

		GeoPoint p = new GeoPoint(
				MercatorProjection.pixelYToLatitude(dy, mZoomLevel),
				MercatorProjection.pixelXToLongitude(dx, mZoomLevel));

		//	Log.d(">>>", "fromScreenPixels " + p);

		return p;
	}

	/**
	 * get the screen pixel for a GeoPoint
	 * @param geoPoint ...
	 * @param reuse ...
	 * @return ...
	 */
	public synchronized Point project(GeoPoint geoPoint, Point reuse) {
		Point out = reuse == null ? new Point() : reuse;

		double x = MercatorProjection.longitudeToPixelX(geoPoint.getLongitude(),
				mZoomLevel);
		double y = MercatorProjection.latitudeToPixelY(geoPoint.getLatitude(),
				mZoomLevel);

		mv[0] = (float) (x - mPosX) * mScale;
		mv[1] = (float) (y - mPosY) * mScale;
		mv[2] = 0;
		mv[3] = 1;

		Matrix.multiplyMV(mv, 0, mViewMatrix, 0, mv, 0);
		Matrix.multiplyMV(mv, 0, mProjMatrix, 0, mv, 0);

		out.x = (int) (mv[0] / mv[3] * mWidth / 2);
		out.y = (int) (mv[1] / mv[3] * mHeight / 2);

		//	Log.d(">>>", "project: " + out.x + " " + out.y);

		return out;
	}

	public synchronized void getMVP(float[] matrix) {
		Matrix.multiplyMM(matrix, 0, mProjMatrix, 0, mViewMatrix, 0);
	}

	//	public static Point project(float x, float y, float[] matrix, float[] tmpVec, Point reuse) {
	//		Point out = reuse == null ? new Point() : reuse;
	//
	//		tmpVec[0] = x;
	//		tmpVec[1] = y;
	//		tmpVec[2] = 0;
	//		tmpVec[3] = 1;
	//
	//		Matrix.multiplyMV(tmpVec, 0, matrix, 0, tmpVec, 0);
	//
	//		out.x = (int) (tmpVec[0] / tmpVec[3] * mWidth / 2);
	//		out.y = (int) (tmpVec[1] / tmpVec[3] * mHeight / 2);
	//
	//		return out;
	//	}

	private void unproject(float x, float y, float z, float[] coords, int position) {
		mv[0] = x;
		mv[1] = y;
		mv[2] = z - 1f;
		mv[3] = 1;

		Matrix.multiplyMV(mv, 0, mUnprojMatrix, 0, mv, 0);

		if (mv[3] != 0) {
			coords[position] = mv[0] / mv[3];
			coords[position + 1] = mv[1] / mv[3];
		} else {
			// else what?
			Log.d(TAG, "uproject failed");
		}
	}

	private void updateMatrix() {
		// - view matrix
		// 1. scale to window coordinates
		// 2. rotate
		// 3. tilt

		// - projection matrix
		// 4. translate to near-plane
		// 5. apply projection

		Matrix.setRotateM(mRotMatrix, 0, mRotation, 0, 0, 1);

		// tilt map
		float tilt = mTilt;
		Matrix.setRotateM(mTmpMatrix, 0, tilt, 1, 0, 0);

		// apply first rotation, then tilt
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

	/** @return true if this MapViewPosition is valid, false otherwise. */
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
	 * @param mx the amount of pixels to move the map horizontally.
	 * @param my the amount of pixels to move the map vertically.
	 */
	public synchronized void moveMap(float mx, float my) {

		double dx = mx / mScale;
		double dy = my / mScale;

		if (mMapView.enableRotation || mMapView.enableCompass) {
			double rad = Math.toRadians(mRotation);
			double rcos = Math.cos(rad);
			double rsin = Math.sin(rad);
			double x = dx * rcos + dy * rsin;
			double y = dx * -rsin + dy * rcos;
			dx = x;
			dy = y;
		}

		dx = mPosX - dx;
		dy = mPosY - dy;

		mLatitude = MercatorProjection.pixelYToLatitude(dy, mZoomLevel);
		mLatitude = MercatorProjection.limitLatitude(mLatitude);

		mLongitude = MercatorProjection.pixelXToLongitude(dx, mZoomLevel);
		mLongitude = MercatorProjection.wrapLongitude(mLongitude);

		updatePosition();
	}

	/**
	 * -
	 * @param scale ...
	 * @param pivotX ...
	 * @param pivotY ...
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
			if (mScale * scale > 4)
				return false;

			mScale *= scale;
			mMapScale = newScale;
		} else {
			mZoomLevel = (byte) z;
			updatePosition();

			mScale = newScale / (1 << z);
			mMapScale = newScale;
		}

		if (pivotX != 0 || pivotY != 0)
			moveMap(pivotX * (1.0f - scale),
					pivotY * (1.0f - scale));

		return true;
	}

	/**
	 * rotate map around pivot cx,cy
	 * @param angle ...
	 * @param cx ...
	 * @param cy ...
	 */
	public synchronized void rotateMap(float angle, float cx, float cy) {
		moveMap(cx, cy);
		mRotation += angle;

		updateMatrix();
	}

	public synchronized void setRotation(float f) {
		mRotation = f;
		updateMatrix();
	}

	public synchronized boolean tilt(float move) {
		float tilt = mTilt + move;
		if (tilt > MAX_ANGLE)
			tilt = MAX_ANGLE;
		else if (tilt < 0)
			tilt = 0;

		if (mTilt == tilt)
			return false;

		setTilt(tilt);
		return true;
	}

	public synchronized void setTilt(float f) {
		mTilt = f;
		updateMatrix();
	}

	private void setMapCenter(double latitude, double longitude) {
		mLatitude = MercatorProjection.limitLatitude(latitude);
		mLongitude = MercatorProjection.limitLongitude(longitude);
		updatePosition();
	}

	synchronized void setMapCenter(GeoPoint geoPoint) {
		setMapCenter(geoPoint.getLatitude(), geoPoint.getLongitude());
	}

	synchronized void setMapCenter(MapPosition mapPosition) {
		mZoomLevel = mMapView.limitZoomLevel(mapPosition.zoomLevel);
		mMapScale = 1 << mZoomLevel;
		setMapCenter(mapPosition.lat, mapPosition.lon);
	}

	synchronized void setZoomLevel(byte zoomLevel) {
		mZoomLevel = mMapView.limitZoomLevel(zoomLevel);
		mMapScale = 1 << mZoomLevel;
		updatePosition();
	}

	synchronized void setScale(float scale) {
		mScale = scale;
	}

	private void updatePosition() {
		mPosX = MercatorProjection.longitudeToPixelX(mLongitude, mZoomLevel);
		mPosY = MercatorProjection.latitudeToPixelY(mLatitude, mZoomLevel);
	}

	private double mStartX;
	private double mStartY;
	private double mEndX;
	private double mEndY;
	private float mDuration = 500;
	private Point mTmpPoint;

	public synchronized void animateTo(GeoPoint geoPoint) {
		MercatorProjection.projectPoint(geoPoint, mZoomLevel, mTmpPoint);

		mEndX = MercatorProjection.longitudeToPixelX(geoPoint.getLongitude(), mZoomLevel);
		mEndY = MercatorProjection.latitudeToPixelY(geoPoint.getLatitude(), mZoomLevel);
		mStartX = mPosX;
		mStartY = mPosY;

		mDuration = 300;
		mHandler.start((int) mDuration);
	}

	synchronized void setMapPosition(double x, double y) {

		mLatitude = MercatorProjection.pixelYToLatitude(y, mZoomLevel);
		mLatitude = MercatorProjection.limitLatitude(mLatitude);

		mLongitude = MercatorProjection.pixelXToLongitude(x, mZoomLevel);
		mLongitude = MercatorProjection.wrapLongitude(mLongitude);

		updatePosition();
	}

	void onTick(long millisLeft) {
		double adv = millisLeft / mDuration;
		double mx = (mStartX + (mEndX - mStartX) * (1.0 - adv));
		double my = (mStartY + (mEndY - mStartY) * (1.0 - adv));
		setMapPosition(mx, my);
		mMapView.redrawMap();
	}

	void onFinish() {
		setMapPosition(mEndX, mEndY);
		mMapView.redrawMap();
	}

	static class AnimationHandler extends Handler {
		private final WeakReference<MapViewPosition> mMapViewPosition;
		private static final int MSG = 1;

		long mMillisInFuture;

		long mInterval = 16;

		long mStopTimeInFuture;

		AnimationHandler(MapViewPosition mapAnimator) {
			mMapViewPosition = new WeakReference<MapViewPosition>(mapAnimator);
		}

		public synchronized final void start(int millis) {
			mMillisInFuture = millis;
			MapViewPosition animator = mMapViewPosition.get();
			if (animator == null)
				return;

			if (mMillisInFuture <= 0) {
				animator.onFinish();
				return;
			}

			mStopTimeInFuture = SystemClock.elapsedRealtime() + mMillisInFuture;
			removeMessages(MSG);
			sendMessage(obtainMessage(MSG));
		}

		public final void cancel() {
			removeMessages(MSG);
		}

		@Override
		public void handleMessage(Message msg) {
			MapViewPosition animator = mMapViewPosition.get();
			if (animator == null)
				return;

			final long millisLeft = mStopTimeInFuture
					- SystemClock.elapsedRealtime();

			if (millisLeft <= 0) {
				animator.onFinish();
			} else if (millisLeft < mInterval) {
				// no tick, just delay until done
				sendMessageDelayed(obtainMessage(MSG), millisLeft);
			} else {
				long lastTickStart = SystemClock.elapsedRealtime();
				animator.onTick(millisLeft);

				// take into account user's onTick taking time to
				// execute
				long delay = lastTickStart + mInterval
						- SystemClock.elapsedRealtime();

				// special case: user's onTick took more than interval
				// to
				// complete, skip to next interval
				while (delay < 0)
					delay += mInterval;

				sendMessageDelayed(obtainMessage(MSG), delay);
			}
		}
	}
}
