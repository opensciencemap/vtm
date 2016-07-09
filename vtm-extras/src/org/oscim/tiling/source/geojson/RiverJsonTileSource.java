package org.oscim.tiling.source.geojson;

import org.oscim.core.MapElement;
import org.oscim.core.Tag;

import java.util.Map;

public class RiverJsonTileSource extends GeoJsonTileSource {

    public RiverJsonTileSource() {
        super("http://www.somebits.com:8001/rivers");
    }

    Tag mTagWater = new Tag("waterway", "river");

    @Override
    public void decodeTags(MapElement mapElement, Map<String, Object> properties) {

        mapElement.tags.add(mTagWater);

    }
}
