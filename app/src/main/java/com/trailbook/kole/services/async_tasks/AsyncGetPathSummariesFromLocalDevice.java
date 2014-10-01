package com.trailbook.kole.services.async_tasks;

import android.os.AsyncTask;
import android.util.Log;

import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.events.PathSummaryAddedEvent;
import com.trailbook.kole.helpers.TrailbookFileUtilities;
import com.trailbook.kole.state_objects.BusProvider;
import com.trailbook.kole.state_objects.PathManager;

import java.util.ArrayList;

/**
 * Created by kole on 9/19/2014.
 */
public class AsyncGetPathSummariesFromLocalDevice extends AsyncTask<String, Void, ArrayList<PathSummary>> {

    @Override
    protected ArrayList<PathSummary> doInBackground(String... strings) {
        ArrayList<PathSummary> allSummaries = null;
        ArrayList<PathSummary> cachedCloudSummaries = null;
        PathManager manager = PathManager.getInstance();
        try {
            allSummaries = manager.loadSummariesFromDevice(TrailbookFileUtilities.getInternalPathDirectory());
            Log.d(Constants.TRAILBOOK_TAG, "AsyncGetPathSummariesFromLocalDevice: loaded " + allSummaries.size() + " local summaries");
            cachedCloudSummaries = manager.loadSummariesFromDevice(TrailbookFileUtilities.getInternalCacheDirectory());
            allSummaries.addAll(cachedCloudSummaries);
            Log.d(Constants.TRAILBOOK_TAG, "AsyncGetPathSummariesFromLocalDevice: loaded " + cachedCloudSummaries.size() + " cached cloud summaries");
//            manager.loadPathsFromDevice();
//            manager.loadCachedPathSummaries();
        } catch (Exception e) {
            Log.d(Constants.TRAILBOOK_TAG, "AsyncGetPathSummariesFromLocalDevice: exception getting path summaries.", e);
        }
        return allSummaries;
    }

    @Override
    protected void onPostExecute(ArrayList<PathSummary> summaries) {
        if (summaries == null)
            return;
        Log.d(Constants.TRAILBOOK_TAG, "AsyncGetPathSummariesFromLocalDevice: posting " + summaries.size() + " paths summaries." );
        for (PathSummary summary:summaries) {
            BusProvider.getInstance().post(new PathSummaryAddedEvent(summary));
            //PathManager.getInstance().loadPathFromSummary(summary);
            AsyncGetPathFromLocalDevice asyncGetPath = new AsyncGetPathFromLocalDevice();
            asyncGetPath.execute(summary.getId());
        }

        super.onPostExecute(summaries);
    }
}
