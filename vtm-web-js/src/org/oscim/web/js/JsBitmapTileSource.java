package org.oscim.web.js;

import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.source.bitmap.BitmapTileSource;
import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportOverlay;
import org.timepedia.exporter.client.ExportPackage;

@ExportPackage("vtm")
@Export("BitmapTileSource")
public class JsBitmapTileSource extends BitmapTileSource implements
        ExportOverlay<BitmapTileSource> {

    public JsBitmapTileSource(String url, int zoomMin, int zoomMax) {
        super(url, zoomMin, zoomMax);
    }

    @Override
    public ITileDataSource getDataSource() {
        return null;
    }
    //    @ExportConstructor
    //    public static BitmapTileSource constructor(String url, int zoomMin, int zoomMax) {
    //        return new JsBitmapTileSource(url, zoomMin, zoomMax);
    //    }

}
