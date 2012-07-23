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
package org.mapsforge.android.rendertheme.renderinstruction;

import java.io.IOException;

import org.mapsforge.android.rendertheme.IRenderCallback;
import org.mapsforge.android.rendertheme.RenderThemeHandler;
import org.mapsforge.core.Tag;
import org.xml.sax.Attributes;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.Shader;

/**
 * Represents a closed polygon on the map.
 */
public final class Area implements RenderInstruction {
	/**
	 * @param elementName
	 *            the name of the XML element.
	 * @param attributes
	 *            the attributes of the XML element.
	 * @param level
	 *            the drawing level of this instruction.
	 * @return a new Area with the given rendering attributes.
	 * @throws IOException
	 *             if an I/O error occurs while reading a resource.
	 */
	public static Area create(String elementName, Attributes attributes, int level) throws IOException {
		String src = null;
		int fill = Color.BLACK;
		int stroke = Color.TRANSPARENT;
		float strokeWidth = 0;
		int fade = -1;
		for (int i = 0; i < attributes.getLength(); ++i) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);

			if ("src".equals(name)) {
				src = value;
			} else if ("fill".equals(name)) {
				fill = Color.parseColor(value);
			} else if ("stroke".equals(name)) {
				stroke = Color.parseColor(value);
			} else if ("stroke-width".equals(name)) {
				strokeWidth = Float.parseFloat(value);
			} else if ("fade".equals(name)) {
				fade = Integer.parseInt(value);
			} else {
				RenderThemeHandler.logUnknownAttribute(elementName, name, value, i);
			}
		}

		validate(strokeWidth);
		return new Area(src, fill, stroke, strokeWidth, fade, level);
	}

	private static void validate(float strokeWidth) {
		if (strokeWidth < 0) {
			throw new IllegalArgumentException("stroke-width must not be negative: " + strokeWidth);
		}
	}

	/**
	 * 
	 */
	public final int level;
	/**
	 * 
	 */
	public final Paint paintFill;
	/**
	 * 
	 */
	public final Paint paintOutline;
	/**
	 * 
	 */
	public final float strokeWidth;
	/**
	 * 
	 */
	public final int color;
	/**
	 * 
	 */
	public final int fade;

	private Area(String src, int fill, int stroke, float strokeWidth, int fade, int level) throws IOException {
		super();

		Shader shader = BitmapUtils.createBitmapShader(src);

		if (fill == Color.TRANSPARENT) {
			paintFill = null;
		} else {
			paintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
			paintFill.setShader(shader);
			paintFill.setStyle(Style.FILL);
			paintFill.setColor(fill);
			paintFill.setStrokeCap(Cap.ROUND);
		}

		if (stroke == Color.TRANSPARENT) {
			paintOutline = null;
		} else {
			paintOutline = new Paint(Paint.ANTI_ALIAS_FLAG);
			paintOutline.setStyle(Style.STROKE);
			paintOutline.setColor(stroke);
			paintOutline.setStrokeCap(Cap.ROUND);
		}
		color = fill;
		this.strokeWidth = strokeWidth;
		this.fade = fade;
		this.level = level;
	}

	@Override
	public void destroy() {
		// do nothing
	}

	@Override
	public void renderNode(IRenderCallback renderCallback, Tag[] tags) {
		// do nothing
	}

	@Override
	public void renderWay(IRenderCallback renderCallback, Tag[] tags) {
		if (paintFill != null) {
			renderCallback.renderArea(this);
		}
	}

	@Override
	public void scaleStrokeWidth(float scaleFactor) {
		if (paintOutline != null) {
			paintOutline.setStrokeWidth(strokeWidth * scaleFactor);
		}
	}

	@Override
	public void scaleTextSize(float scaleFactor) {
		// do nothing
	}
}
