/*
 * Copyright 2013 OpenScienceMap
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
package org.oscim.overlay;

import org.oscim.core.MapPosition;
import org.oscim.renderer.overlays.ExtrusionOverlay;
import org.oscim.view.MapView;

import android.os.CountDownTimer;
import android.util.Log;
import android.view.MotionEvent;

/**
 * @author Hannes Janetzek
 */
public class BuildingOverlay extends Overlay {
	private final static String TAG = BuildingOverlay.class.getName();

	final ExtrusionOverlay mExtLayer;

	public BuildingOverlay(MapView mapView) {
		super();
		mMapView = mapView;
		mExtLayer = new ExtrusionOverlay(mapView);
		mLayer = mExtLayer;
	}

	private final MapView mMapView;
	private int multi;

	private float mFadeTime = 300;
	private float mAlpha = 1;

	@Override
	public boolean onTouchEvent(MotionEvent e, MapView mapView) {
		int action = e.getAction() & MotionEvent.ACTION_MASK;
		if (action == MotionEvent.ACTION_POINTER_DOWN) {
			multi++;
		} else if (action == MotionEvent.ACTION_POINTER_UP) {
			multi--;
			if (mPrevZoom != 17 && mAlpha > 0) {
				// finish hiding
				//Log.d(TAG, "add multi hide timer " + mAlpha);
				addShowTimer(mFadeTime * mAlpha, false);
			}
		} else if (action == MotionEvent.ACTION_CANCEL) {
			multi = 0;
			Log.d(TAG, "cancel " + multi);
			if (mTimer != null) {
				mTimer.cancel();
				mTimer = null;
			}
		}

		return false;
	}

	private byte mPrevZoom = 0;

	@Override
	public void onUpdate(MapPosition mapPosition, boolean changed) {
		byte z = mapPosition.zoomLevel;
		if (z == mPrevZoom)
			return;

		if (z == 17) {
			// start showing
			//Log.d(TAG, "add show timer " + mAlpha);
			addShowTimer(mFadeTime * (1 - mAlpha), true);
		} else if (mPrevZoom == 17) {
			// indicate hiding
			if (multi > 0) {
				//Log.d(TAG, "add fade timer " + mAlpha);
				addFadeTimer(mFadeTime * mAlpha, false);
			} else {
				//Log.d(TAG, "add hide timer " + mAlpha);
				addShowTimer(mFadeTime * mAlpha, false);
			}
		}

		mPrevZoom = z;
	}

	void fade(float duration, long tick, boolean dir, float max) {

		float a;
		if (dir)
			a = (1 - max) + (1 - (tick / duration)) * max;
		else
			a = (1 - max) + (tick / duration) * max;

		//Log.d(TAG, "fade " + dir + " " + tick + "\t" + a);

		mAlpha = a;
		mExtLayer.setAlpha(a);
		mMapView.render();
	}

	/* package */CountDownTimer mTimer;

	private void addFadeTimer(final float ms, final boolean dir) {
		if (mTimer != null)
			mTimer.cancel();

		mTimer = new CountDownTimer((long) ms, 16) {
			@Override
			public void onTick(long tick) {
				fade(ms, tick, dir, 0.2f);
			}

			@Override
			public void onFinish() {
				fade(ms, 0, dir, 0.2f);
				mTimer = null;
			}
		}.start();
	}

	private void addShowTimer(final float ms, final boolean dir) {
		final float d = mFadeTime;
		if (mTimer != null)
			mTimer.cancel();

		mTimer = new CountDownTimer((long) ms, 16) {
			@Override
			public void onTick(long tick) {
				fade(d, tick, dir, 1);
			}

			@Override
			public void onFinish() {
				fade(d, 0, dir, 1);
				mTimer = null;

			}
		}.start();
	}
}
