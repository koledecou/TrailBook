package com.trailbook.kole.tools;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.trailbook.kole.data.Constants;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * Created by Fistik on 7/14/2014.
 */

public class TrailbookFileUtilities {

    public static File getInternalPathFile(Context c, String pathId) {
        String fullDirectory = c.getFilesDir().getAbsolutePath() + File.separator + pathId;
        String fileName = pathId + ".tb";
        return new File(fullDirectory, fileName);
    }

    public static File getInternalImageFile(Context c, String pathId, String imageFileName) {
        String fullDirectory = c.getFilesDir().getAbsolutePath() + File.separator + pathId + File.separator + Constants.pathsDir;
        return new File(fullDirectory, imageFileName);
    }

    public static byte[] getBytes(Bitmap bm) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        return byteArray;
    }
}
