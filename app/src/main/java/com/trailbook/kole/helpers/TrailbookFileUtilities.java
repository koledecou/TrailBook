package com.trailbook.kole.helpers;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.Path;
import com.trailbook.kole.data.PathSegment;
import com.trailbook.kole.data.PointAttachedObject;
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
import java.util.ArrayList;

/**
 * Created by Fistik on 7/14/2014.
 */

public class TrailbookFileUtilities {
    public static String getExternalMailTempDirectory() {
        //return TrailBookState.getInstance().getCacheDir().getAbsolutePath() + File.separator + Constants.tempDir;
        //return TrailBookState.getInstance().getCacheDir().getAbsolutePath();
        return Environment.getExternalStorageDirectory().toString() + File.separator + "trailbook" + File.separator + "mail";
    }

    public static String getInternalMailTempDirectory() {
        return TrailBookState.getInstance().getCacheDir().getAbsolutePath() + File.separator + Constants.tempDir;
        //return TrailBookState.getInstance().getCacheDir().getAbsolutePath();
    }

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

    public static String getInternalCommentsDirectory() {
        return TrailBookState.getInstance().getFilesDir().getAbsolutePath() + File.separator  + Constants.commentsDir;
    }

    public static String getInternalCommentsDirectory(String pathId) {
        return getInternalCommentsDirectory() + File.separator + pathId;
    }

    public static File getInternalCommentFile(String pathId, String commentId) {
        String fullDirectory = getInternalCommentsDirectory(pathId);
        String fileName = pathId + "_" + commentId + "_comment.tb";
        return new File(fullDirectory, fileName);
    }

    public static File getInternalPathSummaryFile(String pathId) {
        String fullDirectory = getInternalPathDirectory(pathId);
        return new File(fullDirectory, getSummaryFileName(pathId));
    }

    public static File getCachedPathSummaryFile(String pathId) {
        String fullDirectory = getCacheDirectoryForPath(pathId);
        return new File(fullDirectory, getSummaryFileName(pathId));
    }

    public static File getInternalSegmentPointsFile(String segmentId) {
        String fullDirectory = getInternalSegmentDirectory(segmentId);
        return new File(fullDirectory, getPointsFileName(segmentId));
    }

    public static File getInternalPAOFile(String noteId) {
        String fullDirectory = getInternalNoteDirectory();
        return new File(fullDirectory, getNoteFileName(noteId));
    }

    public static String getSummaryFileName(String pathId) {
        return pathId + "_summary.tb";
    }

    public static String getPointsFileName(String segmentId) {
        return segmentId + "_points.tb";
    }

    public static String getNoteFileName(String noteId) {
        return noteId + "_note.tb";
    }

    public static File getInternalImageFile(String imageFileName) {
        String fullDirectory = TrailBookState.getInstance().getFilesDir().getAbsolutePath() + File.separator + Constants.notesDir + File.separator  + Constants.imageDir;
        return new File(fullDirectory, imageFileName);
    }

    public static ArrayList<String> getInternalImageFiles(ArrayList<String> fileNames) {
        ArrayList<String> imageFileUris = new ArrayList<String>();
        for (String fileName:fileNames) {
            File f = getInternalImageFile(fileName);
            String uri = f.toURI().toString();
            imageFileUris.add(uri);
        }
        return imageFileUris;
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

    public static int getOrientation(Context context, Uri photoUri) {
    /* it's on the external media. */
        Cursor cursor = context.getContentResolver().query(photoUri,
                new String[] { MediaStore.Images.ImageColumns.ORIENTATION }, null, null, null);

        if (cursor.getCount() != 1) {
            return -1;
        }

        cursor.moveToFirst();
        return cursor.getInt(0);
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
            if (imageFile.exists()) {
                entity.addPart("imageFile", new FileBody(imageFile)); //image should be a String
                return entity;
            } else {
                return null;
            }
        } catch (Exception e) {
            Log.e(Constants.TRAILBOOK_TAG, "error getting multipart entity", e);
            return null;
        }
    }

    public static String getBoundry() {
        return "SwA"+Long.toString(System.currentTimeMillis())+"SwA";
    }

    public static String getKeyHashFileName() {
        return TrailBookState.getInstance().getFilesDir().getAbsolutePath() + File.separator  + Constants.keyHashFile;
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
        os.write( ("Content-Disposition: form-data; name=\"" + paramName + "\"\r\n").getBytes());
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

    public static File zipPathToTempFile(Path p) {
        String tempPathDirectory = getExternalMailTempDirectory() + File.separator  + p.summary.getId();
        packagePath(p, tempPathDirectory);

        String tempPathZipFileName = p.summary.getName() + ".tbz";
        String tempPathZipFileFullName =  getExternalMailTempDirectory() + File.separator + tempPathZipFileName;
        Log.d(Constants.TRAILBOOK_TAG, "zipping to " + tempPathZipFileFullName);
        Zipper zipper = new Zipper(tempPathDirectory, tempPathZipFileFullName);
        zipper.zipIt();
        //zipFileAtPath(tempPathDirectory, tempPathZipFileFullName);
        File zipFile = new File(tempPathZipFileFullName);
        return zipFile;
    }

    private static void packagePath(Path path, String targetDir) {
        String pathId =  path.summary.getId();
        try {
            FileUtils.forceMkdir(new File(targetDir));
        } catch (IOException e) {
            Log.e(Constants.TRAILBOOK_TAG, "Error creating temp dir", e);
        }

        copyPathDirectory(pathId, new File(targetDir));
        copySegments(path, targetDir);
        copyNotes(path, targetDir);
    }

    private static void copyNotes(Path path, String tempPathDirectory) {
        for (PointAttachedObject pao:path.paObjects) {
            String noteId = pao.getId();
            File targetDir = new File(tempPathDirectory + File.separator + "notes");
            copyNoteFile(noteId, targetDir);
            ArrayList<String> imageFileNames = pao.getAttachment().getImageFileNames();
            copyImageFiles(imageFileNames, new File(targetDir + File.separator + "images"));
        }
    }

    private static void copyImageFiles(ArrayList<String> fileNames, File targetDir) {
        if (fileNames != null && fileNames.size()>0) {
            for (String fileName : fileNames) {
                if (fileName != null && fileName.length() > 0) {
                    Log.d(Constants.TRAILBOOK_TAG, "TrailbookFileUtilities copying image file: " + fileName);
                    String fullFileName = getInternalImageFileDir() + File.separator + fileName;
                    Log.d(Constants.TRAILBOOK_TAG, "TrailbookFileUtilities copying image file full name: " + fileName);
                    try {
                        FileUtils.copyFileToDirectory(new File(fullFileName), targetDir);
                    } catch (IOException e) {
                        Log.e(Constants.TRAILBOOK_TAG, "Error copyiing image file ", e);
                    }
                }
            }
        }
    }

    private static void copyNoteFile(String noteId, File targetDir) {
        File sourceFile = new File(getInternalNoteDirectory() + File.separator + getNoteFileName(noteId));
        try {
            FileUtils.copyFileToDirectory(sourceFile, targetDir);
        } catch (IOException e) {
            Log.e(Constants.TRAILBOOK_TAG, "Error copyiing path dir ", e);
        }
    }

    private static void copySegments(Path path, String tempPathDirectory) {
        for (PathSegment s:path.segments) {
            String segmentId = s.getId();
            copySegmentDir(segmentId, new File(tempPathDirectory + File.separator + "segments" + File.separator + segmentId));
        }
    }

    private static void copySegmentDir(String segmentId, File targetDir) {
        File sourceDir = new File(getInternalSegmentDirectory(segmentId));
        try {
            FileUtils.copyDirectory(sourceDir, targetDir);
        } catch (IOException e) {
            Log.e(Constants.TRAILBOOK_TAG, "Error copyiing path dir ", e);
        }
    }

    private static void copyPathDirectory(String pathId, File targetDir) {
        File sourceDir = new File(getInternalPathDirectory(pathId));
        try {
            FileUtils.copyDirectory(sourceDir, targetDir);
        } catch (IOException e) {
            Log.e(Constants.TRAILBOOK_TAG, "Error copyiing path dir ", e);
        }
    }

    public static String explodeCompressedPath(String pathZipFileName, String destFolder) {
        Unzipper unzipper = new Unzipper(pathZipFileName, destFolder);
        unzipper.unZipIt();
        PathExtractDirectoryWalker walker = new PathExtractDirectoryWalker();
        walker.putPathFilesInFolders(destFolder);
        copyPathToInternalDirectories(walker);
        return walker.getPathId();
    }

    private static void copyPathToInternalDirectories(PathExtractDirectoryWalker walker) {
        copySummaryFileToCacheDir(walker.getSummaryFile());
        copySummaryFileToPathDir(walker.getPathId(), walker.getSummaryFile());
        copyNotesToNoteDir(walker.getNoteFiles());
        copyImagesToImageDir(walker.getImageFiles());
        copySegmentsToSegmentDir(walker.getSegmentDirectories());
    }

    private static void copySegmentsToSegmentDir(ArrayList<File> segmentDirectories) {
        if (segmentDirectories != null && segmentDirectories.size()>0) {
            for (File segmentDirectory:segmentDirectories) {
                try {
                    Log.d(Constants.TRAILBOOK_TAG, "copying segment dir " + segmentDirectory.getAbsolutePath() + " to " + getInternalSegmentDirectory());
                    FileUtils.copyDirectoryToDirectory(segmentDirectory, new File(getInternalSegmentDirectory()));
                    //FileUtils.copyDirectory(segmentDirectory, new File(getInternalSegmentDirectory()));
                } catch (IOException e) {
                    Log.e(Constants.TRAILBOOK_TAG, "Error copying segment to dir", e);
                }
            }
        }
    }

    private static void copyImagesToImageDir(ArrayList<File> imageFiles) {
        if (imageFiles != null && imageFiles.size()>0) {
            for (File imageFile:imageFiles) {
                try {
                    FileUtils.copyFileToDirectory(imageFile, getInternalImageFileDir());
                } catch (IOException e) {
                    Log.e(Constants.TRAILBOOK_TAG, "Error copying image to dir", e);
                }
            }
        }
    }

    private static void copyNotesToNoteDir(ArrayList<File> noteFiles) {
        if (noteFiles != null && noteFiles.size()>0) {
            for (File noteFile:noteFiles) {
                try {
                    FileUtils.copyFileToDirectory(noteFile, new File(getInternalNoteDirectory()));
                } catch (IOException e) {
                    Log.e(Constants.TRAILBOOK_TAG, "Error copying note to dir", e);
                }
            }
        }
    }

    private static void copySummaryFileToPathDir(String pathId, File summaryFile) {
        createPathDir(pathId);
        try {
            FileUtils.copyFileToDirectory(summaryFile, new File(getInternalPathDirectory(pathId)));
        } catch (IOException e) {
            Log.e(Constants.TRAILBOOK_TAG, "Error copying summary to path dir", e);
        }
    }

    private static void copySummaryFileToCacheDir(File summaryFile) {
        try {
            FileUtils.copyFileToDirectory(summaryFile, new File(getInternalCacheDirectory()));
        } catch (IOException e) {
            Log.e(Constants.TRAILBOOK_TAG, "Error copying summary to cache dir", e);
        }
    }

    private static void createPathDir(String pathId) {
        String pathDir = getInternalPathDirectory(pathId);
        try {
            FileUtils.forceMkdir(new File(pathDir));
        } catch (IOException e) {
            Log.e(Constants.TRAILBOOK_TAG, "Error making path dir ", e);
        }
    }

    public static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
