package org.oscim.tiling.source.bitmap;

import java.io.IOException;
import java.io.InputStream;

import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.Tile;
import org.oscim.tiling.source.ITileDataSink;
import org.oscim.tiling.source.ITileDataSource;
import org.oscim.tiling.source.ITileDecoder;
import org.oscim.tiling.source.common.LwHttp;
import org.oscim.tiling.source.common.UrlTileDataSource;
import org.oscim.tiling.source.common.UrlTileSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		return new UrlTileDataSource(this, new BitmapTileDecoder(), new LwHttp(mUrl));
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
