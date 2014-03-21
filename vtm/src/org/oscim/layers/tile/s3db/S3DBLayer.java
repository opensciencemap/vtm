package org.oscim.layers.tile.s3db;

import java.util.concurrent.CancellationException;

import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.GeometryBuffer.GeometryType;
import org.oscim.core.MapElement;
import org.oscim.core.MercatorProjection;
import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.TileLayer;
import org.oscim.layers.tile.TileLoader;
import org.oscim.layers.tile.TileManager;
import org.oscim.layers.tile.TileRenderer;
import org.oscim.map.Map;
import org.oscim.renderer.ExtrusionRenderer;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.elements.ElementLayers;
import org.oscim.renderer.elements.ExtrusionLayer;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.TileSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S3DBLayer extends TileLayer {
	static final Logger log = LoggerFactory.getLogger(S3DBLayer.class);

	private final static int MAX_CACHE = 20;
	private final static int SRC_ZOOM = 16;

	private final TileSource mTileSource;

	public S3DBLayer(Map map, TileSource tileSource) {
		super(map,
		      new TileManager(map, SRC_ZOOM, SRC_ZOOM, MAX_CACHE),
		      new S3DBRenderer());

		mTileSource = tileSource;
		initLoader(2);
	}

	@Override
	protected S3DBLoader createLoader() {
		return new S3DBLoader(getManager());
	}

	static class S3DBRenderer extends TileRenderer {
		ExtrusionRenderer mExtRenderer;

		public S3DBRenderer() {
			mExtRenderer = new ExtrusionRenderer(this, 16);
		}

		@Override
		protected synchronized void update(GLViewport v) {
			super.update(v);
			mExtRenderer.update(v);
			setReady(mExtRenderer.isReady());
		}

		@Override
		protected synchronized void render(GLViewport v) {
			mExtRenderer.render(v);
		}
	}

	class S3DBLoader extends TileLoader {

		private MapTile mTile;

		/** current TileDataSource used by this MapTileLoader */
		private ITileDataSource mTileDataSource;

		private ExtrusionLayer mExtrusionLayer;

		public S3DBLoader(TileManager tileManager) {
			super(tileManager);
			mTileDataSource = mTileSource.getDataSource();
		}

		@Override
		public void cleanup() {
			mTileDataSource.destroy();
		}

		@Override
		protected boolean loadTile(MapTile tile) {
			mTile = tile;

			double lat = MercatorProjection.toLatitude(tile.y);
			float groundScale = (float) MercatorProjection
			    .groundResolution(lat, 1 << mTile.zoomLevel);

			mExtrusionLayer = new ExtrusionLayer(0, groundScale, null);
			ElementLayers layers = new ElementLayers();
			layers.setExtrusionLayers(mExtrusionLayer);
			tile.data = layers;

			try {
				/* query database, which calls process() callback */
				mTileDataSource.query(mTile, this);
			} catch (CancellationException e) {
				log.debug("{}", e);
				return false;
			} catch (Exception e) {
				log.debug("{}", e);
				return false;
			}

			return true;
		}

		@Override
		public void process(MapElement element) {
			if (element.type == GeometryType.TRIS) {
				mExtrusionLayer.add(element);
			}
		}

		@Override
		public void completed(QueryResult result) {
			mExtrusionLayer = null;
			super.completed(result);
		}

		@Override
		public void setTileImage(Bitmap bitmap) {

		}
	}
}
