package org.oscim.web.js;

import org.oscim.tiling.source.geojson.OsmLanduseJsonTileSource;
import org.timepedia.exporter.client.ExportOverlay;
import org.timepedia.exporter.client.ExportPackage;

@ExportPackage("vtm")
public class JsOsmLanduseJsonTileSource implements ExportOverlay<OsmLanduseJsonTileSource> {
    public JsOsmLanduseJsonTileSource() {
    }
}
