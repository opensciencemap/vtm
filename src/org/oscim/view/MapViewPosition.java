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
import org.oscim.core.PointD;
import org.oscim.core.PointF;
import org.oscim.core.Tile;
import org.oscim.utils.FastMath;
import org.oscim.utils.GlUtils;

import android.opengl.Matrix;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Scroller;

/**
 * A MapPosition stores the latitude and longitude coordinate of a MapView
 * together with its zoom level, rotation and tilt
 */

public class MapViewPosition {
	private static final String TAG = MapViewPosition.class.getName();

	public final static int MAX_ZOOMLEVEL = 17;
	public final static int MIN_ZOOMLEVEL = 2;
	public final static int MAX_END_SCALE = 8;
	public final static double MAX_SCALE = ((1 << MAX_ZOOMLEVEL) * MAX_END_SCALE);
	public final static double MIN_SCALE = (1 << MIN_ZOOMLEVEL);

	// needs to fit for int: 2 * 20 * Tile.TILE_SIZE
	public final static int ABS_ZOOMLEVEL = 20;

	private final static float MAX_ANGLE = 65;

	private final MapView mMapView;

	private double mLatitude;
	private double mLongitude;

	private double mAbsScale;
	private double mAbsX;
	private double mAbsY;

	// mAbsScale * Tile.TILE_SIZE
	// i.e. size of tile 0/0/0 at current scale in pixel
	private double mCurScale;

	// mAbsX * mCurScale
	private double mCurX;

	// mAbsY * mCurScale
	private double mCurY;

	private float mRotation;
	public float mTilt;

	private final AnimationHandler mHandler;
	private final Scroller mScroller;

	MapViewPosition(MapView mapView) {
		mMapView = mapView;
		mLatitude = Double.NaN;
		mLongitude = Double.NaN;
		mRotation = 0.0f;
		mTilt = 0;
		mAbsScale = 1;

		mHandler = new AnimationHandler(this);
		mScroller = new Scroller(mapView.getContext(), new LinearInterpolator() {
			@Override
			public float getInterpolation(float input) {
				return (float) Math.sqrt(input);

			}
		});
	}

	private final float[] mProjMatrix = new float[16];
	private final float[] mProjMatrixI = new float[16];
	private final float[] mUnprojMatrix = new float[16];
	private final float[] mViewMatrix = new float[16];
	private final float[] mVPMatrix = new float[16];
	private final float[] mRotMatrix = new float[16];
	private final float[] mTmpMatrix = new float[16];

	// temporary vars: only use in synchronized functions!
	private final PointD mMovePoint = new PointD();
	private final float[] mv = new float[4];
	private final float[] mu = new float[4];
	private final float[] mBBoxCoords = new float[8];

	private float mHeight, mWidth;

	public final static float VIEW_DISTANCE = 3.0f;
	public final static float VIEW_NEAR = 1;
	public final static float VIEW_FAR = 8;
	// scale map plane at VIEW_DISTANCE to near plane
	public final static float VIEW_SCALE = (VIEW_NEAR / VIEW_DISTANCE) * 0.5f;

	void setViewport(int width, int height) {
		float s = VIEW_SCALE;
		float aspect = height / (float) width;

		Matrix.frustumM(mProjMatrix, 0, -s, s,
				aspect * s, -aspect * s, VIEW_NEAR, VIEW_FAR);

		GlUtils.setTranslation(mTmpMatrix, 0, 0, -VIEW_DISTANCE);
		Matrix.multiplyMM(mProjMatrix, 0, mProjMatrix, 0, mTmpMatrix, 0);

		Matrix.invertM(mProjMatrixI, 0, mProjMatrix, 0);

		mHeight = height;
		mWidth = width;

		updateMatrix();
	}

	/**
	 * Get the current MapPosition
	 *
	 * @param pos MapPosition object to be updated
	 * @return true if current position is different from 'pos'.
	 */
	public synchronized boolean getMapPosition(MapPosition pos) {

		int z = FastMath.log2((int) mAbsScale);
		z = FastMath.clamp(z, MIN_ZOOMLEVEL, MAX_ZOOMLEVEL);
		float scale = (float) (mAbsScale / (1 << z));

		if (pos.lat == mLatitude
				&& pos.lon == mLongitude
				&& pos.zoomLevel == z
				&& pos.scale == scale
				&& pos.angle == mRotation
				&& pos.tilt == mTilt)
			return false;

		pos.lat = mLatitude;
		pos.lon = mLongitude;
		pos.angle = mRotation;
		pos.tilt = mTilt;

		// for tiling
		pos.scale = scale;
		pos.zoomLevel = (byte) z;

		pos.x = mAbsX * (Tile.TILE_SIZE << z);
		pos.y = mAbsY * (Tile.TILE_SIZE << z);

		return true;
	}

	/**
	 * Get a copy of current matrices
	 *
	 * @param view view Matrix
	 * @param proj projection Matrix
	 * @param vp view and projection
	 */
	public synchronized void getMatrix(float[] view, float[] proj, float[] vp) {
		if (view != null)
			System.arraycopy(mViewMatrix, 0, view, 0, 16);

		if (proj != null)
			System.arraycopy(mProjMatrix, 0, proj, 0, 16);

		if (vp != null)
			System.arraycopy(mVPMatrix, 0, vp, 0, 16);
	}

	/**
	 * Get the inverse projection of the viewport, i.e. the
	 * coordinates with z==0 that will be projected exactly
	 * to screen corners by current view-projection-matrix.
	 *
	 * @param box float[8] will be set.
	 */
	public synchronized void getMapViewProjection(float[] box) {
		float t = getZ(1);
		float t2 = getZ(-1);

		// top-right
		unproject(1, -1, t, box, 0);
		// top-left
		unproject(-1, -1, t, box, 2);
		// bottom-left
		unproject(-1, 1, t2, box, 4);
		// bottom-right
		unproject(1, 1, t2, box, 6);
	}

	/*
	 * Get Z-value of the map-plane for a point on screen -
	 * calculate the intersection of a ray from camera origin
	 * and the map plane
	 */
	private float getZ(float y) {
		// origin is moved by VIEW_DISTANCE
		double cx = VIEW_DISTANCE;
		// 'height' of the ray
		double ry = y * (mHeight / mWidth) * 0.5f;

		// tilt of the plane (center is kept on x = 0)
		double t = Math.toRadians(mTilt);
		double px = y * Math.sin(t);
		double py = y * Math.cos(t);

		double ua = 1 + (px * ry) / (py * cx);

		mv[0] = 0;
		mv[1] = (float) (ry / ua);
		mv[2] = (float) (cx - cx / ua);
		mv[3] = 1;

		Matrix.multiplyMV(mv, 0, mProjMatrix, 0, mv, 0);

		return mv[2] / mv[3];
	}

	private void unproject(float x, float y, float z, float[] coords, int position) {
		mv[0] = x;
		mv[1] = y;
		mv[2] = z;
		mv[3] = 1;

		Matrix.multiplyMV(mv, 0, mUnprojMatrix, 0, mv, 0);

		coords[position + 0] = mv[0] / mv[3];
		coords[position + 1] = mv[1] / mv[3];
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

		int z = FastMath.log2((int) mAbsScale);
		z = FastMath.clamp(z, MIN_ZOOMLEVEL, MAX_ZOOMLEVEL);
		float scale = (float) (mAbsScale / (1 << z));

		return new MapPosition(mLatitude, mLongitude, (byte) z, scale, mRotation);
	}

	/**
	 * ...
	 *
	 * @return BoundingBox containing view
	 */
	public synchronized BoundingBox getViewBox() {

		float[] coords = mBBoxCoords;

		float t = getZ(1);
		float t2 = getZ(-1);

		unproject(1, -1, t, coords, 0);
		unproject(-1, -1, t, coords, 2);
		unproject(-1, 1, t2, coords, 4);
		unproject(1, 1, t2, coords, 6);

		double dx, dy;
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;

		double maxX = Double.MIN_VALUE;
		double maxY = Double.MIN_VALUE;

		for (int i = 0; i < 8; i += 2) {
			dx = mCurX - coords[i + 0];
			dy = mCurY - coords[i + 1];

			minX = Math.min(minX, dx);
			maxX = Math.max(maxX, dx);
			minY = Math.min(minY, dy);
			maxY = Math.max(maxY, dy);
		}

		minX = MercatorProjection.toLongitude(minX, mCurScale);
		maxX = MercatorProjection.toLongitude(maxX, mCurScale);
		minY = MercatorProjection.toLatitude(minY, mCurScale);
		maxY = MercatorProjection.toLatitude(maxY, mCurScale);

		// yea, this is upside down..
		BoundingBox bbox = new BoundingBox(maxY, minX, minY, maxX);

		Log.d(TAG, "getScreenBoundingBox " + bbox);

		return bbox;
	}

	/**
	 * For x, y in screen coordinates set Point to map-tile
	 * coordinates at returned scale.
	 *
	 * @param x screen coordinate
	 * @param y screen coordinate
	 * @param out Point coords will be set
	 * @return current map scale
	 */
	public synchronized float getScreenPointOnMap(float x, float y, PointD out) {

		// scale to -1..1
		float mx = 1 - (x / mWidth * 2);
		float my = 1 - (y / mHeight * 2);

		unproject(-mx, my, getZ(-my), mu, 0);

		out.x = mCurX + mu[0];
		out.y = mCurY + mu[1];

		return (float) mAbsScale;
	}

	/**
	 * Get the GeoPoint for x,y in screen coordinates.
	 * (only used by MapEventsOverlay currently)
	 *
	 * @param x screen coordinate
	 * @param y screen coordinate
	 * @return the corresponding GeoPoint
	 */
	public synchronized GeoPoint fromScreenPixels(float x, float y) {
		// scale to -1..1
		float mx = 1 - (x / mWidth * 2);
		float my = 1 - (y / mHeight * 2);

		unproject(-mx, my, getZ(-my), mu, 0);

		double dx = mCurX + mu[0];
		double dy = mCurY + mu[1];
		dx /= mAbsScale * Tile.TILE_SIZE;
		dy /= mAbsScale * Tile.TILE_SIZE;

		if (dx > 1) {
			while (dx > 1)
				dx -= 1;
		} else {
			while (dx < 0)
				dx += 1;
		}

		if (dy > 1)
			dy = 1;
		else if (dy < 0)
			dy = 0;

		GeoPoint p = new GeoPoint(
				MercatorProjection.toLatitude(dy),
				MercatorProjection.toLongitude(dx));

		return p;
	}

	/**
	 * Get the screen pixel for a GeoPoint
	 *
	 * @param geoPoint the GeoPoint
	 * @param out Point projected to screen pixel
	 */
	public synchronized void project(GeoPoint geoPoint, PointF out) {

		double x = MercatorProjection.longitudeToX(geoPoint.getLongitude()) * mCurScale;
		double y = MercatorProjection.latitudeToY(geoPoint.getLatitude()) * mCurScale;

		mv[0] = (float) (x - mCurX);
		mv[1] = (float) (y - mCurY);

		mv[2] = 0;
		mv[3] = 1;

		Matrix.multiplyMV(mv, 0, mVPMatrix, 0, mv, 0);

		// positive direction is down and right;
		out.x = (int) ((mv[0] / mv[3]) * (mWidth / 2));
		out.y = (int) -((mv[1] / mv[3]) * (mHeight / 2));
	}

	private void updateMatrix() {
		// --- view matrix
		// 1. scale to window coordinates
		// 2. rotate
		// 3. tilt

		// --- projection matrix
		// 4. translate to VIEW_DISTANCE
		// 5. apply projection

		Matrix.setRotateM(mRotMatrix, 0, mRotation, 0, 0, 1);

		// tilt map
		float tilt = mTilt;
		Matrix.setRotateM(mTmpMatrix, 0, tilt, 1, 0, 0);

		// apply first rotation, then tilt
		Matrix.multiplyMM(mRotMatrix, 0, mTmpMatrix, 0, mRotMatrix, 0);

		// scale to window coordinates
		GlUtils.setScaleM(mTmpMatrix, 1 / mWidth, 1 / mWidth, 1);

		Matrix.multiplyMM(mViewMatrix, 0, mRotMatrix, 0, mTmpMatrix, 0);

		Matrix.multiplyMM(mVPMatrix, 0, mProjMatrix, 0, mViewMatrix, 0);

		//--- unproject matrix:
		// Matrix.multiplyMM(mTmpMatrix, 0, mProjMatrix, 0, mViewMatrix, 0);
		// Matrix.invertM(mUnprojMatrix, 0, mTmpMatrix, 0);

		// inverse scale
		GlUtils.setScaleM(mUnprojMatrix, mWidth, mWidth, 1);

		// inverse rotation and tilt
		Matrix.transposeM(mTmpMatrix, 0, mRotMatrix, 0);

		// (AB)^-1 = B^-1*A^-1, unapply scale, tilt and rotation
		Matrix.multiplyMM(mTmpMatrix, 0, mUnprojMatrix, 0, mTmpMatrix, 0);

		// (AB)^-1 = B^-1*A^-1, unapply projection
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
	 *
	 * @param mx the amount of pixels to move the map horizontally.
	 * @param my the amount of pixels to move the map vertically.
	 */
	public synchronized void moveMap(float mx, float my) {
		// stop animation
		mHandler.cancel();

		PointD p = applyRotation(mx, my);

		move(p.x, p.y);
	}

	private synchronized void move(double mx, double my) {
		mAbsX = (mCurX - mx) / mCurScale;
		mAbsY = (mCurY - my) / mCurScale;

		// clamp latitude
		mAbsY = FastMath.clamp(mAbsY, 0, 1);

		// wrap longitude
		while (mAbsX > 1)
			mAbsX -= 1;
		while (mAbsX < 0)
			mAbsX += 1;

		mLongitude = MercatorProjection.toLongitude(mAbsX);
		mLatitude = MercatorProjection.toLatitude(mAbsY);

		updatePosition();
	}

	private synchronized void moveAbs(double x, double y) {
		double f = Tile.TILE_SIZE << ABS_ZOOMLEVEL;

		mAbsX = x / f;
		mAbsY = y / f;

		// clamp latitude
		mAbsY = FastMath.clamp(mAbsY, 0, 1);

		// wrap longitude
		while (mAbsX > 1)
			mAbsX -= 1;
		while (mAbsX < 0)
			mAbsX += 1;

		mLongitude = MercatorProjection.toLongitude(mAbsX);
		mLatitude = MercatorProjection.toLatitude(mAbsY);

		updatePosition();
	}

	private PointD applyRotation(float mx, float my) {

		if (mMapView.mRotationEnabled || mMapView.mCompassEnabled) {
			double rad = Math.toRadians(mRotation);
			double rcos = Math.cos(rad);
			double rsin = Math.sin(rad);
			float x = (float) (mx * rcos + my * rsin);
			float y = (float) (mx * -rsin + my * rcos);
			mx = x;
			my = y;
		}

		mMovePoint.x = mx;
		mMovePoint.y = my;
		return mMovePoint;
	}

	/**
	 * @param scale map by this factor
	 * @param pivotX ...
	 * @param pivotY ...
	 * @return true if scale was changed
	 */
	public synchronized boolean scaleMap(float scale, float pivotX, float pivotY) {
		// stop animation
		mHandler.cancel();

		// just sanitize input
		scale = FastMath.clamp(scale, 0.5f, 2);

		double newScale = mAbsScale * scale;

		newScale = FastMath.clamp(newScale, MIN_SCALE, MAX_SCALE);

		if (newScale == mAbsScale)
			return false;

		scale = (float) (newScale / mAbsScale);

		mAbsScale = newScale;

		if (pivotX != 0 || pivotY != 0)
			moveMap(pivotX * (1.0f - scale),
					pivotY * (1.0f - scale));
		else
			updatePosition();

		return true;
	}

	/**
	 * rotate map around pivot cx,cy
	 *
	 * @param radians ...
	 * @param cx ...
	 * @param cy ...
	 */
	public synchronized void rotateMap(double radians, float cx, float cy) {

		double rsin = Math.sin(radians);
		double rcos = Math.cos(radians);

		float x = (float) (cx * rcos + cy * -rsin - cx);
		float y = (float) (cx * rsin + cy * rcos - cy);

		moveMap(x, y);

		mRotation += Math.toDegrees(radians);

		updateMatrix();
	}

	public synchronized void setRotation(float f) {
		mRotation = f;
		updateMatrix();
	}

	public synchronized boolean tiltMap(float move) {
		float tilt = FastMath.clamp(mTilt + move, 0, MAX_ANGLE);

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
		mAbsX = MercatorProjection.longitudeToX(mLongitude);
		mAbsY = MercatorProjection.latitudeToY(mLatitude);
		updatePosition();
	}

	synchronized void setMapCenter(GeoPoint geoPoint) {
		setMapCenter(geoPoint.getLatitude(), geoPoint.getLongitude());
	}

	synchronized void setMapCenter(MapPosition mapPosition) {
		setZoomLevelLimit(mapPosition.zoomLevel);
		setMapCenter(mapPosition.lat, mapPosition.lon);
	}

	synchronized void setZoomLevel(byte zoomLevel) {
		setZoomLevelLimit(zoomLevel);
		updatePosition();
	}

	private void setZoomLevelLimit(byte zoomLevel) {
		mAbsScale = FastMath.clamp(1 << zoomLevel, MIN_SCALE, MAX_SCALE);
	}

	private void updatePosition() {
		mCurScale = mAbsScale * Tile.TILE_SIZE;

		mCurX = mAbsX * mCurScale;
		mCurY = mAbsY * mCurScale;
	}

	/************************************************************************/
	// TODO move to MapAnimator:

	private double mScrollX;
	private double mScrollY;
	private double mStartX;
	private double mStartY;
	private double mEndX;
	private double mEndY;

	private double mStartScale;
	private double mEndScale;

	private float mDuration = 500;
	private final static double LOG4 = Math.log(4);

	private boolean mAnimMove;
	private boolean mAnimFling;
	private boolean mAnimScale;
	private final AccelerateDecelerateInterpolator mDecInterpolator = new AccelerateDecelerateInterpolator();

	public synchronized void animateTo(BoundingBox bbox) {

		// calculate the minimum scale at which the bbox is completely visible
		double dx = Math.abs(MercatorProjection.longitudeToX(bbox.getMaxLongitude())
				- MercatorProjection.longitudeToX(bbox.getMinLongitude()));

		double dy = Math.abs(MercatorProjection.latitudeToY(bbox.getMinLatitude())
				- MercatorProjection.latitudeToY(bbox.getMaxLatitude()));

		double aspect = (Math.min(mWidth, mHeight) / Tile.TILE_SIZE);
		double z = Math.min(
				-LOG4 * Math.log(dx) + aspect,
				-LOG4 * Math.log(dy) + aspect);

		double newScale = Math.pow(2, z);

		newScale = FastMath.clamp(newScale, MIN_SCALE, 1 << ABS_ZOOMLEVEL);

		float scale = (float) (newScale / mAbsScale);

		Log.d(TAG, "scale to " + bbox + " " + z + " " + newScale + " " + mAbsScale
				+ " " + FastMath.log2((int) newScale) + " " + scale);

		mEndScale = mAbsScale * scale - mAbsScale;
		//mEndScale = scale - 1;
		mStartScale = mAbsScale;

		// reset rotation/tilt
		//mTilt = 0;
		//mRotation = 0;
		//updateMatrix();
		double f = Tile.TILE_SIZE << ABS_ZOOMLEVEL;
		mStartX = mAbsX * f;
		mStartY = mAbsY * f;

		GeoPoint geoPoint = bbox.getCenterPoint();

		mEndX = MercatorProjection.longitudeToX(geoPoint.getLongitude()) * f;
		mEndY = MercatorProjection.latitudeToY(geoPoint.getLatitude()) * f;
		mEndX -= mStartX;
		mEndY -= mStartY;
		mAnimMove = true;
		mAnimScale = true;
		mAnimFling = false;
		mDuration = 500;

		mHandler.start((int) mDuration);
	}

	public synchronized void animateTo(GeoPoint geoPoint) {
		double f = Tile.TILE_SIZE << ABS_ZOOMLEVEL;

		mStartX = mAbsX * f;
		mStartY = mAbsY * f;

		mEndX = MercatorProjection.longitudeToX(geoPoint.getLongitude()) * f;
		mEndY = MercatorProjection.latitudeToY(geoPoint.getLatitude()) * f;

		mEndX -= mStartX;
		mEndY -= mStartY;

		mAnimMove = true;
		mAnimScale = false;
		mAnimFling = false;

		mDuration = 300;
		mHandler.start(mDuration);
	}

	public synchronized void animateFling(int velocityX, int velocityY,
			int minX, int maxX, int minY, int maxY) {

		mScrollX = 0;
		mScrollY = 0;

		mScroller.fling(0, 0, velocityX, velocityY, minX, maxX, minY, maxY);
		mAnimFling = true;
		mAnimMove = false;
		mAnimScale = false;

		//mMapView.mGLView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

		mDuration = 250;
		mHandler.start(mDuration);
	}

	public synchronized void animateZoom(float scale) {
		mStartScale = mAbsScale;
		mEndScale = mAbsScale * scale - mAbsScale;

		//mMapView.mGLView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

		mDuration = 300;
		mHandler.start(mDuration);
	}

	public void updateAnimation() {
		//scroll();
	}

	synchronized boolean scroll() {
		if (mScroller.isFinished()) {
			return false;
		}
		mScroller.computeScrollOffset();

		double dx = mScroller.getCurrX();
		double dy = mScroller.getCurrY();

		int mx = (int) (dx - mScrollX);
		int my = (int) (dy - mScrollY);

		if (mx >= 1 || my >= 1 || mx <= -1 || my <= -1) {
			moveMap(mx, my);
			mScrollX = dx;
			mScrollY = dy;
		}
		return true;
	}

	void onTick(long millisLeft) {
		boolean changed = false;

		float adv = (1.0f - millisLeft / mDuration);
		adv = mDecInterpolator.getInterpolation(adv);

		if (mAnimScale) {
			if (mEndScale > 0)
				//	double s = (1 + adv * adv * mEndScale);
				//	mAbsScale = mStartScale * s;
				//	Log.d(TAG, "scale: " + s + " " + mAbsScale + " " + mStartScale);
				//}
				mAbsScale = mStartScale + (mEndScale * (Math.pow(2, adv) - 1));
			else
				mAbsScale = mStartScale + (mEndScale * adv);

			changed = true;
		}

		if (mAnimMove) {
			moveAbs(mStartX + mEndX * adv, mStartY + mEndY * adv);
			changed = true;
		}

		if (changed) {
			updatePosition();
		}

		if (mAnimFling && scroll())
			changed = true;

		if (changed)
			mMapView.redrawMap(true);
	}

	void onFinish() {

		if (mAnimMove) {
			moveAbs(mStartX + mEndX, mStartY + mEndY);
		}

		if (mAnimScale) {
			mAbsScale = mStartScale + mEndScale;
		}

		updatePosition();

		//mMapView.mGLView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

		mMapView.redrawMap(true);
	}

	/**
	 * below is borrowed from CountDownTimer class:
	 * Copyright (C) 2008 The Android Open Source Project
	 */
	static class AnimationHandler extends Handler {
		private final WeakReference<MapViewPosition> mMapViewPosition;
		private static final int MSG = 1;

		long mMillisInFuture;

		long mInterval = 16;

		long mStopTimeInFuture;

		AnimationHandler(MapViewPosition mapAnimator) {
			mMapViewPosition = new WeakReference<MapViewPosition>(mapAnimator);
		}

		public synchronized final void start(float millis) {
			mMillisInFuture = (int) millis;
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
