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
package org.oscim.layers.tile.vector;

import org.oscim.backend.input.MotionEvent;
import org.oscim.core.MapPosition;
import org.oscim.layers.InputLayer;
import org.oscim.map.Map;
import org.oscim.renderer.ExtrusionRenderer;
import org.oscim.renderer.MapRenderer.Matrices;
import org.oscim.tiling.TileRenderer;
import org.oscim.utils.FastMath;

/**
 * @author Hannes Janetzek
 */
public class BuildingLayer extends InputLayer {
	//private final static String TAG = BuildingOverlay.class.getName();

	final ExtrusionRenderer mExtLayer;

	public BuildingLayer(Map map, TileRenderer tileRenderer) {
		super(map);
		mExtLayer = new ExtrusionRenderer(tileRenderer) {
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
						mMap.render();
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
						mMap.render();
					} else
						mStartTime = 0;
				}
				//Log.d(TAG, show + " > " + mAlpha);
				super.update(pos, changed, m);
			}
		};
		mRenderer = mExtLayer;
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
