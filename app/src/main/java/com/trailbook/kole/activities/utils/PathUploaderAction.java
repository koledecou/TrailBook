package com.trailbook.kole.activities.utils;

import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.state_objects.TrailBookState;
import com.trailbook.kole.worker_fragments.WorkerFragment;

/**
 * Created by kole on 10/5/2014.
 */
public class PathUploaderAction implements Action {
    public WorkerFragment workerFragment;
    public PathSummary summary;

    public PathUploaderAction(WorkerFragment workerFragment, PathSummary summary) {
        this.workerFragment = workerFragment;
        this.summary = summary;
    }

    public void execute() {
        String userId = TrailBookState.getInstance().getCurrentUser().userId;
        summary.setOwnerID(userId);
        workerFragment.startPathUploadMongo(summary);
    }
}
