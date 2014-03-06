package org.oscim.layers;

import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.TileLoader;
import org.oscim.layers.tile.TileManager;
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.map.Map;
import org.oscim.tiling.source.bitmap.BitmapTileSource;

public class JeoTileLayer extends BitmapTileLayer {

	public JeoTileLayer(Map map, BitmapTileSource tileSource) {
		super(map, tileSource);
	}

	@Override
	protected TileLoader createLoader(TileManager tm) {
		return new TileLoader(tm) {

			@Override
			public void cleanup() {
				// TODO Auto-generated method stub

			}

			@Override
			protected boolean executeJob(MapTile tile) {
				// TODO Auto-generated method stub
				return false;
			}

		};
	}

}
