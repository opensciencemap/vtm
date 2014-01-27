package org.oscim.layers.tile.vector.labeling;

import org.oscim.core.MapPosition;
import org.oscim.renderer.elements.SymbolLayer;
import org.oscim.renderer.elements.TextLayer;
import org.oscim.renderer.elements.TextureLayer;

final class LabelTask {

	final TextureLayer layers;
	final TextLayer textLayer;
	final SymbolLayer symbolLayer;

	final MapPosition pos;

	LabelTask() {
		pos = new MapPosition();

		symbolLayer = new SymbolLayer();
		textLayer = new TextLayer();

		layers = symbolLayer;
		symbolLayer.next = textLayer;
	}

}
