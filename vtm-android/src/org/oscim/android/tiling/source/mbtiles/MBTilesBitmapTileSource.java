/*
 * Copyright 2019 Andrea Antonello
 * Copyright 2019 devemux86
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
package org.oscim.android.tiling.source.mbtiles;

import org.oscim.core.BoundingBox;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.TileSource;

/**
 * A tile source for MBTiles raster databases.
 */
public class MBTilesBitmapTileSource extends TileSource {

    private final MBTilesBitmapTileDataSource mTileDataSource;

    /**
     * Create a MBTiles tile source.
     *
     * @param path the path to the MBTiles database.
     */
    public MBTilesBitmapTileSource(String path) {
        this(path, null, null);
    }

    /**
     * Create a MBTiles tile source.
     *
     * @param path             the path to the MBTiles database.
     * @param alpha            an optional alpha value [0-255] to make the tiles transparent.
     * @param transparentColor an optional color that will be made transparent in the bitmap.
     */
    public MBTilesBitmapTileSource(String path, Integer alpha, Integer transparentColor) {
        mTileDataSource = new MBTilesBitmapTileDataSource(path, alpha, transparentColor);
    }

    @Override
    public void close() {
        mTileDataSource.dispose();
    }


    public String getAttribution() {
        return mTileDataSource.getAttribution();
    }

    public BoundingBox getBounds() {
        return mTileDataSource.getBounds();
    }

    @Override
    public ITileDataSource getDataSource() {
        return mTileDataSource;
    }

    public String getDescription() {
        return mTileDataSource.getDescription();
    }

    /**
     * @return the image format (jpg, png)
     */
    public String getFormat() {
        return mTileDataSource.getFormat();
    }

    public int getMaxZoom() {
        return mTileDataSource.getMaxZoom();
    }

    public int getMinZoom() {
        return mTileDataSource.getMinZoom();
    }

    public String getName() {
        return mTileDataSource.getName();
    }

    public String getVersion() {
        return mTileDataSource.getVersion();
    }

    @Override
    public OpenResult open() {
        return OpenResult.SUCCESS;
    }
}
