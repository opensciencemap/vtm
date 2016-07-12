package org.oscim.web.js;

import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.map.Map;
import org.oscim.tiling.TileSource;
import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportOverlay;
import org.timepedia.exporter.client.ExportPackage;

@ExportPackage("vtm")
@Export("BitmapTileLayer")
public class JsBitmapTileLayer extends BitmapTileLayer implements ExportOverlay<BitmapTileLayer> {

    public JsBitmapTileLayer(Map map, TileSource tileSource) {
        super(map, tileSource);
    }

    //    @ExportConstructor
    //    public static BitmapTileLayer constructor(Map map, TileSource tileSource) {
    //        return new JsBitmapTileLayer(map, tileSource);
    //    }
}
