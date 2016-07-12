package org.oscim.tiling.source.bitmap;

import com.google.gwt.event.dom.client.ErrorEvent;
import com.google.gwt.event.dom.client.ErrorHandler;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.RootPanel;

import org.oscim.gdx.client.GwtBitmap;
import org.oscim.layers.tile.LoadDelayTask;
import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.TileLoader;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.QueryResult;
import org.oscim.tiling.source.LwHttp;
import org.oscim.tiling.source.UrlTileSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        super(url, "/{Z}/{X}/{Y}.png", zoomMin, zoomMax);
    }

    public BitmapTileSource(String url, int zoomMin, int zoomMax, String extension) {
        super(url, "/{Z}/{X}/{Y}" + extension, zoomMin, zoomMax);
    }

    public BitmapTileSource(String url, String tilePath, int zoomMin, int zoomMax) {
        super(url, tilePath, zoomMin, zoomMax);
    }

    @Override
    public ITileDataSource getDataSource() {
        return new BitmapTileDataSource(this);
    }

    public class BitmapTileDataSource implements ITileDataSource {

        protected final UrlTileSource mTileSource;

        public BitmapTileDataSource(BitmapTileSource bitmapTileSource) {
            mTileSource = bitmapTileSource;
        }

        @Override
        public void query(final MapTile tile, final ITileDataSink sink) {

            String url = mTileSource.getTileUrl(tile);

            SafeUri uri = UriUtils.fromTrustedString(url);

            final Image img = new Image();
            img.setVisible(false);

            /* As if researching CORS issues doesnt result in
             * enough headache...
             *
             * Here are some more special Chrome/Webkit quirks:
             * MUST SET CORS BEFORE URL! */
            img.getElement().setAttribute("crossorigin", "anonymous");
            img.setUrl(uri);

            RootPanel.get().add(img);

            img.addLoadHandler(new LoadHandler() {
                public void onLoad(LoadEvent event) {
                    TileLoader.postLoadDelay(new LoadDelayTask<Image>(tile, sink, img) {

                        @Override
                        public void continueLoading() {
                            if (!tile.state(MapTile.State.LOADING)) {
                                sink.completed(QueryResult.FAILED);
                                RootPanel.get().remove(data);
                            } else {
                                sink.setTileImage(new GwtBitmap(data));
                                sink.completed(QueryResult.SUCCESS);
                            }
                        }
                    });
                }
            });

            img.addErrorHandler(new ErrorHandler() {

                @Override
                public void onError(ErrorEvent event) {
                    sink.completed(QueryResult.FAILED);
                    RootPanel.get().remove(img);
                }
            });
        }

        @Override
        public void dispose() {
        }

        @Override
        public void cancel() {
        }

    }
}
