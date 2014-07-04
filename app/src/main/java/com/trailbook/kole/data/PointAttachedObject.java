package com.trailbook.kole.data;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

public class PointAttachedObject implements Comparable{
    public LatLng point;
    public Object attachment;

    public PointAttachedObject (LatLng c, Object attachment) {
        this.point = c;
        this.attachment = attachment;
    }

    public void setAttachment(Object a) {
        this.attachment=a;
    }

    public Object getAttachment() {
        return attachment;
    }

    public float[] getDistanceAndBearingFrom (LatLng location) {
        float[] results = null;
        Location.distanceBetween(location.latitude, location.longitude,
                point.latitude, point.longitude, results);
        //returns distance in meters
        return results;
    }

    @Override
    public int compareTo(Object pao2) {
        Object attachment2 = ((PointAttachedObject)pao2).attachment;
        if (attachment instanceof Note && attachment2 instanceof Note) {
            Note note2 = (Note)((PointAttachedObject)pao2).attachment;
            long id1 = ((Note)attachment).id;
            long id2 = ((Note)attachment2).id;
            long compare=id1-id2;
            if (compare>0) return 1;
            else if (compare <0) return -1;
            else return 0;
        }

        return 0;
    }
}
