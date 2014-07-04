package com.trailbook.kole.events;

import com.trailbook.kole.data.PathSummary;

import java.util.ArrayList;

/**
 * Created by Fistik on 7/3/2014.
 */
public class PathSummaryAddedEvent {

    PathSummary pathSummary;
    public PathSummaryAddedEvent(PathSummary pathSummary) {
        this.pathSummary=pathSummary;
    }

    public PathSummary getPathSummary() {
        return pathSummary;
    }
}
