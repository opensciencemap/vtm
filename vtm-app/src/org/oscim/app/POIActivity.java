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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
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

import org.osmdroid.location.FourSquareProvider;
import org.osmdroid.location.POI;

import java.util.List;

/**
 * Activity showing POIs as a list.
 *
 * @author M.Kergall
 */

// TODO implement:
// http://codehenge.net/blog/2011/06/android-development-tutorial-
//        asynchronous-lazy-loading-and-caching-of-listview-images/

public class POIActivity extends Activity {

    AutoCompleteTextView poiTagText;
    POIAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.items_list);

        ListView list = (ListView) findViewById(R.id.items);

        Intent myIntent = getIntent();

        final List<POI> pois = App.poiSearch.getPOIs();
        final int currentNodeId = myIntent.getIntExtra("ID", -1);
        POIAdapter adapter = new POIAdapter(this, pois);
        mAdapter = adapter;

        list.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View view, int position, long index) {
                //log.debug("poi on click: " + position);
                Intent intent = new Intent();
                intent.putExtra("ID", position);
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        list.setAdapter(adapter);
        list.setSelection(currentNodeId);

        // POI search interface:
        String[] poiTags = getResources().getStringArray(R.array.poi_tags);
        poiTagText = (AutoCompleteTextView) findViewById(R.id.poiTag);
        ArrayAdapter<String> textadapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line,
                poiTags);
        poiTagText.setAdapter(textadapter);

        //        Button setPOITagButton = (Button) findViewById(R.id.buttonSetPOITag);
        //        setPOITagButton.setOnClickListener(new View.OnClickListener() {
        //            @Override
        //            public void onClick(View v) {
        //                hideKeyboard();
        //                //Start search:
        //                App.poiSearch.getPOIAsync(poiTagText.getText().toString());
        //            }
        //        });

        // FIXME!
        Button btn = (Button) findViewById(R.id.pois_btn_flickr);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();
                App.poiSearch.getPOIAsync(POISearch.TAG_FLICKR);
            }
        });

        btn = (Button) findViewById(R.id.pois_btn_nominatim);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();
                String text = poiTagText.getText().toString();
                if (text == null || text.length() == 0)
                    App.poiSearch.getPOIAsync("bremen");
                else
                    App.poiSearch.getPOIAsync(text);
            }
        });

        btn = (Button) findViewById(R.id.pois_btn_wikipedia);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();
                App.poiSearch.getPOIAsync(POISearch.TAG_WIKIPEDIA);
            }
        });

        btn = (Button) findViewById(R.id.pois_btn_foursquare);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();
                App.poiSearch.getPOIAsync(POISearch.TAG_FOURSQUARE
                        + poiTagText.getText().toString());
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

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(poiTagText.getWindowToken(), 0);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // from SearchableDictionary Example:
        // Because this activity has set launchMode="singleTop", the system calls this method
        // to deliver the intent if this activity is currently the foreground activity when
        // invoked again (when the user executes a search from this activity, we don't create
        // a new instance of this activity, so the system delivers the search intent here)
        //            handleIntent(intent);

        //        final ArrayList<POI> pois = intent.getParcelableArrayListExtra("POI");
        //        final int currentNodeId = intent.getIntExtra("ID", -1);
        //        POIAdapter adapter = new POIAdapter(this, pois);
        //        mAdapter.setPOI(pois);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.items) {
            //AdapterView.AdapterContextMenuInfo info =
            // (AdapterView.AdapterContextMenuInfo) menuInfo;
            //log.debug("list context menu created " + info.position);

            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.poi_menu, menu);

        }

        super.onCreateContextMenu(menu, v, menuInfo);

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        //log.debug("context menu item selected " + item.getItemId());

        if (item.getItemId() == R.id.menu_link) {

            AdapterView.AdapterContextMenuInfo info =
                    (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

            POI poi = (POI) mAdapter.getItem(info.position);
            if (poi == null || poi.url == null)
                return false;

            if (poi.serviceId == POI.POI_SERVICE_4SQUARE) {
                FourSquareProvider.browse(this, poi);
                return true;
            } else {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                i.setData(Uri.parse(poi.url));
                startActivity(i);

            }
            return true;

        }

        return super.onContextItemSelected(item);
    }

    class POIObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            mAdapter.notifyDataSetChanged();
        }
    }
}

class POIAdapter extends BaseAdapter implements OnClickListener {
    private Context mContext;
    private final List<POI> mPois;

    public POIAdapter(Context context, List<POI> pois) {
        mContext = context;
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
    public View getView(int position, View view, ViewGroup viewGroup) {
        POI entry = (POI) getItem(position);
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.item_layout, null);

            ViewHolder holder = new ViewHolder();
            holder.title = (TextView) view.findViewById(R.id.title);
            holder.details = (TextView) view.findViewById(R.id.details);
            holder.thumbnail = (ImageView) view.findViewById(R.id.thumbnail);
            view.setTag(holder);
        }

        ViewHolder holder = (ViewHolder) view.getTag();

        holder.title.setText((entry.url == null ? "" : "[link] ") + entry.type);
        holder.details.setText(entry.description);

        entry.fetchThumbnail(holder.thumbnail);

        return view;
    }

    @Override
    public void onClick(View arg0) {
        //nothing to do.
    }

    class ViewHolder {
        public TextView title;
        public TextView details;
        public ImageView thumbnail;
    }
}
