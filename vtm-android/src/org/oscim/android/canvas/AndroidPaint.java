/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package org.oscim.android.canvas;

import org.oscim.backend.canvas.Paint;

import android.graphics.Paint.FontMetrics;
import android.graphics.Typeface;

class AndroidPaint implements Paint {
	private static int getStyle(org.oscim.backend.canvas.Paint.FontStyle fontStyle) {
		switch (fontStyle) {
			case BOLD:
				return 1;
			case BOLD_ITALIC:
				return 3;
			case ITALIC:
				return 2;
			case NORMAL:
				return 0;
		}

		throw new IllegalArgumentException("unknown font style: " + fontStyle);
	}

	private static Typeface getTypeface(org.oscim.backend.canvas.Paint.FontFamily fontFamily) {
		switch (fontFamily) {
			case DEFAULT:
				return Typeface.DEFAULT;
			case DEFAULT_BOLD:
				return Typeface.DEFAULT_BOLD;
			case MONOSPACE:
				return Typeface.MONOSPACE;
			case SANS_SERIF:
				return Typeface.SANS_SERIF;
			case SERIF:
				return Typeface.SERIF;
		}

		throw new IllegalArgumentException("unknown font family: " + fontFamily);
	}

	final android.graphics.Paint mPaint;

	AndroidPaint() {
		mPaint = new android.graphics.Paint(
		                                    android.graphics.Paint.ANTI_ALIAS_FLAG);
	}

	@Override
	public int getColor() {
		return mPaint.getColor();
	}

	@Override
	public void setColor(int color) {
		mPaint.setColor(color);
	}

	@Override
	public void setStrokeCap(Cap cap) {
		android.graphics.Paint.Cap androidCap = android.graphics.Paint.Cap
		    .valueOf(cap.name());
		mPaint.setStrokeCap(androidCap);
	}

	@Override
	public void setStrokeWidth(float width) {
		mPaint.setStrokeWidth(width);
	}

	@Override
	public void setStyle(Style style) {
		mPaint.setStyle(android.graphics.Paint.Style.valueOf(style.name()));
	}

	@Override
	public void setTextAlign(Align align) {
		//mPaint.setTextAlign(android.graphics.Paint.Align.valueOf(align.name()));
	}

	@Override
	public void setTextSize(float textSize) {
		mPaint.setTextSize(textSize);
	}

	@Override
	public void setTypeface(FontFamily fontFamily, FontStyle fontStyle) {
		Typeface typeface = Typeface.create(getTypeface(fontFamily),
		                                    getStyle(fontStyle));
		mPaint.setTypeface(typeface);
	}

	@Override
	public float measureText(String text) {
		return mPaint.measureText(text);
	}

	@Override
	public float getFontHeight() {
		FontMetrics fm = mPaint.getFontMetrics();
		return (float) Math.ceil(Math.abs(fm.bottom) + Math.abs(fm.top));
	}

	@Override
	public float getFontDescent() {
		FontMetrics fm = mPaint.getFontMetrics();
		// //fontDescent = (float) Math.ceil(Math.abs(fm.descent));
		return Math.abs(fm.bottom);
	}
}
