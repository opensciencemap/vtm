/*
 * Copyright 2013 Tobias Knerr
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.oscim.utils.osm;

import java.util.Collection;

/**
 * OSM dataset containing nodes, areas and relations
 */
public class OSMData {

	private final Collection<Bound> bounds;
	private final Collection<OSMNode> nodes;
	private final Collection<OSMWay> ways;
	private final Collection<OSMRelation> relations;

	public OSMData(Collection<Bound> bounds, Collection<OSMNode> nodes,
			Collection<OSMWay> ways, Collection<OSMRelation> relations) {

		this.bounds = bounds;
		this.nodes = nodes;
		this.ways = ways;
		this.relations = relations;

	}

	public Collection<OSMNode> getNodes() {
		return nodes;
	}

	public Collection<OSMWay> getWays() {
		return ways;
	}

	public Collection<OSMRelation> getRelations() {
		return relations;
	}

	public Collection<Bound> getBounds() {
		return bounds;
	}

}
