package com.trailbook.kole.services.async_tasks;

import android.os.AsyncTask;
import android.util.Log;

import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.Path;
import com.trailbook.kole.data.PathSegment;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.events.MapObjectAddedEvent;
import com.trailbook.kole.events.SegmentUpdatedEvent;
import com.trailbook.kole.state_objects.BusProvider;
import com.trailbook.kole.state_objects.PathManager;

import java.util.ArrayList;

/**
 * Created by kole on 9/19/2014.
 */
public class AsyncGetPathFromLocalDevice extends AsyncTask<String, Void, ArrayList<Path>> {

    @Override
    protected ArrayList<Path> doInBackground(String... pathIds) {
        PathManager manager = PathManager.getInstance();
        ArrayList<Path> paths = new ArrayList<Path>();
        try {
            for (String pathId:pathIds) {
                if (!manager.isStoredLocally(pathId))
                    continue;
                Path path = manager.loadPathFromDevice(pathId);
                if (path != null && path.summary != null) {
                    paths.add(path);
                    Log.d(Constants.TRAILBOOK_TAG, "AsyncGetPathFromLocalDevice: loaded path " + path.summary.getName());
                }
            }
        } catch (Exception e) {
            Log.d(Constants.TRAILBOOK_TAG, "AsyncGetPathFromLocalDevice: exception getting path summaries.", e);
        }
        return paths;
    }

    @Override
    protected void onPostExecute(ArrayList<Path> paths) {
        if (paths == null)
            return;
        for (Path path:paths) {
            if (path.paObjects != null) {
                Log.d(Constants.TRAILBOOK_TAG, "AsyncGetPathFromLocalDevice: posting " + path.paObjects.size() + " point objects.");
                for (PointAttachedObject pao : path.paObjects) {
                    BusProvider.getInstance().post(new MapObjectAddedEvent(pao));
                }
            }

            if (path.segments != null) {
                Log.d(Constants.TRAILBOOK_TAG, "AsyncGetPathFromLocalDevice: posting " + path.segments.size() + " segments.");
                for (PathSegment segment : path.segments) {
                    BusProvider.getInstance().post(new SegmentUpdatedEvent(segment));
                }
            }
        }

        super.onPostExecute(paths);
    }
}
