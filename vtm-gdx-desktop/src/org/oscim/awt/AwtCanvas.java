package org.oscim.awt;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Canvas;
import org.oscim.backend.canvas.Paint;

public class AwtCanvas implements Canvas {

	Graphics2D canvas;

	public AwtCanvas() {

	}

	@Override
	public void setBitmap(Bitmap bitmap) {
		if (canvas != null)
			canvas.dispose();

		AwtBitmap awtBitamp = (AwtBitmap)bitmap;

		canvas = awtBitamp.bitmap.createGraphics();
		//awtBitamp.bitmap.
		//bitmap.eraseColor();
		canvas.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 0));
		//canvas.setBackground(new Color(1,1,1,1));
		canvas.setColor(Color.BLACK);

		//Gdx.app.log("set bitmap ",  bitmap + " "+ bitmap.getWidth() + " " +bitmap.getHeight());
		canvas.fillRect(0,0,bitmap.getWidth(),bitmap.getHeight());
		//canvas.clearRect(0, 0, bitmap.getWidth(),bitmap.getHeight());

		canvas.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

		canvas.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON );
		//canvas.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		//canvas.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		//canvas.setRenderingHint(RenderingHints.KEY_KERNING, RenderingHints.VALUE_RENDER_QUALITY);


	}

	@Override
	public void drawText(String string, float x, float y, Paint stroke) {
		AwtPaint p = (AwtPaint)stroke;

		canvas.setFont(p.font);
		canvas.setColor(p.color);

		canvas.drawString(string, (int)x, (int)y);
	}

	@Override
    public void drawBitmap(Bitmap bitmap, float x, float y) {
	    // TODO Auto-generated method stub
	    throw new UnknownError("not implemented");
    }
}
