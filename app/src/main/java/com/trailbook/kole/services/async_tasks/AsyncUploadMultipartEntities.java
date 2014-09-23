package com.trailbook.kole.services.async_tasks;

import android.os.AsyncTask;
import android.util.Log;

import com.trailbook.kole.data.Constants;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * Created by kole on 8/11/2014.
 */

public class AsyncUploadMultipartEntities extends AsyncTask<MultipartEntity, Void, String> {

    private String destinationUrl;
    public AsyncUploadMultipartEntities(String destinationUrl) {
        this.destinationUrl = destinationUrl;
    }

    protected String doInBackground(MultipartEntity... entities) {
        try {

            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(destinationUrl);

            for (MultipartEntity entity:entities) {
                httppost.setEntity(entity);
                HttpResponse response = httpClient.execute(httppost);
                Log.d(Constants.TRAILBOOK_TAG, "AsyncUploadMultipartEntities: post complete. response:" + response.toString());
            }
        } catch (Exception e) {
            Log.e(Constants.TRAILBOOK_TAG, "AsyncUploadMultipartEntities: error in uploading file.", e);
        }

        return null;
    }

    protected void onPostExecute(String responseCode) {
        Log.d(Constants.TRAILBOOK_TAG, "AsyncUploadMultipartEntities: Response code: " + responseCode);
        // TODO: check this.exception
        // TODO: do something after execute completes
    }
}