package com.trailbook.kole.data;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.trailbook.kole.data.geo.GeoLineString;
import com.trailbook.kole.helpers.TrailbookPathUtilities;

import java.util.ArrayList;

/**
 * Created by kole on 8/18/2014.
 */
public class PathSegment {
    String _id;
    GeoLineString points;

    public PathSegment(String id) {
        points = new GeoLineString();
        this._id = id;
    }

    public void addPoint(LatLng newPoint) {
        //only add it if it's far enough away from the last one.
        // If it's too far away it's a bad point.
        if (points.size() < 1){
            points.add(newPoint);
        }else{
            LatLng last = points.getLast();
            float delta = TrailbookPathUtilities.getDistanceInMeters(last, newPoint);
            if (delta > 200) {
                Log.d("trailbook", "segmentId: " + _id + " dist: " + String.valueOf(delta) + " index: " + points.size());
            }
            if (delta>Constants.MIN_DISTANCE_BETWEEN_POINTS && delta < Constants.MAX_DISTANCE_BETWEEN_POINTS)
                points.add(newPoint);
        }
    }

    public void addPoints(ArrayList<LatLng> newPoints) {
        points.addAll(newPoints);
    }

    public ArrayList<LatLng> getPoints() {
        return points.toLatLngArray();
    }

    public String getId() {
        return _id;
    }

    public void removePoints() {
        points = new GeoLineString();
    }

    public void setPoints(ArrayList<LatLng> newPoints) {
        removePoints();
        points.addAll(newPoints);
    }

    public void deletePoint(LatLng point) {
        points.deletePoint(point);
    }

    public void movePoint(LatLng oldLoc, LatLng newLoc) {
        points.movePoint(oldLoc, newLoc);
    }
}
