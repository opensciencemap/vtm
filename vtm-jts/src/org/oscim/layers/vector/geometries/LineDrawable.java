package org.oscim.layers.vector.geometries;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.oscim.core.GeoPoint;

import java.util.List;

/**
 * Predefined class for drawing lines and line strings on the map.
 */
public class LineDrawable extends JtsDrawable {

    public LineDrawable(Geometry line, Style style) {
        super(style);
        if (line.getDimension() != 1)
            throw new IllegalArgumentException("Geometry not a Line");

        this.geometry = line;
    }

    public LineDrawable(List<GeoPoint> points) {
        this(points, Style.defaultStyle());
    }

    public LineDrawable(List<GeoPoint> points, Style style) {
        super(style);
        if (points.size() < 2)
            return;

        double[] coords = new double[points.size() * 2];
        int c = 0;
        for (GeoPoint p : points) {
            coords[c++] = p.getLongitude();
            coords[c++] = p.getLatitude();
        }
        this.geometry = new LineString(coordFactory.create(coords, 2), geomFactory);
    }

    public LineDrawable(double[] lonLat, Style style) {
        this(new LineString(coordFactory.create(lonLat, 2), geomFactory), style);
    }
}
