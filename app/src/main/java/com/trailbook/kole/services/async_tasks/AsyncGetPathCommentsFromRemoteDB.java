package com.trailbook.kole.services.async_tasks;

import android.os.AsyncTask;

import com.trailbook.kole.data.TrailBookComment;
import com.trailbook.kole.events.PathCommentsReceivedFromCloudEvent;
import com.trailbook.kole.services.database.TrailbookRemoteDatabase;
import com.trailbook.kole.state_objects.BusProvider;

import java.util.ArrayList;

/**
 * Created by kole on 9/19/2014.
 */
public class AsyncGetPathCommentsFromRemoteDB extends AsyncTask<String, Void, ArrayList<TrailBookComment>> {

    @Override
    protected ArrayList<TrailBookComment> doInBackground(String... pathIds) {
        ArrayList<TrailBookComment> comments = null;
        if (pathIds[0] != null) {
            try {
                TrailbookRemoteDatabase db = TrailbookRemoteDatabase.getInstance();
                comments = db.getCommentsForPath(pathIds[0]);
            } catch (Exception e) {
            }
        }
        return comments;
    }

    @Override
    protected void onPostExecute(ArrayList<TrailBookComment> comments) {
        if (comments != null) {
            BusProvider.getInstance().post(new PathCommentsReceivedFromCloudEvent(comments));
        }

        super.onPostExecute(comments);
    }
}
