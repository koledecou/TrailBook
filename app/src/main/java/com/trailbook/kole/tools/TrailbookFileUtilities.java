package com.trailbook.kole.tools;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.trailbook.kole.data.Constants;

import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.filefilter.FileFilterUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Fistik on 7/14/2014.
 */

public class TrailbookFileUtilities {

    public static String getInternalPathDirectory(Context c) {
        return c.getFilesDir().getAbsolutePath() + File.separator  + Constants.pathsDir;
    }

    public static File getInternalPathFile(Context c, String pathId) {
        String fullDirectory = getInternalPathDirectory(c) + File.separator + pathId;
        String fileName = pathId + ".tb";
        return new File(fullDirectory, fileName);
    }

    public static File getInternalImageFile(Context c, String pathId, String imageFileName) {
        File fullDirectory = getInternalImageDirForPath(c, pathId);
        return new File(fullDirectory, imageFileName);
    }

    public static File getInternalImageDirForPath(Context c, String pathId) {
        String fullDirectory = getInternalPathDirectory(c) + File.separator + pathId + File.separator + Constants.imageDir;
        return new File(fullDirectory);
    }

    public static byte[] getBytes(Bitmap bm) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        return byteArray;
    }

    /** Create a file Uri for saving an image or video */
    public static Uri getOutputMediaFileUri(String fileName){
        return Uri.fromFile(getOutputMediaFile(fileName));
    }

    /** Create a File for saving an image or video */
    public static File getOutputMediaFile(String fileName){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File (sdCard.getAbsolutePath() + "/com.trailbook.kole/images");
        dir.mkdirs();
        File file = new File(dir, fileName);
        return file;
    }

    public static Bitmap rotate(Bitmap bitmap, int degree) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix mtx = new Matrix();
        mtx.postRotate(degree);

        return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
    }

    public static Bitmap getRotatedBitmap(Uri uri) {
        String fileName = uri.getPath();
        return getRotatedBitmap(fileName);
    }

    public static Bitmap getRotatedBitmap(String fileName) {
        Bitmap realImage = BitmapFactory.decodeFile(fileName);
        try {
            ExifInterface exif=new ExifInterface(fileName);

            Log.d("nilai exif ", exif.getAttribute(ExifInterface.TAG_ORIENTATION));
            if(exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("6")){

                realImage=rotate(realImage, 90);
            }else if(exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("8")){
                realImage=rotate(realImage, 270);
            }else if(exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("3")){
                realImage=rotate(realImage, 180);
            }

            return realImage;
        } catch (Exception e) {
            return realImage;
        }
    }

    public static Bitmap getRotatedBitmap(Bitmap image, String orientation) {
        try {
            if(orientation.equalsIgnoreCase("6")){
                image=rotate(image, 90);
            }else if(orientation.equalsIgnoreCase("8")){
                image=rotate(image, 270);
            }else if(orientation.equalsIgnoreCase("3")){
                image=rotate(image, 180);
            }

            return image;
        } catch (Exception e) {
            return image;
        }
    }

    public static Bitmap scaleBitmapToWidth(Bitmap bitmap, int newWidth) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        double ratio = (double)height/(double)width;
        int newHeight = (int)Math.round(newWidth*ratio);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false);
    }

    public static void saveImageForPath(Context c, Bitmap bitmap, String pathId, String fileName) {
        File dir = TrailbookFileUtilities.getInternalImageDirForPath(c, pathId);
        File imageFile = new File(dir, fileName);
        FileOutputStream fOut;
        try {
            fOut = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
            fOut.flush();
            fOut.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
