package com.trailbook.kole.data;

import java.util.ArrayList;

/**
 * Created by kole on 9/11/2014.
 */
public class Path {
    public PathSummary summary;
    public ArrayList<PathSegment> segments;
    public ArrayList<PointAttachedObject<Note>> notes;
    public ArrayList<PointAttachedObject<Climb>> climbs;

    public Path(PathSummary summary, ArrayList<PathSegment> segments, ArrayList<PointAttachedObject<Note>> notes) {
        this.segments = segments;
        this.summary = summary;
        this.notes = notes;
    }
}
