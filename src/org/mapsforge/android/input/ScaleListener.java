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

import android.view.ScaleGestureDetector;

class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener {
	private final MapView mMapView;
	private MapViewPosition mMapPosition;
	private float mCenterX;
	private float mCenterY;
	// private float mFocusX;
	// private float mFocusY;
	private float mScale;

	// private boolean mScaling;
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
	public boolean onScale(ScaleGestureDetector scaleGestureDetector) {

		float focusX = scaleGestureDetector.getFocusX();
		float focusY = scaleGestureDetector.getFocusY();
		mScale = scaleGestureDetector.getScaleFactor();

		// mMapPosition.moveMap((focusX - mFocusX), (focusY - mFocusY));
		// if (mScale > 1.001 || mScale < 0.999) {

		mMapPosition.scaleMap(mScale,
				focusX - mCenterX,
				focusY - mCenterY);
		mMapView.redrawTiles();
		// mScale = 1;

		// mFocusX = focusX;
		// mFocusY = focusY;

		// }
		// else if (Math.abs(focusX - mFocusX) > 0.5 || Math.abs(focusY - mFocusY) > 0.5) {
		// mMapPosition.moveMap((focusX - mFocusX), (focusY - mFocusY));
		//
		// mFocusX = focusX;
		// mFocusY = focusY;
		// mScale = 1;
		// mMapView.redrawTiles();
		// }

		return true;
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {

		mCenterX = mMapView.getWidth() >> 1;
		mCenterY = mMapView.getHeight() >> 1;
		// mFocusX = scaleGestureDetector.getFocusX();
		// mFocusY = scaleGestureDetector.getFocusY();
		mScale = 1;
		mMapPosition = mMapView.getMapPosition();
		// mScaling = false;
		return true;
	}

	@Override
	public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
		// do nothing
	}
}
