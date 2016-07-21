package org.osmdroid.location;

import org.oscim.core.BoundingBox;

import java.util.List;

public interface POIProvider {

    public List<POI> getPOIInside(BoundingBox boundingBox, String query, int maxResults);
}
