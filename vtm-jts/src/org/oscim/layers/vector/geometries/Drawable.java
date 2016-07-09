package org.oscim.layers.vector.geometries;

import com.vividsolutions.jts.geom.Geometry;

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
