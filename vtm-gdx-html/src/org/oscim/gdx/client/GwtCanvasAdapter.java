package org.oscim.gdx.client;

import java.io.InputStream;

import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Paint;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.TextMetrics;

public class GwtCanvasAdapter extends CanvasAdapter {

	public static boolean NO_STROKE_TEXT = false;

	public static final GwtCanvasAdapter INSTANCE = new GwtCanvasAdapter();
	static final Context2d ctx;

	static {
		Canvas canvas = Canvas.createIfSupported();
		canvas.setCoordinateSpaceWidth(1);
		canvas.setCoordinateSpaceHeight(1);
		ctx = canvas.getContext2d();
	}

	static synchronized float getTextWidth(String text, String font) {
		ctx.setFont(font);
		TextMetrics tm = ctx.measureText(text);
		return (float) tm.getWidth();
	}

	@Override
	public Bitmap decodeBitmap(InputStream in) {
		//ImageData data = new ImageData();
		return null;
	}

	@Override
	public Bitmap loadBitmapAsset(String fileName) {
		return new GwtBitmap(fileName);
	}

	@Override
	public int getColor(Color color) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Paint getPaint() {
		return new GwtPaint();
	}

	@Override
	public int parseColor(String colorString) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Bitmap getBitmap(int width, int height, int format) {
		return new GwtBitmap(width, height, format);
	}

	@Override
	public org.oscim.backend.canvas.Canvas getCanvas() {
		return new GwtCanvas();
	}

}
