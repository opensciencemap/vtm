package org.oscim.gdx.client;

import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Paint;

import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.Context2d.LineJoin;

public class GwtCanvas implements org.oscim.backend.canvas.Canvas {
	GwtBitmap bitmap;

	public GwtCanvas() {
		// canvas comes with gdx pixmap
	}

	@Override
	public void setBitmap(Bitmap bitmap) {
		this.bitmap = (GwtBitmap) bitmap;
		Context2d ctx = this.bitmap.pixmap.getContext();

		ctx.clearRect(0, 0, this.bitmap.getWidth(), this.bitmap.getHeight());
		ctx.setLineJoin(LineJoin.BEVEL);
	}

	@Override
	public void drawText(String string, float x, float y, Paint paint) {
		if (bitmap == null) {
			//log.debug("no bitmap set");
			return;
		}

		GwtPaint p = (GwtPaint) paint;

		if (p.stroke && GwtCanvasAdapter.NO_STROKE_TEXT)
			return;

		Context2d ctx = bitmap.pixmap.getContext();
		ctx.setFont(p.font);

		if (p.stroke) {
			ctx.setLineWidth(p.strokeWidth);
			ctx.setStrokeStyle(p.color);
			ctx.strokeText(string, (int) (x + 1), (int) (y + 1));
		} else {
			ctx.setFillStyle(p.color);
			ctx.fillText(string, (int) (x + 1), (int) (y + 1));
		}
	}

	@Override
	public void drawBitmap(Bitmap bitmap, float x, float y) {
	}
}
