package com.trailbook.kole.services.upload;

import android.app.IntentService;
import android.content.Intent;

import com.trailbook.kole.data.Path;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.helpers.TrailbookFileUtilities;
import com.trailbook.kole.services.database.TrailbookRemoteDatabase;
import com.trailbook.kole.state_objects.PathManager;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.util.ArrayList;

/**
 * Created by kole on 9/19/2014.
 */
public class UploadPathService extends IntentService {
    public static final String PATH_ID_KEY = "PATH_ID";
    public static final String SERVICE_NAME = "UPLOAD_PATH_SERVICE";

    // Defines a custom Intent action
    public static final String BROADCAST_ACTION = "com.trailbook.kole.services.upload.UploadPathService.BROADCAST";
    public static final String EXTENDED_DATA_STATUS_KEY = "STATUS";
    public static final int STATUS_COMPLETE = 100;
    public static final int STATUS_FAILURE = -1;

    private int mTotalOfItems = 0;
    private int mNumberOfItemsUploaded = 0;

    private boolean mFailure = false;

    public UploadPathService() {
        super(SERVICE_NAME);
    }

    /**
     * This method is invoked on the worker thread with a request to process.
     * Only one Intent is processed at a time, but the processing happens on a
     * worker thread that runs independently from other application logic.
     * So, if this code takes a long time, it will hold up other requests to
     * the same IntentService, but it will not hold up anything else.
     * When all requests have been handled, the IntentService stops itself,
     * so you should not call {@link #stopSelf}.
     *
     * @param intent The value passed to {@link
     *               android.content.Context#startService(android.content.Intent)}.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        String pathId = intent.getStringExtra(PATH_ID_KEY);
        Path pathContainer = PathManager.getInstance().getPath(pathId);
        mTotalOfItems = getNumberOfUploadItems(pathContainer.summary);
        mFailure = false;
        uploadPathToDatabase(pathContainer);
        postImages(pathContainer.summary);
        if (!mFailure)
            sendCompletedBroadcast();
    }

    private int getNumberOfUploadItems(PathSummary summary) {
        int nItems = 1; //count the path db upload as one item
        ArrayList<PointAttachedObject> paObjects = PathManager.getInstance().getPointObjectsForPath(summary.getId());
        for (PointAttachedObject pao:paObjects) {
            ArrayList<String> imageFileNames = pao.getAttachment().getImageFileNames();
            if (imageFileNames != null) {
                nItems += imageFileNames.size();
            }
        }
        return nItems;
    }

    private void sendFailedBroadcast(String pathId) {
        Intent broadcastIntent = getBroadcastIntent(STATUS_FAILURE);
        broadcastIntent.putExtra(PATH_ID_KEY, pathId);
        sendBroadcast(broadcastIntent);
    }

    private void sendCompletedBroadcast() {
        Intent broadcastIntent = getBroadcastIntent(STATUS_COMPLETE);
        sendBroadcast(broadcastIntent);
    }

    private void sendProgressBroadcast(int percentComplete) {
        Intent broadcastIntent = getBroadcastIntent(percentComplete);
        sendBroadcast(broadcastIntent);
    }

    private Intent getBroadcastIntent(int percentComplete) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(BROADCAST_ACTION);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(EXTENDED_DATA_STATUS_KEY, percentComplete);
        return broadcastIntent;
    }

    private void uploadPathToDatabase(Path pathContainer) {
        try {
            TrailbookRemoteDatabase db = TrailbookRemoteDatabase.getInstance();

            boolean success = db.uploadPathContainer(pathContainer);
            if (success) {
                //todo: send broadcast message to application
                mNumberOfItemsUploaded++;
                sendProgressBroadcast(getCurrentPercentage());
            } else {
                mFailure = true;
                sendFailedBroadcast(pathContainer.summary.getId());
            }
        } catch (Exception e) {
            mFailure = true;
            sendFailedBroadcast(pathContainer.summary.getId());
        }
        return;
    }

    private int getCurrentPercentage() {
        return Math.round(((float)mNumberOfItemsUploaded/(float)mTotalOfItems)*100);
    }


    private void postImages(PathSummary summary) {
        ArrayList<PointAttachedObject> paObjects = PathManager.getInstance().getPointObjectsForPath(summary.getId());
        for (PointAttachedObject pao:paObjects) {
            ArrayList<String> imageFileNames = pao.getAttachment().getImageFileNames();
            if (imageFileNames != null) {
                for (String imageFileName : imageFileNames) {
                    postImage(imageFileName, summary.getId());
                }
            }
        }
    }

    private void postImage(String imageFileName, String pathId) {
        ArrayList<MultipartEntity> entities = new ArrayList<MultipartEntity>();
        if (imageFileName != null && imageFileName.length()>0) {
            MultipartEntity entity = TrailbookFileUtilities.getMultipartEntityForPAOImage(imageFileName);
            if (entity != null)
                entities.add(entity);
        }
        startImageUpload(entities, pathId);
    }

    private void startImageUpload (ArrayList<MultipartEntity> entities, String pathId) {
        try {
            String destinationUrl = TrailbookFileUtilities.getImageUploadUrl();
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(destinationUrl);

            for (MultipartEntity entity:entities) {
                httppost.setEntity(entity);
                HttpResponse response = httpClient.execute(httppost);
                mNumberOfItemsUploaded++;
                sendProgressBroadcast(getCurrentPercentage());
            }
        } catch (Exception e) {
            mFailure = true;
            sendFailedBroadcast(pathId);
        }

        return;
    }
}
