package org.oscim.layers.vector;

import static org.oscim.core.MercatorProjection.latitudeToY;
import static org.oscim.core.MercatorProjection.longitudeToX;

import org.oscim.core.GeometryBuffer;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class JtsConverter {
	double x, y, scale;
	final double outScale;

	public void setPosition(double x, double y, double scale) {
		this.x = x;
		this.y = y;
		this.scale = scale * outScale;
	}

	public JtsConverter(double outScale) {
		this.outScale = outScale;
	}

	private final Coordinate mTmpCoord = new Coordinate();

	public void transformPolygon(GeometryBuffer g, Polygon polygon) {
		Coordinate coord = mTmpCoord;

		CoordinateSequence ring = polygon.getExteriorRing().getCoordinateSequence();

		g.startPolygon();
		for (int j = 0; j < ring.size() - 1; j++) {
			ring.getCoordinate(j, coord);
			addPoint(g, coord);
		}
		for (int j = 0, n = polygon.getNumInteriorRing(); j < n; j++) {
			g.startHole();
			ring = polygon.getInteriorRingN(j).getCoordinateSequence();
			for (int k = 0; k < ring.size() - 1; k++) {
				ring.getCoordinate(k, coord);
				addPoint(g, coord);
			}
		}
	}

	public void transformLineString(GeometryBuffer g, LineString linestring) {
		Coordinate coord = mTmpCoord;

		CoordinateSequence line = linestring.getCoordinateSequence();

		g.startLine();
		for (int j = 0, n = line.size(); j < n; j++) {
			line.getCoordinate(j, coord);
			addPoint(g, coord);
		}
	}

	public void transformPoint(GeometryBuffer g, Point point) {
		Coordinate coord = mTmpCoord;

		g.startPoints();
		coord.x = point.getX();
		coord.y = point.getY();
		addPoint(g, coord);
	}

	public void addPoint(GeometryBuffer g, Coordinate coord) {
		g.addPoint((float) ((longitudeToX(coord.x) - x) * scale),
		           (float) ((latitudeToY(coord.y) - y) * scale));
	}

	public void addPoint(GeometryBuffer g, double lon, double lat) {
		g.addPoint((float) ((longitudeToX(lon) - x) * scale),
		           (float) ((latitudeToY(lat) - y) * scale));
	}

}
