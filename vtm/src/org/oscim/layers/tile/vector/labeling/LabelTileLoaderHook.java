package org.oscim.layers.tile.vector.labeling;

import static org.oscim.core.GeometryBuffer.GeometryType.LINE;
import static org.oscim.core.GeometryBuffer.GeometryType.POINT;
import static org.oscim.core.GeometryBuffer.GeometryType.POLY;
import static org.oscim.layers.tile.vector.labeling.LabelLayer.LABEL_DATA;

import org.oscim.core.MapElement;
import org.oscim.core.PointF;
import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.vector.VectorTileLayer.TileLoaderThemeHook;
import org.oscim.renderer.elements.ElementLayers;
import org.oscim.renderer.elements.SymbolItem;
import org.oscim.renderer.elements.TextItem;
import org.oscim.theme.styles.RenderStyle;
import org.oscim.theme.styles.SymbolStyle;
import org.oscim.theme.styles.TextStyle;

public class LabelTileLoaderHook implements TileLoaderThemeHook {

	//public final static LabelTileData EMPTY = new LabelTileData();

	private LabelTileData get(MapTile tile) {
		// FIXME could be 'this'..
		LabelTileData ld = (LabelTileData) tile.getData(LABEL_DATA);
		if (ld == null) {
			ld = new LabelTileData();
			tile.addData(LABEL_DATA, ld);
		}
		return ld;
	}

	@Override
	public boolean render(MapTile tile, ElementLayers layers, MapElement element,
	        RenderStyle style, int level) {

		if (style instanceof TextStyle) {
			LabelTileData ld = get(tile);

			TextStyle text = (TextStyle) style;
			if (element.type == LINE) {
				String value = element.tags.getValue(text.textKey);
				if (value == null || value.length() == 0)
					return false;

				int offset = 0;
				for (int i = 0, n = element.index.length; i < n; i++) {
					int length = element.index[i];
					if (length < 4)
						break;

					WayDecorator.renderText(null, element.points, value, text,
					                        offset, length, ld);
					offset += length;
				}
			}
			else if (element.type == POLY) {
				// TODO place somewhere on polygon
				String value = element.tags.getValue(text.textKey);
				if (value == null || value.length() == 0)
					return false;

				float x = 0;
				float y = 0;
				int n = element.index[0];

				for (int i = 0; i < n;) {
					x += element.points[i++];
					y += element.points[i++];
				}
				x /= (n / 2);
				y /= (n / 2);

				ld.labels.push(TextItem.pool.get().set(x, y, value, text));
			}
			else if (element.type == POINT) {
				String value = element.tags.getValue(text.textKey);
				if (value == null || value.length() == 0)
					return false;

				for (int i = 0, n = element.getNumPoints(); i < n; i++) {
					PointF p = element.getPoint(i);
					ld.labels.push(TextItem.pool.get().set(p.x, p.y, value, text));
				}
			}
		}
		else if ((element.type == POINT) && (style instanceof SymbolStyle)) {
			SymbolStyle symbol = (SymbolStyle) style;

			if (symbol.texture == null)
				return false;

			LabelTileData ld = get(tile);

			for (int i = 0, n = element.getNumPoints(); i < n; i++) {
				PointF p = element.getPoint(i);

				SymbolItem it = SymbolItem.pool.get();
				it.set(p.x, p.y, symbol.texture, true);
				ld.symbols.push(it);
			}
		}
		return false;
	}

	@Override
	public void complete(MapTile tile, boolean success) {
	}

}
