package com.trailbook.kole.services.async_tasks;

import android.os.AsyncTask;
import android.util.Log;

import com.squareup.otto.Bus;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.Path;
import com.trailbook.kole.events.PathReceivedEvent;
import com.trailbook.kole.services.database.TrailbookRemoteDatabase;
import com.trailbook.kole.state_objects.BusProvider;

/**
 * Created by kole on 9/19/2014.
 */
public class AsyncGetPathFromRemoteDB extends AsyncTask<String, Void, Path> {
    Bus mBus;

    public AsyncGetPathFromRemoteDB() {
        super();
        this.mBus = BusProvider.getInstance();
        mBus.register(this);
    }

    @Override
    protected Path doInBackground(String... pathIds) {
        Path pathContainer = null;
        try {
            TrailbookRemoteDatabase db = TrailbookRemoteDatabase.getInstance();
            String pathId = pathIds[0];
            pathContainer = db.getPath(pathId);
        } catch (Exception e) {
            Log.d(Constants.TRAILBOOK_TAG, "AsyncGetPathSummaries: exception getting path summaries.  DB may not be available", e);
        }
        return pathContainer;
    }

    @Override
    protected void onPostExecute(Path path) {
        if (path != null) {
            mBus.post(new PathReceivedEvent(path));
        }
        super.onPostExecute(path);
    }
}
