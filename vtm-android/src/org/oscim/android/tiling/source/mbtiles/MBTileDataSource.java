package org.oscim.android.tiling.source.mbtiles;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.oscim.layers.tile.MapTile;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.source.ITileDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Created by chaohan on 14-4-3.
 */
public class MBTileDataSource implements ITileDataSource {

    static final Logger log = LoggerFactory.getLogger(MBTileDataSource.class);

    MBTileSource mMBTileSource;
    ITileDecoder mTileDecoder;

    public final static String TABLE_TILES = "tiles";
    public final static String COL_TILES_ZOOM_LEVEL = "zoom_level";
    public final static String COL_TILES_TILE_COLUMN = "tile_column";
    public final static String COL_TILES_TILE_ROW = "tile_row";
    public final static String COL_TILES_TILE_DATA = "tile_data";

    public MBTileDataSource(MBTileSource tileSource, ITileDecoder tileDecoder){
        mMBTileSource = tileSource;
        mTileDecoder = tileDecoder;
    }

    public void query(MapTile tile, ITileDataSink sink){


        try {
            InputStream ret = null;
            final String[] tiledata = { COL_TILES_TILE_DATA };
            final String[] xyz = {
                    Integer.toString(tile.tileX)
                    , Double.toString(Math.pow(2, tile.zoomLevel) - tile.tileY - 1)  // Use Google Tiling Spec
                    , Integer.toString(tile.zoomLevel)
            };

            final Cursor cur = mMBTileSource.db.query(TABLE_TILES, tiledata, "tile_column=? and tile_row=? and zoom_level=?", xyz, null, null, null);

            if(cur.getCount() != 0) {
                cur.moveToFirst();
                ret = new ByteArrayInputStream(cur.getBlob(0));
            }
            cur.close();
            if(ret != null) {
                if (mTileDecoder.decode(tile, sink, ret)) {
                    sink.completed(ITileDataSink.QueryResult.SUCCESS);
                    return;
                }
            }
        } catch(final Throwable e) {
            log.warn("Error getting db stream: " + tile, e);
            sink.completed(ITileDataSink.QueryResult.FAILED);
        }
        sink.completed(ITileDataSink.QueryResult.FAILED);

    }

    public void destroy(){

    }
}
