package org.oscim.android.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;

import org.jeo.data.VectorDataset;
import org.jeo.map.Style;
import org.oscim.layers.OSMIndoorLayer;
import org.oscim.layers.tile.vector.BuildingLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.test.JeoTest;
import org.oscim.theme.VtmThemes;
import org.oscim.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;

public class JeoIndoorMapActivity extends BaseMapActivity {
	public static final Logger log = LoggerFactory.getLogger(JeoIndoorMapActivity.class);

	// from http://overpass-turbo.eu/s/2vp
	String PATH = "https://gist.github.com/anonymous/8960337/raw/overpass.geojson";
	//String PATH = "https://gist.github.com/hjanetzek/9280925/raw/overpass.geojson";

	private OSMIndoorLayer mIndoorLayer;

	public JeoIndoorMapActivity() {
		super(R.layout.jeo_indoor_map);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mMap.addTask(new Runnable() {
			@Override
			public void run() {
				showToast("load data");
				InputStream is = null;
				try {
					//	File file = new File(Environment.getExternalStorageDirectory()
					//	    .getAbsolutePath(), "osmindoor.json");
					//	is = new FileInputStream(file);

					URL url = new URL(PATH);
					URLConnection conn = url.openConnection();
					is = conn.getInputStream();
					loadJson(is);
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					IOUtils.closeQuietly(is);
				}
			}
		});

		mMap.layers().add(new BuildingLayer(mMap, mBaseLayer));
		mMap.layers().add(new LabelLayer(mMap, mBaseLayer));
		mMap.setTheme(VtmThemes.TRONRENDER);

		//mMap.setMapPosition(49.417, 8.673, 1 << 17);
		mMap.setMapPosition(53.5620092, 9.9866457, 1 << 16);

		//	mMap.layers().add(new TileGridLayer(mMap));
		//	String file = Environment.getExternalStorageDirectory().getAbsolutePath();
		//	VectorDataset data = (VectorDataset) JeoTest.getJsonData(file + "/states.json", true);
		//	Style style = JeoTest.getStyle();
		//	mMap.layers().add(new JeoVectorLayer(mMap, data, style));
	}

	void loadJson(InputStream is) {
		showToast("got data");

		VectorDataset data = JeoTest.readGeoJson(is);
		Style style = JeoTest.getStyle();
		mIndoorLayer = new OSMIndoorLayer(mMap, data, style);
		mMap.layers().add(mIndoorLayer);

		showToast("data ready");
		mMap.updateMap(true);

		mIndoorLayer.activeLevels[0] = true;
		shift();
	}

	public void showToast(final String text) {
		final Context ctx = this;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast toast = Toast.makeText(ctx, text, Toast.LENGTH_SHORT);
				toast.show();
			}
		});
	}

	boolean mShift = true;

	public void shift() {
		if (!mShift)
			return;

		mMap.postDelayed(new Runnable() {

			@Override
			public void run() {
				for (int i = 0; i < 10; i++) {
					if (mIndoorLayer.activeLevels[i]) {
						mIndoorLayer.activeLevels[i] = false;
						mIndoorLayer.activeLevels[(i + 1) % 9] = true;
						mIndoorLayer.update();
						break;
					}
				}
				shift();
			}
		}, 200);

	}

	public void onClick(View v) {
		mShift = false;

		if (mIndoorLayer == null)
			return;

		int i = 0;

		if (v instanceof ToggleButton) {
			ToggleButton b = (ToggleButton) v;
			i = (b.getTextOn().charAt(0) - '0') + 1;
		}

		if (i < 0 || i > 9)
			i = 0;

		mIndoorLayer.activeLevels[i] ^= true;
		((ToggleButton) v).setChecked(mIndoorLayer.activeLevels[i]);
		log.debug(Arrays.toString(mIndoorLayer.activeLevels));
		mIndoorLayer.update();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}
}
