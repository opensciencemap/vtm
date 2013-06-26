package org.oscim.gdx.client;

import org.oscim.backend.canvas.Bitmap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.Pixmap;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.RootPanel;

public class GwtBitmap implements Bitmap {
	Pixmap bitmap;
	Image image;

	public GwtBitmap(Image data) {
		ImageElement imageElement = ImageElement.as(data.getElement());
		bitmap = new Pixmap(imageElement);
		image = data;
	}

	@Override
	public int getWidth() {
		return bitmap.getWidth();
	}

	@Override
	public int getHeight() {
		return bitmap.getHeight();
	}

	@Override
	public void recycle() {
		bitmap.dispose();
	}

	@Override
	public int[] getPixels() {
		return null;
	}

	@Override
	public void eraseColor(int color) {
	}

	@Override
	public int uploadToTexture(boolean replace) {

		Gdx.gl.glTexImage2D(GL10.GL_TEXTURE_2D, 0, bitmap.getGLInternalFormat(), bitmap.getWidth(),
		                    bitmap.getHeight(), 0,
		                    bitmap.getGLFormat(), bitmap.getGLType(), bitmap.getPixels());

		bitmap.dispose();

		if (image != null)
			RootPanel.get().remove(image);

		return 1;
	}

}
