package org.oscim.android.tiling.source.mbtiles;

import android.database.sqlite.SQLiteDatabase;

import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.Tile;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.TileSource;
import org.oscim.tiling.source.ITileDecoder;
import org.oscim.tiling.source.bitmap.BitmapTileSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by chaohan on 14-4-3.
 */
public class MBTileSource extends TileSource {
    static final Logger log = LoggerFactory.getLogger(MBTileSource.class);

    SQLiteDatabase db;


    public ITileDataSource getDataSource(){
        return new MBTileDataSource(this, new BitmapTileDecoder());
    }

    public boolean setFile(String filename) {
        setOption("file", filename);

        File file = new File(filename);

        if (!file.exists()) {
            return false;
        } else if (!file.isFile()) {
            return false;
        } else if (!file.canRead()) {
            return false;
        }

        return true;
    }

    public OpenResult open(){
        if (!options.containsKey("file"))
            return new OpenResult("no mb file set");

        try {
            // make sure to close any previously opened file first
            //close();

            File file = new File(options.get("file"));

            // check if the file exists and is readable
            if (!file.exists()) {
                return new OpenResult("file does not exist: " + file);
            } else if (!file.isFile()) {
                return new OpenResult("not a file: " + file);
            } else if (!file.canRead()) {
                return new OpenResult("cannot read file: " + file);
            }

            // open the file in read only mode
            db = SQLiteDatabase.openDatabase(file.getAbsolutePath(),null,
                SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.OPEN_READONLY);

            return OpenResult.SUCCESS;
        } catch (Exception e) {
            log.error(e.getMessage());
            // make sure that the file is closed
            close();
            return new OpenResult(e.getMessage());
        }
    }

    public void close(){
        if (db != null)
            db.close();
    }

    public class BitmapTileDecoder implements ITileDecoder {

        @Override
        public boolean decode(Tile tile, ITileDataSink sink, InputStream is)
                throws IOException {

            Bitmap bitmap = CanvasAdapter.g.decodeBitmap(is);
            if (!bitmap.isValid()) {
                log.debug("{} invalid bitmap", tile);
                return false;
            }
            sink.setTileImage(bitmap);

            return true;
        }
    }
}
