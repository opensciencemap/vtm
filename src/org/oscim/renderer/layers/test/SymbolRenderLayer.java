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
package org.oscim.renderer.layers.test;

import java.io.IOException;

import org.oscim.core.MapPosition;
import org.oscim.renderer.GLRenderer.Matrices;
import org.oscim.renderer.layers.BasicRenderLayer;
import org.oscim.renderer.sublayers.SymbolItem;
import org.oscim.renderer.sublayers.SymbolLayer;
import org.oscim.theme.renderinstruction.BitmapUtils;
import org.oscim.view.MapView;

public class SymbolRenderLayer extends BasicRenderLayer {
	boolean initialize = true;

	public SymbolRenderLayer(MapView mapView) {
		super(mapView);
		SymbolLayer l = new SymbolLayer();
		layers.textureLayers = l;

		SymbolItem it = SymbolItem.pool.get();
		it.billboard = false;

		try {
			it.bitmap = BitmapUtils.createBitmap("jar:symbols/cafe.png");
		} catch (IOException e) {
			e.printStackTrace();

		}
		l.addSymbol(it);

		// compile layer on next frame
		newData = true;
	}

	@Override
	public void update(MapPosition position, boolean changed, Matrices matrices) {
		if (initialize){
			initialize = false;
			mMapPosition.copy(position);
		}
	}
}
