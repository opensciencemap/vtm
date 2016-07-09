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
package org.oscim.tiling;

import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.MapElement;

/**
 * ITileDataSink callbacks (implemented by MapTileLoader)
 */
public interface ITileDataSink {
    /**
     * Pass read MapElement data to loader.
     * <p/>
     * NOTE: MapElement passed belong to the caller! i.e. dont hold references
     * to any of its data after callback function returns.
     */
    void process(MapElement element);

    /**
     * Pass read Bitmap data to loader.
     */
    void setTileImage(Bitmap bitmap);

    /**
     * Notify loader that tile loading is completed.
     */
    void completed(QueryResult result);
}
