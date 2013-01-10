/*
 * Copyright 2012, 2013 OpenScienceMap
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

import org.oscim.renderer.overlays.TextOverlay;
import org.oscim.view.MapView;

import android.util.Log;
import android.view.MotionEvent;

/**
 * @author Hannes Janetzek
 */
public class LabelingOverlay extends Overlay {
	private final static String TAG = LabelingOverlay.class.getName();
	final TextOverlay mTextLayer;

	public LabelingOverlay(MapView mapView) {
		super();
		mTextLayer = new TextOverlay(mapView);
		mLayer = mTextLayer;
	}

	private int multi;

	@Override
	public boolean onTouchEvent(MotionEvent e, MapView mapView) {
		int action = e.getAction() & MotionEvent.ACTION_MASK;
		if (action == MotionEvent.ACTION_POINTER_DOWN) {
			multi++;
			mTextLayer.hold(true);
		} else if (action == MotionEvent.ACTION_POINTER_UP) {
			multi--;
			if (multi == 0)
				mTextLayer.hold(false);
		} else if (action == MotionEvent.ACTION_CANCEL) {
			multi = 0;
			Log.d(TAG, "cancel " + multi);
			mTextLayer.hold(false);
		}

		return false;
	}
}
