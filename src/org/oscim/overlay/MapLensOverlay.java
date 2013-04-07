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
package org.oscim.overlay;

import org.oscim.renderer.overlays.TileOverlay;
import org.oscim.view.MapView;

import android.view.MotionEvent;

public class MapLensOverlay extends Overlay {
	private final TileOverlay mTileLayer;

	public MapLensOverlay(MapView mapView) {
		super(mapView);

		mLayer = mTileLayer = new TileOverlay(mapView);
	}

	@Override
	public boolean onTouchEvent(MotionEvent e) {
		int action = e.getAction() & MotionEvent.ACTION_MASK;

		//if (action == MotionEvent.ACTION_MOVE){
		//		if (action == MotionEvent.ACTION_DOWN){
		//			mTileLayer.setPointer(e.getX(), e.getY());
		//			mMapView.render();
		//		} else
		if (action == MotionEvent.ACTION_POINTER_DOWN) {
			mTileLayer.setPointer((e.getX(0) + e.getX(1)) / 2, (e.getY(0) + e.getY(1)) / 2);
			mMapView.render();
		}

		return false;
	}
}
