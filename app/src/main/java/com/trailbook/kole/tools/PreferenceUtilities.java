package com.trailbook.kole.tools;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.trailbook.kole.activities.R;
import com.trailbook.kole.data.Constants;

import java.text.DecimalFormat;

/**
 * Created by kole on 8/28/2014.
 */
public class PreferenceUtilities {
    public static int getStrayFromPathTriggerDistanceInMeters(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        if (!prefs.getBoolean("enable_offroute_alert", true)) {
            return Integer.MAX_VALUE;
        }
        String sTriggerDist = prefs.getString("off_route_distance_pref", "75");
        int triggerDist = Integer.valueOf(sTriggerDist);

        if ((prefs.getString("unit_of_measure_pref", "US")).equalsIgnoreCase("US")){
            triggerDist = (int) Math.round(feetToMeters(triggerDist));
        }
        return triggerDist;
    }

    public static String getDistString(Context c, double distInMeters) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        double dist;
        String unitOfDistance;
        String formatter = "#";
        if ((prefs.getString("unit_of_measure_pref", "US")).equalsIgnoreCase("US")) {
            dist = metersToFeet(distInMeters);
            if (dist < 1320) {
                unitOfDistance = c.getResources().getString(R.string.feet);                
            } else {
                unitOfDistance = c.getResources().getString(R.string.miles);
                dist = feetToMiles(dist);
                formatter = "#.#";
            }
        } else {
            dist = distInMeters;
            if (dist < 1000) {
                unitOfDistance = c.getResources().getString(R.string.meters);
            } else {
                unitOfDistance = c.getResources().getString(R.string.kilometers);
                dist = metersToKilometers(dist);
                formatter = "#.#";
            }
        }
        Log.d(Constants.TRAILBOOK_TAG, "dist, " + dist);
        Log.d(Constants.TRAILBOOK_TAG, "unitOfDistance, " + unitOfDistance);
        Log.d(Constants.TRAILBOOK_TAG, "formatter, " + formatter);
        DecimalFormat df = new DecimalFormat(formatter);
        String distString = df.format(dist) + " " + unitOfDistance;

        Log.d(Constants.TRAILBOOK_TAG, "distString, " + distString);
        return distString;
    }

    private static double metersToKilometers(double meters) {
        return (double)meters/1000d;
    }

    private static double feetToMiles(double feet) {
        return (double)feet/5280d;
    }

    public static String getBearingString(Context context, double bearingInDegrees)
    {
        if (bearingInDegrees < 0) {
            bearingInDegrees = 360 + bearingInDegrees;
        }
        Log.d(Constants.TRAILBOOK_TAG, "bearingInDegrees, " + bearingInDegrees);
        //todo: localize
        String directions[] = {"N", "NE", "E", "SE", "S", "SW", "W", "NW", "N"};
        Log.d(Constants.TRAILBOOK_TAG, "intBearing, " +  ((double)bearingInDegrees % 360) / 45);
        return directions[ (int)Math.round((  ((double)bearingInDegrees % 360) / 45)) ];
    }

    public static double feetToMeters(double feet) {
        return (double)feet * 0.3048d;
    }

    public static double metersToFeet(double meters) {
        return (double)meters / 0.3048d;
    }
}

