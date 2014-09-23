package com.trailbook.kole.events;

import com.trailbook.kole.data.PathSummary;

/**
 * Created by Fistik on 7/3/2014.
 */
public class PathUpdatedEvent {

    PathSummary summary;
    public PathUpdatedEvent(PathSummary summary) {
        this.summary=summary;
    }

    public PathSummary getPath() {
        return summary;
    }
}
