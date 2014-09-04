package com.trailbook.kole.tools;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

/**
 * Created by kole on 8/28/2014.
 */
public class MapUtilities {
    public static void navigateTo(Context callingContext, String coords) {
        try {
            Uri uri = getMapsUri(coords);
            Intent i = new Intent(android.content.Intent.ACTION_VIEW,uri);
            callingContext.startActivity(i);
        } catch (Exception e) {
            Toast.makeText(callingContext, "Maps Failed to Launch", Toast.LENGTH_SHORT);
        }
    }

    public static Uri getMapsUri(String startLatLon) {
        String geoUriString = "geo:0,0?q=" + startLatLon;
        return Uri.parse(geoUriString);
    }
}
