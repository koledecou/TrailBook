package com.trailbook.kole.services.async_tasks;

import android.os.AsyncTask;
import android.util.Log;

import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.events.PathDeletedEvent;
import com.trailbook.kole.services.database.TrailbookRemoteDatabase;
import com.trailbook.kole.state_objects.BusProvider;
import com.trailbook.kole.state_objects.PathManager;

import java.util.ArrayList;

/**
 * Created by kole on 9/19/2014.
 */
public class AsyncCloudDeletePath extends AsyncTask<PathSummary, Void, ArrayList<PathSummary>> {

    @Override
    protected ArrayList<PathSummary> doInBackground(PathSummary... pathSummaries) {
        ArrayList<PathSummary> deletedPaths = new ArrayList<PathSummary>();
        try {
            TrailbookRemoteDatabase db = TrailbookRemoteDatabase.getInstance();
            for (PathSummary summary : pathSummaries) {
                Log.d(Constants.TRAILBOOK_TAG, "AsyncCloudDeletePath: deleting " + summary.getName());
                db.cloudDeletePath(summary);
                deletedPaths.add(summary);
            }
        } catch (Exception e) {
            Log.d(Constants.TRAILBOOK_TAG, "AsyncCloudDeletePath: exception deleting.  DB may not be available", e);
        }
        return deletedPaths;
    }

    @Override
    protected void onPostExecute(ArrayList<PathSummary> deletedPaths) {
        for (PathSummary summary : deletedPaths) {
            Log.d(Constants.TRAILBOOK_TAG, "AsyncCloudDeletePath: delete completed for path " + summary.getId());
            PathManager.getInstance().deletePathFromCloudCache(summary.getId());
            BusProvider.getInstance().post(new PathDeletedEvent(summary));
        }
        super.onPostExecute(deletedPaths);
    }
}
