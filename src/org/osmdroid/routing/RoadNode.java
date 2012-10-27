package org.osmdroid.routing;

import org.oscim.core.GeoPoint;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Road intersection, with instructions to continue.
 * 
 * @author M.Kergall
 */
public class RoadNode implements Parcelable {
	/**
	 * @see <a
	 *      href="http://open.mapquestapi.com/guidance/#maneuvertypes">Maneuver
	 *      Types</a>
	 */
	public int maneuverType;
	/** textual information on what to do at this intersection */
	public String instructions;
	/** index in road links array - internal use only, for MapQuest directions */
	public int nextRoadLink;
	/** in km to the next node */
	public double length;
	/** in seconds to the next node */
	public double duration;
	/** position of the node */
	public GeoPoint location;

	public RoadNode() {
		maneuverType = 0;
		nextRoadLink = -1;
		length = duration = 0.0;
	}

	// --- Parcelable implementation

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeInt(maneuverType);
		out.writeString(instructions);
		out.writeDouble(length);
		out.writeDouble(duration);
		out.writeParcelable(location, 0);
	}

	public static final Parcelable.Creator<RoadNode> CREATOR = new
			Parcelable.Creator<RoadNode>() {
				@Override
				public RoadNode createFromParcel(Parcel in) {
					RoadNode rn = new RoadNode();
					rn.maneuverType = in.readInt();
					rn.instructions = in.readString();
					rn.length = in.readDouble();
					rn.duration = in.readDouble();
					rn.location = in.readParcelable(GeoPoint.class.getClassLoader());
					return rn;
				}

				@Override
				public RoadNode[] newArray(int size) {
					return new RoadNode[size];
				}
			};

}
