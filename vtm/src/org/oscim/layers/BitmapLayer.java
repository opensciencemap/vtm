package org.oscim.layers;

import org.oscim.map.Map;
import org.oscim.renderer.BitmapRenderer;
import org.oscim.renderer.LayerRenderer;

public class BitmapLayer extends GenericLayer {

	public BitmapLayer(Map map, LayerRenderer renderer) {
		super(map, new BitmapRenderer());
	}

}
