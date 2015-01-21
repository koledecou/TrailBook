package com.trailbook.kole.helpers;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.model.Marker;
import com.trailbook.kole.activities.R;

public class MapPointSelectedAdaptor implements InfoWindowAdapter {
    private View popup=null;
    private LayoutInflater inflater=null;

    public static final String pointMarkerTitle = "SELECTED_POINT";

    public MapPointSelectedAdaptor(LayoutInflater inflater) {
        this.inflater=inflater;
    }

    @Override
    public View getInfoWindow(Marker marker) {
        return(null);
    }

    @SuppressLint("InflateParams")
    @Override
    public View getInfoContents(Marker marker) {
        if (marker.getTitle().equals(pointMarkerTitle)) {
            if (popup == null) {
                popup = inflater.inflate(R.layout.point_selected_help_dialog, null);
            }

            TextView tv = (TextView) popup.findViewById(R.id.instructions);
            tv.setText(R.string.edit_point_help_text);
            return (popup);
        } else {
            return null;
        }
    }
}