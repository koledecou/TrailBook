package com.trailbook.kole.services.async_tasks;

import android.os.AsyncTask;
import android.util.Log;

import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.data.TrailBookComment;
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
                Log.d(Constants.TRAILBOOK_TAG, "AsyncUploadComment: uploading " + ((TrailBookComment)paoComment.getAttachment()).getShortContent());
            }

            db.uploadAttachedComment(paoComment);
            return paoComment;

        } catch (Exception e) {
            if (paoComment != null)
                Log.d(Constants.TRAILBOOK_TAG, "AsyncUploadPath: upload failed for comment " +((TrailBookComment)paoComment.getAttachment()).getShortContent(), e);

            //todo: create failure notification
            return null;
        }
    }

    @Override
    protected void onPostExecute(PointAttachedObject paoComment) {
        if (paoComment != null)
            Log.d(Constants.TRAILBOOK_TAG, "AsyncUploadPath: upload completed for comment " + ((TrailBookComment)paoComment.getAttachment()).getShortContent());

        super.onPostExecute(paoComment);
    }
}
