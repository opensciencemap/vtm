/*
 * Copyright 2013 Hannes Janetzek
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
package org.oscim.utils.osmpbf;

import org.openstreetmap.osmosis.osmbinary.file.BlockInputStream;
import org.oscim.core.osm.OsmData;

import java.io.IOException;
import java.io.InputStream;

public class OsmPbfReader {

    public static OsmData process(InputStream is) {
        OsmPbfParser parser = new OsmPbfParser();

        try {
            (new BlockInputStream(is, parser)).process();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }

        return parser.getData();
    }
}
