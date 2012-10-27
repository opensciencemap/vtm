package org.osmdroid.routing;

import java.util.ArrayList;

import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;
import org.osmdroid.utils.BonusPackHelper;
import org.osmdroid.utils.DouglasPeuckerReducer;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**
 * describes the way to go from a position to an other. Normally returned by a
 * call to a Directions API (from MapQuest, GoogleMaps or other)
 * @see MapQuestRoadManager
 * @see GoogleRoadManager
 * @see OSRMRoadManager
 * @author M.Kergall
 */
public class Road implements Parcelable {
	/**
	 * @see #STATUS_INVALID STATUS_INVALID
	 * @see #STATUS_OK STATUS_OK
	 * @see #STATUS_DEFAULT STATUS_DEFAULT
	 */
	public int status;

	/** length of the whole route in km. */
	public double length;
	/** duration of the whole trip in sec. */
	public double duration;
	public ArrayList<RoadNode> nodes;
	/** */
	/** there is one leg between each waypoint */
	public ArrayList<RoadLeg> legs;
	/** full shape: polyline, as an array of GeoPoints */
	public ArrayList<GeoPoint> routeHigh;
	/** the same, in low resolution (less points) */
	private ArrayList<GeoPoint> routeLow;
	/** road bounding box */
	public BoundingBox boundingBox;

	/** STATUS_INVALID = road not built */
	public static final int STATUS_INVALID = 0;
	/** STATUS_OK = road properly retrieved and built */
	public static final int STATUS_OK = 1;
	/**
	 * STATUS_DEFAULT = any issue (technical issue, or no possible route) led to
	 * build a default road
	 */
	public static final int STATUS_DEFAULT = 2;

	private void init() {
		status = STATUS_INVALID;
		length = 0.0;
		duration = 0.0;
		nodes = new ArrayList<RoadNode>();
		routeHigh = new ArrayList<GeoPoint>();
		routeLow = null;
		legs = new ArrayList<RoadLeg>();
		boundingBox = null;
	}

	public Road() {
		init();
	}

	/**
	 * default constructor when normal loading failed: the road shape only
	 * contains the waypoints; All distances and times are at 0; there is no
	 * node; status equals DEFAULT.
	 * @param waypoints
	 *            ...
	 */
	public Road(ArrayList<GeoPoint> waypoints) {
		init();
		int n = waypoints.size();
		for (int i = 0; i < n; i++) {
			GeoPoint p = waypoints.get(i);
			routeHigh.add(p);
		}
		for (int i = 0; i < n - 1; i++) {
			RoadLeg leg = new RoadLeg(/* i, i+1, mLinks */);
			legs.add(leg);
		}
		boundingBox = BoundingBox.fromGeoPoints(routeHigh);
		status = STATUS_DEFAULT;
	}

	/**
	 * @return the road shape in "low resolution" = simplified by around 10
	 *         factor.
	 */
	public ArrayList<GeoPoint> getRouteLow() {
		if (routeLow == null) {
			// Simplify the route (divide number of points by around 10):
			int n = routeHigh.size();
			routeLow = DouglasPeuckerReducer.reduceWithTolerance(routeHigh, 1500.0);
			Log.d(BonusPackHelper.LOG_TAG, "Road reduced from " + n + " to " + routeLow.size()
					+ " points");
		}
		return routeLow;
	}

	public void setRouteLow(ArrayList<GeoPoint> route) {
		routeLow = route;
	}

	/**
	 * @param pLength
	 *            in km
	 * @param pDuration
	 *            in sec
	 * @return a human-readable length&duration text.
	 */
	public String getLengthDurationText(double pLength, double pDuration) {
		String result;
		if (pLength >= 100.0) {
			result = (int) (pLength) + " km, ";
		} else if (pLength >= 1.0) {
			result = Math.round(pLength * 10) / 10.0 + " km, ";
		} else {
			result = (int) (pLength * 1000) + " m, ";
		}
		int totalSeconds = (int) pDuration;
		int hours = totalSeconds / 3600;
		int minutes = (totalSeconds / 60) - (hours * 60);
		int seconds = (totalSeconds % 60);
		if (hours != 0) {
			result += hours + " h ";
		}
		if (minutes != 0) {
			result += minutes + " min";
		}
		if (hours == 0 && minutes == 0) {
			result += seconds + " s";
		}
		return result;
	}

	/**
	 * @return length and duration of the whole road, or of a leg of the road,
	 *         as a String, in a readable format.
	 * @param leg
	 *            leg index, starting from 0. -1 for the whole road
	 */
	public String getLengthDurationText(int leg) {
		double len = (leg == -1 ? this.length : legs.get(leg).length);
		double dur = (leg == -1 ? this.duration : legs.get(leg).duration);
		return getLengthDurationText(len, dur);
	}

	protected double distanceLLSquared(GeoPoint p1, GeoPoint p2) {
		double deltaLat = p2.latitudeE6 - p1.latitudeE6;
		double deltaLon = p2.longitudeE6 - p1.longitudeE6;
		return (deltaLat * deltaLat + deltaLon * deltaLon);
	}

	/**
	 * As MapQuest and OSRM doesn't provide legs information, we have to rebuild
	 * it, using the waypoints and the road nodes. <br>
	 * Note that MapQuest legs fit well with waypoints, as there is a
	 * "dedicated" node for each waypoint. But OSRM legs are not precise, as
	 * there is no node "dedicated" to waypoints.
	 * @param waypoints
	 *            ...
	 */
	public void buildLegs(ArrayList<GeoPoint> waypoints) {
		legs = new ArrayList<RoadLeg>();
		int firstNodeIndex = 0;
		// For all intermediate waypoints, search the node closest to the
		// waypoint
		int w = waypoints.size();
		int n = nodes.size();
		for (int i = 1; i < w - 1; i++) {
			GeoPoint waypoint = waypoints.get(i);
			double distanceMin = -1.0;
			int nodeIndexMin = -1;
			for (int j = firstNodeIndex; j < n; j++) {
				GeoPoint roadPoint = nodes.get(j).location;
				double dSquared = distanceLLSquared(roadPoint, waypoint);
				if (nodeIndexMin == -1 || dSquared < distanceMin) {
					distanceMin = dSquared;
					nodeIndexMin = j;
				}
			}
			// Build the leg as ending with this closest node:
			RoadLeg leg = new RoadLeg(firstNodeIndex, nodeIndexMin, nodes);
			legs.add(leg);
			firstNodeIndex = nodeIndexMin + 1; // restart next leg from end
		}
		// Build last leg ending with last node:
		RoadLeg lastLeg = new RoadLeg(firstNodeIndex, n - 1, nodes);
		legs.add(lastLeg);
	}

	// --- Parcelable implementation

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeInt(status);
		out.writeDouble(length);
		out.writeDouble(duration);
		out.writeList(nodes);
		out.writeList(legs);
		out.writeList(routeHigh);
		out.writeParcelable(boundingBox, 0);
	}

	public static final Parcelable.Creator<Road> CREATOR = new Parcelable.Creator<Road>() {
		@Override
		public Road createFromParcel(Parcel source) {
			return new Road(source);
		}

		@Override
		public Road[] newArray(int size) {
			return new Road[size];
		}
	};

	@SuppressWarnings("unchecked")
	private Road(Parcel in) {
		status = in.readInt();
		length = in.readDouble();
		duration = in.readDouble();

		nodes = in.readArrayList(RoadNode.class.getClassLoader());
		legs = in.readArrayList(RoadLeg.class.getClassLoader());
		routeHigh = in.readArrayList(GeoPoint.class.getClassLoader());
		boundingBox = in.readParcelable(BoundingBox.class.getClassLoader());
	}
}
