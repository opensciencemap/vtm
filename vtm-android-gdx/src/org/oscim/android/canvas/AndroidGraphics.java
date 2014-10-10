/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package org.oscim.android.canvas;

import java.io.IOException;
import java.io.InputStream;

import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Canvas;
import org.oscim.backend.canvas.Paint;
import org.oscim.layers.marker.MarkerItem.HotspotPlace;
import org.oscim.layers.marker.MarkerSymbol;

import android.content.res.Resources;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public final class AndroidGraphics extends CanvasAdapter {

	public static void init() {
		CanvasAdapter.init(new AndroidGraphics());
	}

	public static android.graphics.Paint getAndroidPaint(Paint paint) {
		return ((AndroidPaint) paint).mPaint;
	}

	private AndroidGraphics() {
		// do nothing
	}

	@Override
	public Bitmap decodeBitmapImpl(InputStream inputStream) {
		return new AndroidBitmap(inputStream);
	}

	@Override
	public Bitmap loadBitmapAssetImpl(String fileName) {
		try {
			return createBitmap(fileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Paint newPaintImpl() {
		return new AndroidPaint();
	}

	@Override
	public Bitmap newBitmapImpl(int width, int height, int format) {
		return new AndroidBitmap(width, height, format);
	}

	@Override
	public Canvas newCanvasImpl() {
		return new AndroidCanvas();
	}

	//-------------------------------------
	public static Bitmap drawableToBitmap(Drawable drawable) {
		if (drawable instanceof BitmapDrawable) {
			return new AndroidBitmap(((BitmapDrawable) drawable).getBitmap());
		}

		android.graphics.Bitmap bitmap = android.graphics.Bitmap
		    .createBitmap(drawable.getIntrinsicWidth(),
		                  drawable.getIntrinsicHeight(),
		                  Config.ARGB_8888);

		android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);

		return new AndroidBitmap(bitmap);
	}

	public static Bitmap drawableToBitmap(Resources res, int resId) {
		return new AndroidBitmap(res.openRawResource(resId));
	}

	/**
	 * @deprecated
	 */
	public static MarkerSymbol makeMarker(Drawable drawable, HotspotPlace place) {
		if (place == null)
			place = HotspotPlace.CENTER;

		return new MarkerSymbol(drawableToBitmap(drawable), place);
	}

	/**
	 * @deprecated
	 */
	public static MarkerSymbol makeMarker(Resources res, int resId, HotspotPlace place) {
		if (place == null)
			place = HotspotPlace.CENTER;

		InputStream in = res.openRawResource(resId);
		return new MarkerSymbol(new AndroidBitmap(in), place);
	}
}
