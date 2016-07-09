package org.oscim.layers.vector.geometries;

import org.oscim.core.GeoPoint;
import org.oscim.utils.geom.GeomBuilder;

/**
 * Predefined class for drawing circles on the map. Circles are by default
 * made of 32 segments.
 */
public class CircleDrawable extends JtsDrawable {

    public static int MEDIUM_QUALITY = 32;
    public static int HIGH_QUALITY = 64;

    /**
     * Constructs a circle given the real-world radius in km. Keep in mind that
     * this technique is computationally costly for circles with huge number or
     * segments.
     *
     * @param center   GeoPoint - center of the circle
     * @param radiusKm Radius of the circle in kilometers.
     */
    public CircleDrawable(GeoPoint center, double radiusKm) {
        super(Style.DEFAULT_STYLE);
        GeomBuilder gb = new GeomBuilder();
        for (int i = 0; i < MEDIUM_QUALITY; i++) {
            GeoPoint point = findGeoPointWithGivenDistance(center,
                    i * Math.PI / MEDIUM_QUALITY * 2,
                    radiusKm);
            gb.points(point.getLongitude(), point.getLatitude());
        }
        geometry = gb.toPolygon();
    }

    /**
     * Constructs a circle given the real-world radius in km. Keep in mind that
     * this technique is computationally costly for circles with huge number or
     * segments.
     *
     * @param center   GeoPoint - center of the circle
     * @param radiusKm Radius of the circle in kilometers. The size of the
     *                 circle may be distorted due to the Mercator projections
     *                 properties.
     * @param style    FillableGeometryStyle with color and transparency
     *                 information for the circle
     */
    public CircleDrawable(GeoPoint center, double radiusKm, Style style) {
        super(style);
        GeomBuilder gb = new GeomBuilder();
        for (int i = 0; i < MEDIUM_QUALITY; i++) {
            GeoPoint point = findGeoPointWithGivenDistance(center,
                    i * Math.PI / MEDIUM_QUALITY * 2,
                    radiusKm);
            gb.points(point.getLongitude(), point.getLatitude());
        }
        geometry = gb.toPolygon();
    }

    /**
     * Constructs a circle given the real-world radius in km. Keep in mind that
     * this technique is computationally costly for circles with huge number or
     * segments.
     *
     * @param center           GeoPoint - center of the circle
     * @param radiusKm         Radius of the circle in kilometers. The size of the
     *                         circle may be distorted due to the Mercator projections
     *                         properties.
     * @param quadrantSegments the number of segments a quarter of circle will
     *                         have. Use Circle.LOW_PRECISION for quick rendering,
     *                         Circle.MEDIUM_PRECISION for good rendering and quality or
     *                         Circle.HIGH_PRECISION for high quality.
     * @param style            FillableGeometryStyle with color and transparency
     *                         information for the circle
     */
    public CircleDrawable(GeoPoint center, double radiusKm, int quadrantSegments,
                          Style style) {
        super(style);
        GeomBuilder gb = new GeomBuilder();
        for (int i = 0; i < quadrantSegments; i++) {
            GeoPoint point = findGeoPointWithGivenDistance(center,
                    i * Math.PI / quadrantSegments * 2,
                    radiusKm);
            gb.points(point.getLongitude(), point.getLatitude());
        }
        geometry = gb.toPolygon();
    }

    /**
     * This function finds a GeoPoint offset by a distance in the direction
     * given in the bearing parameter. It is an approximation due to the
     * Mercator projections properties
     *
     * @param startPoint
     * @param initialBearingRadians
     * @param distanceKilometres
     * @return a new GeoPoint located distanceKilometers away from the
     * startPoint in the direction of the initialBearing
     */
    private static GeoPoint findGeoPointWithGivenDistance(GeoPoint startPoint,
                                                          double initialBearingRadians, double distanceKilometres) {
        double radiusEarthKilometres = 6371.01;
        double distRatio = distanceKilometres / radiusEarthKilometres;
        double distRatioSine = Math.sin(distRatio);
        double distRatioCosine = Math.cos(distRatio);

        double startLatRad = degreesToRadians(startPoint.getLatitude());
        double startLonRad = degreesToRadians(startPoint.getLongitude());

        double startLatCos = Math.cos(startLatRad);
        double startLatSin = Math.sin(startLatRad);

        double endLatRads = Math.asin((startLatSin * distRatioCosine)
                + (startLatCos * distRatioSine * Math.cos(initialBearingRadians)));

        double endLonRads = startLonRad
                + Math.atan2(
                Math.sin(initialBearingRadians) * distRatioSine * startLatCos,
                distRatioCosine - startLatSin * Math.sin(endLatRads));

        return new GeoPoint(radiansToDegrees(endLatRads), radiansToDegrees(endLonRads));

    }

    /**
     * translates an angle from degrees to radians
     *
     * @param degrees
     * @return the angle in radians
     */
    private static double degreesToRadians(double degrees) {
        double degToRadFactor = Math.PI / 180;
        return degrees * degToRadFactor;
    }

    /**
     * translates an angle from radians to degrees
     *
     * @param radians
     * @return the angle in degrees
     */
    private static double radiansToDegrees(double radians) {
        double radToDegFactor = 180 / Math.PI;
        return radians * radToDegFactor;
    }

}
