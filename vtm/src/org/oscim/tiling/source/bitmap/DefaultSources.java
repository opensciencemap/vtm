package org.oscim.tiling.source.bitmap;

import org.oscim.core.Tile;
import org.oscim.layers.tile.bitmap.BitmapTileLayer.FadeStep;

/**
 * Do not use in applications unless you read through and comply to
 * their terms of use! Only added here for testing puposes.
 */
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
			super("http://opensciencemap.org/cors-stamen/toner", 0, 16);
		}
	}

	public static class StamenWatercolor extends BitmapTileSource {
		public StamenWatercolor() {
			super("http://a.tile.stamen.com/watercolor", 0, 16);
		}
	}

	public static class ImagicoLandcover extends BitmapTileSource {
		public ImagicoLandcover() {
			super("http://www.imagico.de/map/tiles/landcover", 0, 6);
			setExtension(".jpg");
		}
	}

	public static class MapQuestAerial extends BitmapTileSource {
		public MapQuestAerial() {
			super("http://otile1.mqcdn.com/tiles/1.0.0/sat", 0, 8);
			setExtension(".jpg");
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
		private final StringBuilder sb = new StringBuilder(32);

		public ArcGISWorldShaded() {
			super("http://server.arcgisonline.com/ArcGIS/rest/services", 0, 13);
		}

		@Override
		public synchronized String getTileUrl(Tile tile) {
			sb.setLength(0);
			//sb.append("/World_Imagery/MapServer/tile/");
			sb.append("/World_Shaded_Relief/MapServer/tile/");
			sb.append(tile.zoomLevel);
			sb.append('/').append(tile.tileY);
			sb.append('/').append(tile.tileX);
			return sb.toString();
		}
	}

	public static class HillShadeHD extends BitmapTileSource {
		private final StringBuilder sb = new StringBuilder(32);

		public HillShadeHD() {
			super("http://129.206.74.245:8004/tms_hs.ashx", 2, 16);
		}

		@Override
		public synchronized String getTileUrl(Tile tile) {
			sb.setLength(0);
			sb.append("?x=").append(tile.tileX);
			sb.append("&y=").append(tile.tileY);
			sb.append("&z=").append(tile.zoomLevel);
			return sb.toString();
		}
	}

	/**
	 * https://github.com/opensciencemap/vtm/issues/18
	 * https://developers.google.com/maps/faq
	 */
	public static class GoogleMaps extends BitmapTileSource {
		private final StringBuilder sb = new StringBuilder(60);

		public GoogleMaps(String hostName) {
			super(hostName, 1, 20); //jpeg for sat
		}

		@Override
		public synchronized String getTileUrl(Tile tile) {
			sb.setLength(0);
			sb.append("/vt/x="); //lyrs=y&
			sb.append(tile.tileX);
			sb.append("&y=");
			sb.append(tile.tileY);
			sb.append("&z=");
			sb.append(tile.zoomLevel);
			sb.append("&s=Galileo&scale=2");
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
