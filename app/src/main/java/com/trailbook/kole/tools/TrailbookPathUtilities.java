package com.trailbook.kole.tools;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.trailbook.kole.data.Path;

import java.util.Date;

/**
 * Created by kole on 7/19/2014.
 */
public class TrailbookPathUtilities {
    private static String newNoteId;

    public static double getNearestDistanceFromPointToPath(LatLng currentLocation, Path path) {
        double minDist = Double.MAX_VALUE;
        for (LatLng thisPathPoint:path.getPoints()) {
            float thisDist = getDistanceInMeters(currentLocation, thisPathPoint);
            if ( thisDist < minDist)
                minDist = thisDist;
        }

        return minDist;
    }

    public static float getDistanceInMeters(LatLng p1, LatLng p2) {
        float[] results = new float[5];
        Location.distanceBetween(p1.latitude, p1.longitude, p2.latitude, p2.longitude, results);
        return results[0];
    }

    public static LatLng locationToLatLon(Location l) {
        return new LatLng(l.getLatitude(), l.getLongitude());
    }

    public static String getNewNoteId() {
        Date date = new Date();
        return String.valueOf(date.getTime());
    }


    public static String getNewPathId() {
        Date date = new Date();
        return String.valueOf(date.getTime());
    }

}
