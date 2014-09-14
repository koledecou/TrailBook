package com.trailbook.kole.location_processors;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.trailbook.kole.data.Constants;
import com.trailbook.kole.state_objects.PathManager;
import com.trailbook.kole.worker_fragments.LocationServicesFragment;

/**
 * Created by kole on 7/20/2014.
 */
public class PathLeaderLocationProcessor implements LocationServicesFragment.LocationProcessor {
    private static final float MIN_ACCURACY = 60; //don't record points that might be worse than 200 ft
    Context mContext;
    PathManager mPathManager;
    String mSegmentId;
    String mPathId;
    Location mLastLocation = null;

    public PathLeaderLocationProcessor(Context context, String segmentId, String pathId) {
        mContext = context;
        this.mSegmentId = segmentId;
        this.mPathId = pathId;
        mPathManager = PathManager.getInstance();
    }

    @Override
    public void process(Location newLocation) {
        if (mLastLocation == null)
            mLastLocation = newLocation;
        else {
            if (mLastLocation.distanceTo(newLocation) < Constants.MIN_DISTANCE_BETWEEN_POINTS)
                return;
        }
        Log.d(Constants.TRAILBOOK_TAG, "adding point to segment " + mSegmentId + " "  + newLocation.toString() );
        Log.d(Constants.TRAILBOOK_TAG, "PathLeaderLocationProcessor: accuracy is " + newLocation.getAccuracy());
        if (!newLocation.hasAccuracy() || newLocation.getAccuracy() < MIN_ACCURACY &&
                mPathManager.getSegment(mSegmentId) != null &&
                mPathManager.getPath(mPathId) != null) {
            mPathManager.addPointToSegment(mSegmentId, mPathId, newLocation);
            mPathManager.savePath(mPathId, mContext);
        }
    }

    public void removeAllNotifications() {
    }
}
