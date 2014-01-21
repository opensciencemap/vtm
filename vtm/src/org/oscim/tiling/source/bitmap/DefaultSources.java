package org.oscim.tiling.source.bitmap;

import org.oscim.core.Tile;
import org.oscim.layers.tile.BitmapTileLayer.FadeStep;

public class DefaultSources {

	public static class OpenStreetMap extends BitmapTileSource {
		public OpenStreetMap() {
			super("http://tile.openstreetmap.org", 0, 18);
		}
	}

	public static class OSMTransport extends BitmapTileSource {
		public OSMTransport() {
			super("http://a.tile.thunderforest.com/transport", 0, 18);
		}
	}

	public static class StamenToner extends BitmapTileSource {
		public StamenToner() {
			super("http://a.tile.stamen.com/toner", 0, 16);
		}
	}

	public static class StamenWatercolor extends BitmapTileSource {
		public StamenWatercolor() {
			super("http://a.tile.stamen.com/watercolor", 0, 16);
		}
	}

	public static class ImagicoLandcover extends BitmapTileSource {
		public ImagicoLandcover() {
			super("http://www.imagico.de/map/tiles/landcover",
			      0, 6, "image/jpeg", ".jpg");
		}
	}

	public static class MapQuestAerial extends BitmapTileSource {
		public MapQuestAerial() {
			super("http://otile1.mqcdn.com/tiles/1.0.0/sat",
			      0, 8, "image/jpeg", ".jpg");
		}

		@Override
		public FadeStep[] getFadeSteps() {
			return fadeSteps;
		}
	}

	public static class NaturalEarth extends BitmapTileSource {
		public NaturalEarth() {
			super("http://opensciencemap.org/tiles/ne", 0, 8);
		}

		@Override
		public FadeStep[] getFadeSteps() {
			return fadeSteps;
		}
	}

	public static class ArcGISWorldShaded extends BitmapTileSource {
		public ArcGISWorldShaded() {
			super("http://server.arcgisonline.com/ArcGIS/rest/services",
			      0, 6, "image/jpg", "");
		}

		@Override
		public String getTileUrl(Tile tile) {
			StringBuilder sb = new StringBuilder(32);
			//sb.append("/World_Imagery/MapServer/tile/");
			sb.append("/World_Shaded_Relief/MapServer/tile/");
			sb.append(tile.zoomLevel);
			sb.append('/').append(tile.tileY);
			sb.append('/').append(tile.tileX);
			return sb.toString();
		}
	}

	public static class HillShadeHD extends BitmapTileSource {
		public HillShadeHD() {
			super("http://129.206.74.245:8004/tms_hs.ashx",
			      2, 16, "image/png", "");
		}

		@Override
		public String getTileUrl(Tile tile) {
			StringBuilder sb = new StringBuilder(32);
			sb.append("?x=").append(tile.tileX);
			sb.append("&y=").append(tile.tileY);
			sb.append("&z=").append(tile.zoomLevel);
			return sb.toString();
		}
	}

	final static FadeStep[] fadeSteps = new FadeStep[] {
	        new FadeStep(0, 8 - 1, 1, 0.7f),
	        // dont fade between zoom-min/max
	        // fade above zoom max + 2, interpolate 1 to 0
	        new FadeStep(8 - 1, 8 + 1, 0.7f, 0)
	};
}
