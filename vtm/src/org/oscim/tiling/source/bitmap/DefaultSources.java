package org.oscim.tiling.source.bitmap;

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
			super("http://a.tile.stamen.com/toner", 0, 18);
		}
	}

	public static class StamenWatercolor extends BitmapTileSource {
		public StamenWatercolor() {
			super("http://a.tile.stamen.com/watercolor", 0, 16);
		}
	}

	public static class ImagicoLandcover extends BitmapTileSource {
		public ImagicoLandcover() {
			super("http://www.imagico.de/map/tiles/landcover", 0, 6, ".jpg");
		}
	}

	public static class MapQuestAerial extends BitmapTileSource {
		public MapQuestAerial() {
			super("http://otile1.mqcdn.com/tiles/1.0.0/sat", 0, 8, ".jpg");
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
			super("http://server.arcgisonline.com/ArcGIS/rest/services" +
			        "/World_Shaded_Relief/MapServer/tile/",
			      "{Z}/{Y}/{X}", 0, 13);
		}
	}

	public static class HillShadeHD extends BitmapTileSource {
		public HillShadeHD() {
			super("http://129.206.74.245:8004/tms_hs.ashx",
			      "?x={X}&y={Y}&z={Z}", 2, 16);
		}
	}

	/**
	 * https://github.com/opensciencemap/vtm/issues/18
	 * https://developers.google.com/maps/faq
	 */
	public static class GoogleMaps extends BitmapTileSource {
		public GoogleMaps(String hostName) {
			super(hostName, "/vt/x={X}&y={Y}&z={Z}&s=Galileo&scale=2", 1, 20); //jpeg for sat
		}
	}

	final static FadeStep[] fadeSteps = new FadeStep[] {
	        new FadeStep(0, 8 - 1, 1, 0.7f),
	        // dont fade between zoom-min/max
	        // fade above zoom max + 2, interpolate 1 to 0
	        new FadeStep(8 - 1, 8 + 1, 0.7f, 0)
	};
}
