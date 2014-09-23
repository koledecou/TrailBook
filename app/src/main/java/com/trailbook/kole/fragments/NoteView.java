package com.trailbook.kole.fragments;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.media.Image;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;
import com.trailbook.kole.activities.R;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.Note;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.events.LocationChangedEvent;
import com.trailbook.kole.helpers.PreferenceUtilities;
import com.trailbook.kole.helpers.TrailbookFileUtilities;
import com.trailbook.kole.state_objects.BusProvider;
import com.trailbook.kole.state_objects.PathManager;
import com.trailbook.kole.state_objects.TrailBookState;

public class NoteView extends LinearLayout {
    PathManager mPathManager;

    PointAttachedObject<Note> mPaoNote;
    String mNoteId = "1";
    String mContent = "This is a sample path";
    String mImageFileName = null;
    Image mImage = null;
    TextView mTextViewContent;
    TextView mTextViewLocationInfo;
    ImageView mImageView;
    ImageView mExpandImage;
    Bus mBus = BusProvider.getInstance();
    Location mCurrentLocation;
    int mLayoutId = R.layout.view_note;

    public NoteView(Context context) {
        super(context);
    }

    public NoteView(Context context, AttributeSet attrs) {
        super(context, attrs);
//        loadViews();
    }

    public NoteView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
//        loadViews();
    }

    void init(int layoutId, int id) {
        mLayoutId = layoutId;
        if (id != -1)
            setId(id);
        loadViews();
    }

    @Subscribe
    public void onLocationChangedEvent(LocationChangedEvent event){
        Log.d(Constants.TRAILBOOK_TAG, "NoteView: location changed event recieved," + event.getLocation());
        mCurrentLocation = event.getLocation();
        setRelativeLocationString();
    }

    public String getNoteId() {
        return mNoteId;
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
            Log.d(Constants.TRAILBOOK_TAG, "loading image :" + mImageFileName);
            Picasso.with(getContext()).load(TrailbookFileUtilities.getInternalImageFile(getContext(), mImageFileName)).into(mImageView);
        }

        setRelativeLocationString();
    }

    private void setRelativeLocationString() {
        LatLng noteLocation = mPaoNote.getLocation();
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

    void loadViews(){
        mBus = BusProvider.getInstance();
        mBus.register(this);
        LayoutInflater inflater = LayoutInflater.from(this.getContext());
        inflater.inflate(mLayoutId, this);

        mTextViewContent=(TextView)findViewById(R.id.vn_text_content);
        mTextViewContent.setMinWidth(R.dimen.small_note_view_min_text_panel_width);
        mImageView=(ImageView)findViewById(R.id.vn_image);
        mTextViewLocationInfo=(TextView)findViewById(R.id.vn_navigation_details);
        mTextViewLocationInfo.setMinWidth(R.dimen.small_note_view_min_text_panel_width);

        mExpandImage = (ImageView)findViewById(R.id.vn_button_expand);
        if (mExpandImage != null) {
            if (TrailBookState.getMode() == TrailBookState.MODE_LEAD) {
                mExpandImage.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_expand_edit));
            }
        }
    }
}
