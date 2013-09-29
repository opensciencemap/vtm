package org.oscim.theme.renderinstruction;

import org.oscim.theme.IRenderTheme.Callback;

public class Extrusion extends RenderInstruction {

	public Extrusion(int level, int colorSides, int colorTop, int colorLine) {
		this.colorSide = colorSides;
		this.colorTop = colorTop;
		this.colorLine = colorLine;

		this.level = level;
	}

	@Override
	public void renderWay(Callback renderCallback) {
		renderCallback.renderExtrusion(this, this.level);
	}

	private final int level;

	public final int colorTop;
	public final int colorSide;
	public final int colorLine;
}
