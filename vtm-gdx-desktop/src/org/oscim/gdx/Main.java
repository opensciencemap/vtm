package org.oscim.gdx;

import org.oscim.awt.AwtGraphics;
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.GLAdapter;
import org.oscim.core.Tile;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.utils.SharedLibraryLoader;

public class Main {

	public static void main(String[] args) {
		LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
		cfg.title = "vtm-gdx";
		cfg.useGL20 = true;
		cfg.width = 1280;
		cfg.height = 800;
		cfg.stencil= 8;
		cfg.foregroundFPS = 20;
		//cfg.samples = 4;

		// set our globals
        CanvasAdapter.g = AwtGraphics.INSTANCE;
        GLAdapter.g = new GdxGLAdapter();
		Tile.SIZE = 256;

        new SharedLibraryLoader().load("vtm-jni");

		new LwjglApplication(new GdxMap(), cfg);
	}
}
