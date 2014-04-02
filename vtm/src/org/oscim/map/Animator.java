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

import static org.oscim.core.MercatorProjection.latitudeToY;
import static org.oscim.core.MercatorProjection.longitudeToX;
import static org.oscim.utils.FastMath.clamp;

import org.oscim.backend.CanvasAdapter;
import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.core.Point;
import org.oscim.core.Tile;
import org.oscim.renderer.MapRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Animator {
	static final Logger log = LoggerFactory.getLogger(Animator.class);

	//static final Logger log = LoggerFactory.getLogger(MapAnimator.class);

	private final static int ANIM_NONE = 0;
	private final static int ANIM_MOVE = 1 << 0;
	private final static int ANIM_SCALE = 1 << 1;
	private final static int ANIM_ROTATE = 1 << 2;
	private final static int ANIM_TILT = 1 << 3;
	private final static int ANIM_FLING = 1 << 4;

	private final Map mMap;

	private final MapPosition mCurPos = new MapPosition();
	private final MapPosition mStartPos = new MapPosition();
	private final MapPosition mDeltaPos = new MapPosition();

	private final Point mScroll = new Point();
	private final Point mPivot = new Point();
	private final Point mVelocity = new Point();

	private float mDuration = 500;
	private long mAnimEnd = -1;

	private int mState = ANIM_NONE;

	public Animator(Map map) {
		mMap = map;
	}

	public synchronized void animateTo(long duration, BoundingBox bbox) {
		mMap.getMapPosition(mStartPos);
		/* TODO for large distance first scale out, then in
		 * calculate the maximum scale at which the BoundingBox
		 * is completely visible */
		double dx = Math.abs(longitudeToX(bbox.getMaxLongitude())
		        - longitudeToX(bbox.getMinLongitude()));

		double dy = Math.abs(latitudeToY(bbox.getMinLatitude())
		        - latitudeToY(bbox.getMaxLatitude()));

		log.debug("anim bbox " + bbox);

		double zx = mMap.getWidth() / (dx * Tile.SIZE);
		double zy = mMap.getHeight() / (dy * Tile.SIZE);
		double newScale = Math.min(zx, zy);

		GeoPoint p = bbox.getCenterPoint();

		mDeltaPos.set(longitudeToX(p.getLongitude()) - mStartPos.x,
		              latitudeToY(p.getLatitude()) - mStartPos.y,
		              newScale - mStartPos.scale,
		              -mStartPos.bearing,
		              -mStartPos.tilt);

		animStart(duration, ANIM_MOVE | ANIM_SCALE | ANIM_ROTATE | ANIM_TILT);
	}

	public synchronized void animateTo(BoundingBox bbox) {
		animateTo(1000, bbox);
	}

	public synchronized void animateTo(long duration, GeoPoint geoPoint,
	        double scale, boolean relative) {
		mMap.getMapPosition(mStartPos);

		if (relative)
			scale = mStartPos.scale * scale;

		scale = clamp(scale, Viewport.MIN_SCALE, Viewport.MAX_SCALE);

		mDeltaPos.set(longitudeToX(geoPoint.getLongitude()) - mStartPos.x,
		              latitudeToY(geoPoint.getLatitude()) - mStartPos.y,
		              scale - mStartPos.scale,
		              0, 0);

		animStart(duration, ANIM_MOVE | ANIM_SCALE);
	}

	public synchronized void animateTo(GeoPoint p) {
		animateTo(500, p, 1, true);
	}

	public synchronized void animateTo(long duration, MapPosition pos) {
		mMap.getMapPosition(mStartPos);

		pos.scale = clamp(pos.scale,
		                  Viewport.MIN_SCALE,
		                  Viewport.MAX_SCALE);

		mDeltaPos.set(pos.x - mStartPos.x,
		              pos.y - mStartPos.y,
		              pos.scale - mStartPos.scale,
		              pos.bearing - mStartPos.bearing,
		              clamp(pos.tilt, 0, Viewport.MAX_TILT) - mStartPos.tilt);

		animStart(duration, ANIM_MOVE | ANIM_SCALE | ANIM_ROTATE | ANIM_TILT);
	}

	public synchronized void animateZoom(long duration, double scaleBy,
	        float pivotX, float pivotY) {
		mMap.getMapPosition(mCurPos);

		if (mState == ANIM_SCALE)
			scaleBy = (mStartPos.scale + mDeltaPos.scale) * scaleBy;
		else
			scaleBy = mCurPos.scale * scaleBy;

		mStartPos.copy(mCurPos);
		scaleBy = clamp(scaleBy, Viewport.MIN_SCALE, Viewport.MAX_SCALE);

		mDeltaPos.scale = scaleBy - mStartPos.scale;

		mPivot.x = pivotX;
		mPivot.y = pivotY;

		animStart(duration, ANIM_SCALE);
	}

	public synchronized void animateFling(float velocityX, float velocityY,
	        int minX, int maxX, int minY, int maxY) {

		if (velocityX * velocityX + velocityY * velocityY < 2048)
			return;

		mMap.getMapPosition(mStartPos);

		mScroll.x = 0;
		mScroll.y = 0;

		float duration = 500;

		float flingFactor = 240 / CanvasAdapter.dpi;
		mVelocity.x = velocityX * flingFactor;
		mVelocity.y = velocityY * flingFactor;
		mVelocity.x = clamp(mVelocity.x, minX, maxX);
		mVelocity.y = clamp(mVelocity.y, minY, maxY);
		if (Double.isNaN(mVelocity.x) || Double.isNaN(mVelocity.y)) {
			log.debug("fling NaN!");
			return;
		}

		animStart(duration, ANIM_FLING);
	}

	private void animStart(float duration, int state) {
		mState = state;
		mCurPos.copy(mStartPos);
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

		ViewController v = mMap.viewport();

		synchronized (v) {
			/* cancel animation when position was changed since last
			 * update, i.e. when it was modified outside the animator. */
			if (v.getMapPosition(mCurPos)) {
				animCancel();
				return;
			}

			float adv = clamp(1.0f - millisLeft / mDuration, 0, 1);

			double scaleAdv = 1;
			if ((mState & ANIM_SCALE) != 0) {
				scaleAdv = doScale(v, adv);
			}

			if ((mState & ANIM_MOVE) != 0) {
				v.moveTo(mStartPos.x + mDeltaPos.x * (adv / scaleAdv),
				         mStartPos.y + mDeltaPos.y * (adv / scaleAdv));
			}

			if ((mState & ANIM_FLING) != 0) {
				adv = (float) Math.sqrt(adv);
				double dx = mVelocity.x * adv;
				double dy = mVelocity.y * adv;
				if ((dx - mScroll.x) != 0 || (dy - mScroll.y) != 0) {
					v.moveMap((float) (dx - mScroll.x),
					          (float) (dy - mScroll.y));
					mScroll.x = dx;
					mScroll.y = dy;
				}
			}
			if ((mState & ANIM_ROTATE) != 0) {
				v.setRotation(mStartPos.bearing + mDeltaPos.bearing * adv);
			}

			if ((mState & ANIM_TILT) != 0) {
				v.setTilt(mStartPos.tilt + mDeltaPos.tilt * adv);
			}

			if (millisLeft <= 0)
				animCancel();

			/* remember current map position */
			changed = v.getMapPosition(mCurPos);
		}

		if (changed) {
			/* render and inform layers that position has changed */
			mMap.updateMap(true);
		} else {
			/* just render next frame */
			mMap.render();
		}
	}

	private double doScale(ViewController v, float adv) {
		double newScale = mStartPos.scale + mDeltaPos.scale * Math.sqrt(adv);

		v.scaleMap((float) (newScale / mCurPos.scale),
		           (float) mPivot.x, (float) mPivot.y);

		return newScale / (mStartPos.scale + mDeltaPos.scale);
	}

	public synchronized void cancel() {
		mState = ANIM_NONE;
	}
}
