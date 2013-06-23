package org.oscim.awt;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;

import javax.imageio.ImageIO;

import org.oscim.backend.canvas.Bitmap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.BufferUtils;

public class AwtBitmap implements Bitmap {

	BufferedImage bitmap;
	int width;
	int height;

	public AwtBitmap(int width, int height, int format) {
		bitmap = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		this.width = width;
		this.height = height;
	}

	AwtBitmap(InputStream inputStream) throws IOException {
        this.bitmap = ImageIO.read(inputStream);
}
	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public int[] getPixels() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void eraseColor(int transparent) {
		// TODO Auto-generated method stub
	}

	private static IntBuffer tmpBuffer = BufferUtils.newIntBuffer(256 * 256);
	private static int[] tmpPixel = new int[256 * 256];
	@Override
	public int uploadToTexture(boolean replace) {
		int[] pixels;
		IntBuffer buffer;

		if (width == 256 && height == 256){
			pixels = tmpPixel;
			buffer = tmpBuffer;
			buffer.clear();
		}else{
			pixels  = new int[width * height];
			buffer  = BufferUtils.newIntBuffer(width * height);
		}


		bitmap.getRGB(0, 0, width, height, pixels, 0, width);

		buffer.put(pixels);
		buffer.flip();

		Gdx.gl20.glTexImage2D(GL20.GL_TEXTURE_2D, 0, GL20.GL_RGBA, width,
				height, 0, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, buffer);

		return 0;
	}

	@Override
    public void recycle() {
	    // TODO Auto-generated method stub

    }
}
