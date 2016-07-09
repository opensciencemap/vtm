package org.oscim.layers;

import org.oscim.core.Box;
import org.oscim.core.osm.OsmElement;
import org.oscim.layers.vector.AbstractVectorLayer;
import org.oscim.map.Map;

public class OsmVectorLayer extends AbstractVectorLayer<OsmElement> {

    public OsmVectorLayer(Map map) {
        super(map);
    }

    @Override
    protected void processFeatures(Task t, Box b) {

    }

}
