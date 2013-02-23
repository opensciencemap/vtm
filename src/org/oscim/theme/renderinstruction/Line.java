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
import java.util.regex.Pattern;

import org.oscim.core.Tag;
import org.oscim.theme.IRenderCallback;
import org.oscim.theme.RenderThemeHandler;
import org.oscim.utils.GlUtils;
import org.xml.sax.Attributes;

import android.graphics.Color;
import android.graphics.Paint.Cap;

/**
 * Represents a polyline on the map.
 */
public final class Line extends RenderInstruction {
	private static final Pattern SPLIT_PATTERN = Pattern.compile(",");

	/**
	 * @param line
	 *            ...
	 * @param elementName
	 *            the name of the XML element.
	 * @param attributes
	 *            the attributes of the XML element.
	 * @param level
	 *            the drawing level of this instruction.
	 * @param isOutline
	 *            ...
	 * @return a new Line with the given rendering attributes.
	 */
	public static Line create(Line line, String elementName, Attributes attributes,
			int level, boolean isOutline) {

		// Style name
		String style = null;
		// Bitmap
		//String src = null;

		float width = 0;
		Cap cap = Cap.ROUND;

		// Extras
		int fade = -1;
		boolean fixed = false;
		float blur = 0;
		float min = 0;

		// Stipple
		int stipple = 0;
		float stippleWidth = 0;

		float[] color = null;
		float[] stippleColor = null;

		if (line != null) {
			color = line.color;
			fixed = line.fixed;
			fade = line.fade;
			cap = line.cap;
			blur = line.blur;
			min = line.min;
			stipple = line.stipple;
			stippleColor = line.stippleColor;
			stippleWidth = line.stippleWidth;
		}

		for (int i = 0; i < attributes.getLength(); ++i) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);

			if ("name".equals(name))
				style = value;
			else if ("src".equals(name)) {
				//src = value;
			} else if ("stroke".equals(name)) {
				int stroke = Color.parseColor(value);
				color = GlUtils.colorToFloatP(stroke);
			} else if ("width".equals(name)) {
				width = Float.parseFloat(value);
			} else if ("cap".equals(name)) {
				cap = Cap.valueOf(value.toUpperCase(Locale.ENGLISH));
			} else if ("fixed".equals(name)) {
				fixed = Boolean.parseBoolean(value);
			} else if ("stipple".equals(name)) {
				stipple = Integer.parseInt(value);
			} else if ("stipple-stroke".equals(name)) {
				int stroke = Color.parseColor(value);
				stippleColor = GlUtils.colorToFloatP(stroke);
			} else if ("stipple-width".equals(name)) {
				stippleWidth = Float.parseFloat(value);
			} else if ("fade".equals(name)) {
				fade = Integer.parseInt(value);
			} else if ("min".equals(name)) {
				min = Float.parseFloat(value);
			} else if ("blur".equals(name)) {
				blur = Float.parseFloat(value);
			} else if ("from".equals(name)) {
			} else {
				RenderThemeHandler.logUnknownAttribute(elementName, name, value, i);
			}
		}

		// hint that sth is missing
		if (color == null)
			color = GlUtils.colorToFloatP(Color.RED);

		if (stipple != 0 && stippleColor == null)
			stippleColor = GlUtils.colorToFloatP(Color.GREEN);

		// inherit properties from 'line'
		if (line != null) {
			// use stroke width relative to 'line'
			width = line.width + width;
			if (width <= 0)
				width = 1;

		} else if (!isOutline) {
			validate(width);
		}

		return new Line(level, style, color, width, cap, fixed,
				stipple, stippleColor, stippleWidth,
				fade, blur, isOutline, min);
	}

	private static void validate(float strokeWidth) {
		if (strokeWidth < 0) {
			throw new IllegalArgumentException("width must not be negative: "
					+ strokeWidth);
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

	private final int level;

	public final String style;
	public final float width;
	public final float[] color;
	public final Cap cap;
	public final boolean outline;
	public final boolean fixed;
	public final int fade;
	public final float blur;
	public final float min;

	public final int stipple;
	public final float[] stippleColor;
	public final float stippleWidth;

	private Line(int level, String style, float[] stroke, float width,
			Cap cap, boolean fixed,
			int stipple, float[] stippleColor, float stippleWidth,
			int fade, float blur, boolean isOutline, float min) {

		this.level = level;
		this.style = style;
		this.outline = isOutline;

		// paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		//
		// if (src != null) {
		// Shader shader = BitmapUtils.createBitmapShader(src);
		// paint.setShader(shader);
		// }
		//
		// paint.setStyle(Style.STROKE);
		// paint.setColor(stroke);
		// if (strokeDasharray != null) {
		// paint.setPathEffect(new DashPathEffect(strokeDasharray, 0));
		// }

		this.cap = cap;
		this.color = stroke;
		this.width = width;
		this.fixed = fixed;

		this.stipple = stipple;
		this.stippleColor = stippleColor;
		this.stippleWidth = stippleWidth;

		this.blur = blur;
		this.fade = fade;
		this.min = min;
	}

	public Line(int stroke, float width, Cap cap) {
		this.level = 0;
		this.blur = 0;
		this.cap = cap;
		this.outline = false;
		this.style = "";
		this.width = width;
		this.fixed = true;
		this.fade = -1;
		this.stipple = 0;
		this.stippleColor = null;
		this.stippleWidth = 0;
		this.min = 0;
		this.color = GlUtils.colorToFloatP(stroke);
	}

	public Line(int stroke, float width, int stipple) {
		this.level = 0;
		this.blur = 0;
		this.cap = Cap.BUTT;
		this.outline = false;
		this.style = "";
		this.width = width;
		this.fixed = true;
		this.fade = -1;
		this.stipple = stipple;
		this.stippleColor = null;
		this.stippleWidth = 0;
		this.min = 0;
		color = GlUtils.colorToFloatP(stroke);
	}

	@Override
	public void renderWay(IRenderCallback renderCallback, Tag[] tags) {
		renderCallback.renderWay(this, level);
	}

	// @Override
	// public void scaleStrokeWidth(float scaleFactor) {
	// paint.setStrokeWidth(strokeWidth * scaleFactor);
	// }


}
