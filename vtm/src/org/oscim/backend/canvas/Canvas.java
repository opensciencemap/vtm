package org.oscim.backend.canvas;

public interface Canvas {

	void setBitmap(Bitmap bitmap);

	void drawText(String string, float x, float y, Paint stroke);

	void drawBitmap(Bitmap bitmap, float x, float y);

}
