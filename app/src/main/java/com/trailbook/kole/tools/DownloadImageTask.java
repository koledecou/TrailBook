package com.trailbook.kole.tools;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import com.trailbook.kole.data.Constants;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.InputStream;

public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
    File mDeviceFile;
    Context mContext;

    public DownloadImageTask(File deviceFile) {
        this.mDeviceFile = deviceFile;
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

    protected void onPostExecute(Bitmap bitmap) {
        try {
            Log.d(Constants.TRAILBOOK_TAG, "Async download saving image: " + mDeviceFile);
            if (bitmap != null)
                FileUtils.writeByteArrayToFile(mDeviceFile, TrailbookFileUtilities.getBytes(bitmap));
        } catch (Exception e) {
            Log.e(Constants.TRAILBOOK_TAG,"exception in async download. ", e);
        }
    }
}