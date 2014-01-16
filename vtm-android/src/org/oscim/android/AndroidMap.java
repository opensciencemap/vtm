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
package org.oscim.android;

import org.oscim.map.Map;

import android.widget.RelativeLayout.LayoutParams;

public class AndroidMap extends Map {

	private final MapView mMapView;
	boolean mWaitRedraw;
	final GLView mGLView;
	boolean mPausing = false;

	private final Runnable mRedrawRequest = new Runnable() {
		@Override
		public void run() {
			mWaitRedraw = false;
			redrawMapInternal(false);
		}
	};

	public AndroidMap(MapView mapView) {
		super();

		mMapView = mapView;
		mGLView = new GLView(mapView.getContext(), this);

		LayoutParams params =
		        new LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,
		                         android.view.ViewGroup.LayoutParams.MATCH_PARENT);

		mapView.addView(mGLView, params);

		//mGestureDetector =
	}

	@Override
	public int getWidth() {
		return mMapView.getWidth();
	}

	@Override
	public int getHeight() {
		return mMapView.getHeight();
	}

	@Override
	public void updateMap(boolean requestRender) {
		if (requestRender && !mClearMap && !mPausing) // && mInitialized)
			mGLView.requestRender();

		if (!mWaitRedraw) {
			mWaitRedraw = true;
			mMapView.post(mRedrawRequest);
		}
	}

	@Override
	public void render() {
		if (mClearMap)
			updateMap(false);
		else
			mGLView.requestRender();
	}

	void redrawMapInternal(boolean forceRedraw) {
		boolean clear = mClearMap;

		if (forceRedraw && !clear)
			mGLView.requestRender();

		updateLayers();

		if (clear) {
			mGLView.requestRender();
			mClearMap = false;
		}
	}

	@Override
	public boolean post(Runnable runnable) {
		return mMapView.post(runnable);
	}

	@Override
	public boolean postDelayed(Runnable action, long delay) {
		return mMapView.postDelayed(action, delay);
	}


	public void pause(boolean pause) {
	    mPausing = pause;
    }

}
