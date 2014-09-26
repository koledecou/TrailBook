package com.trailbook.kole.data;

import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.trailbook.kole.data.geo.GeoPoint;

public class PointAttachedObject<T> {
    //public LatLng point;
    T attachment;
    GeoPoint point;
    String _id;
    public String attachmentType = "Climb";

    public PointAttachedObject(String id, LatLng c, T attachment) {
        this.point = new GeoPoint();
        this.point.setCoordinates(new double[]{c.longitude, c.latitude});
        this.attachment = attachment;
        this._id = id;
        this.attachmentType = attachment.getClass().getSimpleName();
        Log.d(Constants.TRAILBOOK_TAG, "PAO: Creating attachment with type " +attachmentType);
    }

    public void setAttachment(T a) {
        this.attachment=a;
        this.attachmentType = a.getClass().getSimpleName();
    }

    public T getAttachment() {
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

    public void updateAttachmentType() {
        if (attachment != null)
            this.attachmentType = attachment.getClass().getSimpleName();
    }
}
