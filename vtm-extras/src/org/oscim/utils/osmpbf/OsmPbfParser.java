// This software is released into the Public Domain.  See copying.txt for details.
package org.oscim.utils.osmpbf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.openstreetmap.osmosis.osmbinary.BinaryParser;
import org.openstreetmap.osmosis.osmbinary.Osmformat;
import org.oscim.backend.Log;
import org.oscim.core.Tag;
import org.oscim.core.TagSet;
import org.oscim.utils.osm.OSMData;
import org.oscim.utils.osm.OSMMember;
import org.oscim.utils.osm.OSMNode;
import org.oscim.utils.osm.OSMRelation;
import org.oscim.utils.osm.OSMWay;

/**
 * Class that reads and parses binary files and sends the contained entities to
 * the sink.
 */
public class OsmPbfParser extends BinaryParser {

	@Override
	public void complete() {
		//sink.complete();
	}

	//    /** Get the osmosis object representing a the user in a given Info protobuf.
	//     * @param info The info protobuf.
	//     * @return The OsmUser object */
	//    OsmUser getUser(Osmformat.Info info) {
	//        // System.out.println(info);
	//        if (info.hasUid() && info.hasUserSid()) {
	//            if (info.getUid() < 0) {
	//              return OsmUser.NONE;
	//            }
	//            return new OsmUser(info.getUid(), getStringById(info.getUserSid()));
	//        } else {
	//            return OsmUser.NONE;
	//        }
	//    }

	/**
	 * The magic number used to indicate no version number metadata for this
	 * entity.
	 */
	static final int NOVERSION = -1;
	/** The magic number used to indicate no changeset metadata for this entity. */
	static final int NOCHANGESET = -1;

	HashMap<Long, OSMNode> mNodeMap = new HashMap<Long, OSMNode>();
	HashMap<Long, OSMWay> mWayMap = new HashMap<Long, OSMWay>();

	@Override
	protected void parseNodes(List<Osmformat.Node> nodes) {
		for (Osmformat.Node i : nodes) {
			int tagCnt = i.getKeysCount();

			TagSet tags = new TagSet(tagCnt);

			//  List<Tag> tags = new ArrayList<Tag>();
			for (int j = 0; j < tagCnt; j++) {
				tags.add(new Tag(getStringById(i.getKeys(j)), getStringById(i.getVals(j))));
			}
			// long id, int version, Date timestamp, OsmUser user,
			// long changesetId, Collection<Tag> tags,
			// double latitude, double longitude
			OSMNode tmp;
			long id = i.getId();
			double latf = parseLat(i.getLat()), lonf = parseLon(i.getLon());

			//			if (i.hasInfo()) {
			//				Osmformat.Info info = i.getInfo();
			//				tmp = new OSMNode(new CommonEntityData(id, info.getVersion(), getDate(info),
			//						getUser(info), info.getChangeset(), tags), latf, lonf);
			//			} else {
			tmp = new OSMNode(latf, lonf, tags, id);
			//				tmp = new Node(new CommonEntityData(id, NOVERSION, NODATE, OsmUser.NONE,
			//						NOCHANGESET, tags), latf, lonf);
			//			}
			//sink.process(new NodeContainer(tmp));
			mNodeMap.put(Long.valueOf(id), tmp);
		}
	}

	@Override
	protected void parseDense(Osmformat.DenseNodes nodes) {
		long lastId = 0, lastLat = 0, lastLon = 0;

		int j = 0; // Index into the keysvals array.

		// Stuff for dense info
		//		long lasttimestamp = 0, lastchangeset = 0;
		//		int lastuserSid = 0, lastuid = 0;
		//	DenseInfo di = null;
		//		if (nodes.hasDenseinfo()) {
		//			di = nodes.getDenseinfo();
		//		}

		for (int i = 0; i < nodes.getIdCount(); i++) {
			OSMNode tmp;
			TagSet tags = new TagSet(4);
			long lat = nodes.getLat(i) + lastLat;
			lastLat = lat;
			long lon = nodes.getLon(i) + lastLon;
			lastLon = lon;
			long id = nodes.getId(i) + lastId;
			lastId = id;
			double latf = parseLat(lat), lonf = parseLon(lon);
			// If empty, assume that nothing here has keys or vals.
			if (nodes.getKeysValsCount() > 0) {
				while (nodes.getKeysVals(j) != 0) {
					int keyid = nodes.getKeysVals(j++);
					int valid = nodes.getKeysVals(j++);
					tags.add(new Tag(getStringById(keyid), getStringById(valid)));
				}
				j++; // Skip over the '0' delimiter.
			}
			// Handle dense info.
			//            if (di != null) {
			//              int uid = di.getUid(i) + lastuid; lastuid = uid;
			//              int userSid = di.getUserSid(i) + lastuserSid; lastuserSid = userSid;
			//              long timestamp = di.getTimestamp(i) + lasttimestamp; lasttimestamp = timestamp;
			//              int version = di.getVersion(i);
			//              long changeset = di.getChangeset(i) + lastchangeset; lastchangeset = changeset;
			//
			//              Date date = new Date(date_granularity * timestamp);

			//OsmUser user;
			//              if (uid < 0) {
			//                user = OsmUser.NONE;
			//              } else {
			//                user = new OsmUser(uid, getStringById(userSid));
			//              }
			//
			//              tmp = new OSMNode(id, tags, latf, lonf);
			//            } else {
			tmp = new OSMNode(latf, lonf, tags, id);

			mNodeMap.put(Long.valueOf(id), tmp);

			//            }

			//sink.process(new NodeContainer(tmp));
		}
	}

	@Override
	protected void parseWays(List<Osmformat.Way> ways) {
		for (Osmformat.Way i : ways) {
			int tagCnt = i.getKeysCount();
			TagSet tags = new TagSet(tagCnt);

			//  List<Tag> tags = new ArrayList<Tag>();
			for (int j = 0; j < tagCnt; j++) {
				tags.add(new Tag(getStringById(i.getKeys(j)), getStringById(i.getVals(j))));
			}
			//			List<Tag> tags = new ArrayList<Tag>();
			//			for (int j = 0; j < ; j++) {
			//				tags.add(new Tag(getStringById(i.getKeys(j)), getStringById(i.getVals(j))));
			//			}

			long lastId = 0;
			List<OSMNode> nodes = new ArrayList<OSMNode>();
			for (long j : i.getRefsList()) {
				OSMNode n = mNodeMap.get(Long.valueOf(j + lastId));
				if (n == null)
					n = new OSMNode(Double.NaN, Double.NaN, null, j + lastId);

				nodes.add(n);
				lastId = j + lastId;
			}

			long id = i.getId();

			// long id, int version, Date timestamp, OsmUser user,
			// long changesetId, Collection<Tag> tags,
			// List<WayNode> wayNodes
			OSMWay tmp;
			//			if (i.hasInfo()) {
			//				Osmformat.Info info = i.getInfo();
			//				tmp = new Way(new CommonEntityData(id, info.getVersion(), getDate(info),
			//						getUser(info), info.getChangeset(), tags), nodes);
			//			} else {
			tmp = new OSMWay(tags, id, nodes);
			//			}

			mWayMap.put(Long.valueOf(id), tmp);

			//sink.process(new WayContainer(tmp));
		}
	}

	@Override
	protected void parseRelations(List<Osmformat.Relation> rels) {
		for (Osmformat.Relation i : rels) {
			int tagCnt = i.getKeysCount();
			TagSet tags = new TagSet(tagCnt);

			for (int j = 0; j < tagCnt; j++)
				tags.add(new Tag(getStringById(i.getKeys(j)), getStringById(i.getVals(j))));

			long id = i.getId();

			long lastMid = 0;
			List<OSMMember> nodes = new ArrayList<OSMMember>();
			int memberCnt = i.getMemidsCount();

			//			for (int j = 0; j < memberCnt; j++) {
			//				long mid = lastMid + i.getMemids(j);
			//				lastMid = mid;
			//				String role = getStringById(i.getRolesSid(j));
			//
			//				Osmformat.Relation.MemberType t = i.getTypes(j);
			//
			//				if (t == Osmformat.Relation.MemberType.NODE) {
			//					etype = EntityType.Node;
			//				} else if (t == Osmformat.Relation.MemberType.WAY) {
			//					etype = EntityType.Way;
			//				} else if (t == Osmformat.Relation.MemberType.RELATION) {
			//					etype = EntityType.Relation;
			//				} else {
			//					assert false; // TODO; Illegal file?
			//				}
			//
			//				nodes.add(new OSMMember(mid, etype, role));
			//			}

			// long id, int version, TimestampContainer timestampContainer,
			// OsmUser user,
			// long changesetId, Collection<Tag> tags,
			// List<RelationMember> members
			OSMRelation tmp = new OSMRelation(tags, id, memberCnt);

			//			if (i.hasInfo()) {
			//				Osmformat.Info info = i.getInfo();
			//				tmp = new Relation(new CommonEntityData(id, info.getVersion(), getDate(info),
			//						getUser(info), info.getChangeset(), tags), nodes);
			//			} else {
			//				tmp = new Relation(new CommonEntityData(id, NOVERSION, NODATE, OsmUser.NONE,
			//						NOCHANGESET, tags), nodes);
			//			}
			//			sink.process(new RelationContainer(tmp));
		}
	}

	@Override
	public void parse(Osmformat.HeaderBlock block) {
		for (String s : block.getRequiredFeaturesList()) {
			if (s.equals("OsmSchema-V0.6")) {
				continue; // We can parse this.
			}
			if (s.equals("DenseNodes")) {
				continue; // We can parse this.
			}
			throw new RuntimeException("File requires unknown feature: " + s);
		}

		//        if (block.hasBbox()) {
		//            String source = OsmosisConstants.VERSION;
		//            if (block.hasSource()) {
		//                source = block.getSource();
		//            }
		//
		//            double multiplier = .000000001;
		//            double rightf = block.getBbox().getRight() * multiplier;
		//            double leftf = block.getBbox().getLeft() * multiplier;
		//            double topf = block.getBbox().getTop() * multiplier;
		//            double bottomf = block.getBbox().getBottom() * multiplier;
		//
		//            Bound bounds = new Bound(rightf, leftf, topf, bottomf, source);
		//            sink.process(new BoundContainer(bounds));
		//        }
	}

	public OSMData getData() {

		//		for (Entry<OSMRelation, List<TmpRelation>> entry : relationMembersForRelation
		//				.entrySet()) {
		//
		//			OSMRelation relation = entry.getKey();
		//
		//			for (TmpRelation member : entry.getValue()) {
		//
		//				OSMElement memberObject = null;
		//
		//				if ("node".equals(member)) {
		//					memberObject = nodesById.get(member.id);
		//				} else if ("way".equals(member)) {
		//					memberObject = waysById.get(member.id);
		//				} else if ("relation".equals(member)) {
		//					memberObject = relationsById.get(member.id);
		//				} else {
		//					// log("missing relation " + member.id);
		//					continue;
		//				}
		//
		//				if (memberObject != null) {
		//					OSMMember ownMember = new OSMMember(member.role,
		//							memberObject);
		//
		//					relation.relationMembers.add(ownMember);
		//				}
		//			}
		//		}

		// give up references to original collections

		ArrayList<OSMWay> ways = new ArrayList<OSMWay>(mWayMap.values());
		ArrayList<OSMNode> nodes = new ArrayList<OSMNode>(mNodeMap.values());

		Log.d("..", "nodes: " + nodes.size() + " ways: " + ways.size());

		return new OSMData(null, nodes, ways, null);
	}
}
