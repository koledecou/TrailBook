package com.trailbook.kole.fragments.point_attched_object_view;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;
import com.trailbook.kole.activities.R;
import com.trailbook.kole.data.Attachment;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.events.LocationChangedEvent;
import com.trailbook.kole.helpers.PreferenceUtilities;
import com.trailbook.kole.helpers.TrailbookFileUtilities;
import com.trailbook.kole.state_objects.BusProvider;
import com.trailbook.kole.state_objects.PathManager;
import com.trailbook.kole.state_objects.TrailBookState;

import java.util.ArrayList;

/**
 * Created by kole on 9/29/2014.
 */
public class PointAttachedObjectView extends LinearLayout implements View.OnClickListener {
    int mLayoutId = R.layout.view_note_small;
    public Location mCurrentLocation;
    public PointAttachedObject mPaObject;
    String mPaoId = "1";
    protected ArrayList<String> mImageFileNames = null;
    protected ImageView mImageView;
    ImageView mExpandImage;
    TextView mTextViewLocationInfo;
    ImageView mNextArrowView;
    ImageView mPreviousArrowView;

    int mCurrentImageIndex = 0;

    public PointAttachedObjectView(Context context) {
        super(context);
    }

    public PointAttachedObjectView(Context context, AttributeSet attrs) {
        super(context, attrs);
//        loadViews();
    }

    public PointAttachedObjectView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
//        loadViews();
    }

    public void init(int layoutId, int id) {
        BusProvider.getInstance().register(this);

        mLayoutId = layoutId;
        if (id != -1)
            setId(id);

        loadViews();
    }

    public int getLayoutId() {
        return mLayoutId;
    }

    public String getPaoId() {
        return mPaoId;
    }

    public void setPaoId(String paoId) {
        Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ": setPaoId: " + paoId);
        mPaoId = paoId;
        mPaObject = PathManager.getInstance().getPointAttachedObject(paoId);
        if (mPaObject == null)
            return;

        populateFieldsFromObject(mPaObject);
        setRelativeLocationString();
    }

    public void populateFieldsFromObject(PointAttachedObject pao) {
        Attachment a = pao.getAttachment();
        mImageFileNames = a.getImageFileNames();
        if (mImageFileNames != null && mImageFileNames.size()>0) {
            mCurrentImageIndex = 0;
            mImageView.setVisibility(VISIBLE);
            loadCurrentImage();
        } else {
            Log.d(Constants.TRAILBOOK_TAG, "no images");
            mImageView.setVisibility(GONE);
        }
        if (mNextArrowView != null && mPreviousArrowView != null)
            showOrHideSliderArrows(mPaObject.getAttachment().getImageFileNames());
    }

    private void loadCurrentImage() {
        Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ": loading image :" + mImageFileNames.get(mCurrentImageIndex));
        Picasso.with(getContext()).load(TrailbookFileUtilities.getInternalImageFile(mImageFileNames.get(mCurrentImageIndex))).into(mImageView);
    }

    @Subscribe
    public void onLocationChangedEvent(LocationChangedEvent event){
        Log.d(Constants.TRAILBOOK_TAG, "NoteView: location changed event recieved," + event.getLocation());
        mCurrentLocation = event.getLocation();
        setRelativeLocationString();
    }

    public void loadViews(){
        LayoutInflater inflater = LayoutInflater.from(this.getContext());
        inflater.inflate(getLayoutId(), this);
        mImageView=(ImageView)findViewById(R.id.vn_image);
        mTextViewLocationInfo=(TextView)findViewById(R.id.vn_navigation_details);
        mExpandImage = (ImageView)findViewById(R.id.vn_button_expand);
        if (mExpandImage != null) {
            if (TrailBookState.getMode() == TrailBookState.MODE_LEAD
                    || TrailBookState.getMode() == TrailBookState.MODE_EDIT) {
                mExpandImage.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_expand_edit));
            }
        }
        mNextArrowView = (ImageView)findViewById(R.id.swipe_right);
        if (mNextArrowView!= null)
            mNextArrowView.setOnClickListener(this);

        mPreviousArrowView = (ImageView)findViewById(R.id.swipe_left);
        if (mPreviousArrowView != null)
            mPreviousArrowView.setOnClickListener(this);
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

    private void showOrHideSliderArrows(ArrayList<String> imageFileNames) {
        Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ": ImageFileNames: " + imageFileNames);
        if (imageFileNames == null || imageFileNames.size() < 2) {
            mNextArrowView.setVisibility(INVISIBLE);
            mPreviousArrowView.setVisibility(INVISIBLE);
        } else {
            mNextArrowView.setVisibility(VISIBLE);
            mPreviousArrowView.setVisibility(VISIBLE);
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.swipe_right) {
            incrementCurrentImageIndex();
        } else if (view.getId() == R.id.swipe_left) {
            decrementCurrentImageIndex();
        }

        loadCurrentImage();
    }

    private void decrementCurrentImageIndex() {
        mCurrentImageIndex--;
        if (mCurrentImageIndex < 0) {
            mCurrentImageIndex = mImageFileNames.size()-1;
        }
    }

    private void incrementCurrentImageIndex() {
        mCurrentImageIndex++;
        if (mCurrentImageIndex >= mImageFileNames.size()) {
            mCurrentImageIndex = 0;
        }
    }
}
