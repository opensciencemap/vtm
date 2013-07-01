package org.oscim.gdx.client;

import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.GL20;
import org.oscim.backend.GLAdapter;
import org.oscim.gdx.GdxMap;
import org.oscim.tilesource.TileSource;

import com.badlogic.gdx.Gdx;

public class GwtGdxMap extends GdxMap {

	public GwtGdxMap(TileSource tileSource) {
		super(tileSource);
	}

	@Override
	public void create() {
		CanvasAdapter.g = GwtCanvasAdapter.INSTANCE;
		GLAdapter.g = (GL20)Gdx.graphics.getGL20();
		GLAdapter.GDX_WEBGL_QUIRKS = true;

		//GLAdapter.NON_PREMUL_CANVAS = true;
		//Gdx.app.setLogLevel(Application.LOG_DEBUG);

		super.create();
	}
}
