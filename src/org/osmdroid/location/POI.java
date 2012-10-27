package org.osmdroid.location;

import org.oscim.app.R;
import org.oscim.core.GeoPoint;
import org.osmdroid.utils.BonusPackHelper;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

/**
 * Point of Interest. Exact content may depend of the POI provider used.
 * @see NominatimPOIProvider
 * @see GeoNamesPOIProvider
 * @author M.Kergall
 */
public class POI implements Parcelable {
	/** IDs of POI services */
	public static int POI_SERVICE_NOMINATIM = 100;
	public static int POI_SERVICE_GEONAMES_WIKIPEDIA = 200;
	public static int POI_SERVICE_FLICKR = 300;
	public static int POI_SERVICE_PICASA = 400;

	/** Identifies the service provider of this POI. */
	public int serviceId;
	/** Nominatim: OSM ID. GeoNames: 0 */
	public long id;
	/** location of the POI */
	public GeoPoint location;
	/** Nominatim "class", GeoNames "feature" */
	public String category;
	/** type or title */
	public String type;
	/** can be the name, the address, a short description */
	public String description;
	/** url of the thumbnail. Null if none */
	public String thumbnailPath;
	/** the thumbnail itself. Null if none */
	public Bitmap thumbnail;
	/** url to a more detailed information page about this POI. Null if none */
	public String url;
	/**
	 * popularity of this POI, from 1 (lowest) to 100 (highest). 0 if not
	 * defined.
	 */
	public int rank;
	/** number of attempts to load the thumbnail that have failed */
	protected int mThumbnailLoadingFailures;

	public POI(int serviceId) {
		this.serviceId = serviceId;
		// lets all other fields empty or null. That's fine.
	}

	protected static int MAX_LOADING_ATTEMPTS = 2;

	/**
	 * @return the POI thumbnail as a Bitmap, if any. If not done yet, it will
	 *         load the POI thumbnail from its url (in thumbnailPath field).
	 */
	public Bitmap getThumbnail() {
		if (thumbnail == null && thumbnailPath != null) {
			Log.d(BonusPackHelper.LOG_TAG, "POI:load thumbnail:" + thumbnailPath);
			thumbnail = BonusPackHelper.loadBitmap(thumbnailPath);
			if (thumbnail == null) {
				mThumbnailLoadingFailures++;
				if (mThumbnailLoadingFailures >= MAX_LOADING_ATTEMPTS) {
					// this path really doesn't work, "kill" it for next calls:
					thumbnailPath = null;
				}
			}
		}
		return thumbnail;
	}

	//	http://stackoverflow.com/questions/7729133/using-asynctask-to-load-images-in-listview
	// TODO see link, there might be a better solution
	/**
	 * Fetch the thumbnail from its url on a thread.
	 * @param imageView
	 *            to update once the thumbnail is retrieved, or to hide if no
	 *            thumbnail.
	 */
	public void fetchThumbnail(final ImageView imageView) {
		if (thumbnail != null) {
			imageView.setImageBitmap(thumbnail);
			imageView.setVisibility(View.VISIBLE);
		} else if (thumbnailPath != null) {
			imageView.setImageResource(R.drawable.ic_empty);
			new ThumbnailTask(imageView).execute(imageView);
		} else {
			imageView.setVisibility(View.GONE);
		}
	}

	class ThumbnailTask extends AsyncTask<ImageView, Void, ImageView> {

		public ThumbnailTask(ImageView iv) {
			iv.setTag(thumbnailPath);
		}

		@Override
		protected ImageView doInBackground(ImageView... params) {
			getThumbnail();
			return params[0];
		}

		@Override
		protected void onPostExecute(ImageView iv) {
			if (thumbnailPath.equals(iv.getTag().toString()))
				iv.setImageBitmap(thumbnail);
		}
	}

	// --- Parcelable implementation

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeInt(serviceId);
		out.writeLong(id);
		out.writeParcelable(location, 0);
		out.writeString(category);
		out.writeString(type);
		out.writeString(description);
		out.writeString(thumbnailPath);
		out.writeParcelable(thumbnail, 0);
		out.writeString(url);
		out.writeInt(rank);
		out.writeInt(mThumbnailLoadingFailures);
	}

	public static final Parcelable.Creator<POI> CREATOR = new Parcelable.Creator<POI>() {
		@Override
		public POI createFromParcel(Parcel in) {
			POI poi = new POI(in.readInt());
			poi.id = in.readLong();
			poi.location = in.readParcelable(GeoPoint.class.getClassLoader());
			poi.category = in.readString();
			poi.type = in.readString();
			poi.description = in.readString();
			poi.thumbnailPath = in.readString();
			poi.thumbnail = in.readParcelable(Bitmap.class.getClassLoader());
			poi.url = in.readString();
			poi.rank = in.readInt();
			poi.mThumbnailLoadingFailures = in.readInt();
			return poi;
		}

		@Override
		public POI[] newArray(int size) {
			return new POI[size];
		}
	};

	//	private POI(Parcel in) {
	//		serviceId = in.readInt();
	//		id = in.readLong();
	//		location = in.readParcelable(GeoPoint.class.getClassLoader());
	//		category = in.readString();
	//		type = in.readString();
	//		description = in.readString();
	//		thumbnailPath = in.readString();
	//		thumbnail = in.readParcelable(Bitmap.class.getClassLoader());
	//		url = in.readString();
	//		rank = in.readInt();
	//		mThumbnailLoadingFailures = in.readInt();
	//	}
}
