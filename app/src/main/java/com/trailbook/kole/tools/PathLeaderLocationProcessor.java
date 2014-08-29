package com.trailbook.kole.tools;

import android.content.Context;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import com.trailbook.kole.activities.MapsActivity;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.worker_fragments.LocationServicesFragment;

/**
 * Created by kole on 7/20/2014.
 */
public class PathLeaderLocationProcessor implements LocationServicesFragment.LocationProcessor {
    Context mContext;
    PathManager mPathManager;
    String mSegmentId;
    String mPathId;

    public PathLeaderLocationProcessor(Context context, String segmentId, String pathId) {
        mContext = context;
        this.mSegmentId = segmentId;
        this.mPathId = pathId;
        mPathManager = PathManager.getInstance();
    }

    @Override
    public void process(Location newLocation) {
        Log.d(Constants.TRAILBOOK_TAG, "adding point to segment " + mSegmentId + " "  + newLocation.toString() );
        mPathManager.addPointToSegment(mSegmentId, mPathId, newLocation);
        mPathManager.savePath(mSegmentId, mContext);
    }
}
