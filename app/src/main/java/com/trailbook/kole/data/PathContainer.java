package com.trailbook.kole.data;

import java.util.ArrayList;

/**
 * Created by kole on 9/11/2014.
 */
public class PathContainer {
    public Path path;
    public ArrayList<PathSegment> segments;

    public PathContainer(Path path, ArrayList<PathSegment> segments) {
        this.segments = segments;
        this.path = path;
    }
}
