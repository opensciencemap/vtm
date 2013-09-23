/*
 * Copyright 2012 Hannes Janetzek
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.oscim.renderer;

import org.oscim.backend.canvas.Color;
import org.oscim.backend.canvas.Paint.Cap;
import org.oscim.core.GeometryBuffer;
import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.renderer.MapRenderer.Matrices;
import org.oscim.renderer.elements.LineLayer;
import org.oscim.renderer.elements.TextItem;
import org.oscim.renderer.elements.TextLayer;
import org.oscim.theme.renderinstruction.Line;
import org.oscim.theme.renderinstruction.Text;

public class GridRenderer extends ElementRenderer {
	// private final static String TILE_FORMAT = "%d/%d/%d";
	private final TextLayer mTextLayer;
	private final Text mText;

	private final LineLayer mLineLayer;

	private final GeometryBuffer mLines;

	private int mCurX, mCurY, mCurZ;

	public GridRenderer() {

		int size = Tile.SIZE;

		// not needed to set but we know:
		// 16 lines 'a' two points
		mLines = new GeometryBuffer(2 * 16, 16);

		float pos = -size * 4;

		// 8 vertical lines
		for (int i = 0; i < 8; i++) {
			float x = pos + i * size;
			mLines.startLine();
			mLines.addPoint(x, pos);
			mLines.addPoint(x, pos + size * 8);
		}

		// 8 horizontal lines
		for (int j = 0; j < 8; j++) {
			float y = pos + j * size;
			mLines.startLine();
			mLines.addPoint(pos, y);
			mLines.addPoint(pos + size * 8, y);
		}

		mText = Text.createText(22, 0, Color.RED, 0, false);

		mTextLayer = layers.addTextLayer(new TextLayer());
		mLineLayer = layers.addLineLayer(0, new Line(0x66000066, 1.5f, Cap.BUTT));
	}

	private void addLabels(int x, int y, int z) {
		int s = Tile.SIZE;

		TextLayer tl = mTextLayer;
		tl.clear();

		for (int yy = -2; yy < 2; yy++) {
			for (int xx = -2; xx < 2; xx++) {

				// String label = String.format(
				// Locale.ROOT, TILE_FORMAT,
				// Integer.valueOf(x + xx),
				// Integer.valueOf(y + yy),
				// Integer.valueOf(z));
				String label = Integer.valueOf(x + xx) + "/" +
				        Integer.valueOf(y + yy) + "/" +
				        Integer.valueOf(z);

				TextItem ti = TextItem.pool.get();
				ti.set(s * xx + s / 2, s * yy + s / 2, label, mText);

				tl.addText(ti);
			}
		}

		// render TextItems to a bitmap and prepare vertex buffer data.
		tl.prepare();

		// release TextItems
		tl.clearLabels();
	}

	@Override
	protected void update(MapPosition pos, boolean changed, Matrices m) {

		// scale coordinates relative to current 'zoom-level' to
		// get the position as the nearest tile coordinate
		int z = 1 << pos.zoomLevel;
		int x = (int) (pos.x * z);
		int y = (int) (pos.y * z);

		// update layers when map moved by at least one tile
		if (x == mCurX && y == mCurY && z == mCurZ)
			return;

		mCurX = x;
		mCurY = y;
		mCurZ = z;

		MapPosition layerPos = mMapPosition;
		layerPos.copy(pos);
		layerPos.x = (double) x / z;
		layerPos.y = (double) y / z;
		layerPos.scale = z;

		addLabels(x, y, pos.zoomLevel);

		mLineLayer.clear();
		mLineLayer.addLine(mLines);

		compile();
	}
}
