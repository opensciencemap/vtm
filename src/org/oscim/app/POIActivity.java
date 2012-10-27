/*
 * Copyright 2012 osmdroidbonuspack: M.Kergall
 * Copyright 2012 Hannes Janetzek
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.oscim.app;

import java.util.ArrayList;

import org.osmdroid.location.POI;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Activity showing POIs as a list.
 * @author M.Kergall
 */

public class POIActivity extends Activity {

	AutoCompleteTextView poiTagText;
	POIAdapter mAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.items_list);

		//		TextView title = (TextView) findViewById(R.id.title);
		//		title.setText("Points of Interest");

		ListView list = (ListView) findViewById(R.id.items);

		Intent myIntent = getIntent();

		final ArrayList<POI> pois = myIntent.getParcelableArrayListExtra("POI");
		final int currentNodeId = myIntent.getIntExtra("ID", -1);
		POIAdapter adapter = new POIAdapter(this, pois);
		mAdapter = adapter;
		//		Log.d(App.TAG, "got POIs:" + (pois == null ? pois : pois.size()));

		list.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int position, long index) {
				Intent intent = new Intent();
				intent.putExtra("ID", position);
				setResult(RESULT_OK, intent);
				finish();
			}
		});

		list.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {

			@Override
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
				// TODO Auto-generated method stub

				Log.d(App.TAG, "context menu created 2");

			}
		});

		list.setAdapter(adapter);
		list.setSelection(currentNodeId);

		// POI search interface:
		String[] poiTags = getResources().getStringArray(R.array.poi_tags);
		poiTagText = (AutoCompleteTextView) findViewById(R.id.poiTag);
		ArrayAdapter<String> textadapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_dropdown_item_1line, poiTags);
		poiTagText.setAdapter(textadapter);

		Button setPOITagButton = (Button) findViewById(R.id.buttonSetPOITag);
		setPOITagButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//Hide the soft keyboard:
				InputMethodManager imm = (InputMethodManager)
						getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(poiTagText.getWindowToken(), 0);
				//Start search:
				App.poiSearch.getPOIAsync(poiTagText.getText().toString());
			}
		});

		registerForContextMenu(list);

		// only show keyboard when nothing in the list yet
		if (pois == null || pois.size() == 0) {
			poiTagText.postDelayed(new Runnable() {
				@Override
				public void run() {
					InputMethodManager keyboard = (InputMethodManager)
							getSystemService(Context.INPUT_METHOD_SERVICE);
					keyboard.showSoftInput(poiTagText, 0);
				}
			}, 200);
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		Log.d(App.TAG, "NEW INTENT!!!!");
		// from SearchableDictionary Example:
		// Because this activity has set launchMode="singleTop", the system calls this method
		// to deliver the intent if this activity is currently the foreground activity when
		// invoked again (when the user executes a search from this activity, we don't create
		// a new instance of this activity, so the system delivers the search intent here)
		//	        handleIntent(intent);

		final ArrayList<POI> pois = intent.getParcelableArrayListExtra("POI");
		//		final int currentNodeId = intent.getIntExtra("ID", -1);
		//		POIAdapter adapter = new POIAdapter(this, pois);
		mAdapter.setPOI(pois);
		mAdapter.notifyDataSetChanged();
	}

	// http://www.mikeplate.com/2010/01/21/show-a-context-menu-for-long-clicks-in-an-android-listview/
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		if (v.getId() == R.id.items) {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			Log.d(App.TAG, "list context menu created " + info.position);

			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.poi_menu, menu);

		}

		super.onCreateContextMenu(menu, v, menuInfo);

	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		Log.d(App.TAG, "context menu item selected " + item.getItemId());

		if (item.getItemId() == R.id.menu_link) {

			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
					.getMenuInfo();

			POI poi = (POI) mAdapter.getItem(info.position);
			if (poi != null && poi.url != null) {

				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(poi.url));
				startActivity(i);

			}
			return true;

		}

		return super.onContextItemSelected(item);
	}

}

class POIAdapter extends BaseAdapter implements OnClickListener {
	private Context mContext;
	private ArrayList<POI> mPois;

	public POIAdapter(Context context, ArrayList<POI> pois) {
		mContext = context;
		mPois = pois;
	}

	public void setPOI(ArrayList<POI> pois) {
		mPois = pois;
	}

	@Override
	public int getCount() {
		if (mPois == null)
			return 0;

		return mPois.size();
	}

	@Override
	public Object getItem(int position) {
		if (mPois == null)
			return null;

		return mPois.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup viewGroup) {
		POI entry = (POI) getItem(position);
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater) mContext
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.item_layout, null);
		} else {

		}

		TextView tvTitle = (TextView) convertView.findViewById(R.id.title);
		tvTitle.setText((entry.url == null ? "" : "[link] ") + entry.type);
		TextView tvDetails = (TextView) convertView.findViewById(R.id.details);
		tvDetails.setText(entry.description);

		ImageView iv = (ImageView) convertView.findViewById(R.id.thumbnail);
		//ivManeuver.setImageBitmap(entry.mThumbnail);
		//		iv.getT
		//		entry.fetchThumbnailOnThread(iv);
		entry.fetchThumbnail(iv);

		return convertView;
	}

	@Override
	public void onClick(View arg0) {
		Log.d(App.TAG, "click" + arg0.getId());
		//nothing to do.
	}

}
