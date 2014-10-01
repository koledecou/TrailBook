package com.trailbook.kole.fragments.point_attched_object_view.climb;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.squareup.picasso.Picasso;
import com.trailbook.kole.activities.R;
import com.trailbook.kole.data.Climb;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.fragments.point_attched_object_view.PointAttachedObjectView;
import com.trailbook.kole.helpers.PreferenceUtilities;
import com.trailbook.kole.helpers.TrailbookFileUtilities;
import com.trailbook.kole.state_objects.BusProvider;
import com.trailbook.kole.state_objects.TrailBookState;

public class ClimbView extends PointAttachedObjectView {
    String mImageFileName = null;

    TextView mTextViewLocationInfo;
    ImageView mImageView;
    ImageView mExpandImage;

    TextView mTextViewName;
    TextView mTextViewGrade;
    TextView mTextViewDescription;
    //todo: rack, type, length, etc...

    public ClimbView(Context context) {
        super(context);
    }

    public ClimbView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ClimbView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void populateFieldsFromObject(PointAttachedObject pao) {
        Climb climb = (Climb)pao.getAttachment();
        mTextViewName.setText(climb.getName());
        mTextViewGrade.setText(climb.getGrade().grade);
        mTextViewDescription.setText(climb.getDescription());

        mImageFileName = climb.getImageFileName();
        if (mImageFileName != null && mImageFileName.length()>0) {
            Log.d(Constants.TRAILBOOK_TAG, "loading image :" + mImageFileName);
            Picasso.with(getContext()).load(TrailbookFileUtilities.getInternalImageFile(mImageFileName)).into(mImageView);
        }
    }

    public void setRelativeLocationString() {
        LatLng noteLocation = mPaObject.getLocation();
        float[] results = new float[5];
        if (mCurrentLocation == null) {
            mCurrentLocation = TrailBookState.getCurrentLocation();
        }

        if (noteLocation != null && mCurrentLocation != null) {
            Log.d(Constants.TRAILBOOK_TAG, "NoteView: note location:" + noteLocation);
            Log.d(Constants.TRAILBOOK_TAG, "NoteView: current location: " + mCurrentLocation);

            Location.distanceBetween(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(), noteLocation.latitude, noteLocation.longitude, results);
            //String relativeLocationMessage = getContext().getResources().getString(R.string.relative_location_message);
            String distAndBearingString  = PreferenceUtilities.getDistanceAndBearingString(getContext(), results[0], results[1]);
            mTextViewLocationInfo.setText(distAndBearingString);
        } else {
            mTextViewLocationInfo.setText("");
        }
        mTextViewLocationInfo.invalidate();
    }

    @Override
    public void loadViews(){
        BusProvider.getInstance().register(this);
        LayoutInflater inflater = LayoutInflater.from(this.getContext());
        inflater.inflate(getLayoutId(), this);

        mTextViewName=(TextView)findViewById(R.id.vc_name);
        mTextViewGrade=(TextView)findViewById(R.id.vc_grade);
        mTextViewDescription=(TextView)findViewById(R.id.vc_description);

        mImageView=(ImageView)findViewById(R.id.vn_image);
        mTextViewLocationInfo=(TextView)findViewById(R.id.vn_navigation_details);
        mTextViewLocationInfo.setMinWidth(R.dimen.small_note_view_min_text_panel_width);

        mExpandImage = (ImageView)findViewById(R.id.vn_button_expand);
        if (mExpandImage != null) {
            if (TrailBookState.getMode() == TrailBookState.MODE_LEAD
                    || TrailBookState.getMode() == TrailBookState.MODE_EDIT) {
                mExpandImage.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_expand_edit));
            }
        }
    }
}
