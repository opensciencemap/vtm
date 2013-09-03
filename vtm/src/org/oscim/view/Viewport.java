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

import org.oscim.core.BoundingBox;
import org.oscim.core.Box;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.core.PointD;
import org.oscim.core.PointF;
import org.oscim.core.Tile;
import org.oscim.utils.FastMath;
import org.oscim.utils.Matrix4;

public class Viewport {
	//private static final String TAG = Viewport.class.getName();

	// needs to fit for int: 2 * 20 * Tile.SIZE
	public final static int MAX_ZOOMLEVEL = 20;
	public final static int MIN_ZOOMLEVEL = 2;

	public final static double MAX_SCALE = (1 << MAX_ZOOMLEVEL);
	public final static double MIN_SCALE = (1 << MIN_ZOOMLEVEL);

	private final static float MAX_TILT = 65;

	private final Map mMap;

	private double mAbsScale;
	private double mAbsX;
	private double mAbsY;

	// mAbsScale * Tile.SIZE
	// i.e. size of tile 0/0/0 at current scale in pixel
	private double mCurScale;

	// mAbsX * mCurScale
	private double mCurX;

	// mAbsY * mCurScale
	private double mCurY;

	private float mRotation;
	private float mTilt;

	private final Matrix4 mProjMatrix = new Matrix4();
	private final Matrix4 mProjMatrixI = new Matrix4();
	private final Matrix4 mRotMatrix = new Matrix4();
	private final Matrix4 mViewMatrix = new Matrix4();
	private final Matrix4 mVPMatrix = new Matrix4();
	private final Matrix4 mUnprojMatrix = new Matrix4();
	private final Matrix4 mTmpMatrix = new Matrix4();

	// temporary vars: only use in synchronized functions!
	private final PointD mMovePoint = new PointD();
	private final float[] mv = new float[4];
	private final float[] mu = new float[4];
	private final float[] mViewCoords = new float[8];

	private final Box mMapBBox = new Box();

	private float mHeight, mWidth;

	public final static float VIEW_DISTANCE = 3.0f;
	public final static float VIEW_NEAR = 1;
	public final static float VIEW_FAR = 8;
	// scale map plane at VIEW_DISTANCE to near plane
	public final static float VIEW_SCALE = (VIEW_NEAR / VIEW_DISTANCE) * 0.5f;

	Viewport(Map map) {
		mMap = map;

		mAbsScale = 4;
		mAbsX = 0.5;
		mAbsY = 0.5;

		mRotation = 0;
		mTilt = 0;

		updatePosition();
	}

	private void updatePosition() {
		mCurScale = mAbsScale * Tile.SIZE;
		mCurX = mAbsX * mCurScale;
		mCurY = mAbsY * mCurScale;
	}

	public void setViewport(int width, int height) {
		float s = VIEW_SCALE;
		float aspect = height / (float) width;
		float[] tmp = new float[16];

		Matrix4.frustumM(tmp, 0, -s, s,
				aspect * s, -aspect * s, VIEW_NEAR, VIEW_FAR);

		mProjMatrix.set(tmp);
		mTmpMatrix.setTranslation(0, 0, -VIEW_DISTANCE);
		mProjMatrix.multiplyRhs(mTmpMatrix);
		mProjMatrix.get(tmp);

		Matrix4.invertM(tmp, 0, tmp, 0);
		mProjMatrixI.set(tmp);

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
		//z = FastMath.clamp(z, MIN_ZOOMLEVEL, MAX_ZOOMLEVEL);
		//float scale = (float) (mAbsScale / (1 << z));

		boolean changed = (pos.zoomLevel != z
				|| pos.x != mAbsX
				|| pos.y != mAbsY
				|| pos.scale != mAbsScale
				|| pos.angle != mRotation
				|| pos.tilt != mTilt);

		pos.angle = mRotation;
		pos.tilt = mTilt;

		pos.x = mAbsX;
		pos.y = mAbsY;
		pos.scale = mAbsScale;

		// for tiling
		pos.zoomLevel = z;

		return changed;
	}

	/**
	 * Get a copy of current matrices
	 *
	 * @param view view Matrix
	 * @param proj projection Matrix
	 * @param vp view and projection
	 */
	public synchronized void getMatrix(Matrix4 view, Matrix4 proj, Matrix4 vp) {
		if (view != null)
			view.copy(mViewMatrix);

		if (proj != null)
			proj.copy(mProjMatrix);

		if (vp != null)
			vp.copy(mVPMatrix);
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
		if (y == 0)
			return 0;

		// origin is moved by VIEW_DISTANCE
		double cx = VIEW_DISTANCE;
		// 'height' of the ray
		double ry = y * (mHeight / mWidth) * 0.5f;

		double ua;

		if (y == 0)
			ua = 1;
		else {
			// tilt of the plane (center is kept on x = 0)
			double t = Math.toRadians(mTilt);
			double px = y * Math.sin(t);
			double py = y * Math.cos(t);
			ua = 1 + (px * ry) / (py * cx);
		}

		mv[0] = 0;
		mv[1] = (float) (ry / ua);
		mv[2] = (float) (cx - cx / ua);

		mProjMatrix.prj(mv);

		return mv[2];
	}

	private void unproject(float x, float y, float z, float[] coords, int position) {
		mv[0] = x;
		mv[1] = y;
		mv[2] = z;

		mUnprojMatrix.prj(mv);

		coords[position + 0] = mv[0];
		coords[position + 1] = mv[1];
	}

	/** @return the current center point of the MapView. */
	public synchronized GeoPoint getMapCenter() {
		return new GeoPoint(MercatorProjection.toLatitude(mAbsY),
				MercatorProjection.toLongitude(mAbsX));
	}

	/**
	 * Get the minimal axis-aligned BoundingBox that encloses
	 * the visible part of the map.
	 *
	 * @return BoundingBox containing view
	 */
	public synchronized BoundingBox getViewBox() {
		getViewBox(mMapBBox);

		// scale map-pixel coordinates at current scale to
		// absolute coordinates and apply mercator projection.
		double minLon = MercatorProjection.toLongitude(mMapBBox.minX);
		double maxLon = MercatorProjection.toLongitude(mMapBBox.maxX);
		// sic(k)
		double minLat = MercatorProjection.toLatitude(mMapBBox.maxY);
		double maxLat = MercatorProjection.toLatitude(mMapBBox.minY);

		return new BoundingBox(minLat, minLon, maxLat, maxLon);
	}

	/**
	 * Get the minimal axis-aligned BoundingBox that encloses
	 * the visible part of the map. Sets box to map coordinates:
	 * minX,minY,maxY,maxY
	 */
	public synchronized void getViewBox(Box box) {
		float[] coords = mViewCoords;
		getMapViewProjection(coords);

		box.minX = coords[0];
		box.maxX = coords[0];
		box.minY = coords[1];
		box.maxY = coords[1];

		for (int i = 2; i < 8; i += 2) {
			box.minX = Math.min(box.minX, coords[i]);
			box.maxX = Math.max(box.maxX, coords[i]);
			box.minY = Math.min(box.minY, coords[i + 1]);
			box.maxY = Math.max(box.maxY, coords[i + 1]);
		}

		box.minX = (mCurX + box.minX) / mCurScale;
		box.maxX = (mCurX + box.maxX) / mCurScale;
		box.minY = (mCurY + box.minY) / mCurScale;
		box.maxY = (mCurY + box.maxY) / mCurScale;
	}

	/**
	 * For x, y in screen coordinates set Point to map-tile
	 * coordinates at returned scale.
	 *
	 * @param x screen coordinate
	 * @param y screen coordinate
	 * @param out Point coords will be set
	 */
	public synchronized void getScreenPointOnMap(float x, float y, double scale, PointD out) {

		// scale to -1..1
		float mx = 1 - (x / mWidth * 2);
		float my = 1 - (y / mHeight * 2);

		unproject(-mx, my, getZ(-my), mu, 0);

		out.x = mu[0];
		out.y = mu[1];

		if (scale != 0) {
			out.x *= scale / mAbsScale;
			out.y *= scale / mAbsScale;
		}
	}

	/**
	 * Get the GeoPoint for x,y in screen coordinates.
	 *
	 * @deprecated
	 * @param x screen coordinate
	 * @param y screen coordinate
	 * @return the corresponding GeoPoint
	 */
	public synchronized GeoPoint fromScreenPixels(float x, float y) {
		fromScreenPixels(x, y, mMovePoint);
		return new GeoPoint(
				MercatorProjection.toLatitude(mMovePoint.y),
				MercatorProjection.toLongitude(mMovePoint.x));
	}

	/**
	 * Get the map position for x,y in screen coordinates.
	 *
	 * @param x screen coordinate
	 * @param y screen coordinate
	 */
	public synchronized void fromScreenPixels(double x, double y, PointD out) {
		// scale to -1..1
		float mx = (float) (1 - (x / mWidth * 2));
		float my = (float) (1 - (y / mHeight * 2));

		unproject(-mx, my, getZ(-my), mu, 0);

		double dx = mCurX + mu[0];
		double dy = mCurY + mu[1];

		dx /= mCurScale;
		dy /= mCurScale;

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

		out.x = dx;
		out.y = dy;
	}

	/**
	 * Get the screen pixel for a GeoPoint
	 *
	 * @deprecated
	 * @param geoPoint the GeoPoint
	 * @param out Point projected to screen pixel relative to center
	 */
	public synchronized void project(GeoPoint geoPoint, PointD out) {
		MercatorProjection.project(geoPoint, out);
		project(out.x, out.y, out);
	}

	/**
	 * Get the screen pixel for map position
	 *
	 * @param out Point projected to screen pixel
	 */
	public synchronized void project(double x, double y, PointD out) {

		mv[0] = (float) (x * mCurScale - mCurX);
		mv[1] = (float) (y * mCurScale - mCurY);

		mv[2] = 0;
		mv[3] = 1;

		mVPMatrix.prj(mv);

		out.x = (mv[0] * (mWidth / 2));
		out.y = -(mv[1] * (mHeight / 2));
	}

	private void updateMatrix() {
		// --- view matrix
		// 1. scale to window coordinates
		// 2. rotate
		// 3. tilt

		// --- projection matrix
		// 4. translate to VIEW_DISTANCE
		// 5. apply projection

		while (mRotation > 360)
			mRotation -= 360;
		while (mRotation < 0)
			mRotation += 360;

		mRotMatrix.setRotation(mRotation, 0, 0, 1);

		// tilt map
		mTmpMatrix.setRotation(mTilt, 1, 0, 0);

		// apply first rotation, then tilt
		mRotMatrix.multiplyMM(mTmpMatrix, mRotMatrix);

		// scale to window coordinates
		mTmpMatrix.setScale(1 / mWidth, 1 / mWidth, 1 / mWidth);

		mViewMatrix.multiplyMM(mRotMatrix, mTmpMatrix);

		mVPMatrix.multiplyMM(mProjMatrix, mViewMatrix);

		//--- unproject matrix:

		// inverse scale
		mUnprojMatrix.setScale(mWidth, mWidth, 1);

		// inverse rotation and tilt
		mTmpMatrix.transposeM(mRotMatrix);

		// (AB)^-1 = B^-1*A^-1, unapply scale, tilt and rotation
		mTmpMatrix.multiplyMM(mUnprojMatrix, mTmpMatrix);

		// (AB)^-1 = B^-1*A^-1, unapply projection
		mUnprojMatrix.multiplyMM(mTmpMatrix, mProjMatrixI);
	}

	/**
	 * Moves this Viewport by the given amount of pixels.
	 *
	 * @param mx the amount of pixels to move the map horizontally.
	 * @param my the amount of pixels to move the map vertically.
	 */
	public synchronized void moveMap(float mx, float my) {
		// stop animation
		animCancel();

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

		updatePosition();
	}

	private PointD applyRotation(float mx, float my) {
		double rad = Math.toRadians(mRotation);
		double rcos = Math.cos(rad);
		double rsin = Math.sin(rad);
		mMovePoint.x = mx * rcos + my * rsin;
		mMovePoint.y = mx * -rsin + my * rcos;
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
		animCancel();

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
		return setTilt(mTilt + move);
	}

	public synchronized boolean setTilt(float tilt) {
		tilt = FastMath.clamp(tilt, 0, MAX_TILT);
		if (tilt == mTilt)
			return false;
		mTilt = tilt;
		updateMatrix();
		return true;
	}

	public synchronized float getTilt() {
		return mTilt;
	}

	private void setMapCenter(double latitude, double longitude) {
		latitude = MercatorProjection.limitLatitude(latitude);
		longitude = MercatorProjection.limitLongitude(longitude);
		mAbsX = MercatorProjection.longitudeToX(longitude);
		mAbsY = MercatorProjection.latitudeToY(latitude);
	}

	public synchronized void setMapPosition(MapPosition mapPosition) {
		mAbsScale = FastMath.clamp(mapPosition.scale, MIN_SCALE, MAX_SCALE);
		mAbsX = mapPosition.x;
		mAbsY = mapPosition.y;
		mTilt = mapPosition.tilt;
		mRotation = mapPosition.angle;
		updatePosition();
		updateMatrix();
	}

	synchronized void setMapCenter(GeoPoint geoPoint) {
		setMapCenter(geoPoint.getLatitude(), geoPoint.getLongitude());
		updatePosition();
	}

	/************************************************************************/

	// TODO move to MapAnimator
	class AnimState {

	}

	private double mScrollX;
	private double mScrollY;
	private double mStartX;
	private double mStartY;
	private double mEndX;
	private double mEndY;

	private double mStartScale;
	private double mEndScale;
	private float mStartRotation;

	private float mDuration = 500;
	private long mAnimEnd = -1;

	private boolean mAnimMove;
	private boolean mAnimFling;
	private boolean mAnimScale;
	private boolean mAnimPivot;
	private GeoPoint mEndPos;
	private double mFinalScale;

	public synchronized void animateTo(BoundingBox bbox) {
		// calculate the maximum scale at which the bbox is completely visible
		double dx = Math.abs(MercatorProjection.longitudeToX(bbox.getMaxLongitude())
				- MercatorProjection.longitudeToX(bbox.getMinLongitude()));

		double dy = Math.abs(MercatorProjection.latitudeToY(bbox.getMinLatitude())
				- MercatorProjection.latitudeToY(bbox.getMaxLatitude()));

		double zx = mWidth / (dx * Tile.SIZE);
		double zy = mHeight / (dy * Tile.SIZE);
		double newScale = Math.min(zx, zy);

		//Log.d(TAG, "scale to " + bbox + " " + newScale + " " + mAbsScale
		//		+ " " + FastMath.log2((int) newScale));

		animateTo(500, bbox.getCenterPoint(), newScale, false);
	}

	public synchronized void animateTo(long duration, GeoPoint geoPoint, double scale,
			boolean relative) {

		if (relative) {
			if (mAnimEnd > 0 && mAnimScale)
				scale = mFinalScale * scale;
			else
				scale = mAbsScale * scale;
		}

		scale = FastMath.clamp(scale, MIN_SCALE, MAX_SCALE);
		mFinalScale = scale;

		scale = (float) (scale / mAbsScale);

		mEndScale = mAbsScale * scale - mAbsScale;
		mStartScale = mAbsScale;
		mStartRotation = mRotation;

		mStartX = mAbsX;
		mStartY = mAbsY;

		mEndX = MercatorProjection.longitudeToX(geoPoint.getLongitude());
		mEndY = MercatorProjection.latitudeToY(geoPoint.getLatitude());
		mEndX -= mStartX;
		mEndY -= mStartY;
		mAnimMove = true;
		mAnimScale = true;
		mAnimFling = false;

		mEndPos = geoPoint;

		animStart(duration);
	}

	public synchronized void animateZoom(long duration, double scale, double pivotX, double pivotY) {

		if (mAnimEnd > 0 && mAnimScale)
			scale = mFinalScale * scale;
		else
			scale = mAbsScale * scale;

		scale = FastMath.clamp(scale, MIN_SCALE, MAX_SCALE);
		mFinalScale = scale;

		scale = (float) (scale / mAbsScale);

		mEndScale = mAbsScale * scale - mAbsScale;
		mStartScale = mAbsScale;
		mStartRotation = mRotation;

		mScrollX = pivotX;
		mScrollY = pivotY;

		mAnimScale = true;
		mAnimPivot = (pivotX != 0 || pivotY != 0);

		mAnimFling = false;
		mAnimMove = false;

		animStart(duration);
	}

	public synchronized void animateTo(GeoPoint geoPoint) {
		animateTo(300, geoPoint, 1, true);
	}

	private void animStart(float duration) {
		mDuration = duration;

		mAnimEnd = System.currentTimeMillis() + (long) duration;
		mMap.render();
	}

	private void animCancel() {
		mAnimEnd = -1;
		mEndPos = null;
		mAnimScale = false;
		mAnimFling = false;
		mAnimMove = false;
		mAnimPivot = false;
	}

	synchronized boolean fling(float adv) {

		adv = (float) Math.sqrt(adv);

		float dx = mVelocityX * adv;
		float dy = mVelocityY * adv;

		if (dx != 0 || dy != 0) {
			PointD p = applyRotation((float) (dx - mScrollX), (float) (dy - mScrollY));
			move(p.x, p.y);

			mScrollX = dx;
			mScrollY = dy;
		}
		return true;
	}

	private float mVelocityX;
	private float mVelocityY;

	public synchronized void animateFling(int velocityX, int velocityY,
			int minX, int maxX, int minY, int maxY) {

		if (velocityX * velocityX + velocityY * velocityY < 2048)
			return;

		mScrollX = 0;
		mScrollY = 0;

		float duration = 500;

		// pi times thumb..
		float flingFactor = (duration / 2500);
		mVelocityX = velocityX * flingFactor;
		mVelocityY = velocityY * flingFactor;
		FastMath.clamp(mVelocityX, minX, maxX);
		FastMath.clamp(mVelocityY, minY, maxY);

		mAnimFling = true;
		mAnimMove = false;
		mAnimScale = false;
		animStart(duration);
	}

	/**
	 * called by GLRenderer at begin of each frame.
	 */
	public void updateAnimation() {
		if (mAnimEnd < 0)
			return;

		long millisLeft = mAnimEnd - System.currentTimeMillis();

		if (millisLeft <= 0) {
			// set final position
			if (mAnimMove) {
				if (mEndPos == null)
					doMove(mStartX + mEndX, mStartY + mEndY);
				else {
					setMapCenter(mEndPos);
					mEndPos = null;
				}
			}

			if (mAnimScale) {
				doScale(mFinalScale);
			}

			updatePosition();
			mMap.updateMap(true);

			animCancel();
			return;
		}

		boolean changed = false;

		float adv = (1.0f - millisLeft / mDuration);

		if (mAnimScale) {
			if (mEndScale > 0)
				doScale(mStartScale + (mEndScale * (Math.pow(2, adv) - 1)));
			else
				doScale(mStartScale + (mEndScale * adv));

			changed = true;
		}

		if (mAnimMove) {
			doMove(mStartX + mEndX * adv, mStartY + mEndY * adv);
			changed = true;
		}

		if (mAnimMove && mAnimScale) {
			mRotation = mStartRotation * (1 - adv);
			updateMatrix();
		}

		if (changed) {
			updatePosition();
		}

		if (mAnimFling && fling(adv))
			changed = true;

		// continue animation
		if (changed) {
			// inform other layers that position has changed
			mMap.updateMap(true);
		} else {
			// just render next frame
			mMap.render();
		}
	}

	private void doScale(double newScale) {
		double scale = mAbsScale;

		mAbsScale = newScale;

		if (mAnimPivot) {
			scale = mAbsScale / scale;

			PointD p = applyRotation(
					(float) (mScrollX * (1.0 - scale)),
					(float) (mScrollY * (1.0 - scale)));
			move(p.x, p.y);
		}
	}

	private void doMove(double x, double y) {
		mAbsX = x;
		mAbsY = y;

		// clamp latitude
		mAbsY = FastMath.clamp(mAbsY, 0, 1);

		// wrap longitude
		while (mAbsX > 1)
			mAbsX -= 1;
		while (mAbsX < 0)
			mAbsX += 1;

		updatePosition();
	}
}
