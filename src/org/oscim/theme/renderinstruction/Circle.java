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
 * Represents a round area on the map.
 */
public final class Circle extends RenderInstruction {
	/**
	 * @param elementName
	 *            the name of the XML element.
	 * @param attributes
	 *            the attributes of the XML element.
	 * @param level
	 *            the drawing level of this instruction.
	 * @return a new Circle with the given rendering attributes.
	 */
	public static Circle create(String elementName, Attributes attributes, int level) {
		Float radius = null;
		boolean scaleRadius = false;
		int fill = Color.TRANSPARENT;
		int stroke = Color.TRANSPARENT;
		float strokeWidth = 0;

		for (int i = 0; i < attributes.getLength(); ++i) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);

			if ("r".equals(name)) {
				radius = Float.valueOf(Float.parseFloat(value));
			} else if ("scale-radius".equals(name)) {
				scaleRadius = Boolean.parseBoolean(value);
			} else if ("fill".equals(name)) {
				fill = Color.parseColor(value);
			} else if ("stroke".equals(name)) {
				stroke = Color.parseColor(value);
			} else if ("stroke-width".equals(name)) {
				strokeWidth = Float.parseFloat(value);
			} else {
				RenderThemeHandler.logUnknownAttribute(elementName, name, value, i);
			}
		}

		validate(elementName, radius, strokeWidth);
		return new Circle(radius, scaleRadius, fill, stroke, strokeWidth, level);
	}

	private static void validate(String elementName, Float radius, float strokeWidth) {
		if (radius == null) {
			throw new IllegalArgumentException("missing attribute r for element: "
					+ elementName);
		} else if (radius.floatValue() < 0) {
			throw new IllegalArgumentException("radius must not be negative: " + radius);
		} else if (strokeWidth < 0) {
			throw new IllegalArgumentException("stroke-width must not be negative: "
					+ strokeWidth);
		}
	}

	public final int level;

	public final int fill;
	public final int outline;
	public final float radius;
	public  float renderRadius;
	public final boolean scaleRadius;
	public final float strokeWidth;

	private Circle(Float radius, boolean scaleRadius, int fill, int stroke,
			float strokeWidth, int level) {
		super();

		this.radius = radius.floatValue();
		this.scaleRadius = scaleRadius;

		this.fill = fill;
		this.outline = stroke;

		this.strokeWidth = strokeWidth;
		this.level = level;

//if (!mScaleRadius) {
//	mRenderRadius = mRadius;
//	if (mOutline != null) {
//		mOutline.setStrokeWidth(mStrokeWidth);
//	}
//}
	}

	@Override
	public void renderNode(IRenderCallback renderCallback) {
			renderCallback.renderPointOfInterestCircle(this, this.level);
	}
}
