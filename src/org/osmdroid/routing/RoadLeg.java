package org.osmdroid.routing;

import java.util.ArrayList;

import org.osmdroid.utils.BonusPackHelper;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**
 * Road Leg is the portion of the road between 2 waypoints (intermediate points
 * requested)
 * 
 * @author M.Kergall
 */
public class RoadLeg implements Parcelable {
	/** in km */
	public double length;
	/** in sec */
	public double duration;
	/** starting node of the leg, as index in nodes array */
	public int startNodeIndex;
	/** and ending node */
	public int endNodeIndex;

	public RoadLeg() {
		length = duration = 0.0;
		startNodeIndex = endNodeIndex = 0;
	}

	public RoadLeg(int startNodeIndex, int endNodeIndex,
			ArrayList<RoadNode> nodes) {
		this.startNodeIndex = startNodeIndex;
		this.endNodeIndex = endNodeIndex;
		length = duration = 0.0;

		for (int i = startNodeIndex; i <= endNodeIndex; i++) {
			RoadNode node = nodes.get(i);
			length += node.length;
			duration += node.duration;
		}
		Log.d(BonusPackHelper.LOG_TAG, "Leg: " + startNodeIndex + "-" + endNodeIndex
				+ ", length=" + length + "km, duration=" + duration + "s");
	}

	//--- Parcelable implementation

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeDouble(length);
		out.writeDouble(duration);
		out.writeInt(startNodeIndex);
		out.writeInt(endNodeIndex);
	}

	public static final Parcelable.Creator<RoadLeg> CREATOR = new Parcelable.Creator<RoadLeg>() {
		@Override
		public RoadLeg createFromParcel(Parcel in) {
			RoadLeg rl = new RoadLeg();
			rl.length = in.readDouble();
			rl.duration = in.readDouble();
			rl.startNodeIndex = in.readInt();
			rl.endNodeIndex = in.readInt();
			return rl;
		}

		@Override
		public RoadLeg[] newArray(int size) {
			return new RoadLeg[size];
		}
	};
}
