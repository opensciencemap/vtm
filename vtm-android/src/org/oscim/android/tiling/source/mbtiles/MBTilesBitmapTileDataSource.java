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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.BoundingBox;
import org.oscim.core.MercatorProjection;
import org.oscim.layers.tile.MapTile;
import org.oscim.map.Viewport;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * A tile data source for MBTiles raster databases.
 */
public class MBTilesBitmapTileDataSource implements ITileDataSource {

    private static final Logger log = LoggerFactory.getLogger(MBTilesBitmapTileDataSource.class);

    private static final String TABLE_TILES = "tiles";
    private static final String COL_TILES_ZOOM_LEVEL = "zoom_level";
    private static final String COL_TILES_TILE_COLUMN = "tile_column";
    private static final String COL_TILES_TILE_ROW = "tile_row";
    private static final String COL_TILES_TILE_DATA = "tile_data";
    private static final String SELECT_TILES = "SELECT " + COL_TILES_TILE_DATA + " from " + TABLE_TILES + " where "
            + COL_TILES_ZOOM_LEVEL + "=? AND " + COL_TILES_TILE_COLUMN + "=? AND " + COL_TILES_TILE_ROW + "=?";

    private static final String TABLE_METADATA = "metadata";
    private static final String COL_METADATA_NAME = "name";
    private static final String COL_METADATA_VALUE = "value";
    private static final String SELECT_METADATA = "select " + COL_METADATA_NAME + "," + COL_METADATA_VALUE + " from "
            + TABLE_METADATA;

    private final Integer mAlpha;
    private final SQLiteDatabase mDatabase;
    private Map<String, String> mMetadata;
    private final Integer mTransparentColor;

    /**
     * Create a MBTiles tile data source.
     *
     * @param path             the path to the MBTiles database.
     * @param alpha            an optional alpha value [0-255] to make the tiles transparent.
     * @param transparentColor an optional color that will be made transparent in the bitmap.
     */
    MBTilesBitmapTileDataSource(String path, Integer alpha, Integer transparentColor) {
        mDatabase = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY);
        mAlpha = alpha;
        mTransparentColor = transparentColor;
    }

    @Override
    public void cancel() {
        mDatabase.close();
    }

    @Override
    public void dispose() {
        mDatabase.close();
    }

    String getAttribution() {
        return getMetadata().get("attribution");
    }

    BoundingBox getBounds() {
        String bounds = getMetadata().get("bounds");
        if (bounds != null) {
            String[] split = bounds.split(",");
            double w = Double.parseDouble(split[0]);
            double s = Double.parseDouble(split[1]);
            double e = Double.parseDouble(split[2]);
            double n = Double.parseDouble(split[3]);
            return new BoundingBox(s, w, n, e);
        }
        return null;
    }

    public String getDescription() {
        return getMetadata().get("description");
    }

    /**
     * @return the image format (jpg, png)
     */
    public String getFormat() {
        return getMetadata().get("format");
    }

    int getMaxZoom() {
        String maxZoom = getMetadata().get("maxzoom");
        if (maxZoom != null)
            return Integer.parseInt(maxZoom);
        return Viewport.MAX_ZOOM_LEVEL;
    }

    private Map<String, String> getMetadata() {
        if (mMetadata == null) {
            mMetadata = new HashMap<>();
            Cursor cursor = null;
            try {
                cursor = mDatabase.rawQuery(SELECT_METADATA, null);
                while (cursor.moveToNext()) {
                    String key = cursor.getString(0);
                    String value = cursor.getString(1);
                    mMetadata.put(key, value);
                }
            } finally {
                if (cursor != null)
                    cursor.close();
            }
        }
        return mMetadata;
    }

    int getMinZoom() {
        String minZoom = getMetadata().get("minzoom");
        if (minZoom != null)
            return Integer.parseInt(minZoom);
        return Viewport.MIN_ZOOM_LEVEL;
    }

    String getName() {
        return getMetadata().get("name");
    }

    public String getVersion() {
        return getMetadata().get("version");
    }

    private static android.graphics.Bitmap processAlpha(android.graphics.Bitmap bitmap, int alpha) {
        android.graphics.Bitmap newBitmap = android.graphics.Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), android.graphics.Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(newBitmap);
        Paint paint = new Paint();
        paint.setAlpha(alpha);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return newBitmap;
    }

    private static android.graphics.Bitmap processTransparentColor(android.graphics.Bitmap bitmap, int colorToRemove) {
        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        for (int i = 0; i < pixels.length; i++) {
            if (pixels[i] == colorToRemove)
                pixels[i] = Color.alpha(Color.TRANSPARENT);
        }
        android.graphics.Bitmap newBitmap = android.graphics.Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), android.graphics.Bitmap.Config.ARGB_8888);
        newBitmap.setPixels(pixels, 0, newBitmap.getWidth(), 0, 0, newBitmap.getWidth(), newBitmap.getHeight());
        return newBitmap;
    }

    @Override
    public void query(MapTile tile, ITileDataSink sink) {
        QueryResult res = QueryResult.FAILED;
        try {
            byte[] bytes = readTile(tile.tileX, tile.tileY, tile.zoomLevel);

            if (mTransparentColor != null || mAlpha != null) {
                android.graphics.Bitmap androidBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (mTransparentColor != null)
                    androidBitmap = processTransparentColor(androidBitmap, mTransparentColor);
                if (mAlpha != null)
                    androidBitmap = processAlpha(androidBitmap, mAlpha);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                androidBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, bos);
                bytes = bos.toByteArray();
            }

            Bitmap bitmap = CanvasAdapter.decodeBitmap(new ByteArrayInputStream(bytes));
            sink.setTileImage(bitmap);
            res = QueryResult.SUCCESS;
        } catch (Exception e) {
            log.debug("{} invalid bitmap", tile);
        } finally {
            sink.completed(res);
        }
    }

    /**
     * Read a Tile's image bytes from the MBTiles database.
     *
     * @param tileX     the x tile index.
     * @param tileY     the y tile index (OSM notation).
     * @param zoomLevel the zoom level.
     * @return the tile image bytes.
     */
    private byte[] readTile(int tileX, int tileY, byte zoomLevel) {
        Cursor cursor = null;
        try {
            long tmsTileY = MercatorProjection.tileYToTMS(tileY, zoomLevel);
            cursor = mDatabase.rawQuery(SELECT_TILES, new String[]{String.valueOf(zoomLevel), String.valueOf(tileX), String.valueOf(tmsTileY)});
            if (cursor.moveToFirst())
                return cursor.getBlob(0);
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }
}
