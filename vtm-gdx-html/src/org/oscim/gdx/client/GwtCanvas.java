package org.oscim.gdx.client;

import org.oscim.backend.Log;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Paint;

import com.google.gwt.canvas.dom.client.Context2d;

public class GwtCanvas implements org.oscim.backend.canvas.Canvas {
	GwtBitmap bitmap;
	public GwtCanvas() {
		// canvas comes with gdx pixmap
	}

	@Override
	public void setBitmap(Bitmap bitmap) {
		this.bitmap = (GwtBitmap) bitmap;
		//this.bitmap.pixmap.setColor(0x00ffffff);
		this.bitmap.pixmap.getContext().clearRect(0, 0, this.bitmap.getWidth(), this.bitmap.getHeight());
	}

	@Override
	public void drawText(String string, float x, float y, Paint paint) {
		if (bitmap == null){
			Log.d("", "no bitmap set on canvas");
		}

		GwtPaint p = (GwtPaint) paint;
		Context2d ctx = bitmap.pixmap.getContext();
		ctx.setFont(p.font);

		if (p.stroke){
			//Log.d("", "stroke " + p.stroke + " " + p.color + " " + p.font + " "+ string);
			ctx.setStrokeStyle(p.color);
			ctx.strokeText(string, x, y);
		} else{
			//Log.d("", "fill " + p.stroke + " " + p.color + " " + p.font + " "+ string);
			ctx.setFillStyle(p.color);
			ctx.fillText(string, x, y);
		}
	}

	@Override
	public void drawBitmap(Bitmap bitmap, float x, float y) {
		// TODO Auto-generated method stub

	}

}
