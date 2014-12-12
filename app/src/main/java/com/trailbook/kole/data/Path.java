package com.trailbook.kole.data;

import java.util.ArrayList;

/**
 * Created by kole on 9/11/2014.
 */
public class Path {
    public PathSummary summary;
    public ArrayList<PathSegment> segments;
    public ArrayList<PointAttachedObject> paObjects;
    public ArrayList<TrailBookComment> comments;

    public Path(PathSummary summary, ArrayList<PathSegment> segments, ArrayList<PointAttachedObject> paObjects, ArrayList<TrailBookComment> comments) {
        this.segments = segments;
        this.summary = summary;
        this.paObjects = paObjects;
        this.comments = comments;
    }

    public Path(PathSummary summary, ArrayList<PathSegment> segments, ArrayList<PointAttachedObject> paObjects) {
        this.segments = segments;
        this.summary = summary;
        this.paObjects = paObjects;
        this.comments = null;
    }
}
