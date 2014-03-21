package org.oscim.layers.tile.s3db;

import org.oscim.backend.canvas.Color;
import org.oscim.layers.tile.TileLayer;
import org.oscim.layers.tile.TileManager;
import org.oscim.layers.tile.TileRenderer;
import org.oscim.map.Map;
import org.oscim.renderer.ExtrusionRenderer;
import org.oscim.renderer.GLViewport;
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
	protected S3DBTileLoader createLoader() {
		return new S3DBTileLoader(getManager(), mTileSource);
	}

	static class S3DBRenderer extends TileRenderer {
		ExtrusionRenderer mExtRenderer;

		public S3DBRenderer() {
			mExtRenderer = new ExtrusionRenderer(this, 16, true, false);
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

	/**
	 * Colors from OSM2World, except roof-red
	 */
	static int getColor(String color, boolean roof) {

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
