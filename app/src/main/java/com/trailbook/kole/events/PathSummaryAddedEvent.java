package com.trailbook.kole.events;

import com.trailbook.kole.data.Path;
import com.trailbook.kole.data.PathSummary;

/**
 * Created by Fistik on 7/5/2014.
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
