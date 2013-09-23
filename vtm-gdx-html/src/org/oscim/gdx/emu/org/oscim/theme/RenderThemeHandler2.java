package org.oscim.theme;

import java.io.InputStream;

import org.oscim.core.MapElement;
import org.oscim.core.Tag;
import org.oscim.theme.renderinstruction.Area;
import org.oscim.theme.renderinstruction.Line;
import org.oscim.theme.renderinstruction.RenderInstruction;

public class RenderThemeHandler2 {
	private final static int AREA_WATER = 0;
	private final static int AREA_WOOD = 1;
	private final static int LINE_HIGHWAY = 2;
	private final static Tag TAG_WATER = new Tag("natural", "water");
	private final static Tag TAG_WOOD = new Tag("natural", "wood");
	private final static Tag TAG_FOREST = new Tag("landuse", "forest");

	private static RenderInstruction[][] instructions = {
	        // water
	        { new Area(2, 0xffafc5e3) },
	        // wood
	        { new Area(1, 0xffd1dbc7) },
	        // highway
	        { new Line(0, 0xffaaaaaa, 1.2f) }

	};

	public static IRenderTheme getRenderTheme(InputStream is) {
		return new StaticTheme();
	}

	static class StaticTheme implements IRenderTheme {

		@Override
		public RenderInstruction[] matchElement(MapElement e, int zoomLevel) {
			if (e.isPoly()) {
				if (e.tags.contains(TAG_WATER))
					return instructions[AREA_WATER];

				if (e.tags.contains(TAG_WOOD) || e.tags.contains(TAG_FOREST))
					return instructions[AREA_WOOD];

			} else if (e.isLine()) {
				if (e.tags.containsKey("highway"))
					return instructions[LINE_HIGHWAY];
			}
			return null;
		}

		@Override
		public void destroy() {
		}

		@Override
		public int getLevels() {
			return 3;
		}

		@Override
		public int getMapBackground() {
			return 0xfafafa;
		}

		@Override
		public void scaleStrokeWidth(float scaleFactor) {

		}

		@Override
		public void scaleTextSize(float scaleFactor) {

		}

	}
}
