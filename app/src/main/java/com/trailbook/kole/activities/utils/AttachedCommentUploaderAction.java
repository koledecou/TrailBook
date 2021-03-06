package com.trailbook.kole.activities.utils;

import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.data.TrailBookComment;
import com.trailbook.kole.data.User;
import com.trailbook.kole.state_objects.TrailBookState;
import com.trailbook.kole.worker_fragments.WorkerFragment;

/**
 * Created by kole on 10/5/2014.
 */
public class AttachedCommentUploaderAction implements Action {
    public WorkerFragment workerFragment;
    public PointAttachedObject paoComment;

    public AttachedCommentUploaderAction(WorkerFragment workerFragment, PointAttachedObject paoComment) {
        this.workerFragment = workerFragment;
        this.paoComment = paoComment;
    }

    public void execute() {
        User user = TrailBookState.getInstance().getCurrentUser();
        TrailBookComment comment = (TrailBookComment)paoComment.getAttachment();
        comment.user = user;
        workerFragment.startAttachedCommentUploadMongo(paoComment);
    }
}
