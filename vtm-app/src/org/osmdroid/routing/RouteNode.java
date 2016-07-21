package org.osmdroid.routing;

import org.oscim.core.GeoPoint;

/**
 * Route intersection, with instructions to continue.
 *
 * @author M.Kergall
 */
public class RouteNode {
    /**
     * @see <a
     * href="http://open.mapquestapi.com/guidance/#maneuvertypes">Maneuver
     * Types</a>
     */
    public int maneuverType;
    /**
     * textual information on what to do at this intersection
     */
    public String instructions;
    /**
     * index in route links array - internal use only, for MapQuest directions
     */
    public int nextRouteLink;
    /**
     * in km to the next node
     */
    public double length;
    /**
     * in seconds to the next node
     */
    public double duration;
    /**
     * position of the node
     */
    public GeoPoint location;

    public RouteNode() {
        maneuverType = 0;
        nextRouteLink = -1;
        length = duration = 0.0;
    }

    // --- Parcelable implementation

    //    @Override
    //    public int describeContents() {
    //        return 0;
    //    }
    //
    //    @Override
    //    public void writeToParcel(Parcel out, int flags) {
    //        out.writeInt(maneuverType);
    //        out.writeString(instructions);
    //        out.writeDouble(length);
    //        out.writeDouble(duration);
    //        out.writeParcelable(location, 0);
    //    }
    //
    //    public static final Parcelable.Creator<RouteNode> CREATOR = new
    //            Parcelable.Creator<RouteNode>() {
    //                @Override
    //                public RouteNode createFromParcel(Parcel in) {
    //                    RouteNode rn = new RouteNode();
    //                    rn.maneuverType = in.readInt();
    //                    rn.instructions = in.readString();
    //                    rn.length = in.readDouble();
    //                    rn.duration = in.readDouble();
    //                    rn.location = in.readParcelable(GeoPoint.class.getClassLoader());
    //                    return rn;
    //                }
    //
    //                @Override
    //                public RouteNode[] newArray(int size) {
    //                    return new RouteNode[size];
    //                }
    //            };

}
