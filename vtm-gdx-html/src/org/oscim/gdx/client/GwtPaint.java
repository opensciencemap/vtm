package org.oscim.gdx.client;

import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Paint;

import com.badlogic.gdx.graphics.Pixmap;

public class GwtPaint implements Paint {

	String color;
	boolean stroke;

	float strokeWidth;
	Align mAlign;

	float fontSize = 12;

	private FontStyle fontStyle = FontStyle.NORMAL;
	private FontFamily fontFamily = FontFamily.DEFAULT;

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
		buildFont();
	}

	@Override
	public void setTypeface(FontFamily fontFamily, FontStyle fontStyle) {
		this.fontStyle = fontStyle;
		this.fontFamily = fontFamily;
		buildFont();
	}

	@Override
	public float measureText(String text) {
		return GwtCanvasAdapter.getTextWidth(text, font);
	}

	// FIXME all estimates. no idea how to properly measure canvas text..
	@Override
	public float getFontHeight() {
		return 2 + fontSize + strokeWidth * 2;
	}

	@Override
	public float getFontDescent() {
		return 4 + strokeWidth;
	}

	void buildFont(){
		StringBuilder sb = new StringBuilder();

		if (this.fontStyle == FontStyle.BOLD)
			sb.append("bold ");
		else if (this.fontStyle == FontStyle.ITALIC)
			sb.append("italic ");

		sb.append(Math.round(this.fontSize));
		sb.append("px ");

		sb.append("Helvetica");

		this.font = sb.toString();

	}
}
