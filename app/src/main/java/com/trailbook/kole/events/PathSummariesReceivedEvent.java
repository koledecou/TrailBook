package com.trailbook.kole.events;

import com.trailbook.kole.data.PathSummary;

import java.util.ArrayList;

/**
 * Created by Fistik on 7/2/2014.
 */
public class PathSummariesReceivedEvent {

    ArrayList<PathSummary> pathSummaries;
    public PathSummariesReceivedEvent(ArrayList<PathSummary> pathSummaries) {
        this.pathSummaries=pathSummaries;
    }

    public ArrayList<PathSummary> getPathSummaries() {
        return pathSummaries;
    }
}
