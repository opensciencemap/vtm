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

import org.oscim.backend.canvas.Color;
import org.oscim.backend.canvas.Paint.Cap;
import org.oscim.theme.IRenderTheme.Callback;

/**
 * Represents a polyline on the map.
 */
public final class Line extends RenderStyle {

	public final static class LineBuilder {
		public int level;

		public String style;
		public float width;
		public int color;
		public Cap cap;
		public boolean outline;
		public boolean fixed;
		public int fade;
		public float blur;

		public int stipple;
		public int stippleColor;
		public float stippleWidth;

		public LineBuilder set(Line line) {
			this.level = line.level;
			this.style = line.style;
			this.width = line.width;
			this.color = line.color;
			this.cap = line.cap;
			this.outline = line.outline;
			this.fixed = line.fixed;
			this.fade = line.fade;
			this.blur = line.blur;
			this.stipple = line.stipple;
			this.stippleColor = line.stippleColor;
			this.stippleWidth = line.stippleWidth;
			return this;
		}

		public LineBuilder setColor(int color) {
			this.color = color;
			return this;
		}

		public LineBuilder setStippleColor(int color) {
			this.stippleColor = color;
			return this;
		}

		public LineBuilder setColor(String color) {
			this.color = Color.parseColor(color);
			return this;
		}

		public Line build() {
			return new Line(this);
		}
	}

	final int level;

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

	private Line(LineBuilder builer) {
		this.level = builer.level;
		this.style = builer.style;
		this.width = builer.width;
		this.color = builer.color;
		this.cap = builer.cap;
		this.outline = builer.outline;
		this.fixed = builer.fixed;
		this.fade = builer.fade;
		this.blur = builer.blur;
		this.stipple = builer.stipple;
		this.stippleColor = builer.stippleColor;
		this.stippleWidth = builer.stippleWidth;
	}

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
