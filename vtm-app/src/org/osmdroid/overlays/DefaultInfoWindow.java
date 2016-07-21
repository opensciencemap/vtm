package org.osmdroid.overlays;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.oscim.android.MapView;
import org.oscim.app.App;
import org.osmdroid.utils.BonusPackHelper;

/**
 * Default implementation of InfoWindow. It handles a text and a description. It
 * also handles optionally a sub-description and an image. Clicking on the
 * bubble will close it.
 *
 * @author M.Kergall
 */
public class DefaultInfoWindow extends InfoWindow {

    // resource ids
    private static int mTitleId = 0, mDescriptionId = 0, mSubDescriptionId = 0, mImageId = 0;

    private static void setResIds(Context context) {
        // get application package name
        String packageName = context.getPackageName();
        Resources res = context.getResources();

        mTitleId = res.getIdentifier("id/bubble_title", null, packageName);
        mDescriptionId = res.getIdentifier("id/bubble_description", null, packageName);
        mSubDescriptionId = res.getIdentifier("id/bubble_subdescription", null, packageName);
        mImageId = res.getIdentifier("id/bubble_image", null, packageName);

        if (mTitleId == 0 || mDescriptionId == 0) {
            Log.e(BonusPackHelper.LOG_TAG, "DefaultInfoWindow: unable to get res ids in "
                    + packageName);
        }
    }

    public DefaultInfoWindow(int layoutResId, MapView mapView) {
        super(layoutResId, mapView);

        if (mTitleId == 0)
            setResIds(App.activity);

        // default behaviour: close it when clicking on the bubble:
        mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                close();
            }
        });
    }

    @Override
    public void onOpen(ExtendedMarkerItem item) {
        String title = item.getTitle();
        if (title == null)
            title = "";

        ((TextView) mView.findViewById(mTitleId)).setText(title);

        String snippet = item.getDescription();
        if (snippet == null)
            snippet = "";

        ((TextView) mView.findViewById(mDescriptionId)).setText(snippet);

        // handle sub-description, hidding or showing the text view:
        TextView subDescText = (TextView) mView.findViewById(mSubDescriptionId);
        String subDesc = item.getSubDescription();
        if (subDesc != null && !("".equals(subDesc))) {
            subDescText.setText(subDesc);
            subDescText.setVisibility(View.VISIBLE);
        } else {
            subDescText.setVisibility(View.GONE);
        }

        // handle image
        ImageView imageView = (ImageView) mView.findViewById(mImageId);
        Drawable image = item.getImage();
        if (image != null) {
            // or setBackgroundDrawable(image)?
            imageView.setImageDrawable(image);
            imageView.setVisibility(View.VISIBLE);
        } else
            imageView.setVisibility(View.GONE);

    }

    @Override
    public void onClose() {
        // by default, do nothing
    }

}
