/*
 * Copyright 2018 Gustl22
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
package org.oscim.layers.tile;

public class ZoomLimiter {

    private final int mMinZoom;
    private final int mMaxZoom;

    private final TileManager mTileManager;

    /**
     * Indicates that layer isn't processed again over zoom limit.
     */
    private final int mZoomLimit;

    /**
     * Layer would avoid rendering tiles over a specific zoom limit.
     */
    public ZoomLimiter(TileManager tileManager, int minZoom, int maxZoom, int zoomLimit) {
        if (zoomLimit < minZoom || zoomLimit > maxZoom) {
            throw new IllegalArgumentException("Zoom limit is out of range");
        }
        mTileManager = tileManager;
        mMinZoom = minZoom;
        mMaxZoom = maxZoom;
        mZoomLimit = zoomLimit;
    }

    public void addZoomLimit() {
        if (mZoomLimit < mMaxZoom)
            mTileManager.addZoomLimit(mZoomLimit);
    }

    public int getMaxZoom() {
        return mMaxZoom;
    }

    public int getMinZoom() {
        return mMinZoom;
    }

    /**
     * Get tile of zoom limit if zoom level is larger than limit.
     */
    public MapTile getTile(MapTile t) {
        if (t.zoomLevel > mZoomLimit && t.zoomLevel <= mMaxZoom) {
            int diff = t.zoomLevel - mZoomLimit;
            return mTileManager.getTile(t.tileX >> diff, t.tileY >> diff, mZoomLimit);
        }
        return t;
    }

    public TileManager getTileManager() {
        return mTileManager;
    }

    public int getZoomLimit() {
        return mZoomLimit;
    }

    public void removeZoomLimit() {
        if (mZoomLimit < mMaxZoom)
            mTileManager.removeZoomLimit(mZoomLimit);
    }

    public interface IZoomLimiter {
        /**
         * Add zoom limit to tile manager to load these tiles.
         */
        void addZoomLimit();

        /**
         * Remove zoom limit from tile manager.
         */
        void removeZoomLimit();
    }
}
