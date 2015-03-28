package com.trailbook.kole.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

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
            e.printStackTrace();
        }
        return mIcon11;
    }

    protected void onPostExecute(Bitmap bitmap) {
        try {
            if (bitmap != null)
                FileUtils.writeByteArrayToFile(mDeviceFile, TrailbookFileUtilities.getBytes(bitmap));
        } catch (Exception e) {
        }
    }
}