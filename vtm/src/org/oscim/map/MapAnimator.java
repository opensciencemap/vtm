/*
 * Copyright 2013 Hannes Janetzek
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
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
package org.oscim.map;

import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Point;
import org.oscim.core.Tile;
import org.oscim.renderer.MapRenderer;
import org.oscim.utils.FastMath;

// TODO: rewrite

public class MapAnimator {

	//static final Logger log = LoggerFactory.getLogger(MapAnimator.class);

	public MapAnimator(Map map, Viewport viewport) {
		mViewport = viewport;
		mMap = map;
	}

	private final int ANIM_NONE = 0;
	private final int ANIM_MOVE = 1 << 0;
	private final int ANIM_SCALE = 1 << 1;
	private final int ANIM_FLING = 1 << 2;
	private final int ANIM_BBOX = 1 << 3;
	private final int ANIM_ROTATE = 1 << 4;
	private final int ANIM_TILT = 1 << 5;

	private final Map mMap;
	private final Viewport mViewport;

	private final MapPosition mPos = new MapPosition();
	private final MapPosition mStartPos = new MapPosition();
	private final MapPosition mDeltaPos = new MapPosition();

	private final Point mScroll = new Point();
	private final Point mPivot = new Point();
	private final Point mVelocity = new Point();

	private double mScaleBy;

	private float mDuration = 500;
	private long mAnimEnd = -1;

	private int mState = ANIM_NONE;

	public synchronized void animateTo(BoundingBox bbox) {
		// TODO for large distatance first scale out, then in

		// calculate the maximum scale at which the bbox is completely visible
		double dx = Math.abs(MercatorProjection.longitudeToX(bbox.getMaxLongitude())
		        - MercatorProjection.longitudeToX(bbox.getMinLongitude()));

		double dy = Math.abs(MercatorProjection.latitudeToY(bbox.getMinLatitude())
		        - MercatorProjection.latitudeToY(bbox.getMaxLatitude()));

		double zx = mMap.getWidth() / (dx * Tile.SIZE);
		double zy = mMap.getHeight() / (dy * Tile.SIZE);
		double newScale = Math.min(zx, zy);

		animateTo(500, bbox.getCenterPoint(), newScale, false);

		mState = ANIM_MOVE | ANIM_SCALE | ANIM_BBOX;
	}

	public synchronized void animateTo(long duration, GeoPoint geoPoint, double scale,
	        boolean relative) {

		mViewport.getMapPosition(mPos);

		if (relative) {
			if (mAnimEnd > 0 && (mState & ANIM_SCALE) != 0)
				scale = mDeltaPos.scale * scale;
			else
				scale = mPos.scale * scale;
		}

		scale = FastMath.clamp(scale, Viewport.MIN_SCALE, Viewport.MAX_SCALE);
		mDeltaPos.scale = scale;

		mScaleBy = scale - mPos.scale;

		mStartPos.scale = mPos.scale;
		mStartPos.angle = mPos.angle;

		mStartPos.x = mPos.x;
		mStartPos.y = mPos.y;

		mDeltaPos.x = MercatorProjection.longitudeToX(geoPoint.getLongitude());
		mDeltaPos.y = MercatorProjection.latitudeToY(geoPoint.getLatitude());

		mDeltaPos.x -= mStartPos.x;
		mDeltaPos.y -= mStartPos.y;

		mState = ANIM_MOVE | ANIM_SCALE;

		animStart(duration);
	}

	public synchronized void animateTo(long duration, MapPosition mapPosition) {

		mViewport.getMapPosition(mPos);

		mapPosition.scale = FastMath.clamp(mapPosition.scale,
		                                   Viewport.MIN_SCALE,
		                                   Viewport.MAX_SCALE);
		mDeltaPos.scale = mapPosition.scale;

		mScaleBy = mapPosition.scale - mPos.scale;

		mStartPos.x = mPos.x;
		mStartPos.y = mPos.y;
		mStartPos.scale = mPos.scale;
		mStartPos.angle = mPos.angle;
		mStartPos.tilt = mPos.tilt;

		mDeltaPos.x = MercatorProjection.longitudeToX(mapPosition.getLongitude()) - mStartPos.x;
		mDeltaPos.y = MercatorProjection.latitudeToY(mapPosition.getLatitude()) - mStartPos.y;

		mDeltaPos.angle = mStartPos.angle - mapPosition.angle;
		while (mDeltaPos.angle > 180)
			mDeltaPos.angle -= 360;
		while (mDeltaPos.angle < -180)
			mDeltaPos.angle += 360;

		mDeltaPos.tilt = mapPosition.tilt - mStartPos.tilt;

		mState = ANIM_MOVE | ANIM_SCALE | ANIM_ROTATE | ANIM_TILT;

		animStart(duration);
	}

	public synchronized void animateZoom(long duration, double scale, float pivotX, float pivotY) {

		mViewport.getMapPosition(mPos);

		if (mAnimEnd > 0 && (mState & ANIM_SCALE) != 0)
			scale = mDeltaPos.scale * scale;
		else
			scale = mPos.scale * scale;

		scale = FastMath.clamp(scale, Viewport.MIN_SCALE, Viewport.MAX_SCALE);
		mDeltaPos.scale = scale;

		mScaleBy = scale - mPos.scale;

		mStartPos.scale = mPos.scale;
		mStartPos.angle = mPos.angle;

		mPivot.x = pivotX;
		mPivot.y = pivotY;

		mState = ANIM_SCALE;

		animStart(duration);
	}

	public synchronized void animateTo(GeoPoint geoPoint) {
		animateTo(300, geoPoint, 1, true);
	}

	public synchronized void animateFling(int velocityX, int velocityY,
	        int minX, int maxX, int minY, int maxY) {

		if (velocityX * velocityX + velocityY * velocityY < 2048)
			return;

		mViewport.getMapPosition(mPos);

		mScroll.x = 0;
		mScroll.y = 0;

		float duration = 500;

		// pi times thumb..
		float flingFactor = (duration / 2500);
		mVelocity.x = velocityX * flingFactor;
		mVelocity.y = velocityY * flingFactor;
		FastMath.clamp(mVelocity.x, minX, maxX);
		FastMath.clamp(mVelocity.y, minY, maxY);

		mState = ANIM_FLING;

		animStart(duration);
	}

	private void animStart(float duration) {
		mDuration = duration;
		mAnimEnd = System.currentTimeMillis() + (long) duration;
		mMap.render();
	}

	private void animCancel() {
		mState = ANIM_NONE;
		mPivot.x = 0;
		mPivot.y = 0;
	}

	/**
	 * called by MapRenderer at begin of each frame.
	 */
	public synchronized void updateAnimation() {
		if (mState == ANIM_NONE)
			return;

		long millisLeft = mAnimEnd - MapRenderer.frametime;

		boolean changed = false;

		synchronized (mViewport) {

			// cancel animation when position was changed since last
			// update, i.e. when it was modified outside the animator.
			if (mViewport.getMapPosition(mPos)) {
				animCancel();
				return;
			}

			if (millisLeft <= 0) {
				// set final position
				if ((mState & ANIM_MOVE) != 0)
					mViewport.moveTo(mStartPos.x + mDeltaPos.x,
					                 mStartPos.y + mDeltaPos.y);

				if ((mState & ANIM_SCALE) != 0) {
					if (mScaleBy > 0)
						doScale(mStartPos.scale + (mScaleBy - 1));
					else
						doScale(mStartPos.scale + mScaleBy);
				}

				mMap.updateMap(true);
				animCancel();
				return;
			}

			float adv = FastMath.clamp(1.0f - millisLeft / mDuration, 0, 1);

			if ((mState & ANIM_SCALE) != 0) {
				if (mScaleBy > 0)
					doScale(mStartPos.scale + (mScaleBy * (Math.pow(2, adv) - 1)));
				else
					doScale(mStartPos.scale + (mScaleBy * adv));

				changed = true;
			}

			if ((mState & ANIM_MOVE) != 0) {
				mViewport.moveTo(mStartPos.x + mDeltaPos.x * adv,
				                 mStartPos.y + mDeltaPos.y * adv);
				changed = true;
			}

			if ((mState & ANIM_BBOX) != 0) {
				if (mPos.angle > 180)
					mPos.angle -= 360;
				mViewport.setRotation(mPos.angle * (1 - adv));
				mViewport.setTilt(mPos.tilt * (1 - adv));
			}

			if ((mState & ANIM_FLING) != 0) {
				adv = (float) Math.sqrt(adv);
				double dx = mVelocity.x * adv;
				double dy = mVelocity.y * adv;
				if ((dx - mScroll.x) != 0 || (dy - mScroll.y) != 0) {

					mViewport.moveMap((float) (dx - mScroll.x),
					                  (float) (dy - mScroll.y));
					mScroll.x = dx;
					mScroll.y = dy;
					changed = true;
				}
			}
			if ((mState & ANIM_ROTATE) != 0) {
				mViewport.setRotation(mStartPos.angle + mDeltaPos.angle * adv);
				changed = true;
			}

			if ((mState & ANIM_TILT) != 0) {
				mViewport.setTilt(mStartPos.tilt + mDeltaPos.tilt * adv);
				changed = true;
			}

			// remember current map position
			mViewport.getMapPosition(mPos);

		}

		// continue animation
		if (changed) {
			// render and inform layers that position has changed
			mMap.updateMap(true);
		} else {
			// just render next frame
			mMap.render();
		}

	}

	private void doScale(double newScale) {
		mViewport.scaleMap((float) (newScale / mPos.scale),
		                   (float) mPivot.x, (float) mPivot.y);
	}

	public synchronized void cancel() {
		mState = ANIM_NONE;
	}
}
