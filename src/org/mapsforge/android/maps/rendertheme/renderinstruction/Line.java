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
package org.mapsforge.android.maps.rendertheme.renderinstruction;

import java.io.IOException;
import java.util.Locale;
import java.util.regex.Pattern;

import org.mapsforge.android.maps.rendertheme.RenderCallback;
import org.mapsforge.android.maps.rendertheme.RenderThemeHandler;
import org.mapsforge.core.Tag;
import org.xml.sax.Attributes;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.Shader;

/**
 * Represents a polyline on the map.
 */
public final class Line implements RenderInstruction {
	private static final Pattern SPLIT_PATTERN = Pattern.compile(",");

	/**
	 * @param elementName
	 *            the name of the XML element.
	 * @param attributes
	 *            the attributes of the XML element.
	 * @param level
	 *            the drawing level of this instruction.
	 * @return a new Line with the given rendering attributes.
	 * @throws IOException
	 *             if an I/O error occurs while reading a resource.
	 */
	public static Line create(String elementName, Attributes attributes, int level) throws IOException {
		String src = null;
		int stroke = Color.BLACK;
		float strokeWidth = 0;
		float[] strokeDasharray = null;
		Cap strokeLinecap = Cap.ROUND;
		int outline = -1;
		// int fade = -1;
		boolean fixed = false;

		for (int i = 0; i < attributes.getLength(); ++i) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);

			if ("src".equals(name)) {
				src = value;
			} else if ("stroke".equals(name)) {
				stroke = Color.parseColor(value);
			} else if ("stroke-width".equals(name)) {
				strokeWidth = Float.parseFloat(value);
			} else if ("stroke-dasharray".equals(name)) {
				strokeDasharray = parseFloatArray(value);
			} else if ("stroke-linecap".equals(name)) {
				strokeLinecap = Cap.valueOf(value.toUpperCase(Locale.ENGLISH));
			} else if ("outline".equals(name)) {
				outline = Integer.parseInt(value);
			} else if ("fade".equals(name)) {
				// fade = Integer.parseInt(value);
			} else if ("fixed".equals(name)) {
				fixed = Boolean.parseBoolean(value);
			} else {
				RenderThemeHandler.logUnknownAttribute(elementName, name, value, i);
			}
		}

		validate(strokeWidth);
		return new Line(src, stroke, strokeWidth, strokeDasharray, strokeLinecap, level, outline, fixed);
	}

	private static void validate(float strokeWidth) {
		if (strokeWidth < 0) {
			throw new IllegalArgumentException("stroke-width must not be negative: " + strokeWidth);
		}
	}

	static float[] parseFloatArray(String dashString) {
		String[] dashEntries = SPLIT_PATTERN.split(dashString);
		float[] dashIntervals = new float[dashEntries.length];
		for (int i = 0; i < dashEntries.length; ++i) {
			dashIntervals[i] = Float.parseFloat(dashEntries[i]);
		}
		return dashIntervals;
	}

	/**
	 * 
	 */
	public final int level;
	/**
	 * 
	 */
	public final Paint paint;
	/**
	 * 
	 */
	public final float strokeWidth;
	/**
	 * 
	 */
	public final boolean round;
	/**
	 * 
	 */
	public final int color;
	/**
	 * 
	 */
	public final int outline;

	/**
	 * 
	 */
	public final boolean fixed;

	private Line(String src, int stroke, float strokeWidth, float[] strokeDasharray, Cap strokeLinecap, int level,
			int outline, boolean fixed)
			throws IOException {
		super();

		Shader shader = BitmapUtils.createBitmapShader(src);

		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setShader(shader);
		paint.setStyle(Style.STROKE);
		paint.setColor(stroke);
		if (strokeDasharray != null) {
			paint.setPathEffect(new DashPathEffect(strokeDasharray, 0));
		}
		paint.setStrokeCap(strokeLinecap);
		round = strokeLinecap == Cap.ROUND;
		this.color = stroke;
		this.strokeWidth = strokeWidth;
		this.level = level;
		this.outline = outline;
		this.fixed = fixed;
	}

	@Override
	public void destroy() {
		// do nothing
	}

	@Override
	public void renderNode(RenderCallback renderCallback, Tag[] tags) {
		// do nothing
	}

	@Override
	public void renderWay(RenderCallback renderCallback, Tag[] tags) {
		// renderCallback.renderWay(mPaint, mLevel, mColor, mStrokeWidth, mRound, mOutline);
		renderCallback.renderWay(this);
	}

	@Override
	public void scaleStrokeWidth(float scaleFactor) {
		paint.setStrokeWidth(strokeWidth * scaleFactor);
	}

	@Override
	public void scaleTextSize(float scaleFactor) {
		// do nothing
	}
}
