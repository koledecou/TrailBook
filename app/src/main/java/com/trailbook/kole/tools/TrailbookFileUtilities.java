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
import com.trailbook.kole.data.Note;
import com.trailbook.kole.data.Path;

import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;

import android.net.http.AndroidHttpClient;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.jar.Attributes;

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
        try {
            FileUtils.writeByteArrayToFile(imageFile, TrailbookFileUtilities.getBytes(bitmap));
        } catch (Exception e) {
            Log.e(Constants.TRAILBOOK_TAG, "exception in saving image ", e);
            e.printStackTrace();
        }
    }

    public static Bitmap loadImageFromFileForNote(Context c, Note note) {
        String imageFileName = note.getImageFileName();
        String pathId = note.getParentPathId();

        File fullDirectory = TrailbookFileUtilities.getInternalImageDirForPath(c, pathId);
        String fileNameWithPath = fullDirectory.toString() + "/" + imageFileName;
        return loadBitmapFromFile(fileNameWithPath);
    }

    public static Bitmap loadBitmapFromFile (String fileNameWithPath) {
        Bitmap bitmap = BitmapFactory.decodeFile(fileNameWithPath);
        return bitmap;
    }

    public static String getImageUploadUrl() {
        URI uploadPicturesURI = null;
        String uploadPicturesURL = Constants.BASE_URL + Constants.uploadImage;
        try {
            uploadPicturesURI = new URI(uploadPicturesURL);
        } catch (URISyntaxException e) {
            Log.e(Constants.TRAILBOOK_TAG, "error getting URI", e);
        }

        return uploadPicturesURL;
    }

    public static MultipartEntity getMultipartEntityForNoteImage(Context c, Note n) {
        try {
            File imageFile = getInternalImageFile(c, n.getParentPathId(), n.getImageFileName());
            MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);

            entity.addPart("pathId", new StringBody(n.getParentPathId()));
            entity.addPart("noteId", new StringBody(n.getNoteID()));
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

}
