package com.trailbook.kole.events;

import com.google.android.gms.maps.model.LatLng;
import com.trailbook.kole.data.PathSummary;

import java.util.ArrayList;

/**
 * Created by Fistik on 7/5/2014.
 */
public class SegmentPointsReceivedEvent {
    public class SegmentIDWithPoints {
        String segmentId;
        ArrayList<LatLng> points;

        public ArrayList<LatLng> getPoints() {
            return points;
        }
        public String getSegmentId() {
            return segmentId;
        }
    }

    SegmentIDWithPoints result;
    public SegmentPointsReceivedEvent(SegmentIDWithPoints segmentIDWithPoints) {
        result = segmentIDWithPoints;
    }

    public ArrayList<LatLng> getPoints() {
        return result.getPoints();
    }
    public String getSegmentId() {
        return result.getSegmentId();
    }
}
