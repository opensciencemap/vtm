package org.osmdroid.routing;

import java.util.ArrayList;

import org.oscim.core.GeoPoint;
import org.oscim.overlay.PathOverlay;
import org.oscim.view.MapView;

import android.content.Context;
import android.graphics.Paint;

/**
 * Generic class to get a route between a start and a destination point, going
 * through a list of waypoints.
 * 
 * @see MapQuestRoadManager
 * @see GoogleRoadManager
 * @see OSRMRoadManager
 * @author M.Kergall
 */
public abstract class RoadManager {

	protected String mOptions;

	public abstract Road getRoad(ArrayList<GeoPoint> waypoints);

	public RoadManager() {
		mOptions = "";
	}

	/**
	 * Add an option that will be used in the route request. Note that some
	 * options are set in the request in all cases.
	 * 
	 * @param requestOption
	 *            see provider documentation. Just one example:
	 *            "routeType=bicycle" for MapQuest; "mode=bicycling" for Google.
	 */
	public void addRequestOption(String requestOption) {
		mOptions += "&" + requestOption;
	}

	protected String geoPointAsString(GeoPoint p) {
		StringBuffer result = new StringBuffer();
		double d = p.getLatitude();
		result.append(Double.toString(d));
		d = p.getLongitude();
		result.append("," + Double.toString(d));
		return result.toString();
	}

	public static PathOverlay buildRoadOverlay(MapView mapView, Road road, Paint paint,
			Context context) {
		PathOverlay roadOverlay = new PathOverlay(mapView, 0, context);
		roadOverlay.setPaint(paint);
		if (road != null) {
			ArrayList<GeoPoint> polyline = road.routeHigh;
			for (GeoPoint p : polyline) {
				roadOverlay.addPoint(p);
			}
		}
		return roadOverlay;
	}

	/**
	 * Builds an overlay for the road shape with a default (and nice!) color.
	 * 
	 * @param mapView
	 *            ..
	 * @param road
	 *            ..
	 * @param context
	 *            ..
	 * @return route shape overlay
	 */
	public static PathOverlay buildRoadOverlay(MapView mapView, Road road, Context context) {
		Paint paint = new Paint();
		paint.setColor(0x800000FF);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(5);
		return buildRoadOverlay(mapView, road, paint, context);
	}

}
