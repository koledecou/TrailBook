package com.trailbook.kole.services.async_tasks;

import android.os.AsyncTask;

import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.services.database.TrailbookRemoteDatabase;

/**
 * Created by kole on 9/19/2014.
 */
public class AsyncUploadAttachedComment extends AsyncTask<PointAttachedObject, Void, PointAttachedObject> {

    @Override
    protected PointAttachedObject doInBackground(PointAttachedObject... paoComments) {
        PointAttachedObject paoComment = null;
        try {
            TrailbookRemoteDatabase db = TrailbookRemoteDatabase.getInstance();
            if (paoComments.length>0) {
                paoComment = paoComments[0];
            }

            db.uploadAttachedComment(paoComment);
            return paoComment;

        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void onPostExecute(PointAttachedObject paoComment) {
        super.onPostExecute(paoComment);
    }
}
