package com.trailbook.kole.data;

import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.trailbook.kole.data.geo.GeoPoint;

public class PointAttachedObject {
    //public LatLng point;
    Attachment attachment;
    String _id;
    GeoPoint point;

    public PointAttachedObject(String id, LatLng c, Attachment attachment) {
        this.point = new GeoPoint();
        this.point.setCoordinates(new double[]{c.longitude, c.latitude});
        this.attachment = attachment;
        this._id = id;
        Log.d(Constants.TRAILBOOK_TAG, "PAO: Creating attachment with type " +attachment.getType());
    }

    public void setAttachment(Attachment a) {
        this.attachment=a;
    }

    public Attachment getAttachment() {
        return attachment;
    }

    public float[] getDistanceAndBearingFrom (LatLng location) {
        float[] results = null;
        LatLng p = point.toLatLng();
        Location.distanceBetween(location.latitude, location.longitude,
                p.latitude, p.longitude, results);
        //returns distance in meters
        return results;
    }

    public LatLng getLocation() {
        return point.toLatLng();
    }

    public String getId() {
        return _id;
    }

}
