/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package org.oscim.theme.renderinstruction;

import java.util.Locale;

import org.oscim.core.Tag;
import org.oscim.theme.IRenderCallback;
import org.oscim.theme.RenderThemeHandler;
import org.xml.sax.Attributes;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.Style;
import android.graphics.Typeface;

/**
 * Represents a text along a polyline on the map.
 */
public final class Text extends RenderInstruction {
	/**
	 * @param elementName
	 *            the name of the XML element.
	 * @param attributes
	 *            the attributes of the XML element.
	 * @param caption
	 *            ...
	 * @return a new Text with the given rendering attributes.
	 */
	public static Text create(String elementName, Attributes attributes, boolean caption) {
		String textKey = null;
		FontFamily fontFamily = FontFamily.DEFAULT;
		FontStyle fontStyle = FontStyle.NORMAL;
		float fontSize = 0;
		int fill = Color.BLACK;
		int stroke = Color.BLACK;
		float strokeWidth = 0;
		String style = null;
		float dy = 0;
		int priority = Integer.MAX_VALUE;

		for (int i = 0; i < attributes.getLength(); ++i) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);
			if ("name".equals(name))
				style = value;
			else if ("k".equals(name)) {
				textKey = TextKey.getInstance(value);
			} else if ("font-family".equals(name)) {
				fontFamily = FontFamily.valueOf(value.toUpperCase(Locale.ENGLISH));
			} else if ("font-style".equals(name)) {
				fontStyle = FontStyle.valueOf(value.toUpperCase(Locale.ENGLISH));
			} else if ("font-size".equals(name)) {
				fontSize = Float.parseFloat(value);
			} else if ("fill".equals(name)) {
				fill = Color.parseColor(value);
			} else if ("stroke".equals(name)) {
				stroke = Color.parseColor(value);
			} else if ("stroke-width".equals(name)) {
				strokeWidth = Float.parseFloat(value);
			} else if ("caption".equals(name)) {
				caption = Boolean.parseBoolean(value);
			} else if ("priority".equals(name)) {
				priority = Integer.parseInt(value);
			} else if ("dy".equals(name)) {
				dy = Float.parseFloat(value);
			} else {
				RenderThemeHandler.logUnknownAttribute(elementName, name, value, i);
			}
		}

		validate(elementName, textKey, fontSize, strokeWidth);

		Typeface typeface = null;
		if (fontFamily == FontFamily.DEFAULT) {
			if (fontStyle == FontStyle.NORMAL)
				typeface = typefaceNormal;
			else if (fontStyle == FontStyle.BOLD)
				typeface = typefaceBold;
		}

		if (typeface == null)
			typeface = Typeface.create(fontFamily.toTypeface(), fontStyle.toInt());

		return new Text(style, textKey, typeface, fontSize, fill, stroke, strokeWidth, dy, caption, priority);
	}

	private static Typeface typefaceNormal = Typeface.create(FontFamily.DEFAULT.toTypeface(),
			FontStyle.NORMAL.toInt());

	private static Typeface typefaceBold = Typeface.create(FontFamily.DEFAULT.toTypeface(),
			FontStyle.BOLD.toInt());

	private static void validate(String elementName, String textKey, float fontSize,
			float strokeWidth) {
		if (textKey == null) {
			throw new IllegalArgumentException("missing attribute k for element: "
					+ elementName);
		} else if (fontSize < 0) {
			throw new IllegalArgumentException("font-size must not be negative: "
					+ fontSize);
		} else if (strokeWidth < 0) {
			throw new IllegalArgumentException("stroke-width must not be negative: "
					+ strokeWidth);
		}
	}

	public final float fontSize;
	public final Paint paint;
	public Paint stroke;
	public String textKey;

	public String style;
	public final boolean caption;
	public final float dy;
	public final int priority;

	public float fontHeight;
	public float fontDescent;
	public static Text createText(float fontSize, float strokeWidth, int fill, int outline,
			boolean billboard) {

		return new Text("", "", typefaceNormal, fontSize, fill, outline, strokeWidth, 0, billboard, Integer.MAX_VALUE);
	}

	private Text(String style, String textKey, Typeface typeface, float fontSize,
			int fill, int outline, float strokeWidth, float dy, boolean caption, int priority) {

		this.style = style;
		this.textKey = textKey;
		this.caption = caption;
		this.dy = dy;
		this.priority = priority;

		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setTextAlign(Align.CENTER);
		paint.setTypeface(typeface);
		paint.setColor(fill);
		paint.setTextSize(fontSize);

		if (strokeWidth > 0) {
			stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
			stroke.setStyle(Style.STROKE);
			stroke.setTextAlign(Align.CENTER);
			stroke.setTypeface(typeface);
			stroke.setColor(outline);
			stroke.setStrokeWidth(strokeWidth);
			stroke.setTextSize(fontSize);
		} else
			stroke = null;

		this.fontSize = fontSize;

		FontMetrics fm = paint.getFontMetrics();
		fontHeight = (float) Math.ceil(Math.abs(fm.bottom) + Math.abs(fm.top));
		fontDescent = (float) Math.ceil(Math.abs(fm.descent));
	}

	@Override
	public void renderNode(IRenderCallback renderCallback, Tag[] tags) {
		if (caption)
			renderCallback.renderPointOfInterestCaption(this);
	}

	@Override
	public void renderWay(IRenderCallback renderCallback, Tag[] tags) {
		if (caption)
			renderCallback.renderAreaCaption(this);
		else
			renderCallback.renderWayText(this);
	}

	@Override
	public void scaleTextSize(float scaleFactor) {
		paint.setTextSize(fontSize * scaleFactor);
		if (stroke != null)
			stroke.setTextSize(fontSize * scaleFactor);

		FontMetrics fm = paint.getFontMetrics();
		fontHeight = (float) Math.ceil(Math.abs(fm.bottom) + Math.abs(fm.top));
		fontDescent = (float) Math.ceil(Math.abs(fm.descent));
	}
}
