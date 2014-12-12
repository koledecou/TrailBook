package com.trailbook.kole.services.async_tasks;

import android.os.AsyncTask;
import android.util.Log;

import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.TrailBookComment;
import com.trailbook.kole.services.database.TrailbookRemoteDatabase;

/**
 * Created by kole on 9/19/2014.
 */
public class AsyncUploadComment extends AsyncTask<TrailBookComment, Void, TrailBookComment> {

    @Override
    protected TrailBookComment doInBackground(TrailBookComment... comments) {
        TrailBookComment comment = null;
        try {
            TrailbookRemoteDatabase db = TrailbookRemoteDatabase.getInstance();
            if (comments.length>0) {
                comment = comments[0];
            }

            Log.d(Constants.TRAILBOOK_TAG, "AsyncUploadComment: uploading " + comment.comment);
            db.uploadComment(comment);
            return comment;

        } catch (Exception e) {
            if (comment != null)
                Log.d(Constants.TRAILBOOK_TAG, "AsyncUploadPath: upload failed for comment " + comment.comment, e);

            //todo: create failure notification
            return null;
        }
    }

    @Override
    protected void onPostExecute(TrailBookComment comment) {
        if (comment != null)
            Log.d(Constants.TRAILBOOK_TAG, "AsyncUploadPath: upload completed for comment " + comment.comment);

        super.onPostExecute(comment);
    }
}
