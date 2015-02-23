package com.trailbook.kole.activities;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;

import com.trailbook.kole.data.Constants;
import com.trailbook.kole.helpers.TrailbookFileUtilities;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PathFileReceiverActivity extends Activity {
    String mId;
    private final String tempDirectory = TrailbookFileUtilities.getInternalMailTempDirectory();
    private final String tempFileName = tempDirectory + File.separator + "import.zip";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String action = intent.getAction();
        InputStream input = null;
        input = getInputStream(intent, action, input);
        cleanupTempDirectories();
        WriteInputStreamToTempFile(input, tempFileName);
        String pathId = TrailbookFileUtilities.explodeCompressedPath(tempFileName, tempDirectory + File.separator + "import");
        Intent launchTrailbookIntent = new Intent(this, TrailBookActivity.class);
        launchTrailbookIntent.putExtra(TrailBookActivity.INITIAL_PATH_ID_KEY, pathId);
        //launchTrailbookIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(launchTrailbookIntent);
    }

    private void cleanupTempDirectories() {
        try {
            FileUtils.deleteDirectory(new File(tempDirectory));
            FileUtils.forceMkdir(new File(tempDirectory));
            Log.d(Constants.TRAILBOOK_TAG, "error deleting temp directory");
        } catch (IOException e) {
            Log.e(Constants.TRAILBOOK_TAG, "Error creating temp dir", e);
        }

    }

    private InputStream getInputStream(Intent intent, String action, InputStream input) {
        if (action.compareTo(Intent.ACTION_VIEW) == 0) {
            String scheme = intent.getScheme();
            ContentResolver resolver = getContentResolver();

            if (scheme.compareTo(ContentResolver.SCHEME_CONTENT) == 0) {
                Uri uri = intent.getData();
                String name = getContentName(resolver, uri);

                Log.d(Constants.TRAILBOOK_TAG, "Content intent detected: " + action + " : " + intent.getDataString() + " : " + intent.getType() + " : " + name);
                try {
                    input = resolver.openInputStream(uri);
                } catch (FileNotFoundException e) {
                    Log.e(Constants.TRAILBOOK_TAG, "Error recieving path:", e);
                }
            } else if (scheme.compareTo(ContentResolver.SCHEME_FILE) == 0) {
                Uri uri = intent.getData();
                String name = uri.getLastPathSegment();

                Log.d(Constants.TRAILBOOK_TAG, "File intent detected: " + action + " : " + intent.getDataString() + " : " + intent.getType() + " : " + name);
                try {
                    input = resolver.openInputStream(uri);
                } catch (FileNotFoundException e) {
                    Log.e(Constants.TRAILBOOK_TAG, "Error recieving path:", e);
                }
            } else if (scheme.compareTo("http") == 0) {
                // TODO Import from HTTP!
            } else if (scheme.compareTo("ftp") == 0) {
                // TODO Import from FTP!
            }
        }
        return input;
    }

    private String getContentName(ContentResolver resolver, Uri uri){
        Cursor cursor = resolver.query(uri, null, null, null, null);
        cursor.moveToFirst();
        int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
        if (nameIndex >= 0) {
            return cursor.getString(nameIndex);
        } else {
            return null;
        }
    }

    private void WriteInputStreamToTempFile(InputStream in, String file) {
        StringBuffer sb = new StringBuffer();
        try {
            OutputStream out = new FileOutputStream(new File(file));

            int size = 0;
            byte[] buffer = new byte[1024];

            while ((size = in.read(buffer)) != -1) {
                out.write(buffer, 0, size);
                sb.append(buffer);
            }

            out.close();
        }
        catch (Exception e) {
            Log.e(Constants.TRAILBOOK_TAG, "WriteInputStreamToTempFile exception: " + e.getMessage());
        }
        Log.d(Constants.TRAILBOOK_TAG, sb.toString());
    }
}
