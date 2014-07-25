package com.trailbook.kole.tools;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.trailbook.kole.data.ButtonActions;
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
import java.util.Collection;
import java.util.HashMap;
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
                addNoteToPath(thisPath, paoNote);
            }
        }
        bus.post(new AllNotesAddedEvent(pointNotes, pathId));
    }

    public void addNoteToPath(Path p, PointAttachedObject<Note> paoNote) {
        Log.d(Constants.TRAILBOOK_TAG, "adding note: " + paoNote.getLocation() + "   " + paoNote.attachment.getNoteContent());
        p.addPointNote(paoNote);
        bus.post(new NoteAddedEvent(paoNote));
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
        String pathString = TrailbookPathUtilities.getPathJSONString(path);
        File pathFile = TrailbookFileUtilities.getInternalPathFile(c, pathId);
        try {
            FileUtils.write(pathFile, pathString);
            path.setDownloaded(true);
            bus.post(new PathUpdatedEvent(path));
        } catch (IOException e) {
            Log.e(Constants.TRAILBOOK_TAG, "Error saving path", e);
        }
    }

    public void loadPathsFromDevice(Activity activity) {
        PathDirectoryWalker walker = new PathDirectoryWalker();
        String pathRootDir = TrailbookFileUtilities.getInternalPathDirectory(activity);
        ArrayList<String> pathFileContents = walker.getPathFileContentsFromDevice(pathRootDir);
        for (String thisContent:pathFileContents) {
            loadFile(thisContent);
        }
    }

    private void loadFile(String content) {
        Gson gson = new Gson();
        Path p = gson.fromJson(content, Path.class);
        p.setDownloaded(true);
        Log.d(Constants.TRAILBOOK_TAG, p.getSummary().getName() + ", " + p.getId());
        addPath(p);
    }

    public void addPath(Path p) {
        String id = p.getSummary().getId();
        if (mPaths.get(id)==null)
            mPaths.put(id, p);
        bus.post(new PathUpdatedEvent(p));
    }

    public ButtonActions getButtonActions(String pathId) {
        ButtonActions actions = new ButtonActions();

        Path p = getPath(pathId);
        if (!p.isDownloaded()) {
            actions.mCanFollowPath = false;
            actions.mCanDownloadPath = true;
        } else {
            actions.mCanFollowPath = true;
            actions.mCanDownloadPath = false;
        }

        return actions;
    }

    public String makeNewPath(String pathName) {
        String pathId = TrailbookPathUtilities.getNewPathId();
        Path p = new Path(pathId);
        PathSummary summary = p.getSummary();
        summary.setName(pathName);
        summary.setDescription("");

        addPath(p);
        return pathId;
    }

    public void addPointToPath(String mPathId, Location newLocation) {
        Path p = getPath(mPathId);
        PathSummary summary = p.getSummary();

        LatLng point = new LatLng(newLocation.getLatitude(), newLocation.getLongitude());
        p.addPoint(point);
        p.setStartAndEnd();
        bus.post(new PathUpdatedEvent(p));
    }

    public PointAttachedObject<Note> getNote(String noteId) {
        Collection<Path> pathColl = mPaths.values();
        for (Path p: pathColl) {
            HashMap<String,PointAttachedObject<Note>> notes = p.getPointNotes();
            PointAttachedObject<Note> paoNote = notes.get(noteId);
            if (paoNote != null)
                return paoNote;
        }
        return null;
    }

    public ArrayList<PathSummary> getDownloadedPathSummaries() {
        ArrayList<PathSummary> downloadedPathSummaries = new ArrayList<PathSummary>();
        for (Path p:mPaths.values()) {
            if (p.isDownloaded())
                downloadedPathSummaries.add(p.getSummary());
        }
        return downloadedPathSummaries;
    }
}
