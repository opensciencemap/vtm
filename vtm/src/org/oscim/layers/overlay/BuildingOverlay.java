/*
 * Copyright 2013 Hannes Janetzek
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
package org.oscim.layers.overlay;

import org.oscim.backend.input.MotionEvent;
import org.oscim.core.MapPosition;
import org.oscim.renderer.GLRenderer.Matrices;
import org.oscim.renderer.layers.ExtrusionRenderLayer;
import org.oscim.utils.FastMath;
import org.oscim.view.MapView;

/**
 * @author Hannes Janetzek
 */
public class BuildingOverlay extends Overlay {
	//private final static String TAG = BuildingOverlay.class.getName();

	final ExtrusionRenderLayer mExtLayer;

	public BuildingOverlay(MapView mapView, org.oscim.layers.tile.TileRenderLayer tileRenderLayer) {
		super(mapView);
		mExtLayer = new ExtrusionRenderLayer(tileRenderLayer) {
			private long mStartTime;

			@Override
			public void update(MapPosition pos, boolean changed, Matrices m) {

				boolean show = pos.scale >= (1 << MIN_ZOOM);

				if (show) {
					if (mAlpha < 1) {
						long now = System.currentTimeMillis();

						if (mStartTime == 0) {
							mStartTime = now;
						}
						float a = (now - mStartTime) / mFadeTime;
						mAlpha = FastMath.clamp(a, 0, 1);
						mMapView.render();
					} else
						mStartTime = 0;
				} else {
					if (mAlpha > 0) {
						long now = System.currentTimeMillis();
						if (mStartTime == 0) {
							mStartTime = now + 100;
						}
						long diff = (now - mStartTime);
						if (diff > 0) {
							float a = 1 - diff / mFadeTime;
							mAlpha = FastMath.clamp(a, 0, 1);
						}
						mMapView.render();
					} else
						mStartTime = 0;
				}
				//Log.d(TAG, show + " > " + mAlpha);
				super.update(pos, changed, m);
			}
		};
		mLayer = mExtLayer;
	}

	//private int multi;

	private final float mFadeTime = 500;

	private final static int MIN_ZOOM = 17;

	@Override
	public boolean onTouchEvent(MotionEvent e) {
		//		int action = e.getAction() & MotionEvent.ACTION_MASK;
		//		if (action == MotionEvent.ACTION_POINTER_DOWN) {
		//			multi++;
		//		} else if (action == MotionEvent.ACTION_POINTER_UP) {
		//			multi--;
		//			if (!mActive && mAlpha > 0) {
		//				// finish hiding
		//				//Log.d(TAG, "add multi hide timer " + mAlpha);
		//				addShowTimer(mFadeTime * mAlpha, false);
		//			}
		//		} else if (action == MotionEvent.ACTION_CANCEL) {
		//			multi = 0;
		//			Log.d(TAG, "cancel " + multi);
		//			if (mTimer != null) {
		//				mTimer.cancel();
		//				mTimer = null;
		//			}
		//		}

		return false;
	}

}
