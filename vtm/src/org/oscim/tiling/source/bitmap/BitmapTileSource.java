package org.oscim.tiling.source.bitmap;

import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.Tile;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.source.ITileDecoder;
import org.oscim.tiling.source.LwHttp;
import org.oscim.tiling.source.UrlTileDataSource;
import org.oscim.tiling.source.UrlTileSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class BitmapTileSource extends UrlTileSource {
    static final Logger log = LoggerFactory.getLogger(LwHttp.class);

    public static class Builder<T extends Builder<T>> extends UrlTileSource.Builder<T> {

        public Builder() {
            super(null, "/{Z}/{X}/{Y}.png", 0, 17);
        }

        public BitmapTileSource build() {
            return new BitmapTileSource(this);
        }
    }

    protected BitmapTileSource(Builder<?> builder) {
        super(builder);
    }

    @SuppressWarnings("rawtypes")
    public static Builder<?> builder() {
        return new Builder();
    }

    /**
     * Create BitmapTileSource for 'url'
     * <p/>
     * By default path will be formatted as: url/z/x/y.png
     * Use e.g. setExtension(".jpg") to overide ending or
     * implement getUrlString() for custom formatting.
     */
    public BitmapTileSource(String url, int zoomMin, int zoomMax) {
        this(url, "/{Z}/{X}/{Y}.png", zoomMin, zoomMax);
    }

    public BitmapTileSource(String url, int zoomMin, int zoomMax, String extension) {
        this(url, "/{Z}/{X}/{Y}" + extension, zoomMin, zoomMax);
    }

    public BitmapTileSource(String url, String tilePath, int zoomMin, int zoomMax) {
        super(builder()
                .url(url)
                .tilePath(tilePath)
                .zoomMin(zoomMin)
                .zoomMax(zoomMax));
    }

    @Override
    public ITileDataSource getDataSource() {
        return new UrlTileDataSource(this, new BitmapTileDecoder(), getHttpEngine());
    }

    public class BitmapTileDecoder implements ITileDecoder {

        @Override
        public boolean decode(Tile tile, ITileDataSink sink, InputStream is)
                throws IOException {

            Bitmap bitmap = CanvasAdapter.decodeBitmap(is);
            if (!bitmap.isValid()) {
                log.debug("{} invalid bitmap", tile);
                return false;
            }
            sink.setTileImage(bitmap);

            return true;
        }
    }
}
