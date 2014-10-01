package com.trailbook.kole.fragments.point_attched_object_view;

import android.content.Context;
import android.location.Location;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.LinearLayout;

import com.squareup.otto.Subscribe;
import com.trailbook.kole.activities.R;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.events.LocationChangedEvent;
import com.trailbook.kole.state_objects.PathManager;

/**
 * Created by kole on 9/29/2014.
 */
public abstract class PointAttachedObjectView extends LinearLayout {
    int mLayoutId = R.layout.view_note_small;
    public Location mCurrentLocation;
    public PointAttachedObject mPaObject;
    String mPaoId = "1";

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

    @Subscribe
    public void onLocationChangedEvent(LocationChangedEvent event){
        Log.d(Constants.TRAILBOOK_TAG, "NoteView: location changed event recieved," + event.getLocation());
        mCurrentLocation = event.getLocation();
        setRelativeLocationString();
    }

    public abstract void loadViews();
    public abstract void setRelativeLocationString();
    public abstract void populateFieldsFromObject(PointAttachedObject pao);
}
