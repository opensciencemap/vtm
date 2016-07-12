/*
 * Copyright 2012 Hannes Janetzek
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
package org.oscim.layers.tile;

import java.util.Arrays;
import java.util.Comparator;

/**
 * TileSet for use with TileManager.getActiveTiles(TileSet) to get the current
 * tiles. Tiles will locked and not be modifed until getActiveTiles passes them
 * back to TileManager on a second invocation or TileManager.releaseTiles().
 */
public final class TileSet {
    public int cnt = 0;
    public MapTile[] tiles;

    /**
     * update counter will be set by getActiveTiles when TileSet has changed
     */
    int serial;

    public TileSet() {
        tiles = new MapTile[1];
    }

    public TileSet(int numTiles) {
        tiles = new MapTile[numTiles];
    }

    public MapTile getTile(int x, int y) {
        for (int i = 0; i < cnt; i++)
            if (tiles[i].tileX == x && tiles[i].tileY == y)
                return tiles[i];

        return null;
    }

    /**
     * Locked tiles to ensure that they are not released from cache.
     * Call releaseTiles() when tiles are not needed any longer.
     */
    public void lockTiles() {
        synchronized (TileSet.class) {
            for (int i = 0; i < cnt; i++)
                tiles[i].lock();
        }
    }

    /**
     * Release locked tiles.
     */
    public void releaseTiles() {
        synchronized (TileSet.class) {
            for (int i = 0; i < cnt; i++)
                tiles[i].unlock();
        }
        Arrays.fill(tiles, null);
        cnt = 0;
        serial = 0;
    }

    /**
     * Clone TileSet from source. Release previous tiles and lock
     * new tiles.
     */
    public void setTiles(TileSet source) {
        /* lock tiles (and their proxies) to not be removed from cache */
        source.lockTiles();

        /* unlock previous tiles */
        releaseTiles();

        if (source.tiles.length != tiles.length) {
            tiles = new MapTile[source.tiles.length];
        }

        System.arraycopy(source.tiles, 0, tiles, 0, source.cnt);

        cnt = source.cnt;
    }

    public static Comparator<MapTile> coordComparator = new CoordComparator();

    public static class CoordComparator implements Comparator<MapTile> {

        @Override
        public int compare(MapTile lhs, MapTile rhs) {
            if (lhs.tileX == rhs.tileX) {
                if (lhs.tileY == rhs.tileY)
                    return 0;

                if (lhs.tileY < rhs.tileY)
                    return 1;

                return -1;
            }
            if (lhs.tileX < rhs.tileX)
                return 1;

            return -1;
        }
    }

    public boolean contains(MapTile t) {
        for (int i = 0; i < cnt; i++)
            if (tiles[i].equals(t))
                return true;

        return false;
    }
}
