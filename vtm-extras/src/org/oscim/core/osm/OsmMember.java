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

public class OsmMember {
    public enum MemberType {
        NODE,
        WAY,
        RELATIOM
    }

    public final String role;
    public final OsmElement member;

    public OsmMember(String role, OsmElement member) {
        assert role != null && member != null;
        this.role = role;
        this.member = member;
    }

    @Override
    public String toString() {
        return role + ":" + member;
    }

}
