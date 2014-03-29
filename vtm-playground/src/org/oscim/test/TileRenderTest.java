package org.oscim.test;

import org.oscim.core.MercatorProjection;
import org.oscim.gdx.GdxMap;
import org.oscim.gdx.GdxMapApp;
import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.MapTile.TileNode;
import org.oscim.layers.tile.TileLoader;
import org.oscim.layers.tile.TileManager;
import org.oscim.layers.tile.TileSet;
import org.oscim.layers.tile.VectorTileRenderer;
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.VectorTileLoader;
import org.oscim.renderer.MapRenderer;
import org.oscim.theme.DebugTheme;
import org.oscim.tiling.TileSource;
import org.oscim.tiling.source.bitmap.DefaultSources;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;

public class TileRenderTest extends GdxMap {

	static boolean loadOneTile = true;
	static int tileX = 34365 >> 2;
	static int tileY = 21333 >> 2;
	static byte tileZ = 14;

	@Override
	public void createLayers() {
		final TileManager tileManager;

		MapRenderer.setBackgroundColor(0xff888888);
		MapTile tile = new MapTile(null, tileX, tileY, tileZ);

		double w = 1.0 / (1 << tile.zoomLevel);
		double minLon = MercatorProjection.toLongitude(tile.x);
		double maxLon = MercatorProjection.toLongitude(tile.x + w);
		double minLat = MercatorProjection.toLatitude(tile.y + w);
		double maxLat = MercatorProjection.toLatitude(tile.y);

		mMap.setMapPosition(minLat + (maxLat - minLat) / 2, minLon
		        + (maxLon - minLon) / 2, 1 << tile.zoomLevel);

		// mMap.setMapPosition(53.0521, 8.7951, 1 << 15);

		if (loadOneTile) {

			tile = new MapTile(new TileNode(), tileX, tileY, tileZ);
			// setup tile quad-tree, expected for locking
			// tile.node= new ;
			tile.node.parent = tile.node;
			tile.node.parent.parent = tile.node;

			// setup TileSet contatining one tile
			final TileSet tiles = new TileSet();
			tiles.cnt = 1;
			tiles.tiles[0] = tile;
			tiles.lockTiles();

			tileManager = new TileManager(mMap, 0, 32, 100) {
				@Override
				public boolean getActiveTiles(TileSet tileSet) {
					if (tileSet == null)
						tileSet = new TileSet(1);

					tileSet.setTiles(tiles);
					return true;
				}

				@Override
				public void releaseTiles(TileSet tileSet) {
					tileSet.releaseTiles();
				}
			};
		} else {
			// create TileManager and calculate tiles for current position
			tileManager = new TileManager(mMap, 0, 32, 100);
			tileManager.init();
			tileManager.update(mMap.getMapPosition());
		}

		final VectorTileLoader[] tileLoader = { null };

		VectorTileLayer l = new VectorTileLayer(mMap, tileManager,
		                                        new VectorTileRenderer(), 1) {

			protected TileLoader createLoader() {
				tileLoader[0] = new VectorTileLoader(this) {

					public boolean loadTile(MapTile tile) {
						mTile = tile;
						return super.loadTile(tile);
					}
				};
				return tileLoader[0];
			};
		};

		TileSource tileSource = new OSciMap4TileSource();

		l.setTileSource(tileSource);

		//IRenderTheme theme = ThemeLoader.load(VtmThemes.TRONRENDER);
		//l.setRenderTheme(theme);
		l.setRenderTheme(new DebugTheme());

		if (loadOneTile) {
			tileLoader[0].loadTile(tile);
			tileManager.jobCompleted(tile, true);
		} else {
			MapTile t;
			while ((t = tileManager.getTileJob()) != null) {
				tileLoader[0].loadTile(t);
				tileManager.jobCompleted(t, true);
			}
		}

		mMap.setBackgroundMap(new BitmapTileLayer(mMap, new DefaultSources.StamenToner()));

		mMap.layers().add(l);
	}

	public static void main(String[] args) {
		GdxMapApp.init();
		GdxMapApp.run(new TileRenderTest(), null, 256);
	}
}
