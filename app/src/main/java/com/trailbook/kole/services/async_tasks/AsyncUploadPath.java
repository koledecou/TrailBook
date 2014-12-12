package com.trailbook.kole.services.async_tasks;

import android.os.AsyncTask;
import android.util.Log;

import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.data.Path;
import com.trailbook.kole.services.database.TrailbookRemoteDatabase;

import java.util.ArrayList;

/**
 * Created by kole on 9/19/2014.
 */
public class AsyncUploadPath extends AsyncTask<Path, Void, ArrayList<PathSummary>> {

    @Override
    protected ArrayList<PathSummary> doInBackground(Path... pathContainers) {
        ArrayList<PathSummary> uploadedPaths = new ArrayList<PathSummary>();
        try {
            TrailbookRemoteDatabase db = TrailbookRemoteDatabase.getInstance();
            for (Path pathContainer : pathContainers) {
                Log.d(Constants.TRAILBOOK_TAG, "AsyncUploadPath: uploading " + pathContainer.summary.getName());
                boolean success = db.uploadPathContainer(pathContainer);
                if (success)
                    uploadedPaths.add(pathContainer.summary);
                else
                    Log.d(Constants.TRAILBOOK_TAG, "AsyncUploadPath: upload completed for path " + pathContainer.summary.getName());
                    //todo: create failure notification
//                Toast.makeText(TrailBookState.getInstance(), TrailBookState.getInstance().getString(R.string.upload_failed) + pathContainer.path.getName(), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.d(Constants.TRAILBOOK_TAG, "AsyncUploadPath: exception getting path summaries.  DB may not be available", e);
        }
        return uploadedPaths;
    }

    @Override
    protected void onPostExecute(ArrayList<PathSummary> uploadedPaths) {
        for (PathSummary p : uploadedPaths) {
            Log.d(Constants.TRAILBOOK_TAG, "AsyncUploadPath: upload completed for path " + p.getName());
            //todo: success notification
        }
        super.onPostExecute(uploadedPaths);
    }
}
