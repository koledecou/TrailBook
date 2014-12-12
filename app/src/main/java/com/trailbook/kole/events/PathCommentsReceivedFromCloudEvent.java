package com.trailbook.kole.events;

import com.trailbook.kole.data.TrailBookComment;

import java.util.ArrayList;

/**
 * Created by Fistik on 7/2/2014.
 */
public class PathCommentsReceivedFromCloudEvent {

    ArrayList<TrailBookComment> pathComments;
    public PathCommentsReceivedFromCloudEvent(ArrayList<TrailBookComment> pathComments) {
        this.pathComments=pathComments;
    }

    public ArrayList<TrailBookComment> getPathComments() {
        return pathComments;
    }
}
