package org.oscim.web.js;

import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.map.Map;
import org.oscim.theme.IRenderTheme;
import org.oscim.tiling.TileSource;
import org.timepedia.exporter.client.ExportOverlay;
import org.timepedia.exporter.client.ExportPackage;

@ExportPackage("vtm")
public class JsVectorTileLayer implements ExportOverlay<VectorTileLayer> {
    public JsVectorTileLayer(Map map, TileSource tileSource) {
    }

    public boolean setTileSource(TileSource tileSource) {
        return false;
    }

    public void setRenderTheme(IRenderTheme theme) {
    }
}
