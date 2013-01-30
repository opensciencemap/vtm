/*
 * Copyright 2013 OpenScienceMap.org
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
package org.oscim.renderer.overlays;

import org.oscim.core.MapPosition;
import org.oscim.view.MapView;

public class CustomOverlay extends RenderOverlay {

	public CustomOverlay(MapView mapView) {
		super(mapView);
	}

	@Override
	public void update(MapPosition curPos, boolean positionChanged, boolean tilesChanged) {
		// tell GLRender to call 'compile'
		newData = true;
	}

	@Override
	public void compile() {
		// tell GLRender to call 'render'
		isReady = true;
	}

	@Override
	public void render(MapPosition pos, float[] mv, float[] proj) {
	}
}
