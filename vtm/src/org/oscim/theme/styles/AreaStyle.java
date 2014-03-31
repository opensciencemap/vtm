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

import static org.oscim.backend.canvas.Color.parseColor;

import org.oscim.backend.canvas.Color;
import org.oscim.renderer.elements.TextureItem;
import org.oscim.theme.IRenderTheme.Callback;

public class AreaStyle extends RenderStyle {

	/** Drawing order level */
	private final int level;

	/** Style name */
	public final String style;

	/** Fill color */
	public final int color;

	/** Fade-out zoom-level */
	public final int fadeScale;

	/** Fade to blendColor zoom-level */
	public final int blendColor;

	/** Blend fill color */
	public final int blendScale;

	/** Pattern texture */
	public final TextureItem texture;

	/** Outline */
	public final LineStyle outline;

	public AreaStyle(int color) {
		this(0, color);
	}

	public AreaStyle(int level, int color) {
		this.level = level;
		this.style = "";
		this.fadeScale = -1;
		this.blendColor = 0;
		this.blendScale = -1;
		this.color = color;
		this.texture = null;
		this.outline = null;
	}

	public AreaStyle(AreaBuilder b) {
		this.level = b.level;
		this.style = b.style;
		this.fadeScale = b.fadeScale;
		this.blendColor = b.blendColor;
		this.blendScale = b.blendScale;
		this.color = b.color;
		this.texture = b.texture;

		if (b.outline != null &&
		        b.outlineColor == b.outline.color &&
		        b.outlineWidth == b.outline.width) {
			this.outline = b.outline;
		} else if (b.outlineColor != Color.TRANSPARENT) {
			this.outline = new LineStyle(-1, b.outlineColor, b.outlineWidth);
		} else {
			this.outline = null;
		}
	}

	@Override
	public void update() {
		super.update();

		if (outline != null)
			outline.update();
	}

	@Override
	public AreaStyle current() {
		return (AreaStyle) mCurrent;
	}

	@Override
	public void renderWay(Callback renderCallback) {
		renderCallback.renderArea(this, level);

		if (outline != null)
			renderCallback.renderWay(outline, level + 1);
	}

	public static class AreaBuilder implements StyleBuilder {
		public int level;
		public String style;
		public LineStyle outline;
		public int color;
		public int fadeScale;
		public int blendColor;
		public int blendScale;

		public int outlineColor;
		public float outlineWidth;

		public TextureItem texture;

		public AreaBuilder set(AreaStyle area) {
			if (area == null)
				return reset();

			this.level = area.level;
			this.style = area.style;
			this.fadeScale = area.fadeScale;
			this.blendColor = area.blendColor;
			this.blendScale = area.blendScale;
			this.color = area.color;
			this.texture = area.texture;
			this.outline = area.outline;
			if (area.outline != null) {
				this.outlineColor = outline.color;
				this.outlineWidth = outline.width;
			} else {
				outlineColor = Color.TRANSPARENT;
				outlineWidth = 1;
			}

			return this;
		}

		public AreaBuilder style(String name) {
			this.style = name;
			return this;
		}

		public AreaBuilder level(int level) {
			this.level = level;
			return this;
		}

		public AreaBuilder outline(int color, float width) {
			this.outlineColor = color;
			this.outlineWidth = width;
			return this;
		}

		public AreaBuilder outlineColor(int color) {
			this.outlineColor = color;
			return this;
		}

		public AreaBuilder outlineColor(String color) {
			this.outlineColor = parseColor(color);
			return this;
		}

		public AreaBuilder outlineWidth(float width) {
			this.outlineWidth = width;
			return this;
		}

		public AreaBuilder color(int color) {
			this.color = color;
			return this;
		}

		public AreaBuilder color(String color) {
			this.color = parseColor(color);
			return this;
		}

		public AreaBuilder blendScale(int zoom) {
			this.blendScale = zoom;
			return this;
		}

		public AreaBuilder blendColor(int color) {
			this.blendColor = color;
			return this;
		}

		public AreaBuilder blendColor(String color) {
			this.blendColor = parseColor(color);
			return this;
		}

		public AreaBuilder texture(TextureItem texture) {
			this.texture = texture;
			return this;
		}

		public AreaBuilder fadeScale(int zoom) {
			this.fadeScale = zoom;
			return this;
		}

		public AreaBuilder reset() {
			color = Color.BLACK;

			outlineColor = Color.TRANSPARENT;
			outlineWidth = 1;

			fadeScale = -1;
			blendScale = -1;
			blendColor = Color.TRANSPARENT;
			style = null;
			texture = null;
			return this;
		}

		public AreaStyle build() {
			return new AreaStyle(this);
		}
	}
}
