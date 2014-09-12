package com.trailbook.kole.events;

import com.trailbook.kole.data.Path;

/**
 * Created by kole on 9/5/2014.
 */
public class PathDeletedEvent {
    Path path;
    public PathDeletedEvent(Path path) {
        this.path=path;
    }

    public Path getPath() {
        return path;
    }
}
