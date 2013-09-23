package org.oscim.gdx.client;

import com.google.gwt.core.client.JavaScriptObject;

class MapConfig extends JavaScriptObject {
	protected MapConfig() {
	}

	public static native MapConfig get()/*-{
		return $wnd.mapconfig;
	}-*/;

	public final native double getLatitude() /*-{
		return this.latitude;
	}-*/;

	public final native double getLongitude() /*-{
		return this.longitude;
	}-*/;

	public final native int getZoom() /*-{
		return this.zoom;
	}-*/;

	public final native String getTileSource() /*-{
		return this.tilesource;
	}-*/;

	public final native String getTileUrl() /*-{
		return this.tileurl;
	}-*/;

	public final native String getBackgroundLayer() /*-{
		return this.background;
	}-*/;
}
