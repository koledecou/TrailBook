package com.trailbook.kole.services.async_tasks;

import android.os.AsyncTask;
import android.util.Log;

import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.events.PathSummariesReceivedFromCloudEvent;
import com.trailbook.kole.services.database.TrailbookRemoteDatabase;
import com.trailbook.kole.state_objects.BusProvider;
import com.trailbook.kole.state_objects.PathManager;

import java.util.ArrayList;

/**
 * Created by kole on 9/19/2014.
 */
public class AsyncGetPathSummariesFromRemoteDB extends AsyncTask<String, Void, ArrayList<PathSummary>> {

    @Override
    protected ArrayList<PathSummary> doInBackground(String... strings) {
        ArrayList<PathSummary> allPaths = new ArrayList<PathSummary>();
        try {
            TrailbookRemoteDatabase db = TrailbookRemoteDatabase.getInstance();
            PathManager manager = PathManager.getInstance();
            ArrayList<String> pathsInCloudCache = manager.getPathIdsInCloudCache();
            ArrayList<PathSummary> newPaths = getPathsNotInCacheYet(pathsInCloudCache);
            allPaths.addAll(newPaths);
            ArrayList<PathSummary> cachedPathsNeedingUpdate = getOutOfDateCachedPaths(pathsInCloudCache);
            allPaths.addAll(cachedPathsNeedingUpdate);
        } catch (Exception e) {
            Log.d(Constants.TRAILBOOK_TAG, "AsyncGetPathSummaries: exception getting path summaries.  DB may not be available", e);
        }
        return allPaths;
    }

    private ArrayList<PathSummary> getOutOfDateCachedPaths(ArrayList<String> pathsInCloudCache) {
        TrailbookRemoteDatabase db = TrailbookRemoteDatabase.getInstance();
        ArrayList<PathSummary> paths = db.getNewPathSummaries(pathsInCloudCache);
        return paths;
    }


    private ArrayList<PathSummary> getPathsNotInCacheYet(ArrayList<String> pathsInCloudCache) {
        TrailbookRemoteDatabase db = TrailbookRemoteDatabase.getInstance();
        ArrayList<PathSummary> paths = db.getAllPathSummariesExcluding(pathsInCloudCache);
        return paths;
    }

    @Override
    protected void onPostExecute(ArrayList<PathSummary> paths) {
        if (paths != null) {
            Log.d(Constants.TRAILBOOK_TAG, "AsyncGetPathSummaries: got " + paths.size() + " summaries.");
            BusProvider.getInstance().post(new PathSummariesReceivedFromCloudEvent(paths));
        }

        super.onPostExecute(paths);
    }
}
