package com.trailbook.kole.activities.utils;

import com.trailbook.kole.data.TrailBookComment;
import com.trailbook.kole.data.User;
import com.trailbook.kole.state_objects.TrailBookState;
import com.trailbook.kole.worker_fragments.WorkerFragment;

/**
 * Created by kole on 10/5/2014.
 */
public class CommentUploaderAction implements Action {
    public WorkerFragment workerFragment;
    public TrailBookComment comment;

    public CommentUploaderAction(WorkerFragment workerFragment, TrailBookComment comment) {
        this.workerFragment = workerFragment;
        this.comment = comment;
    }

    public void execute() {
        User user = TrailBookState.getInstance().getCurrentUser();
        comment.user = user;
        workerFragment.startCommentUploadMongo(comment);
    }
}
