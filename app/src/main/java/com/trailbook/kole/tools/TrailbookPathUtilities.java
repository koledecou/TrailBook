package com.trailbook.kole.tools;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.trailbook.kole.data.Path;
import com.trailbook.kole.data.PathSegment;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by kole on 7/19/2014.
 */
public class TrailbookPathUtilities {
    private static String newNoteId;

    public static double getNearestDistanceFromPointToPath(LatLng currentLocation, Path path) {
        double minDist = Double.MAX_VALUE;
        ArrayList<PathSegment> segments = PathManager.getInstance().getSegmentsForPath(path);
        for (PathSegment s: segments) {
            for (LatLng thisPathPoint : s.getPoints()) {
                float thisDist = getDistanceInMeters(currentLocation, thisPathPoint);
                if (thisDist < minDist)
                    minDist = thisDist;
            }
        }

        return minDist;
    }

    public static float getDistanceInMeters(LatLng p1, LatLng p2) {
        float[] results = new float[5];
        Location.distanceBetween(p1.latitude, p1.longitude, p2.latitude, p2.longitude, results);
        return results[0];
    }

    public static LatLng locationToLatLon(Location l) {
        return new LatLng(l.getLatitude(), l.getLongitude());
    }

    public static String getNewNoteId() {
        Date date = new Date();
        return String.valueOf(date.getTime());
    }


    public static String getNewPathId() {
        Date date = new Date();
        return String.valueOf(date.getTime());
    }

    public static String getNewSegmentId() {
        Date date = new Date();
        return String.valueOf(date.getTime());
    }

    public static String getPathSummaryJSONString(Path path) {
        Gson gson = new GsonBuilder().setExclusionStrategies(new PathExclusionStrategy()).create();
        return gson.toJson(path.getSummary());
    }

    public static String getSegmentListJSONString(Path path) {
        Gson gson = new GsonBuilder().setExclusionStrategies(new PathExclusionStrategy()).create();
        return gson.toJson(path.getSegmentIdList());
    }

    public static String getSegmentNotesJSONString(PathSegment segment) {
        Gson gson = new GsonBuilder().setExclusionStrategies(new PathExclusionStrategy()).create();
        return gson.toJson(segment.getPointNotes());
    }

    public static String getSegmentPointsJSONString(PathSegment segment) {
        Gson gson = new GsonBuilder().setExclusionStrategies(new PathExclusionStrategy()).create();
        return gson.toJson(segment.getPoints());
    }

    public static String getPathJSONString(Path path) {
        Gson gson = new GsonBuilder().setExclusionStrategies(new PathExclusionStrategy()).create();
        return gson.toJson(path);
    }

    public static String getSegmentJSONString(PathSegment s) {
        Gson gson = new GsonBuilder().setExclusionStrategies(new PathExclusionStrategy()).create();
        return gson.toJson(s);
    }

    public static String getPathSegmentMapJSONString(Path p) {
        Gson gson = new GsonBuilder().setExclusionStrategies(new PathExclusionStrategy()).create();
        return gson.toJson(p.getSegmentIdList());
    }
}
