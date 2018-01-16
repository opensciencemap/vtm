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

import org.locationtech.jts.geom.Geometry;
import org.oscim.core.TagSet;

import java.util.ArrayList;
import java.util.List;

public class OsmRelation extends OsmElement {

    public final List<OsmMember> relationMembers;

    // content added after constructor call

    public OsmRelation(TagSet tags, long id, int initialMemberSize) {
        super(tags, id);
        this.relationMembers =
                new ArrayList<OsmMember>(initialMemberSize);
    }

    @Override
    public String toString() {
        return "r" + id;
    }

    @Override
    public Geometry toJts() {
        return null;
    }
}
