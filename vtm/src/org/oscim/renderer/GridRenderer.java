/*
 * Copyright 2012 Hannes Janetzek
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
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
import org.oscim.core.Tile;
import org.oscim.renderer.elements.LineLayer;
import org.oscim.renderer.elements.TextItem;
import org.oscim.renderer.elements.TextLayer;
import org.oscim.theme.styles.LineStyle;
import org.oscim.theme.styles.TextStyle;
import org.oscim.theme.styles.TextStyle.TextBuilder;

public class GridRenderer extends ElementRenderer {
	private final TextLayer mTextLayer;
	private final TextStyle mText;
	private final LineLayer mLineLayer;
	private final GeometryBuffer mLines;
	private final StringBuilder mStringBuffer;

	private int mCurX, mCurY, mCurZ;

	public GridRenderer() {
		this(1, new LineStyle(Color.LTGRAY, 1.2f, Cap.BUTT),
		     new TextBuilder().setFontSize(22).setColor(Color.RED).build());
	}

	public GridRenderer(int numLines, LineStyle lineStyle, TextStyle textStyle) {
		int size = Tile.SIZE;

		// not needed to set but we know:
		// 16 lines 'a' two points
		mLines = new GeometryBuffer(2 * 16, 16);

		float pos = -size * 4;

		// 8 vertical lines
		for (int i = 0; i < 8 * numLines; i++) {
			float x = pos + i * size / numLines;
			mLines.startLine();
			mLines.addPoint(x, pos);
			mLines.addPoint(x, pos + size * 8);
		}

		// 8 horizontal lines
		for (int j = 0; j < 8 * numLines; j++) {
			float y = pos + j * size / numLines;
			mLines.startLine();
			mLines.addPoint(pos, y);
			mLines.addPoint(pos + size * 8, y);
		}

		mText = textStyle;

		if (mText != null) {
			mTextLayer = layers.addTextLayer(new TextLayer());
		} else {
			mTextLayer = null;
		}

		mLineLayer = layers.addLineLayer(0, lineStyle);
		mLineLayer.addLine(mLines);

		mStringBuffer = new StringBuilder(32);
	}

	private void addLabels(int x, int y, int z) {
		int s = Tile.SIZE;

		TextLayer tl = mTextLayer;
		tl.clear();

		StringBuilder sb = mStringBuffer;

		for (int yy = -2; yy < 2; yy++) {
			for (int xx = -2; xx < 2; xx++) {

				sb.setLength(0);
				sb.append(x + xx)
				    .append(" / ")
				    .append(y + yy)
				    .append(" / ")
				    .append(z);

				TextItem ti = TextItem.pool.get();
				ti.set(s * xx + s / 2, s * yy + s / 2, sb.toString(), mText);

				tl.addText(ti);
			}
		}

		// render TextItems to a bitmap and prepare vertex buffer data.
		tl.prepare();

		// release TextItems
		tl.clearLabels();
	}

	@Override
	protected void update(GLViewport v) {

		// scale coordinates relative to current 'zoom-level' to
		// get the position as the nearest tile coordinate
		int z = 1 << v.pos.zoomLevel;
		int x = (int) (v.pos.x * z);
		int y = (int) (v.pos.y * z);

		// update layers when map moved by at least one tile
		if (x == mCurX && y == mCurY && z == mCurZ)
			return;

		mCurX = x;
		mCurY = y;
		mCurZ = z;

		mMapPosition.copy(v.pos);
		mMapPosition.x = (double) x / z;
		mMapPosition.y = (double) y / z;
		mMapPosition.scale = z;

		if (mText != null) {
			addLabels(x, y, v.pos.zoomLevel);
			layers.setBaseLayers(mLineLayer);
			mLineLayer.addLine(mLines);
			compile();

		} else if (layers.vbo == null) {
			compile();
		}
	}
}
