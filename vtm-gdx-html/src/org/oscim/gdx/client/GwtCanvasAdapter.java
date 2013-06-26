package org.oscim.gdx.client;

import java.io.InputStream;

import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Canvas;
import org.oscim.backend.canvas.Paint;

public class GwtCanvasAdapter extends CanvasAdapter {

	@Override
	public Bitmap decodeBitmap(InputStream in) {

		return null;
	}

	@Override
	public int getColor(Color color) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Paint getPaint() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int parseColor(String colorString) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Bitmap getBitmap(int width, int height, int format) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Canvas getCanvas() {
		// TODO Auto-generated method stub
		return null;
	}

}
