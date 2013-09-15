package org.oscim.awt;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Canvas;
import org.oscim.backend.canvas.Paint;

public class AwtGraphics extends CanvasAdapter {
	public static final AwtGraphics INSTANCE = new AwtGraphics();

	private AwtGraphics() {
		// do nothing
	}

	@Override
	public int getColor(Color color) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Paint getPaint() {
		return new AwtPaint();
	}

	@Override
	public int parseColor(String colorString) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Bitmap getBitmap(int width, int height, int format) {
		return new AwtBitmap(width, height, format);
	}

	@Override
	public Canvas getCanvas() {
		return new AwtCanvas();
	}

	static final BufferedImage image;

	static final Graphics2D canvas;

	static {
		image  = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		canvas = image.createGraphics();
		canvas.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON );
		//canvas.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		//canvas.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
	}
	static synchronized FontMetrics getFontMetrics(Font font) {
		canvas.setFont(font);
		// get character measurements
		FontMetrics fm = canvas.getFontMetrics();
		// int ascent = fm.getMaxAscent();
		// int descent = fm.getMaxDescent();
		// int advance = fm.charWidth('W'); // width of widest char, more
		// reliable than getMaxAdvance();
		// int leading = fm.getLeading();
		//
		return fm;
	}

	static synchronized float getTextWidth(FontMetrics fm, String text) {
		//return (float)fm.getStringBounds(text, canvas).getWidth();
		return fm.stringWidth(text);
	}

	@Override
    public Bitmap decodeBitmap(InputStream inputStream) {
		try {
	        return new AwtBitmap(inputStream);
        } catch (IOException e) {
	        e.printStackTrace();
	        return null;
        }
	}

	@Override
	public Bitmap loadBitmapAsset(String fileName) {
		try {
			return createBitmap(fileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
