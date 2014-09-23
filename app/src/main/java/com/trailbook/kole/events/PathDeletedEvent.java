package com.trailbook.kole.events;

import com.trailbook.kole.data.PathSummary;

/**
 * Created by kole on 9/5/2014.
 */
public class PathDeletedEvent {
    PathSummary path;
    public PathDeletedEvent(PathSummary path) {
        this.path=path;
    }

    public PathSummary getPath() {
        return path;
    }
}
