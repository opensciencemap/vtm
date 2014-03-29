package org.oscim.test;

import org.oscim.backend.canvas.Color;
import org.oscim.core.GeometryBuffer;
import org.oscim.gdx.GdxMap;
import org.oscim.gdx.GdxMapApp;
import org.oscim.layers.GenericLayer;
import org.oscim.renderer.ElementRenderer;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.MapRenderer;
import org.oscim.renderer.elements.LineLayer;
import org.oscim.theme.styles.LineStyle;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

public class LineTest extends GdxMap {

	@Override
	protected boolean onKeyDown(int keycode) {
		if (keycode == Input.Keys.NUM_1) {
			angle++;
			mMap.render();
			return true;

		}

		if (keycode == Input.Keys.NUM_2) {
			angle--;
			mMap.render();
			return true;

		}
		return false;
	}

	float angle = 0;

	@Override
	protected void createLayers() {
		mMap.layers().add(new GenericLayer(mMap, new ElementRenderer() {
			boolean init;

			LineLayer ll = layers.addLineLayer(0,
			                                   new LineStyle(Color.fade(Color.CYAN, 0.5f), 1.5f));

			GeometryBuffer g = new GeometryBuffer(10, 1);

			@Override
			protected void update(GLViewport v) {
				if (!init) {
					mMapPosition.copy(v.pos);
					init = true;

					// g.addPoint(0, 0);
					// g.addPoint(0, 1);
					// g.addPoint(0.1f, 0);
					//
					// g.addPoint(1, 1);
					// g.addPoint(2, 0);
					//
					// g.addPoint(2, 1);
					// g.addPoint(2, 0);
					//
					// g.addPoint(3, 1);
					// g.addPoint(3, 0);
					// g.addPoint(3, 1);
					//
					// for (int i = 0; i < 60; i++){
					// g.startLine();
					// g.addPoint(0, 0);
					// g.addPoint(0, 1);
					// }
					//
					// g.scale(100, 100);
					//
					// ll.addLine(g);
					//
					// compile();
				}

				layers.clear();
				layers.setBaseLayers(ll);
				g.clear();
				for (int i = 0; i < 60; i++) {
					g.startLine();
					g.addPoint(-1, 0);
					g.addPoint(0, 0);
					g.addPoint((float) Math.cos(Math.toRadians(angle)),
					           (float) Math.sin(Math.toRadians(angle)));
				}

				g.scale(100, 100);

				ll.addLine(g);

				compile();

				angle = Gdx.input.getX() / 2f % 360;

				MapRenderer.animate();
			}
		}));
	}

	public static void main(String[] args) {
		GdxMapApp.init();
		GdxMapApp.run(new LineTest(), null, 256);
	}
}
