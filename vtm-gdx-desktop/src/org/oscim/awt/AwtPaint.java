package org.oscim.awt;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.font.TextAttribute;
import java.text.AttributedCharacterIterator.Attribute;
import java.util.HashMap;
import java.util.Map;

//import org.oscim.graphics.Align;
import org.oscim.backend.canvas.Bitmap;
//import org.oscim.graphics.Cap;
//import org.oscim.graphics.FontFamily;
//import org.oscim.graphics.FontStyle;
import org.oscim.backend.canvas.Paint;
//import org.oscim.graphics.Style;

public class AwtPaint implements Paint {
	static final Font defaultFont;
	static {
		Map<Attribute, Object> textAttributes = new HashMap<Attribute, Object>();
		textAttributes.put(TextAttribute.KERNING, TextAttribute.KERNING_ON);
		textAttributes.put(TextAttribute.FAMILY, "SansSerif");
		textAttributes.put(TextAttribute.SIZE, 13);

		defaultFont = Font.getFont(textAttributes);

	}

	Font font = defaultFont; //new Font("Default", Font.PLAIN, 13);

	FontMetrics fm;
	Color color = new Color(0.1f,0.1f,0.1f,1);

	@Override
	public int getColor() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getTextHeight(String text) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getTextWidth(String text) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setBitmapShader(Bitmap bitmap) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setColor(int color) {
		this.color = new Color(
				((color >> 16) & 0xff)/255f,
				((color >> 8) & 0xff)/255f,
				((color >> 0) & 0xff)/255f,
				((color >> 24) & 0xff)/255f
				);
	}

	@Override
	public void setDashPathEffect(float[] strokeDasharray) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setStrokeCap(Cap cap) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setStrokeWidth(float width) {
		//int size = font.getSize();
		//font = font.deriveFont(size + width * 4);

		// TODO Auto-generated method stub

	}

	@Override
	public void setStyle(Style style) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setTextAlign(Align align) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setTextSize(float textSize) {
		font = font.deriveFont(textSize - 4);

	}

	@Override
	public void setTypeface(FontFamily fontFamily, FontStyle fontStyle) {
		// TODO Auto-generated method stub

	}

	@Override
	public float measureText(String text) {
		if (fm == null)
			fm = AwtGraphics.getFontMetrics(this.font);
		float w = AwtGraphics.getTextWidth(fm, text);
		//Gdx.app.log("text width:", text + " " + w);
		return w;
		//return fm.getStringBounds(text, A).getWidth();
		//return AwtGraphics.getTextWidth(fm, text);
		//return fm.stringWidth(text);
	}

	@Override
	public float getFontHeight() {
		if (fm == null)
			fm = AwtGraphics.getFontMetrics(this.font);

		float height = fm.getHeight();

		//Gdx.app.log("text height", " " + height);
		return height;
	}

	@Override
	public float getFontDescent() {
		if (fm == null)
			fm = AwtGraphics.getFontMetrics(this.font);

		float desc = fm.getDescent();
		//Gdx.app.log("text descent", " " + desc);

		return desc;
	}

}
