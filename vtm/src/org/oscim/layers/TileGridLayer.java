package org.oscim.layers;

import org.oscim.map.Map;
import org.oscim.renderer.GridRenderer;

public class TileGridLayer extends GenericLayer {

	public TileGridLayer(Map map) {
		super(map, new GridRenderer());
	}

}
