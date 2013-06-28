package org.oscim.gdx.client;

import org.oscim.backend.Log;
import org.oscim.backend.canvas.Bitmap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.Pixmap;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.RootPanel;

public class GwtBitmap implements Bitmap {
	Pixmap pixmap;
	Image image;
	boolean disposable;
	public GwtBitmap(Image data) {
		ImageElement imageElement = ImageElement.as(data.getElement());
		pixmap = new Pixmap(imageElement);
		image = data;
	}

	/** always argb8888 */
	public GwtBitmap(int width, int height, int format) {
		pixmap = new Pixmap(width, height, null);
	}

	public GwtBitmap(String fileName) {
		FileHandle handle = Gdx.files.internal(fileName);
		pixmap = new Pixmap(handle);
		disposable = true;
	}

	@Override
	public int getWidth() {
		return pixmap.getWidth();
	}

	@Override
	public int getHeight() {
		return pixmap.getHeight();
	}

	@Override
	public void recycle() {
		pixmap.dispose();
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

		Gdx.gl.glTexImage2D(GL10.GL_TEXTURE_2D, 0, pixmap.getGLInternalFormat(), pixmap.getWidth(),
				pixmap.getHeight(), 0,
				pixmap.getGLFormat(), pixmap.getGLType(), pixmap.getPixels());


		if (disposable || image != null){
			Log.d("", "dispose pixmap " +getWidth() +"/" + getHeight());
			pixmap.dispose();

			if (image != null)
				RootPanel.get().remove(image);
		}

		return 1;
	}

}
