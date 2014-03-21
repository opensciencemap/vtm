package org.oscim.layers.tile;

import org.oscim.renderer.MapRenderer;

import com.badlogic.gdx.Gdx;

public abstract class LoadDelayTask implements Runnable {

	@Override
	public void run() {
		if (MapRenderer.frametime == TileLoader.lastLoadTime) {
			Gdx.app.postRunnable(this);
			return;
		}
		continueLoading();
	}

	public abstract void continueLoading();
}
