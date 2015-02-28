package com.trailbook.kole.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;

import com.trailbook.kole.data.Constants;
import com.trailbook.kole.state_objects.TrailBookState;

/**
 * Created by kole on 2/24/2015.
 */
public class TrailbookLauncher extends Activity {

    private static final long SPLASH_DISPLAY_LENGTH = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);

        /* New Handler to start the Menu-Activity
         * and close this Splash-Screen after some seconds.*/
        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {
                /* Create an Intent that will start the Menu-Activity. */
                launchMap();
            }
        }, SPLASH_DISPLAY_LENGTH);

    }

    private void launchMap() {
        Log.d(Constants.TRAILBOOK_TAG, "Launching TrailBook");
        Intent launchTrailbookIntent = new Intent(this, TrailBookActivity.class);
        launchTrailbookIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        launchTrailbookIntent.putExtra(TrailBookActivity.INITIAL_PATH_ID_KEY, TrailBookState.NO_START_PATH);
        startActivity(launchTrailbookIntent);
        finish();
    }
}