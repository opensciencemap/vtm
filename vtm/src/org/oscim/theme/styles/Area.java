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
import org.oscim.renderer.elements.TextureItem;
import org.oscim.theme.IRenderTheme.Callback;

/**
 * Represents a closed polygon on the map.
 */
public final class Area extends RenderStyle {

	public static class AreaBuilder {
		public int level;
		public String style;
		public Line outline;
		public int color;
		public int fade;
		public int blendColor;
		public int blend;
		public TextureItem texture;

		public AreaBuilder set(Area area) {
			this.level = area.level;
			this.style = area.style;
			this.fade = area.fade;
			this.blendColor = area.blendColor;
			this.blend = area.blend;
			this.color = area.color;
			this.texture = area.texture;
			this.outline = area.outline;
			return this;
		}

		public AreaBuilder setColor(int color) {
			this.color = color;
			return this;
		}

		public AreaBuilder setColor(String color) {
			this.color = Color.parseColor(color);
			return this;
		}

		public AreaBuilder setBlendColor(int color) {
			this.blendColor = color;
			return this;
		}

		public AreaBuilder setBlendColor(String color) {
			this.blendColor = Color.parseColor(color);
			return this;
		}

		public Area build() {
			return new Area(this);
		}
	}

	public Area(int color) {
		this(0, color);
	}

	public Area(int level, int color) {
		this.level = level;
		this.style = "";
		this.fade = -1;
		this.blendColor = 0;
		this.blend = -1;
		this.color = color;
		this.texture = null;
		this.outline = null;
	}

	public Area(String style, int color, int stroke, float strokeWidth,
	        int fade, int level, int blend, int blendColor, TextureItem texture) {

		this.style = style;
		this.color = color;
		this.blendColor = blendColor;
		this.blend = blend;
		this.fade = fade;
		this.level = level;
		this.texture = texture;

		if (stroke == Color.TRANSPARENT) {
			this.outline = null;
			return;
		}

		this.outline = new Line(level + 1, stroke, strokeWidth);
	}

	public Area(AreaBuilder areaBuilder) {
		this.level = areaBuilder.level;
		this.style = areaBuilder.style;
		this.fade = areaBuilder.fade;
		this.blendColor = areaBuilder.blendColor;
		this.blend = areaBuilder.blend;
		this.color = areaBuilder.color;
		this.texture = areaBuilder.texture;
		this.outline = areaBuilder.outline;
	}

	@Override
	public void renderWay(Callback renderCallback) {
		renderCallback.renderArea(this, level);

		if (outline != null)
			renderCallback.renderWay(outline, level + 1);
	}

	private final int level;
	public final String style;
	public final Line outline;
	public final int color;
	public final int fade;
	public final int blendColor;
	public final int blend;
	public final TextureItem texture;

	public void update() {
		super.update();

		if (outline != null)
			outline.update();
	}
}
