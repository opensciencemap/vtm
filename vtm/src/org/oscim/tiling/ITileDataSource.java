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
package org.oscim.tiling;

import org.oscim.layers.tile.MapTile;

public interface ITileDataSource {

    /**
     * @param tile        the tile to load.
     * @param mapDataSink the callback to handle the extracted map elements.
     */
    abstract void query(MapTile tile, ITileDataSink mapDataSink);

    /**
     * Implementations should cancel and release all resources
     */
    abstract void dispose();

    /**
     * Implementations should cancel their IO work and return
     */
    abstract void cancel();

}
