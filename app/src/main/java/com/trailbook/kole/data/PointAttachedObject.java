package com.trailbook.kole.data;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.annotations.Expose;

public class PointAttachedObject<T> {
    public LatLng point;
    public T attachment;

    public PointAttachedObject (LatLng c, T attachment) {
        this.point = c;
        this.attachment = attachment;
    }

    public void setAttachment(T a) {
        this.attachment=a;
    }

    public T getAttachment() {
        return attachment;
    }

    public float[] getDistanceAndBearingFrom (LatLng location) {
        float[] results = null;
        Location.distanceBetween(location.latitude, location.longitude,
                point.latitude, point.longitude, results);
        //returns distance in meters
        return results;
    }

    public LatLng getLocation() {
        return point;
    }
}
