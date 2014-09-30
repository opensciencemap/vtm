/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
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
package org.oscim.awt;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Stroke;
import java.awt.font.TextAttribute;
import java.text.AttributedCharacterIterator.Attribute;
import java.util.HashMap;
import java.util.Map;

import org.oscim.backend.canvas.Paint;

public class AwtPaint implements Paint {

	final static float TEXT_OFFSET = 2;

	private static int getCap(Cap cap) {
		switch (cap) {
			case BUTT:
				return BasicStroke.CAP_BUTT;
			case ROUND:
				return BasicStroke.CAP_ROUND;
			case SQUARE:
				return BasicStroke.CAP_SQUARE;
		}

		throw new IllegalArgumentException("unknown cap: " + cap);
	}

	static final Font defaultFont;
	static {
		Map<Attribute, Object> textAttributes = new HashMap<Attribute, Object>();
		textAttributes.put(TextAttribute.KERNING, TextAttribute.KERNING_ON);
		textAttributes.put(TextAttribute.FAMILY, "Arial");
		textAttributes.put(TextAttribute.SIZE, 14);

		defaultFont = Font.getFont(textAttributes);
	}

	Font font = defaultFont; // new Font("Default", Font.PLAIN, 13);
	Stroke stroke;
	FontMetrics fm;
	Color color = new Color(0.1f, 0.1f, 0.1f, 1);

	private int cap;
	private float strokeWidth;

	//private Align mAlign;

	@Override
	public int getColor() {
		return 0;
	}

	@Override
	public void setColor(int c) {
		color = new Color(((c >> 16) & 0xff) / 255f,
		                  ((c >> 8) & 0xff) / 255f,
		                  ((c >> 0) & 0xff) / 255f,
		                  ((c >> 24) & 0xff) / 255f);
	}

	@Override
	public void setStrokeCap(Cap cap) {
		this.cap = getCap(cap);
		createStroke();
	}

	@Override
	public void setStrokeWidth(float width) {
		strokeWidth = width + 1;
		createStroke();

		// int size = font.getSize();
		// font = font.deriveFont(size + width * 4);

		// TODO Auto-generated method stub

	}

	@Override
	public void setStyle(Style style) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setTextAlign(Align align) {
		//mAlign = align;
	}

	@Override
	public void setTextSize(float textSize) {
		font = font.deriveFont(textSize);

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
		return w + 4;
		// return fm.getStringBounds(text, A).getWidth();
		// return AwtGraphics.getTextWidth(fm, text);
		// return fm.stringWidth(text);
	}

	@Override
	public float getFontHeight() {
		if (fm == null)
			fm = AwtGraphics.getFontMetrics(this.font);

		float height = fm.getHeight();

		return height;
	}

	@Override
	public float getFontDescent() {
		if (fm == null)
			fm = AwtGraphics.getFontMetrics(this.font);

		float desc = fm.getDescent();

		return desc;
	}

	private void createStroke() {
		if (strokeWidth <= 0) {
			return;
		}
		stroke = new BasicStroke(strokeWidth, cap, BasicStroke.JOIN_MITER, 1, null, 0);
	}
}
