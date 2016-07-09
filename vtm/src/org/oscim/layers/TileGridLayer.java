package org.oscim.layers;

import org.oscim.backend.canvas.Paint.Cap;
import org.oscim.map.Map;
import org.oscim.renderer.GridRenderer;
import org.oscim.theme.styles.LineStyle;
import org.oscim.theme.styles.TextStyle;

public class TileGridLayer extends GenericLayer {

    public TileGridLayer(Map map) {
        super(map, new GridRenderer());
    }

    public TileGridLayer(Map map, int color, float width, int repeat) {
        super(map, new GridRenderer(repeat, new LineStyle(color, width, Cap.BUTT), null));
    }

    public TileGridLayer(Map map, int color, float width, TextStyle text, int repeat) {
        super(map, new GridRenderer(repeat, new LineStyle(color, width, Cap.BUTT), text));
    }
}
