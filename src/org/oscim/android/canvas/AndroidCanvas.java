package org.oscim.android.canvas;

import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Canvas;
import org.oscim.backend.canvas.Paint;

public class AndroidCanvas implements Canvas {
	final android.graphics.Canvas canvas;

	public AndroidCanvas() {
		this.canvas = new android.graphics.Canvas();
	}
	@Override
	public void setBitmap(Bitmap bitmap) {
		this.canvas.setBitmap(((AndroidBitmap)bitmap).mBitmap);
	}

	@Override
	public void drawText(String string, float x, float y, Paint stroke) {
		this.canvas.drawText(string, x, y, ((AndroidPaint)stroke).mPaint);

	}
	@Override
	public void drawBitmap(Bitmap bitmap, float x, float y) {
		this.canvas.drawBitmap(((AndroidBitmap)bitmap).mBitmap, x, y, null);

	}

}
