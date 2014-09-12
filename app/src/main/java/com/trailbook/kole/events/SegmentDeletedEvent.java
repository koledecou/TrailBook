package com.trailbook.kole.events;

import com.trailbook.kole.data.Path;
import com.trailbook.kole.data.PathSegment;
import com.trailbook.kole.data.PathSummary;

import java.util.ArrayList;

/**
 * Created by Fistik on 7/3/2014.
 */
public class SegmentDeletedEvent {

    PathSegment segment;
    public SegmentDeletedEvent(PathSegment segment) {
        this.segment=segment;
    }

    public PathSegment getSegment() {
        return segment;
    }
}
