package org.oscim.ios.backend;

import java.io.IOException;
import java.io.InputStream;

import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Canvas;
import org.oscim.backend.canvas.Paint;

public class IosGraphics extends CanvasAdapter {

	private static final IosGraphics INSTANCE = new IosGraphics();

	public static CanvasAdapter get() {
		return INSTANCE;
	}

	public static void init() {
		g = INSTANCE;
	}

	@Override
	public Canvas getCanvas() {
		return new IosCanvas();
	}

	@Override
	public Paint getPaint() {
		return new IosPaint();
	}

	@Override
	public Bitmap getBitmap(int width, int height, int format) {
		return new IosBitmap(width, height, format);
	}

	@Override
	public Bitmap decodeBitmap(InputStream inputStream) {
		try {
			return new IosBitmap(inputStream);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public Bitmap loadBitmapAsset(String fileName) {
		return new IosBitmap(fileName);

		//		try {
		//			return createBitmap(fileName);
		//		} catch (IOException e) {
		//			e.printStackTrace();
		//		}
		//		return null;
	}

}
