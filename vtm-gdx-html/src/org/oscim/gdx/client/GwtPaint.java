package org.oscim.gdx.client;

import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Paint;

import com.badlogic.gdx.graphics.Pixmap;

public class GwtPaint implements Paint {

	String color;
	boolean stroke;

	float strokeWidth;
	float fontSize;
	Align mAlign;

	//String font = "12px sans-serif";
	String font = "13px Helvetica";

	//private int cap;

	@Override
	public int getColor() {
		return 0;
	}

	@Override
	public int getTextHeight(String text) {
		return 0;
	}

	@Override
	public int getTextWidth(String text) {
		return 0;
	}

	@Override
	public void setBitmapShader(Bitmap bitmap) {
	}

	@Override
	public void setColor(int color) {
		float a = ((color >>> 24) & 0xff) / 255f;
		int r = (color >>> 16) & 0xff;
		int g = (color >>> 8) & 0xff;
		int b = (color & 0xff) ;

		this.color = Pixmap.make(r, g, b, a);
	}

	@Override
	public void setDashPathEffect(float[] strokeDasharray) {
	}

	@Override
	public void setStrokeCap(Cap cap) {
		stroke = true;
		// TODO
	}

	@Override
	public void setStrokeWidth(float width) {
		stroke = true;
		strokeWidth = width;
	}

	@Override
	public void setStyle(Style style) {
	}

	@Override
	public void setTextAlign(Align align) {
		mAlign = align;
	}

	@Override
	public void setTextSize(float size) {
		fontSize = size;
	}

	@Override
	public void setTypeface(FontFamily fontFamily, FontStyle fontStyle) {

	}

	@Override
	public float measureText(String text) {
		return GwtCanvasAdapter.getTextWidth(text, font);
	}

	@Override
	public float getFontHeight() {
		return 14 + strokeWidth * 2;
	}

	@Override
	public float getFontDescent() {
		return 4 + strokeWidth;
	}

}
