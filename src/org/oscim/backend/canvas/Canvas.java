package org.oscim.backend.canvas;


public interface Canvas {

	void setBitmap(Bitmap bitmap);

	void drawText(String string, float f, float yy, Paint stroke);

	void drawBitmap(Bitmap bitmap, float x, float y);

}
