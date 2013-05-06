package org.oscim.renderer.layers.test;

import java.util.Arrays;

import org.oscim.core.MapPosition;
import org.oscim.graphics.Color;
import org.oscim.graphics.Paint.Cap;
import org.oscim.renderer.GLRenderer.Matrices;
import org.oscim.renderer.layers.BasicRenderLayer;
import org.oscim.renderer.sublayers.LineLayer;
import org.oscim.renderer.sublayers.TextItem;
import org.oscim.renderer.sublayers.TextLayer;
import org.oscim.renderer.sublayers.TextureAtlas;
import org.oscim.renderer.sublayers.TextureAtlas.Rect;
import org.oscim.renderer.sublayers.TextureAtlas.Slot;
import org.oscim.theme.renderinstruction.Line;
import org.oscim.theme.renderinstruction.Text;
import org.oscim.view.MapView;

import android.util.Log;

public class AtlasRenderLayer extends BasicRenderLayer {

	public AtlasRenderLayer(MapView mapView) {
		super(mapView);

		TextureAtlas mAtlas = TextureAtlas.create(2048, 2048, 1);

		LineLayer ll = layers.getLineLayer(0);
		ll.line = new Line(Color.BLUE, 3, Cap.BUTT);
		ll.width = 1f;

		LineLayer ll2 = layers.getLineLayer(1);
		ll2.line = new Line(Color.RED, 3, Cap.BUTT);
		ll2.width = 1f;

		LineLayer ll3 = layers.getLineLayer(2);
		ll3.line = new Line(Color.GREEN, 3, Cap.BUTT);
		ll3.width = 1f;

		TextLayer tl = new TextLayer();
		Text t = Text.createText(20, 0, Color.BLACK, 0, false);
		layers.textureLayers = tl;

		float[] points = new float[10];

		for (int i = 0; i < 400; i++) {
			int w = (int) (20 + Math.random() * 256);
			int h = (int) (20 + Math.random() * 56);
			Rect r = mAtlas.getRegion(w, h);
			if (r == null) {
				Log.d("...", "no space left");
				continue;
			}
			r.x += 1;
			r.y += 1;

			points[0] = r.x;
			points[1] = r.y;
			points[2] = r.x + (r.w - 2);
			points[3] = r.y;
			points[4] = r.x + (r.w - 2);
			points[5] = r.y + (r.h - 2);
			points[6] = r.x;
			points[7] = r.y + (r.h - 2);
			points[8] = r.x;
			points[9] = r.y;
			ll.addLine(points, 10, false);

			r.x += 1;
			r.y += 1;
			points[0] = r.x;
			points[1] = r.y;
			points[2] = r.x + (w - 4);
			points[3] = r.y;
			points[4] = r.x + (w - 4);
			points[5] = r.y + (h - 4);
			points[6] = r.x;
			points[7] = r.y + (h - 4);
			points[8] = r.x;
			points[9] = r.y;

			Log.d("...", "add region: " + Arrays.toString(points));
			ll2.addLine(points, 10, false);

			TextItem ti = TextItem.pool.get();
			ti.set(r.x + r.w / 2, r.y + r.h / 2, "" + i, t);

			ti.x1 = 0;
			ti.y1 = 1; // (short) (size / 2);
			ti.x2 = 1; // (short) size;
			ti.y2 = 1;
			tl.addText(ti);
		}

		for (Slot s = mAtlas.mSlots; s != null; s = s.next) {
			points[0] = s.x;
			points[1] = s.y;
			points[2] = s.x + s.w;
			points[3] = s.y;
			points[4] = s.x + s.w;
			points[5] = 2048;
			points[6] = s.x;
			points[7] = 2048;
			points[8] = s.x;
			points[9] = s.y;

			ll3.addLine(points, 10, false);
		}

		tl.prepare();
		TextItem.pool.releaseAll(tl.labels);

		this.newData = true;
	}

	boolean initial = true;

	@Override
	public void update(MapPosition pos, boolean changed, Matrices m) {

		if (initial) {
			mMapPosition.copy(pos);
			this.initial = false;
		}
	}
}
