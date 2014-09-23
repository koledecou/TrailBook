package com.trailbook.kole.events;

import com.trailbook.kole.data.Path;

/**
 * Created by Fistik on 7/5/2014.
 */
public class PathReceivedEvent {

    Path result;
    public PathReceivedEvent(Path path) {
        result = path;
    }

    public Path getPath() {
        return result;
    }
}
