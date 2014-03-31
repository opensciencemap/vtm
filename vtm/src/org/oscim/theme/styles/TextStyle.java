/*
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
package org.oscim.theme.styles;

import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Color;
import org.oscim.backend.canvas.Paint;
import org.oscim.backend.canvas.Paint.Align;
import org.oscim.backend.canvas.Paint.FontFamily;
import org.oscim.backend.canvas.Paint.FontStyle;
import org.oscim.renderer.atlas.TextureRegion;
import org.oscim.theme.IRenderTheme.Callback;

public final class TextStyle extends RenderStyle {

	public static class TextBuilder implements StyleBuilder {

		public String style;
		public float fontSize;

		public String textKey;
		public boolean caption;
		public float dy;
		public int priority;
		public TextureRegion texture;
		public FontFamily fontFamily;
		public FontStyle fontStyle;

		public int color;
		public int stroke;
		public float strokeWidth;

		public TextBuilder reset() {
			fontFamily = FontFamily.DEFAULT;
			fontStyle = FontStyle.NORMAL;
			style = null;
			textKey = null;
			fontSize = 0;
			caption = false;
			priority = Integer.MAX_VALUE;
			texture = null;
			color = Color.BLACK;
			stroke = Color.BLACK;
			strokeWidth = 0;
			dy = 0;
			return this;
		}

		public TextBuilder() {
			reset();
		}

		public TextStyle build() {
			TextStyle t = new TextStyle(this);
			t.fontHeight = t.paint.getFontHeight();
			t.fontDescent = t.paint.getFontDescent();
			return t;
		}

		public TextStyle buildInternal() {
			return new TextStyle(this);
		}

		public TextBuilder setStyle(String style) {
			this.style = style;
			return this;
		}

		public TextBuilder setFontSize(float fontSize) {
			this.fontSize = fontSize;
			return this;
		}

		public TextBuilder setTextKey(String textKey) {
			this.textKey = textKey;
			return this;
		}

		public TextBuilder setCaption(boolean caption) {
			this.caption = caption;
			return this;
		}

		public TextBuilder setOffsetY(float dy) {
			this.dy = dy;
			return this;
		}

		public TextBuilder setPriority(int priority) {
			this.priority = priority;
			return this;
		}

		public TextBuilder setTexture(TextureRegion texture) {
			this.texture = texture;
			return this;
		}

		public TextBuilder setFontFamily(FontFamily fontFamily) {
			this.fontFamily = fontFamily;
			return this;
		}

		public TextBuilder setFontStyle(FontStyle fontStyle) {
			this.fontStyle = fontStyle;
			return this;
		}

		public TextBuilder setColor(int color) {
			this.color = color;
			return this;
		}

		public TextBuilder setStroke(int stroke) {
			this.stroke = stroke;
			return this;
		}

		public TextBuilder setStrokeWidth(float strokeWidth) {
			this.strokeWidth = strokeWidth;
			return this;
		}

		@Override
		public TextBuilder level(int level) {
			return this;
		}
	}

	TextStyle(TextBuilder tb) {
		this.style = tb.style;
		this.textKey = tb.textKey;
		this.caption = tb.caption;
		this.dy = tb.dy;
		this.priority = tb.priority;
		this.texture = tb.texture;

		paint = CanvasAdapter.g.getPaint();
		paint.setTextAlign(Align.CENTER);
		paint.setTypeface(tb.fontFamily, tb.fontStyle);

		paint.setColor(tb.color);
		paint.setTextSize(tb.fontSize);

		if (tb.strokeWidth > 0) {
			stroke = CanvasAdapter.g.getPaint();
			stroke.setStyle(Paint.Style.STROKE);
			stroke.setTextAlign(Align.CENTER);
			stroke.setTypeface(tb.fontFamily, tb.fontStyle);
			stroke.setColor(tb.stroke);
			stroke.setStrokeWidth(tb.strokeWidth);
			stroke.setTextSize(tb.fontSize);
		} else
			stroke = null;

		this.fontSize = tb.fontSize;
	}

	public final String style;

	public final float fontSize;
	public final Paint paint;
	public final Paint stroke;
	public final String textKey;

	public final boolean caption;
	public final float dy;
	public final int priority;

	public float fontHeight;
	public float fontDescent;

	public final TextureRegion texture;

	@Override
	public void renderNode(Callback cb) {
		cb.renderText(this);
	}

	@Override
	public void renderWay(Callback cb) {
		cb.renderText(this);
	}

	@Override
	public TextStyle current() {
		return (TextStyle) mCurrent;
	}

	@Override
	public void scaleTextSize(float scaleFactor) {
		paint.setTextSize(fontSize * scaleFactor);
		if (stroke != null)
			stroke.setTextSize(fontSize * scaleFactor);

		fontHeight = paint.getFontHeight();
		fontDescent = paint.getFontDescent();
	}
}
