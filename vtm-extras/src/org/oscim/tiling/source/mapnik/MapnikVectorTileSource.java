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
package org.oscim.tiling.source.mapnik;

import org.oscim.core.Tile;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.source.UrlTileDataSource;
import org.oscim.tiling.source.UrlTileSource;

public class MapnikVectorTileSource extends UrlTileSource {

    public MapnikVectorTileSource() {
        super("http://d1s11ojcu7opje.cloudfront.net/dev/764e0b8d", "");
        setUrlFormatter(new TileUrlFormatter() {
            @Override
            public String formatTilePath(UrlTileSource tileSource, Tile tile) {
                // url formatter for mapbox streets
                byte[] hexTable = {
                        '0', '1', '2', '3',
                        '4', '5', '6', '7',
                        '8', '9', 'a', 'b',
                        'c', 'd', 'e', 'f'
                };
                StringBuilder sb = new StringBuilder();
                sb.append('/');
                sb.append(hexTable[(tile.tileX) % 16]);
                sb.append(hexTable[(tile.tileY) % 16]);
                sb.append('/');
                sb.append(tile.zoomLevel);
                sb.append('/');
                sb.append(tile.tileX);
                sb.append('/');
                sb.append(tile.tileY);

                return sb.toString();
            }
        });
    }

    @Override
    public ITileDataSource getDataSource() {
        return new UrlTileDataSource(this, new TileDecoder(), getHttpEngine());
    }
}
