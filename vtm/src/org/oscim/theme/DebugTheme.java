package org.oscim.theme;

import org.oscim.core.GeometryBuffer.GeometryType;
import org.oscim.core.TagSet;
import org.oscim.theme.renderinstruction.RenderInstruction;

public class DebugTheme implements IRenderTheme {

	@Override
	public RenderInstruction[] matchElement(GeometryType type, TagSet tags, int zoomLevel) {
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

}
