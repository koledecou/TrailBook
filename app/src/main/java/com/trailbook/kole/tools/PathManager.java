package com.trailbook.kole.tools;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.Note;
import com.trailbook.kole.data.Path;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.events.AllNotesAddedEvent;
import com.trailbook.kole.events.NoteAddedEvent;
import com.trailbook.kole.events.NotesReceivedEvent;
import com.trailbook.kole.events.PathPointsReceivedEvent;
import com.trailbook.kole.events.PathSummariesReceivedEvent;
import com.trailbook.kole.events.PathSummaryAddedEvent;
import com.trailbook.kole.events.PathUpdatedEvent;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Created by Fistik on 7/3/2014.
 */
public class PathManager {
    private static final PathManager INSTANCE = new PathManager();
    private static Hashtable<String,Path> mPaths;
    private static Bus bus;

    private PathManager() {
        mPaths = new Hashtable<String, Path>();
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
            path.setSummary(summary);
            mPaths.put(id, path);
            bus.post(new PathSummaryAddedEvent(summary));
        }
        bus.post(new PathUpdatedEvent(path));
    }

    @Subscribe
    public void onPathSummariesReceivedEvent(PathSummariesReceivedEvent event){
        ArrayList<PathSummary> summaries = event.getPathSummaries();
        for (PathSummary summary:summaries) {
            //add the path
            addPathSummary(summary);
        }
    }

    @Subscribe
    public void onPathPointsReceivedEvent(PathPointsReceivedEvent event){
        String pathId = event.getPathId();
        ArrayList<LatLng> points = event.getPathPoints();
        Path thisPath = mPaths.get(pathId);
        if (thisPath != null) {
            for (LatLng point:points) {
                thisPath.addPoint(point);
            }
            bus.post(new PathUpdatedEvent(thisPath));
        }
    }

    @Subscribe
    public void onNotesReceivedEvent(NotesReceivedEvent event){
        String pathId = event.getPathId();
        ArrayList<PointAttachedObject<Note>> pointNotes = event.getPointNotes();
        Path thisPath = mPaths.get(pathId);
        if (thisPath != null) {
            for (PointAttachedObject<Note> paoNote:pointNotes) {
                thisPath.addPointNote(paoNote);
                bus.post(new NoteAddedEvent(paoNote));
            }
        }
        bus.post(new AllNotesAddedEvent(pointNotes, pathId));
    }

    public Path getPath(String pathId) {
        return mPaths.get(pathId);
    }

    public PathSummary getPathSummary(String pathId) {
        return mPaths.get(pathId)==null?null:mPaths.get(pathId).getSummary();

    }

    public void savePath(String pathId, Context c) {
        Log.d(Constants.TRAILBOOK_TAG, "Saving " + pathId);
        Path path = getPath(pathId);
        String pathString = getPathString(path);
        File pathFile = TrailbookFileUtilities.getInternalPathFile(c, pathId);
        try {
            FileUtils.write(pathFile, pathString);
            path.setDownloaded(true);
        } catch (IOException e) {
            Log.e(Constants.TRAILBOOK_TAG, "Error saving path", e);
        }
    }

    private String getPathString(Path path) {
        Gson gson = new GsonBuilder().excludeFieldsWithModifiers().create();
        return gson.toJson(path);
    }
}
