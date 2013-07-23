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
		if (!this.bitmap.isAlphaPremultiplied())
			this.bitmap.coerceData(true);
	}

	AwtBitmap(InputStream inputStream) throws IOException {

		this.bitmap = ImageIO.read(inputStream);
		this.width = this.bitmap.getWidth();
		this.height = this.bitmap.getHeight();
		if (!this.bitmap.isAlphaPremultiplied() && this.bitmap.getType() == BufferedImage.TYPE_INT_ARGB)
			this.bitmap.coerceData(true);
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
		return null;
	}

	@Override
	public void eraseColor(int transparent) {
	}

	private static IntBuffer tmpBuffer = BufferUtils.newIntBuffer(512 * 256);
	private static int[] tmpPixel = new int[512 * 256];

	@Override
	public int uploadToTexture(boolean replace) {
		int[] pixels;
		IntBuffer buffer;

		if (width * height < 512 * 256) {
			pixels = tmpPixel;
			buffer = tmpBuffer;
			buffer.clear();
		} else {
			pixels = new int[width * height];
			buffer = BufferUtils.newIntBuffer(width * height);
		}

		// FIXME dont convert to argb when there data is greyscale
		bitmap.getRGB(0, 0, width, height, pixels, 0, width);

		for (int i = 0, n =  width * height; i < n; i++){
			int c = pixels[i];
			// flip blue with red - silly Java
			pixels[i] =  (c & 0xff00ff00) | (c & 0x00ff0000) >>> 16 | (c & 0x000000ff) << 16;
		}

		buffer.put(pixels, 0, width * height);
		buffer.flip();

		Gdx.gl20.glTexImage2D(GL20.GL_TEXTURE_2D, 0, GL20.GL_RGBA, width,
								height, 0, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, buffer);

		return 0;
	}

	@Override
	public void recycle() {
	}
}
