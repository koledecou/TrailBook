package com.trailbook.kole.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.gms.maps.GoogleMap;
import com.trailbook.kole.activities.R;

import java.text.DecimalFormat;

/**
 * Created by kole on 8/28/2014.
 */
public class PreferenceUtilities {
    public static int getStrayFromPathTriggerDistanceInMeters(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        int triggerDist = getStrayFromPathTriggerDistance(c);

        if ((prefs.getString("unit_of_measure_pref", "US")).equalsIgnoreCase("US")){
            triggerDist = (int) Math.round(feetToMeters(triggerDist));
        }
        return triggerDist;
    }

    public static int getStrayFromPathTriggerDistance(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        if (!prefs.getBoolean("enable_offroute_alert", true)) {
            return Integer.MAX_VALUE;
        }
        String sTriggerDist = prefs.getString("off_route_distance_pref", "75");
        int triggerDist = Integer.valueOf(sTriggerDist);

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
        DecimalFormat df = new DecimalFormat(formatter);
        String distString = df.format(dist) + " " + unitOfDistance;

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
        //todo: localize
        String directions[] = {"N", "NE", "E", "SE", "S", "SW", "W", "NW", "N"};
        return directions[ (int)Math.round((  ((double)bearingInDegrees % 360) / 45)) ];
    }

    public static double feetToMeters(double feet) {
        return (double)feet * 0.3048d;
    }

    public static double metersToFeet(double meters) {
        return (double)meters / 0.3048d;
    }

    public static int getNoteAlertDistanceInMeters(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        int triggerDist = getNoteAlertDistance(c);
        if (isUSUnitsPreferred(c)){
            triggerDist = (int) Math.round(feetToMeters(triggerDist));
        }
        return triggerDist;
    }

    public static int getNoteAlertDistance(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        if (!prefs.getBoolean("enable_prox_2", true)) {
            return Integer.MIN_VALUE;
        }
        String sTriggerDist = prefs.getString("triggerRadiusClose", "100");
        int triggerDist = Integer.valueOf(sTriggerDist);
        return triggerDist;
    }

    public static boolean isUSUnitsPreferred(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        if ((prefs.getString("unit_of_measure_pref", "US")).equalsIgnoreCase("US")){
            return true;
        } else {
            return false;
        }
    }

    public static String getDistanceAndBearingString(Context context, float distance, float bearing) {
        String relativeLocationMessage = context.getString(R.string.dist_bearing_string);
        String distanceString = getDistString(context, distance);
        String bearingString = getBearingString(context, bearing);
        return String.format(relativeLocationMessage, distanceString, bearingString);
    }

    public static int getMapType(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        String mapType = prefs.getString("map_type_pref", "HYBIRD");
        if ("TERRAIN".equals(mapType))
            return GoogleMap.MAP_TYPE_TERRAIN;
        else if ("HYBIRD".equals(mapType))
            return GoogleMap.MAP_TYPE_HYBRID;
        else if ("NONE".equals(mapType))
            return GoogleMap.MAP_TYPE_NONE;
        else if ("NORMAL".equals(mapType))
            return GoogleMap.MAP_TYPE_NORMAL;
        else if ("SATELLITE".equals(mapType))
            return GoogleMap.MAP_TYPE_SATELLITE;
        else
            return GoogleMap.MAP_TYPE_SATELLITE;
    }
}

