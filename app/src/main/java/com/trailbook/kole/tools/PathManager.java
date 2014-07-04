package com.trailbook.kole.tools;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.trailbook.kole.data.Path;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.events.PathSummariesReceivedEvent;
import com.trailbook.kole.events.PathSummaryAddedEvent;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Fistik on 7/3/2014.
 */
public class PathManager {
    private static final PathManager INSTANCE = new PathManager();
    private static HashMap<String,Path> mPaths;
    private static Bus bus;

    private PathManager() {
        mPaths = new HashMap<String, Path>();
        bus = BusProvider.getInstance();
        bus.register(this);
    }

    public static PathManager getInstance() {
        return INSTANCE;
    }

    public static void addPathSummary(PathSummary summary) {
        String id = summary.getId();
        Path path = mPaths.get(id);
        if (path==null) {
            path = new Path(id);
            mPaths.put(id, path);
        }
        bus.post(new PathSummaryAddedEvent(summary));
    }

    @Subscribe
    public void onPathSummariesReceivedEvent(PathSummariesReceivedEvent event){
        ArrayList<PathSummary> summaries = event.getPathSummaries();
        for (PathSummary summary:summaries) {
            //add the path
            addPathSummary(summary);
        }
    }
}
