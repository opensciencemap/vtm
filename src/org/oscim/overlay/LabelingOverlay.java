/*
 * Copyright 2012 Hannes Janetzek
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

public class LabelingOverlay extends Overlay {

	//	private TextOverlay mLayer;

	//	@Override
	//	public org.oscim.renderer.overlays.RenderOverlay getLayer() {
	//		return mLayer;
	//	}

	public LabelingOverlay(MapView mapView) {
		super();
		mLayer = new TextOverlay(mapView);
	}
}
