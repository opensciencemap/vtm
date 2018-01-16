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

public abstract class OsmElement {

    public final TagSet tags;
    public final long id;

    public OsmElement(TagSet tags, long id) {
        assert tags != null;
        this.tags = tags;
        this.id = id;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ (id >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        OsmElement other = (OsmElement) obj;
        if (id != other.id)
            return false;
        return true;
    }

    /**
     * returns the id, plus an one-letter prefix for the element type
     */
    @Override
    public String toString() {
        return "?" + id;
    }

    public abstract Geometry toJts();
}
