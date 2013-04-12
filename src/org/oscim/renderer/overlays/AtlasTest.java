package org.oscim.renderer.overlays;

import java.util.Arrays;

import org.oscim.core.MapPosition;
import org.oscim.graphics.Color;
import org.oscim.graphics.Paint.Cap;
import org.oscim.renderer.GLRenderer.Matrices;
import org.oscim.renderer.TextureAtlas;
import org.oscim.renderer.TextureAtlas.Rect;
import org.oscim.renderer.layer.LineLayer;
import org.oscim.theme.renderinstruction.Line;
import org.oscim.view.MapView;

import android.util.Log;

public class AtlasTest extends BasicOverlay {

	public AtlasTest(MapView mapView) {
		super(mapView);

		TextureAtlas mAtlas = TextureAtlas.create(2048, 2048, 1);

		LineLayer ll = layers.getLineLayer(0);
		ll.line = new Line(Color.BLUE, 3, Cap.BUTT);
		ll.width = 1.5f;

		LineLayer ll2 = layers.getLineLayer(1);
		ll2.line = new Line(Color.RED, 3, Cap.BUTT);
		ll2.width = 1.5f;

		float[] points = new float[10];

		for (int i = 0; i < 400; i++) {
			int w = (int) (20 + Math.random() * 256);
			int h = (int) (20 + Math.random() * 56);
			Rect r = mAtlas.getRegion(w, h);
			if (r == null) {
				Log.d("...", "no space left");
				continue;
			}
			points[0] = r.x;
			points[1] = r.y;
			points[2] = r.x + r.w;
			points[3] = r.y;
			points[4] = r.x + r.w;
			points[5] = r.y + r.h;
			points[6] = r.x;
			points[7] = r.y + r.h;
			points[8] = r.x;
			points[9] = r.y;
			ll.addLine(points, 10, false);

			r.x += 2;
			r.y += 2;
			points[0] = r.x;
			points[1] = r.y;
			points[2] = r.x + w;
			points[3] = r.y;
			points[4] = r.x + w;
			points[5] = r.y + h;
			points[6] = r.x;
			points[7] = r.y + h;
			points[8] = r.x;
			points[9] = r.y;

			Log.d("...", "add region: " + Arrays.toString(points));

			ll2.addLine(points, 10, false);

		}
		this.newData = true;
	}

	boolean initial = true;

	@Override
	public void update(MapPosition pos, boolean positionChanged,
			boolean tilesChanged, Matrices m) {

		if (initial) {
			mMapPosition.copy(pos);
			initial = false;
		}
	}
}
