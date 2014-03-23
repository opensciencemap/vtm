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

	static int getColor(String color, boolean roof) {

		if (color.charAt(0) == '#')
			return Color.parseColor(color, Color.CYAN);

		if (roof) {
			if ("brown" == color)
				return Color.get(120, 110, 110);
			if ("red" == color)
				return Color.get(255, 87, 69);
			if ("green" == color)
				return Color.get(150, 200, 130);
			if ("blue" == color)
				return Color.get(100, 50, 200);
		}
		if ("white" == color)
			return Color.get(240, 240, 240);
		if ("black" == color)
			return Color.get(76, 76, 76);
		if ("grey" == color || "gray" == color)
			return Color.get(100, 100, 100);
		if ("red" == color)
			return Color.get(255, 190, 190);
		if ("green" == color)
			return Color.get(190, 255, 190);
		if ("blue" == color)
			return Color.get(190, 190, 255);
		if ("yellow" == color)
			return Color.get(255, 255, 175);
		if ("pink" == color)
			return Color.get(225, 175, 225);
		if ("orange" == color)
			return Color.get(255, 225, 150);
		if ("brown" == color)
			return Color.get(170, 130, 80);
		if ("silver" == color)
			return Color.get(153, 157, 160);
		if ("gold" == color)
			return Color.get(255, 215, 0);
		if ("darkgray" == color || "darkgrey" == color)
			return Color.DKGRAY;
		if ("lightgray" == color || "lightgrey" == color)
			return Color.LTGRAY;
		if ("lightblue" == color)
			return Color.get(173, 216, 230);
		if ("beige" == color)
			return Color.get(245, 245, 220);
		if ("darkblue" == color)
			return Color.get(50, 50, 189);
		if ("transparent" == color)
			return Color.get(64, 64, 64, 64);

		log.debug("unknown color:{}", color);
		return 0;
	}

	static int getMaterialColor(String material, boolean roof) {

		if (roof) {
			if ("glass" == material)
				return Color.fade(Color.get(130, 224, 255), 0.9f);
		}
		if ("roof_tiles" == material)
			return Color.get(216, 167, 111);
		if ("tile" == material)
			return Color.get(216, 167, 111);

		if ("concrete" == material ||
		        "cement_block" == material)
			return Color.get(210, 212, 212);

		if ("metal" == material)
			return Color.get(170, 130, 80);
		if ("tar_paper" == material)
			return Color.get(170, 130, 80);
		if ("eternit" == material)
			return Color.get(216, 167, 111);
		if ("tin" == material)
			return Color.get(170, 130, 80);
		if ("asbestos" == material)
			return Color.get(160, 152, 141);
		if ("glass" == material)
			return Color.get(130, 224, 255);
		if ("slate" == material)
			return Color.get(170, 130, 80);
		if ("zink" == material)
			return Color.get(180, 180, 180);
		if ("gravel" == material)
			return Color.get(170, 130, 80);
		if ("copper" == material)
			// same as roof color:green
			return Color.get(150, 200, 130);
		if ("wood" == material)
			return Color.get(170, 130, 80);
		if ("grass" == material)
			return Color.get(170, 130, 80);
		if ("stone" == material)
			return Color.get(206, 207, 181);
		if ("plaster" == material)
			return Color.get(236, 237, 181);
		if ("brick" == material)
			return Color.get(255, 217, 191);
		if ("stainless_steel" == material)
			return Color.get(153, 157, 160);

		log.debug("unknown material:{}", material);

		return 0;
	}
}
