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


	public Area(int fill) {
		this.level = 0;
		this.style = "";
		this.fade = -1;
		blendColor = 0;
		blend = -1;
		strokeWidth = 0;

		color = fill;
	}


	public Area(String style, String src, int fill, int stroke, float strokeWidth,
			int fade, int level, int blend, int blendFill) {

		this.style = style;

		// if (fill == Color.TRANSPARENT) {
		// paintFill = null;
		// } else {
		// paintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
		// if (src != null) {
		// Shader shader = BitmapUtils.createBitmapShader(src);
		// paintFill.setShader(shader);
		// }

		this.color = fill; //GlUtils.colorToFloatP(fill);
		this.blendColor = blendFill; //GlUtils.colorToFloatP(blendFill);

		this.blend = blend;
		this.strokeWidth = strokeWidth;
		this.fade = fade;
		this.level = level;
	}

	@Override
	public void renderWay(IRenderCallback renderCallback) {
		renderCallback.renderArea(this, this.level);
	}

	private final int level;
	public String style;
	public final float strokeWidth;
	public final int color;
	public final int fade;
	public final int blendColor;
	public final int blend;
}
