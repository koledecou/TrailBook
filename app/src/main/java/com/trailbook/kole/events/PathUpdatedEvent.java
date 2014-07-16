package com.trailbook.kole.events;

import com.trailbook.kole.data.Path;
import com.trailbook.kole.data.PathSummary;

import java.util.ArrayList;

/**
 * Created by Fistik on 7/3/2014.
 */
public class PathUpdatedEvent {

    Path path;
    public PathUpdatedEvent(Path path) {
        this.path=path;
    }

    public Path getPath() {
        return path;
    }
}
