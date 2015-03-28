package com.trailbook.kole.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.trailbook.kole.state_objects.TrailBookState;

/**
 * Created by kole on 2/24/2015.
 */
public class TrailbookLauncher extends Activity {

    private static final long SPLASH_DISPLAY_LENGTH = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        launchMap();
    }

    private void launchMap() {
        Intent launchTrailbookIntent = new Intent(this, TrailBookActivity.class);
        launchTrailbookIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        launchTrailbookIntent.putExtra(TrailBookActivity.INITIAL_PATH_ID_KEY, TrailBookState.NO_START_PATH);
        startActivity(launchTrailbookIntent);
        finish();
    }
}