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

import org.oscim.android.gl.GLView;
import org.oscim.map.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.widget.RelativeLayout.LayoutParams;

public class AndroidMap extends Map {
	static final Logger log = LoggerFactory.getLogger(AndroidMap.class);

	private final MapView mMapView;
	final GLView mGLView;

	private boolean mRenderRequest;
	private boolean mRenderWait;
	private boolean mPausing;

	public AndroidMap(MapView mapView) {
		super();

		mMapView = mapView;
		mGLView = new GLView(mapView.getContext(), this);

		LayoutParams params =
		        new LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,
		                         android.view.ViewGroup.LayoutParams.MATCH_PARENT);

		mapView.addView(mGLView, params);
	}

	@Override
	public int getWidth() {
		return mMapView.getWidth();
	}

	@Override
	public int getHeight() {
		return mMapView.getHeight();
	}

	private final Runnable mRedrawCb = new Runnable() {
		@Override
		public void run() {
			prepareFrame();
			mGLView.requestRender();
		}
	};

	@Override
	public void updateMap(boolean redraw) {
		if (mPausing)
			return;

		if (!mRenderRequest) {
			mRenderRequest = true;
			mMapView.post(mRedrawCb);
		} else {
			mRenderWait = true;
		}
	}

	@Override
	public void render() {
		if (mPausing)
			return;

		updateMap(true);
	}

	@Override
	public void beginFrame() {
	}

	@Override
	public void doneFrame() {
		mRenderRequest = false;

		if (mRenderWait) {
			//log.debug("redraw");
			mRenderWait = false;
			updateMap(true);
			//prepareFrame();
			//mGLView.requestRender();
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
		log.debug("pause... {}", pause);
		mPausing = pause;
	}
}
