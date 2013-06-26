package org.oscim.gdx.client;

import org.oscim.backend.GL20;
import org.oscim.backend.GLAdapter;
import org.oscim.gdx.GdxMap;

import com.badlogic.gdx.Gdx;

public class GwtGdxMap extends GdxMap {

	@Override
	public void create() {
		GLAdapter.INSTANCE = (GL20)Gdx.graphics.getGL20(); //(GL20)Gdx.gl20;
		super.create();
	}
}
