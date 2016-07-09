/*
 * Copyright 2013 Tobias Knerr
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
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
package org.oscim.core.osm;

import java.util.Collection;

/**
 * Osm dataset containing nodes, areas and relations
 */
public class OsmData {

    private final Collection<Bound> bounds;
    private final Collection<OsmNode> nodes;
    private final Collection<OsmWay> ways;
    private final Collection<OsmRelation> relations;

    public OsmData(Collection<Bound> bounds, Collection<OsmNode> nodes,
                   Collection<OsmWay> ways, Collection<OsmRelation> relations) {

        this.bounds = bounds;
        this.nodes = nodes;
        this.ways = ways;
        this.relations = relations;

    }

    public Collection<OsmNode> getNodes() {
        return nodes;
    }

    public Collection<OsmWay> getWays() {
        return ways;
    }

    public Collection<OsmRelation> getRelations() {
        return relations;
    }

    public Collection<Bound> getBounds() {
        return bounds;
    }

}
