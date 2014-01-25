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

	private final String mFileExtension;
	private final String mMimeType;

	public BitmapTileSource(String url) {
		this(url, 0, 17);
	}

	public BitmapTileSource(String url, int zoomMin, int zoomMax) {
		this(url, zoomMin, zoomMax, "image/png", ".png");
	}

	public BitmapTileSource(String url, int zoomMin, int zoomMax, String mimeType,
	        String fileExtension) {
		super(url);
		mZoomMin = zoomMin;
		mZoomMax = zoomMax;
		mFileExtension = fileExtension;
		mMimeType = mimeType;
	}

	public String getTileUrl(Tile tile) {
		return null;
	}

	@Override
	public ITileDataSource getDataSource() {
		LwHttp conn = new LwHttp(mUrl, mMimeType, mFileExtension, false) {
			@Override
			protected int formatTilePath(Tile tile, byte[] path, int curPos) {
				String p = getTileUrl(tile);
				if (p == null)
					return super.formatTilePath(tile, path, curPos);

				byte[] b = p.getBytes();
				System.arraycopy(b, 0, path, curPos, b.length);

				return curPos + b.length;
			}
		};
		return new UrlTileDataSource(this, new BitmapTileDecoder(), conn);
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
