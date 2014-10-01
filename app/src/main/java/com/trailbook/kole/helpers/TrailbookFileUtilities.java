package com.trailbook.kole.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.trailbook.kole.data.Constants;
import com.trailbook.kole.state_objects.TrailBookState;

import org.apache.commons.io.FileUtils;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Created by Fistik on 7/14/2014.
 */

public class TrailbookFileUtilities {

    public static String getInternalPathDirectory() {
        return TrailBookState.getInstance().getFilesDir().getAbsolutePath() + File.separator  + Constants.pathsRootDir;
    }

    public static String getInternalCacheDirectory() {
        return TrailBookState.getInstance().getFilesDir().getAbsolutePath() + File.separator  + Constants.cachedPathsRootDir;
    }

    public static String getInternalPathDirectory(String pathId) {
        return getInternalPathDirectory() + File.separator + pathId;
    }

    public static String getCacheDirectoryForPath(String pathId) {
        return getInternalCacheDirectory() + File.separator + pathId;
    }

    public static String getInternalNoteDirectory() {
        return TrailBookState.getInstance().getFilesDir().getAbsolutePath() + File.separator  + Constants.notesDir;
    }

    public static String getInternalSegmentDirectory() {
        return TrailBookState.getInstance().getFilesDir().getAbsolutePath() + File.separator  + Constants.segmentsDir;
    }

    public static String getInternalSegmentDirectory(String segmentId) {
        return getInternalSegmentDirectory() + File.separator + segmentId;
    }

    public static File getInternalPathSummaryFile(String pathId) {
        String fullDirectory = getInternalPathDirectory(pathId);
        String fileName = pathId + "_summary.tb";
        return new File(fullDirectory, fileName);
    }

    public static File getCachedPathSummaryFile(String pathId) {
        String fullDirectory = getCacheDirectoryForPath(pathId);
        String fileName = pathId + "_summary.tb";
        return new File(fullDirectory, fileName);
    }

    public static File getInternalSegmentPointsFile(String segmentId) {
        String fullDirectory = getInternalSegmentDirectory(segmentId);
        String fileName = segmentId + "_points.tb";
        return new File(fullDirectory, fileName);
    }

    public static File getInternalPAOFile(String noteId) {
        String fullDirectory = getInternalNoteDirectory();
        String fileName = noteId + "_note.tb";
        return new File(fullDirectory, fileName);
    }

    public static File getInternalImageFile(String imageFileName) {
        String fullDirectory = TrailBookState.getInstance().getFilesDir().getAbsolutePath() + File.separator + Constants.notesDir + File.separator  + Constants.imageDir;
        return new File(fullDirectory, imageFileName);
    }

    public static File getInternalImageFileDir() {
        String fullDirectory = TrailBookState.getInstance().getFilesDir().getAbsolutePath() + File.separator + Constants.notesDir + File.separator  + Constants.imageDir;
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

    public static void saveImage(Context c, Bitmap bitmap, String fileName) {
        File dir = getInternalImageFileDir();
        File imageFile = new File(dir, fileName);
        try {
            Log.d(Constants.TRAILBOOK_TAG, "TrailBookUtilities: saving image " +imageFile);
            FileUtils.writeByteArrayToFile(imageFile, TrailbookFileUtilities.getBytes(bitmap));
        } catch (Exception e) {
            Log.e(Constants.TRAILBOOK_TAG, "exception in saving image ", e);
            e.printStackTrace();
        }
    }

    public static Bitmap loadBitmapFromFile (String fileNameWithPath) {
        Bitmap bitmap = BitmapFactory.decodeFile(fileNameWithPath);
        return bitmap;
    }

    public static String getImageUploadUrl() {
        URI uploadPicturesURI = null;
        String uploadPicturesURL = Constants.BASE_CGIBIN_URL + Constants.uploadImage;
        try {
            uploadPicturesURI = new URI(uploadPicturesURL);
        } catch (URISyntaxException e) {
            Log.e(Constants.TRAILBOOK_TAG, "error getting URI", e);
        }

        return uploadPicturesURL;
    }

    public static MultipartEntity getMultipartEntityForPAOImage(String imageFileName) {
        try {
            File imageFile = getInternalImageFile(imageFileName);
            MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);

/*deleteme            entity.addPart("segmentId", new StringBody(n.getParentSegmentId()));
            entity.addPart("noteId", new StringBody(n.getNoteID()));*/
            entity.addPart("imageFile",  new FileBody(imageFile)); //image should be a String

            return entity;
        } catch (Exception e) {
            Log.e(Constants.TRAILBOOK_TAG, "error getting multipart entity", e);
            return null;
        }
    }

    public static String getBoundry() {
        return "SwA"+Long.toString(System.currentTimeMillis())+"SwA";
    }

    public void addFilePart(OutputStream os, String paramName, String fileName, byte[] data) throws Exception {
        os.write( (Constants.delimiter + getBoundry() + "\r\n").getBytes());
        os.write( ("Content-Disposition: form-data; name=\"" + paramName +  "\"; filename=\"" + fileName + "\"\r\n"  ).getBytes());
        os.write( ("Content-Type: application/octet-stream\r\n"  ).getBytes());
        os.write( ("Content-Transfer-Encoding: binary\r\n"  ).getBytes());
        os.write("\r\n".getBytes());
        os.write(data);
        os.write("\r\n".getBytes());
    }

    public void addFormPart(OutputStream os, String paramName, String value) throws Exception {
        writeParamData(os, paramName, value);
    }

    private void writeParamData(OutputStream os, String paramName, String value) throws Exception {
        os.write( (Constants.delimiter + getBoundry() + "\r\n").getBytes());
        os.write( "Content-Type: text/plain\r\n".getBytes());
        os.write( ("Content-Disposition: form-data; name=\"" + paramName + "\"\r\n").getBytes());;
        os.write( ("\r\n" + value + "\r\n").getBytes());
    }

    public static OutputStream connectForMultipart(String url) throws Exception {
        HttpURLConnection con = (HttpURLConnection) ( new URL(url)).openConnection();
        con.setRequestMethod("POST");
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setRequestProperty("Connection", "Keep-Alive");
        con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + getBoundry());
        con.connect();
        OutputStream os = con.getOutputStream();

        return os;
    }

/*    public static String getWebServerImageDir(String segmentId) {
        return Constants.BASE_WEBSERVER_SEGMENT_URL + "/" + segmentId;
    }*/

    public static String getWebServerImageDir() {
        return Constants.BASE_WEBSERVERFILE_URL + "/" + Constants.imageDir;
    }


    public static String readTextFromUri(Context c, Uri uri) throws IOException {
        InputStream inputStream = c.getContentResolver().openInputStream(uri);
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }
        inputStream.close();
        reader.close();
        return stringBuilder.toString();
    }


}
