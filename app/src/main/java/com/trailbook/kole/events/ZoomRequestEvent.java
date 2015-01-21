package com.trailbook.kole.events;

/**
 * Created by kole on 1/16/2015.
 */
public class ZoomRequestEvent {
    private final String mPathId;

    public ZoomRequestEvent(String pathId) {
        mPathId = pathId;
    }

    public String getPathId() {
        return mPathId;
    }
}
