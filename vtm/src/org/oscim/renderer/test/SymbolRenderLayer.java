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
package org.oscim.renderer.test;

import java.io.IOException;

import org.oscim.backend.BitmapUtils;
import org.oscim.core.MapPosition;
import org.oscim.renderer.ElementRenderer;
import org.oscim.renderer.MapRenderer.Matrices;
import org.oscim.renderer.elements.SymbolItem;
import org.oscim.renderer.elements.SymbolLayer;

public class SymbolRenderLayer extends ElementRenderer {
	boolean initialize = true;

	public SymbolRenderLayer() {
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
	}

	@Override
	protected void update(MapPosition position, boolean changed, Matrices matrices) {
		if (initialize){
			initialize = false;
			mMapPosition.copy(position);
			compile();
		}
	}
}
