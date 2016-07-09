/*
 * Copyright 2014 Hannes Janetzek
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
package org.oscim.tiling.source;

import org.oscim.core.Tile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface HttpEngine {

    InputStream read() throws IOException;

    void sendRequest(Tile tile) throws IOException;

    void close();

    void setCache(OutputStream os);

    /**
     * @param success maybe false when tile could not be decoded.
     *                Dont write cache in this case, close socket, etc
     *                at your option.
     * @return true when everything went ok
     */
    boolean requestCompleted(boolean success);

    public interface Factory {
        HttpEngine create(UrlTileSource tileSource);
    }

}
