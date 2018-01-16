package org.oscim.layers.vector.geometries;

import org.locationtech.jts.geom.Geometry;

public interface Drawable {

    /**
     * @return
     */
    public Style getStyle();

    /**
     * @return
     */
    public Geometry getGeometry();
}
