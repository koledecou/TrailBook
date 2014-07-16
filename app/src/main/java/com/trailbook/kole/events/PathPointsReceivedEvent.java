package com.trailbook.kole.events;

import com.google.android.gms.maps.model.LatLng;
import com.trailbook.kole.data.PathSummary;

import java.util.ArrayList;

/**
 * Created by Fistik on 7/5/2014.
 */
public class PathPointsReceivedEvent {
    public class PathIDWithPoints {
        String pathId;
        ArrayList<LatLng> points;

        public ArrayList<LatLng> getPoints() {
            return points;
        }
        public String getPathId() {
            return pathId;
        }
    }

    PathIDWithPoints result;
    public PathPointsReceivedEvent(PathIDWithPoints pathIdWithPoints) {
        result = pathIdWithPoints;
    }

    public ArrayList<LatLng> getPathPoints() {
        return result.getPoints();
    }
    public String getPathId() {
        return result.getPathId();
    }
}
