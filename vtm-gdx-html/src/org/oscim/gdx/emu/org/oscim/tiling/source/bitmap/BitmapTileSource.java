package org.oscim.tiling.source.bitmap;

import org.oscim.gdx.client.GwtBitmap;
import org.oscim.layers.tile.MapTile;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.source.LwHttp;
import org.oscim.tiling.source.UrlTileSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.event.dom.client.ErrorEvent;
import com.google.gwt.event.dom.client.ErrorHandler;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.RootPanel;

public abstract class BitmapTileSource extends UrlTileSource {
	static final Logger log = LoggerFactory.getLogger(LwHttp.class);

	/**
	 * Create BitmapTileSource for 'url'
	 * 
	 * By default path will be formatted as: url/z/x/y.png
	 * Use e.g. setExtension(".jpg") to overide ending or
	 * implement getUrlString() for custom formatting.
	 */
	public BitmapTileSource(String url, int zoomMin, int zoomMax) {
		super(url, zoomMin, zoomMax);
		setExtension(".png");
	}

	@Override
	public ITileDataSource getDataSource() {
		return new BitmapTileDataSource(this);
	}

	public static class BitmapTileDataSource implements ITileDataSource {

		protected final UrlTileSource mTileSource;
		private final byte[] mRequestBuffer = new byte[1024];

		public BitmapTileDataSource(BitmapTileSource bitmapTileSource) {
			mTileSource = bitmapTileSource;
		}

		@Override
		public QueryResult executeQuery(final MapTile tile, final ITileDataSink sink) {

			int pos = mTileSource.formatTilePath(tile, mRequestBuffer, 0);

			String url = mTileSource.getUrl()
			        + (new String(mRequestBuffer, 0, pos));

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
					sink.setTileImage(new GwtBitmap(img));
					sink.completed(true);
				}
			});

			img.addErrorHandler(new ErrorHandler() {

				@Override
				public void onError(ErrorEvent event) {
					sink.completed(false);
					RootPanel.get().remove(img);
				}
			});
			return QueryResult.SUCCESS;
		}

		@Override
		public void destroy() {
		}
	}
}
