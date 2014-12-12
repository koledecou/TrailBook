package com.trailbook.kole.events;

import com.trailbook.kole.data.TrailBookComment;

/**
 * Created by Fistik on 7/2/2014.
 */
public class PathCommentAddedEvent {

    TrailBookComment comment;
    public PathCommentAddedEvent(TrailBookComment comment) {
        this.comment = comment;
    }

    public TrailBookComment getComment() {
        return comment;
    }
}
