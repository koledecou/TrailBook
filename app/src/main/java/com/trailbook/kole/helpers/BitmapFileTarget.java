package com.trailbook.kole.helpers;

import android.graphics.Bitmap;

import com.squareup.picasso.Target;

import org.apache.commons.io.FileUtils;

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
        }
    }

    @Override
    public void onError() {

    }
}
