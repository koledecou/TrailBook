package com.trailbook.kole.services.async_tasks;

import android.os.AsyncTask;
import android.util.Log;

import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.events.PathSummariesReceivedEvent;
import com.trailbook.kole.services.database.TrailbookRemoteDatabase;
import com.trailbook.kole.state_objects.BusProvider;

import java.util.ArrayList;

/**
 * Created by kole on 9/19/2014.
 */
public class AsyncGetPathSummaries extends AsyncTask<String, Void, ArrayList<PathSummary>> {

    @Override
    protected ArrayList<PathSummary> doInBackground(String... strings) {
        ArrayList<PathSummary> paths = null;
        try {
            TrailbookRemoteDatabase db = TrailbookRemoteDatabase.getInstance();
            paths = db.getAllPaths();
        } catch (Exception e) {
            Log.d(Constants.TRAILBOOK_TAG, "AsyncGetPathSummaries: exception getting path summaries.  DB may not be available", e);
        }
        return paths;
    }

    @Override
    protected void onPostExecute(ArrayList<PathSummary> paths) {
        if (paths == null)
            return;

        for (PathSummary path : paths) {
            BusProvider.getInstance().post(new PathSummariesReceivedEvent(paths));
        }
        super.onPostExecute(paths);
    }
}
