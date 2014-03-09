package org.oscim.layers.tile.vector.labeling;

import static org.oscim.core.GeometryBuffer.GeometryType.LINE;
import static org.oscim.core.GeometryBuffer.GeometryType.POINT;
import static org.oscim.core.GeometryBuffer.GeometryType.POLY;

import org.oscim.core.MapElement;
import org.oscim.core.PointF;
import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.vector.VectorTileLayer.TileLoaderHook;
import org.oscim.layers.tile.vector.WayDecorator;
import org.oscim.renderer.elements.ElementLayers;
import org.oscim.renderer.elements.SymbolItem;
import org.oscim.renderer.elements.TextItem;
import org.oscim.theme.styles.RenderStyle;
import org.oscim.theme.styles.Symbol;
import org.oscim.theme.styles.Text;

public class LabelTileLoaderHook implements TileLoaderHook {

	@Override
	public void render(MapTile tile, ElementLayers layers, MapElement element,
	        RenderStyle style, int level) {

		if (style instanceof Text) {
			Text text = (Text) style;
			if (element.type == LINE) {
				String value = element.tags.getValue(text.textKey);
				if (value == null || value.length() == 0)
					return;

				int offset = 0;
				for (int i = 0, n = element.index.length; i < n; i++) {
					int length = element.index[i];
					if (length < 4)
						break;

					WayDecorator.renderText(null, element.points, value, text,
					                        offset, length, tile);
					offset += length;
				}
			}
			else if (element.type == POLY) {
				// TODO place somewhere on polygon
				String value = element.tags.getValue(text.textKey);
				if (value == null || value.length() == 0)
					return;

				float x = 0;
				float y = 0;
				int n = element.index[0];

				for (int i = 0; i < n;) {
					x += element.points[i++];
					y += element.points[i++];
				}
				x /= (n / 2);
				y /= (n / 2);

				tile.labels.push(TextItem.pool.get().set(x, y, value, text));
			}
			else if (element.type == POINT) {
				String value = element.tags.getValue(text.textKey);
				if (value == null || value.length() == 0)
					return;

				for (int i = 0, n = element.getNumPoints(); i < n; i++) {
					PointF p = element.getPoint(i);
					tile.labels.push(TextItem.pool.get().set(p.x, p.y, value, text));
				}
			}
		}
		else if ((element.type == POINT) && (style instanceof Symbol)) {
			Symbol symbol = (Symbol) style;

			if (symbol.texture == null)
				return;

			for (int i = 0, n = element.getNumPoints(); i < n; i++) {
				PointF p = element.getPoint(i);

				SymbolItem it = SymbolItem.pool.get();
				it.set(p.x, p.y, symbol.texture, true);
				tile.symbols.push(it);
			}
		}
	}

}
