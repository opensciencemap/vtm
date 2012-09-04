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
package org.mapsforge.android.input;

import org.mapsforge.android.MapView;
import org.mapsforge.android.MapViewPosition;

import android.os.CountDownTimer;
import android.os.SystemClock;
import android.view.ScaleGestureDetector;
import android.view.animation.DecelerateInterpolator;

class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener {
	private final MapView mMapView;
	private MapViewPosition mMapPosition;
	private float mCenterX;
	private float mCenterY;
	private float mFocusX;
	private float mFocusY;
	private float mScale;
	private boolean mBeginScale;

	/**
	 * Creates a new ScaleListener for the given MapView.
	 * 
	 * @param mapView
	 *            the MapView which should be scaled.
	 */
	ScaleListener(MapView mapView) {
		mMapView = mapView;
	}

	@Override
	public boolean onScale(ScaleGestureDetector gd) {

		mScale = gd.getScaleFactor();
		mFocusX = gd.getFocusX() - mCenterX;
		mFocusY = gd.getFocusY() - mCenterY;

		mSumScale *= mScale;

		mTimeEnd = SystemClock.elapsedRealtime();

		if (!mBeginScale) {
			if (mTimeEnd - mTimeStart > 100) {
				mBeginScale = true;
				mScale = mSumScale;
			}
			else
				return true;
		}

		mMapPosition.scaleMap(mScale, mFocusX, mFocusY);
		mMapView.redrawTiles();

		return true;
	}

	private long mTimeStart;
	private long mTimeEnd;
	private float mSumScale;

	@Override
	public boolean onScaleBegin(ScaleGestureDetector gd) {
		mTimeEnd = mTimeStart = SystemClock.elapsedRealtime();
		mSumScale = 1;
		mBeginScale = false;
		mCenterX = mMapView.getWidth() >> 1;
		mCenterY = mMapView.getHeight() >> 1;
		mScale = 1;
		mMapPosition = mMapView.getMapPosition();
		return true;
	}

	@Override
	public void onScaleEnd(ScaleGestureDetector gd) {
		// Log.d("ScaleListener", "Sum " + mSumScale + " " + (mTimeEnd - mTimeStart));

		if (mTimer == null && mTimeEnd - mTimeStart < 150
				&& (mSumScale < 0.99 || mSumScale > 1.01)) {

			mPrevScale = 0;

			mZooutOut = mSumScale < 0.99;

			mTimer = new CountDownTimer((int) mScaleDuration, 30) {
				@Override
				public void onTick(long tick) {
					scale(tick);
				}

				@Override
				public void onFinish() {
					scale(0);

				}
			}.start();
		}

	}

	private DecelerateInterpolator mBounce = new DecelerateInterpolator();
	private float mPrevScale;
	private CountDownTimer mTimer;
	boolean mZooutOut;
	private final float mScaleDuration = 350;

	boolean scale(long tick) {

		if (mPrevScale >= 1) {
			mTimer = null;
			return false;
		}

		float adv = (mScaleDuration - tick) / mScaleDuration;
		adv = mBounce.getInterpolation(adv);

		float scale = adv - mPrevScale;
		mPrevScale += scale;

		if (mZooutOut) {
			mMapPosition.scaleMap(1 - scale, 0, 0);
		} else {
			mMapPosition.scaleMap(1 + scale, mFocusX, mFocusY);
		}

		mMapView.redrawTiles();

		if (tick == 0)
			mTimer = null;

		return true;
	}
}
