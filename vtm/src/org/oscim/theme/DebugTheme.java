package org.oscim.theme;

import org.oscim.backend.canvas.Color;
import org.oscim.core.GeometryBuffer.GeometryType;
import org.oscim.core.TagSet;
import org.oscim.theme.styles.Area;
import org.oscim.theme.styles.Line;
import org.oscim.theme.styles.RenderStyle;

public class DebugTheme implements IRenderTheme {

	private final static Line[] line = { new Line(1, Color.MAGENTA, 2) };
	private final static Area[] area = { new Area(0, Color.CYAN) };

	@Override
	public RenderStyle[] matchElement(GeometryType type, TagSet tags, int zoomLevel) {
		if (type == GeometryType.LINE)
			return line;
		if (type == GeometryType.POLY)
			return area;

		return null;
	}

	@Override
	public void destroy() {
	}

	@Override
	public int getLevels() {
		return 0;
	}

	@Override
	public int getMapBackground() {
		return 0;
	}

	@Override
	public void scaleTextSize(float scaleFactor) {
	}

	@Override
	public void updateInstructions() {

	}

}
