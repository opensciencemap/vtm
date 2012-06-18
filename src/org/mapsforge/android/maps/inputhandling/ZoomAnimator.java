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
package org.mapsforge.android.maps.inputhandling;

import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.utils.PausableThread;

import android.os.SystemClock;

/**
 * A ZoomAnimator handles the zoom-in and zoom-out animations of the corresponding MapView. It runs in a separate thread
 * to avoid blocking the UI thread.
 */
public class ZoomAnimator extends PausableThread {
	private static final int DEFAULT_DURATION = 250;
	private static final int FRAME_LENGTH_IN_MS = 15;
	private static final String THREAD_NAME = "ZoomAnimator";

	private boolean mExecuteAnimation;
	private final MapView mMapView;
	// private float mPivotX;
	// private float mPivotY;
	private float mScaleFactorApplied;
	private long mTimeStart;
	private float mZoomDifference;
	private float mZoomEnd;
	private float mZoomStart;

	/**
	 * @param mapView
	 *            the MapView whose zoom level changes should be animated.
	 */
	public ZoomAnimator(MapView mapView) {
		super();
		mMapView = mapView;
	}

	/**
	 * @return true if the ZoomAnimator is working, false otherwise.
	 */
	public boolean isExecuting() {
		return mExecuteAnimation;
	}

	/**
	 * Sets the parameters for the zoom animation.
	 * 
	 * @param zoomStart
	 *            the zoom factor at the begin of the animation.
	 * @param zoomEnd
	 *            the zoom factor at the end of the animation.
	 * @param pivotX
	 *            the x coordinate of the animation center.
	 * @param pivotY
	 *            the y coordinate of the animation center.
	 */
	public void setParameters(float zoomStart, float zoomEnd, float pivotX, float pivotY) {
		mZoomStart = zoomStart;
		mZoomEnd = zoomEnd;
		// mPivotX = pivotX;
		// mPivotY = pivotY;
	}

	/**
	 * Starts a zoom animation with the current parameters.
	 */
	public void startAnimation() {
		mZoomDifference = mZoomEnd - mZoomStart;
		mScaleFactorApplied = mZoomStart;
		mExecuteAnimation = true;
		mTimeStart = SystemClock.uptimeMillis();
		synchronized (this) {
			notify();
		}
	}

	@Override
	protected void doWork() throws InterruptedException {
		// calculate the elapsed time
		long timeElapsed = SystemClock.uptimeMillis() - mTimeStart;
		float timeElapsedPercent = Math.min(1, timeElapsed / (float) DEFAULT_DURATION);

		// calculate the zoom and scale values at the current moment
		float currentZoom = mZoomStart + timeElapsedPercent * mZoomDifference;
		float scaleFactor = currentZoom / mScaleFactorApplied;
		mScaleFactorApplied *= scaleFactor;
		// mapView.getFrameBuffer().matrixPostScale(scaleFactor, scaleFactor,
		// pivotX, pivotY);

		// check if the animation time is over
		if (timeElapsed >= DEFAULT_DURATION) {
			mExecuteAnimation = false;
			mMapView.redrawTiles();
		} else {
			mMapView.postInvalidate();
			sleep(FRAME_LENGTH_IN_MS);
		}
	}

	@Override
	protected String getThreadName() {
		return THREAD_NAME;
	}

	@Override
	protected boolean hasWork() {
		return mExecuteAnimation;
	}
}
