package org.oscim.layers;

import org.oscim.backend.canvas.Paint.Cap;
import org.oscim.map.Map;
import org.oscim.renderer.GridRenderer;
import org.oscim.theme.styles.Line;
import org.oscim.theme.styles.Text;

public class TileGridLayer extends GenericLayer {

	public TileGridLayer(Map map) {
		super(map, new GridRenderer());
	}

	public TileGridLayer(Map map, int color, float width, int repeat) {
		super(map, new GridRenderer(repeat, new Line(color, width, Cap.BUTT), null));
	}

	public TileGridLayer(Map map, int color, float width, Text text, int repeat) {
		super(map, new GridRenderer(repeat, new Line(color, width, Cap.BUTT), text));
	}
}
