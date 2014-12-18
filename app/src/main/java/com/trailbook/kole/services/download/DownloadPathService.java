package com.trailbook.kole.services.download;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.Path;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.helpers.TrailbookFileUtilities;
import com.trailbook.kole.services.database.TrailbookRemoteDatabase;
import com.trailbook.kole.state_objects.PathManager;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by kole on 9/19/2014.
 */
public class DownloadPathService extends IntentService {
    public static final String PATH_ID_KEY = "PATH_ID";
    public static final String SERVICE_NAME = "DOWNLOAD_PATH_SERVICE";

    // Defines a custom Intent action
    public static final String BROADCAST_ACTION = "com.trailbook.kole.services.download.DownloadPathService.BROADCAST";
    public static final String EXTENDED_DATA_STATUS_KEY = "STATUS";
    public static final int STATUS_COMPLETE = 100;
    public static final int STATUS_FAILURE = -1;

    private int mTotalOfItems = 0;
    private int mNumberOfItemsUploaded = 0;
    private String mPathId;

    public DownloadPathService() {
        super(SERVICE_NAME);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Path pathContainer = null;
        try {
            mPathId = intent.getStringExtra(PATH_ID_KEY);
            Log.d(Constants.TRAILBOOK_TAG, "DownloadPathService: downloading path " + mPathId);
            TrailbookRemoteDatabase db = TrailbookRemoteDatabase.getInstance();
            pathContainer = db.getPath(mPathId);
            PathManager.getInstance().onPathReceived(pathContainer);

            getImages(pathContainer);
            sendCompletedBroadcast();
        } catch (Exception e) {
            Log.d(Constants.TRAILBOOK_TAG, "DownloadPathService: exception getting path summaries.  DB may not be available", e);
            sendFailedBroadcast();
        }

    }

    private void getImages(Path path) {
        ArrayList<PointAttachedObject> paObjects = path.paObjects;
        for (PointAttachedObject pao:paObjects) {
            ArrayList<String> imageFileNames = pao.getAttachment().getImageFileNames();
            if (imageFileNames != null && imageFileNames.size()>0) {
                for (String imageFileName:imageFileNames) {
                    startGetImage(imageFileName);
                }
            }
        }
    }

    private void startGetImage(String imageFileName) {
        File deviceImageFile = TrailbookFileUtilities.getInternalImageFile(imageFileName);
        Log.d(Constants.TRAILBOOK_TAG, "DownloadPathService: downloading to: " + deviceImageFile);

        String webServerImageFileName = TrailbookFileUtilities.getWebServerImageDir() + "/" + imageFileName;
        Log.d(Constants.TRAILBOOK_TAG, "DownloadPathService: webserver image file name: " + webServerImageFileName);

        Bitmap bitmap = getBitmapFromUrl(webServerImageFileName);
        if (bitmap != null)
            writeBitmapToFile(bitmap, deviceImageFile);

        //TODO: use picasso to get the image?
//        Picasso.with(getActivity()).load(webServerImageFileName).into(new BitmapFileTarget(imageFile));
    }

    private Bitmap getBitmapFromUrl(String webServerImageFileName) {
        Bitmap bitmap = null;
        try {
            InputStream in = new java.net.URL(webServerImageFileName).openStream();
            bitmap = BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            Log.e(Constants.TRAILBOOK_TAG, e.getMessage());
        }
        return bitmap;
    }

    private void writeBitmapToFile(Bitmap bitmap, File file) {
        try {
            Log.d(Constants.TRAILBOOK_TAG, "DownloadPathService saving image: " + file);
            if (bitmap != null)
                FileUtils.writeByteArrayToFile(file, TrailbookFileUtilities.getBytes(bitmap));
        } catch (Exception e) {
            Log.e(Constants.TRAILBOOK_TAG,"exception in DownloadPathService. ", e);
        }
    }

    protected Bitmap doInBackground(String... urls) {
        String urldisplay = urls[0];
        Bitmap mIcon11 = null;
        try {
            InputStream in = new java.net.URL(urldisplay).openStream();
            mIcon11 = BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            Log.e(Constants.TRAILBOOK_TAG, e.getMessage());
            e.printStackTrace();
        }
        return mIcon11;
    }


    private void sendFailedBroadcast() {
        Intent broadcastIntent = getBroadcastIntent(STATUS_FAILURE);
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
        broadcastIntent.putExtra(PATH_ID_KEY, mPathId);
        return broadcastIntent;
    }
}
