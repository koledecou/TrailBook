package com.trailbook.kole.events;

/**
 * Created by kole on 12/17/2014.
 */
public class PathDetailRequestEvent {
    private String mPathId;
    public PathDetailRequestEvent(String pathId) {
        this.mPathId = pathId;
    }

    public String getPathId() {
        return mPathId;
    }
}
