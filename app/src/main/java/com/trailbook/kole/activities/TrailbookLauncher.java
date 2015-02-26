package com.trailbook.kole.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.trailbook.kole.data.Constants;
import com.trailbook.kole.state_objects.TrailBookState;

/**
 * Created by kole on 2/24/2015.
 */
public class TrailbookLauncher extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        launchMap();
    }

    private void launchMap() {
        Log.d(Constants.TRAILBOOK_TAG, "Launching trailbook");
        Intent launchTrailbookIntent = new Intent(this, TrailBookActivity.class);
        launchTrailbookIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        launchTrailbookIntent.putExtra(TrailBookActivity.INITIAL_PATH_ID_KEY, TrailBookState.NO_START_PATH);
        startActivity(launchTrailbookIntent);
        finish();
    }
}