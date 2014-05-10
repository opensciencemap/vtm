/*
 * Copyright 2013 Hannes Janetzek
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
package org.oscim.gdx.client;

import java.io.InputStream;

import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Paint;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.TextMetrics;

public class GwtGdxGraphics extends CanvasAdapter {

	public static boolean NO_STROKE_TEXT = false;

	static final Context2d ctx;

	static {
		Canvas canvas = Canvas.createIfSupported();
		canvas.setCoordinateSpaceWidth(1);
		canvas.setCoordinateSpaceHeight(1);
		ctx = canvas.getContext2d();
	}

	public static synchronized float getTextWidth(String text, String font) {
		ctx.setFont(font);
		TextMetrics tm = ctx.measureText(text);
		return (float) tm.getWidth();
	}

	@Override
	public Bitmap decodeBitmapImpl(InputStream in) {
		//ImageData data = new ImageData();
		return null;
	}

	@Override
	public Bitmap loadBitmapAssetImpl(String fileName) {
		return new GwtBitmap(fileName);
	}

	@Override
	public Paint newPaintImpl() {
		return new GwtPaint();
	}

	@Override
	public Bitmap newBitmapImpl(int width, int height, int format) {
		return new GwtBitmap(width, height, format);
	}

	@Override
	public org.oscim.backend.canvas.Canvas newCanvasImpl() {
		return new GwtCanvas();
	}

	public static void init() {
		CanvasAdapter.init(new GwtGdxGraphics());
	}
}
