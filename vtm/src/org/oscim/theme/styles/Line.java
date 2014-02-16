/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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

import org.oscim.backend.canvas.Paint.Cap;
import org.oscim.theme.IRenderTheme.Callback;

/**
 * Represents a polyline on the map.
 */
public final class Line extends RenderStyle {

	private final int level;

	public final String style;
	public final float width;
	public final int color;
	public final Cap cap;
	public final boolean outline;
	public final boolean fixed;
	public final int fade;
	public final float blur;

	public final int stipple;
	public final int stippleColor;
	public final float stippleWidth;

	public Line(int level, String style, int color, float width,
	        Cap cap, boolean fixed,
	        int stipple, int stippleColor, float stippleWidth,
	        int fade, float blur, boolean isOutline) {

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

		//GlUtils.changeSaturation(color, 1.02f);

		this.cap = cap;
		this.color = color;
		this.width = width;
		this.fixed = fixed;

		this.stipple = stipple;
		this.stippleColor = stippleColor;
		this.stippleWidth = stippleWidth;

		this.blur = blur;
		this.fade = fade;
	}

	public Line(int stroke, float width) {
		this(0, "", stroke, width, Cap.BUTT, true, 0, 0, 0, -1, 0, false);
	}

	public Line(int level, int stroke, float width) {
		this(level, "", stroke, width, Cap.BUTT, true, 0, 0, 0, -1, 0, false);
	}

	public Line(int stroke, float width, Cap cap) {
		this(0, "", stroke, width, cap, true, 0, 0, 0, -1, 0, false);
	}

	@Override
	public void renderWay(Callback renderCallback) {
		renderCallback.renderWay(this, level);
	}
}
