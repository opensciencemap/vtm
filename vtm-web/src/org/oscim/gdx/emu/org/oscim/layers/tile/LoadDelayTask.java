package org.oscim.layers.tile;

import com.badlogic.gdx.Gdx;

import org.oscim.renderer.MapRenderer;
import org.oscim.tiling.ITileDataSink;

public abstract class LoadDelayTask<T> implements Runnable {
    protected final MapTile tile;
    protected final ITileDataSink sink;
    protected final T data;

    public LoadDelayTask(MapTile tile, ITileDataSink sink, T data) {
        this.tile = tile;
        this.sink = sink;
        this.data = data;
    }

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
