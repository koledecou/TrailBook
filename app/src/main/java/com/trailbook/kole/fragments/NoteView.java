package com.trailbook.kole.fragments;

import android.content.Context;
import android.location.Location;
import android.media.Image;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;
import com.trailbook.kole.activities.R;
import com.trailbook.kole.data.ButtonActions;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.Note;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.events.LocationChangedEvent;
import com.trailbook.kole.tools.BusProvider;
import com.trailbook.kole.tools.PathManager;
import com.trailbook.kole.tools.PreferenceUtilities;
import com.trailbook.kole.tools.TrailbookFileUtilities;

public class NoteView extends LinearLayout {
    private PathManager mPathManager;

    private PointAttachedObject<Note> mPaoNote;
    private String mNoteId = "1";
    private String mContent = "This is a sample path";
    private String mImageFileName = null;
    private Image mImage = null;
    private TextView mTextViewContent;
    private TextView mTextViewLocationInfo;
    private ImageView mImageView;
    private Bus mBus = BusProvider.getInstance();
    private Location mCurrentLocation;

    public NoteView(Context context) {
        super(context);
        loadViews();
    }

    public NoteView(Context context, AttributeSet attrs) {
        super(context, attrs);
        loadViews();
    }

    public NoteView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        loadViews();
    }

    @Subscribe
    public void onLocationChangedEvent(LocationChangedEvent event){
        Log.d(Constants.TRAILBOOK_TAG, "location changed event recieved by noteview," + event.getLocation());
        mCurrentLocation = event.getLocation();
        setRelativeLocationString();
    }

    public void setNoteId(String noteId) {
        mNoteId=noteId;
        mPathManager = PathManager.getInstance();
        mPaoNote = mPathManager.getNote(noteId);
        if (mPaoNote == null)
            return;

        Note note = mPaoNote.getAttachment();
        mContent=note.getNoteContent();
        if (mTextViewContent != null)
            mTextViewContent.setText(mContent);

        mImageFileName = note.getImageFileName();
        if (mImageFileName != null && mImageFileName.length()>0) {
            String segmentId = note.getParentSegmentId();
            Log.d(Constants.TRAILBOOK_TAG, "loading image :" + mImageFileName);
            Picasso.with(getContext()).load(TrailbookFileUtilities.getInternalImageFile(getContext(), segmentId, mImageFileName)).into(mImageView);
        }

        setRelativeLocationString();
    }

    private void setRelativeLocationString() {
        LatLng noteLocation = mPaoNote.getLocation();
        float[] results = new float[5];
        if (mCurrentLocation == null) {
            mCurrentLocation = mPathManager.getCurrentLocation();
        }

        if (noteLocation != null && mCurrentLocation != null) {
            Log.d(Constants.TRAILBOOK_TAG, "note location:" + noteLocation);
            Log.d(Constants.TRAILBOOK_TAG, "current location: " + mCurrentLocation);

            Location.distanceBetween(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(), noteLocation.latitude, noteLocation.longitude, results);
            //String relativeLocationMessage = getContext().getResources().getString(R.string.relative_location_message);
            String relativeLocationMessage = "%s %s of you";
            String distanceString = PreferenceUtilities.getDistString(getContext(), results[0]);
            String bearingString = PreferenceUtilities.getBearingString(getContext(), results[1]);
            mTextViewLocationInfo.setText(String.format(relativeLocationMessage, distanceString, bearingString));
        } else {
            mTextViewLocationInfo.setText("");
        }
    }

    private void loadViews(){
        mBus = BusProvider.getInstance();
        mBus.register(this);

        LayoutInflater inflater = LayoutInflater.from(this.getContext());
        inflater.inflate(R.layout.view_note, this);

        mTextViewContent=(TextView)findViewById(R.id.vn_text_content);
        mImageView=(ImageView)findViewById(R.id.vn_image);
        mTextViewLocationInfo=(TextView)findViewById(R.id.vn_navigation_details);

    }
}
