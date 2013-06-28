/*
 * Copyright 2013 Hannes Janetzek
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
package org.oscim.tilesource;

import org.oscim.core.MapElement;

/**
 * MapDatabase callback (implemented by MapTileLoader)
 * .
 * NOTE: MapElement passed belong to the caller! i.e. dont hold
 * references to its arrays after callback function returns.
 */
public interface ITileDataSink {

	void process(MapElement element);

	void completed(boolean success);
}
