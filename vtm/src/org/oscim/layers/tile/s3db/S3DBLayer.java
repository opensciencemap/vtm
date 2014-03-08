package org.oscim.layers.tile.s3db;

import java.util.concurrent.CancellationException;

import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Color;
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
			mExtRenderer = new ExtrusionRenderer(this, 16, true);
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

		private ExtrusionLayer mLayers;
		private ExtrusionLayer mRoofs;

		private float mGroundScale;

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
			mGroundScale = (float) MercatorProjection
			    .groundResolution(lat, 1 << mTile.zoomLevel);

			mLayers = new ExtrusionLayer(0, mGroundScale, Color.get(255, 255, 250));
			mRoofs = new ExtrusionLayer(0, mGroundScale, Color.get(218, 220, 220));
			mLayers.next = mRoofs;

			ElementLayers layers = new ElementLayers();
			layers.setExtrusionLayers(mLayers);
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

		String COLOR_KEY = "c";
		String ROOF_KEY = "roof";

		@Override
		public void process(MapElement element) {
			//log.debug("TAG {}", element.tags);
			if (element.type != GeometryType.TRIS) {
				log.debug("wrong type " + element.type);
				return;
			}
			boolean isRoof = element.tags.containsKey(ROOF_KEY);

			int c = 0;
			if (element.tags.containsKey(COLOR_KEY)) {
				c = getColor(element.tags.getValue(COLOR_KEY), isRoof);
			}

			if (c == 0) {
				if (isRoof)
					mRoofs.add(element);
				else
					mLayers.add(element);
				return;
			}

			for (ExtrusionLayer l = mLayers; l != null; l = (ExtrusionLayer) l.next) {
				if (l.color == c) {
					l.add(element);
					return;
				}
			}
			ExtrusionLayer l = new ExtrusionLayer(0, mGroundScale, c);

			l.next = mRoofs.next;
			mRoofs.next = l;

			l.add(element);
		}

		@Override
		public void completed(QueryResult result) {
			super.completed(result);
		}

		@Override
		public void setTileImage(Bitmap bitmap) {

		}
	}

	/**
	 * Colors from OSM2World, except roof-red
	 */
	private int getColor(String color, boolean roof) {

		try {
			return Color.parseColor(color);
		} catch (Exception e) {

		}

		if (roof) {
			if ("brown".equals(color))
				return Color.get(120, 110, 110);
			if ("red".equals(color))
				return Color.get(255, 87, 69);
			if ("green".equals(color))
				return Color.get(150, 200, 130);
			if ("blue".equals(color))
				return Color.get(100, 50, 200);
		}
		if ("white".equals(color))
			return Color.get(240, 240, 240);
		if ("black".equals(color))
			return Color.get(76, 76, 76);
		if ("grey".equals(color))
			return Color.get(100, 100, 100);
		if ("red".equals(color))
			return Color.get(255, 190, 190);
		if ("green".equals(color))
			return Color.get(190, 255, 190);
		if ("blue".equals(color))
			return Color.get(190, 190, 255);
		if ("yellow".equals(color))
			return Color.get(255, 255, 175);
		if ("pink".equals(color))
			return Color.get(225, 175, 225);
		if ("orange".equals(color))
			return Color.get(255, 225, 150);
		if ("brown".equals(color))
			return Color.get(170, 130, 80);

		return 0;

	}
}
