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

import org.oscim.backend.canvas.Color;
import org.oscim.theme.IRenderCallback;
import org.oscim.theme.RenderThemeHandler;
import org.xml.sax.Attributes;



/**
 * Represents a closed polygon on the map.
 */
public final class Area extends RenderInstruction {
	/**
	 * @param elementName
	 *            the name of the XML element.
	 * @param attributes
	 *            the attributes of the XML element.
	 * @param level
	 *            the drawing level of this instruction.
	 * @return a new Area with the given rendering attributes.
	 */
	public static Area create(String elementName, Attributes attributes, int level) {
		String src = null;
		int fill = Color.BLACK;
		int stroke = Color.TRANSPARENT;
		float strokeWidth = 0;
		int fade = -1;
		int blend = -1;
		int blendFill = Color.BLACK;
		String style = null;

		for (int i = 0; i < attributes.getLength(); ++i) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);
			if ("name".equals(name))
				style = value;
			else if ("src".equals(name)) {
				src = value;
			} else if ("fill".equals(name)) {
				fill = Color.parseColor(value);
			} else if ("stroke".equals(name)) {
				stroke = Color.parseColor(value);
			} else if ("stroke-width".equals(name)) {
				strokeWidth = Float.parseFloat(value);
			} else if ("fade".equals(name)) {
				fade = Integer.parseInt(value);
			} else if ("blend".equals(name)) {
				blend = Integer.parseInt(value);
			} else if ("blend-fill".equals(name)) {
				blendFill = Color.parseColor(value);
			} else {
				RenderThemeHandler.logUnknownAttribute(elementName, name, value, i);
			}
		}

		validate(strokeWidth);
		return new Area(style, src, fill, stroke, strokeWidth, fade, level, blend,
				blendFill);
	}

	private static void validate(float strokeWidth) {
		if (strokeWidth < 0) {
			throw new IllegalArgumentException("stroke-width must not be negative: "
					+ strokeWidth);
		}
	}

	public Area(int fill) {
		this.level = 0;
		this.style = "";
		this.fade = -1;
		blendColor = 0;
		blend = -1;
		strokeWidth = 0;

		color = fill;
	}

	/**
	 * @param style
	 *            ...
	 * @param src
	 *            ...
	 * @param fill
	 *            ...
	 * @param stroke
	 *            ...
	 * @param strokeWidth
	 *            ...
	 * @param fade
	 *            ...
	 * @param level
	 *            ...
	 * @param blend
	 *            ...
	 * @param blendFill
	 *            ...
	 */
	private Area(String style, String src, int fill, int stroke, float strokeWidth,
			int fade, int level, int blend, int blendFill) {
		super();
		this.style = style;

		// if (fill == Color.TRANSPARENT) {
		// paintFill = null;
		// } else {
		// paintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
		// if (src != null) {
		// Shader shader = BitmapUtils.createBitmapShader(src);
		// paintFill.setShader(shader);
		// }

		color = fill; //GlUtils.colorToFloatP(fill);
		blendColor = blendFill; //GlUtils.colorToFloatP(blendFill);

		this.blend = blend;
		this.strokeWidth = strokeWidth;
		this.fade = fade;
		this.level = level;
	}

	@Override
	public void renderWay(IRenderCallback renderCallback) {
		renderCallback.renderArea(this, this.level);
	}

	public String style;
	private final int level;
	public final float strokeWidth;
	public final int color;
	public final int fade;
	public final int blendColor;
	public final int blend;
}
