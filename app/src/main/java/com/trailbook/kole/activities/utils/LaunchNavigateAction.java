package com.trailbook.kole.activities.utils;

import android.app.Activity;

import com.google.android.gms.maps.model.LatLng;
import com.trailbook.kole.helpers.MapUtilities;

/**
 * Created by kole on 4/22/2015.
 */
public class LaunchNavigateAction implements Action {
    LatLng startCoords;
    Activity launchingActivity;

    public LaunchNavigateAction(Activity launchingActivity, LatLng startCoords) {
        this.startCoords = startCoords;
        this.launchingActivity = launchingActivity;
    }

    @Override
    public void execute() {
        MapUtilities.navigateTo(launchingActivity, String.valueOf(startCoords.latitude) + "," + String.valueOf(startCoords.longitude));
    }
}
