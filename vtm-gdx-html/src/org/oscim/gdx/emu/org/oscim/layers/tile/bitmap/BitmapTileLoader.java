package org.oscim.layers.tile.bitmap;

import static org.oscim.layers.tile.MapTile.State.CANCEL;

import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.MapElement;
import org.oscim.core.Tile;
import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.TileLoader;
import org.oscim.layers.tile.TileManager;
import org.oscim.renderer.elements.BitmapLayer;
import org.oscim.renderer.elements.ElementLayers;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.ITileDataSource.QueryResult;
import org.oscim.tiling.TileSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BitmapTileLoader extends TileLoader implements ITileDataSink {

	protected static final Logger log = LoggerFactory.getLogger(BitmapTileLoader.class);

	private final ITileDataSource mTileDataSource;
	private MapTile mTile;

	public BitmapTileLoader(TileManager tileManager, TileSource tileSource) {
		super(tileManager);
		mTileDataSource = tileSource.getDataSource();
	}

	@Override
	public void cleanup() {
		mTile = null;
	}

	@Override
	protected boolean executeJob(MapTile tile) {
		mTile = tile;
		//QueryResult result = null;
		//try {
		if (mTileDataSource.executeQuery(tile, this) != QueryResult.SUCCESS) {
			return false;
		}
		//} 
		//		catch (CancellationException e) {
		//			log.debug("{} was canceled", mTile);
		//		} catch (Exception e) {
		//			log.debug("{} {}", mTile, e.getMessage());
		//		} finally {
		//			mTile = null;
		//		}
		return true;
		//return result == QueryResult.SUCCESS;
	}

	@Override
	public void setTileImage(Bitmap bitmap) {
		if (isCanceled() || mTile.state(CANCEL))
			return;
		//throw new CancellationException();

		BitmapLayer l = new BitmapLayer(false);
		l.setBitmap(bitmap, Tile.SIZE, Tile.SIZE);
		mTile.layers = new ElementLayers();
		mTile.layers.setTextureLayers(l);
	}

	@Override
	public void process(MapElement element) {

	}

	@Override
	public void completed(boolean success) {
		jobCompleted(mTile, success);
		mTile = null;
	}
}
