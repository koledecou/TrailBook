package com.trailbook.kole.helpers;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polyline;

import java.util.ArrayList;
import java.util.List;

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
            Toast.makeText(callingContext, "Maps Failed to Launch", Toast.LENGTH_SHORT).show();
        }
    }

    public static Uri getMapsUri(String startLatLon) {
        String geoUriString = "geo:0,0?q=" + startLatLon;
        return Uri.parse(geoUriString);
    }

    public static LatLngBounds getBoundsForPolylineArray(ArrayList<Polyline> lines) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Polyline line:lines) {
            List<LatLng> points = line.getPoints();
            for (LatLng point:points) {
                builder.include(point);
            }
        }
        return builder.build();
    }
}
