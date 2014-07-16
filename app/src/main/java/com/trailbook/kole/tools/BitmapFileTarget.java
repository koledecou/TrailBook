package com.trailbook.kole.tools;

import android.graphics.Bitmap;
import android.util.Log;
import com.squareup.picasso.Target;
import org.apache.commons.io.FileUtils;

import com.trailbook.kole.data.Constants;
import com.trailbook.kole.tools.TrailbookFileUtilities;

import java.io.File;
import java.io.IOException;

/**
 * Created by Fistik on 7/12/2014.
 */
public class BitmapFileTarget implements Target {
    File mFile;

    public BitmapFileTarget(File f) {
        this.mFile = f;
    }

    @Override
    public void onSuccess(Bitmap bitmap) {
        try {
            FileUtils.writeByteArrayToFile(mFile, TrailbookFileUtilities.getBytes(bitmap));
        } catch (IOException e) {
            Log.e(Constants.TRAILBOOK_TAG, "Error saving image file", e);
            e.printStackTrace();
        }
    }

    @Override
    public void onError() {
        Log.d("trailbook", "Error getting image");
    }
}
