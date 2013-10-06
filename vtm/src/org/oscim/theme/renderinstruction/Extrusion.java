package org.oscim.theme.renderinstruction;

import org.oscim.backend.canvas.Color;
import org.oscim.theme.IRenderTheme.Callback;

public class Extrusion extends RenderInstruction {

	public Extrusion(int level, int colorSides, int colorTop, int colorLine, int defaultHeight) {

		this.colors = new float[16];
		fillColors(colorSides, colorTop, colorLine, colors);

		this.defaultHeight = defaultHeight;
		this.level = level;
	}

	public static void fillColors(int sides, int top, int lines, float[] mNewColors) {
		float a = Color.aToFloat(top);
		mNewColors[0] = a * Color.rToFloat(top);
		mNewColors[1] = a * Color.gToFloat(top);
		mNewColors[2] = a * Color.bToFloat(top);
		mNewColors[3] = a;

		a = Color.aToFloat(sides);
		mNewColors[4] = a * Color.rToFloat(sides);
		mNewColors[5] = a * Color.gToFloat(sides);
		mNewColors[6] = a * Color.bToFloat(sides);
		mNewColors[7] = a;

		a = Color.aToFloat(sides);
		mNewColors[8] = a * Color.rToFloat(sides);
		mNewColors[9] = a * Color.gToFloat(sides);
		mNewColors[10] = a * Color.bToFloat(sides);
		mNewColors[11] = a;

		a = Color.aToFloat(lines);
		mNewColors[12] = a * Color.rToFloat(lines);
		mNewColors[13] = a * Color.gToFloat(lines);
		mNewColors[14] = a * Color.bToFloat(lines);
		mNewColors[15] = a;
	}

	@Override
	public void renderWay(Callback renderCallback) {
		renderCallback.renderExtrusion(this, this.level);
	}

	private final int level;
	public final float[] colors;
	public final int defaultHeight;
}
