package com.trailbook.kole.tools;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.trailbook.kole.data.ButtonActions;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.Note;
import com.trailbook.kole.data.Path;
import com.trailbook.kole.data.PathSegment;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.events.AllNotesAddedEvent;
import com.trailbook.kole.events.NoteAddedEvent;
import com.trailbook.kole.events.NotesReceivedEvent;
import com.trailbook.kole.events.PathSummariesReceivedEvent;
import com.trailbook.kole.events.PathSummaryAddedEvent;
import com.trailbook.kole.events.PathUpdatedEvent;
import com.trailbook.kole.events.SegmentPointsReceivedEvent;
import com.trailbook.kole.events.SegmentUpdatedEvent;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * Created by Fistik on 7/3/2014.
 */
public class PathManager {
    private static final PathManager INSTANCE = new PathManager();
    private static Hashtable<String,Path> mPaths;
    private static Hashtable<String,PathSegment> mSegments;
    private static Bus bus;

    private Gson gson = new Gson();

    private PathManager() {
        mPaths = new Hashtable<String, Path>();
        mSegments = new Hashtable<String, PathSegment>();
        bus = BusProvider.getInstance();
        bus.register(this);
    }

    public static PathManager getInstance() {
        return INSTANCE;
    }

    public static void addPathSummary(PathSummary summary) {
        String id = summary.getId();
        Log.d(Constants.TRAILBOOK_TAG, "adding path summary,"+id);
        Path path = mPaths.get(id);
        if (path==null) {
            Log.d(Constants.TRAILBOOK_TAG, "creating new path," + id);
            path = new Path(id);
        }
        path.setSummary(summary);
        mPaths.put(id, path);
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

    @Subscribe
    public void onSegmentPointsReceivedEvent(SegmentPointsReceivedEvent event){
        String segmentId = event.getSegmentId();
        ArrayList<LatLng> points = event.getPoints();
        PathSegment thisSegment = mSegments.get(segmentId);
        if (thisSegment != null) {
            thisSegment.removePoints();
        } else {
            addSegmentIfNeeded(segmentId);
        }
        addPointsToSegment(points, thisSegment);
    }

    private void addPointsToSegment(ArrayList<LatLng> points, PathSegment thisSegment) {
        for (LatLng point:points) {
            thisSegment.addPoint(point);
        }
        bus.post(new SegmentUpdatedEvent(thisSegment));
    }

    public void addNoteToSegment(PathSegment s, PointAttachedObject<Note> paoNote) {
        Log.d(Constants.TRAILBOOK_TAG, "adding note: " + paoNote.getLocation() + "   " + paoNote.attachment.getNoteContent());
        s.addPointNote(paoNote);
        bus.post(new NoteAddedEvent(paoNote));
    }

    public Path getPath(String pathId) {
        return mPaths.get(pathId);
    }

    public PathSegment getSegment(String segmentId) {
        return mSegments.get(segmentId);
    }

    public PathSummary getPathSummary(String pathId) {
        return mPaths.get(pathId)==null?null:mPaths.get(pathId).getSummary();
    }

    public void savePath(String pathId, Context c) {
        savePathSummary(pathId,c);
        savePathSegmentMap(pathId, c);
        saveSegments(pathId, c);
        bus.post(new PathUpdatedEvent(getPath(pathId)));
    }

    public void savePathSegmentMap(String pathId, Context c) {
        Path path = getPath(pathId);
        String segmentListJSON = TrailbookPathUtilities.getSegmentListJSONString(path);
        File pathSegmentsFile = TrailbookFileUtilities.getInternalPathSegmentListFile(c, pathId);
        try {
            FileUtils.write(pathSegmentsFile, segmentListJSON);
        } catch (IOException e) {
            Log.e(Constants.TRAILBOOK_TAG, "Error saving path " + pathId, e);
        }
    }

    public void savePathSummary (String pathId, Context c) {
        Log.d(Constants.TRAILBOOK_TAG, "Saving " + pathId);
        Path path = getPath(pathId);
        String pathSummaryJSON = TrailbookPathUtilities.getPathSummaryJSONString(path);
        File pathSummaryFile = TrailbookFileUtilities.getInternalPathSummaryFile(c, pathId);

        try {
            FileUtils.write(pathSummaryFile, pathSummaryJSON);
        } catch (IOException e) {
            Log.e(Constants.TRAILBOOK_TAG, "Error saving path " + pathId, e);
        }
        path.setDownloaded(true);
    }

    public void saveSegments(String pathId, Context c) {
        Path p = getPath(pathId);
        ArrayList<String> segmentIds = p.getSegmentIdList();
        for (String id:segmentIds) {
            saveSegment(id, c);
        }
    }

    public void saveSegment(String segmentId, Context c) {
        Log.d(Constants.TRAILBOOK_TAG, "Saving segment " + segmentId);
        saveSegmentPoints(segmentId, c);
        saveSegmentNotes(segmentId, c);
    }

    public void saveSegmentPoints(String segmentId, Context c) {
        PathSegment s = getSegment(segmentId);
        String segmentPointsString = TrailbookPathUtilities.getSegmentPointsJSONString(s);
        File segmentPointsFile = TrailbookFileUtilities.getInternalSegmentPointsFile(c, s.getId());
        try {
            FileUtils.write(segmentPointsFile, segmentPointsString);
        } catch (IOException e) {
            Log.e(Constants.TRAILBOOK_TAG, "Error saving segment points", e);
        }
    }

    public void saveSegmentNotes(String segmentId, Context c) {
        PathSegment s = getSegment(segmentId);
        String segmentNotesString = TrailbookPathUtilities.getSegmentNotesJSONString(s);
        File segmentNotesFile = TrailbookFileUtilities.getInternalSegmentNotesFile(c, s.getId());
        try {
            FileUtils.write(segmentNotesFile, segmentNotesString);
        } catch (IOException e) {
            Log.e(Constants.TRAILBOOK_TAG, "Error saving segment notes", e);
        }
    }

    public void loadPathsFromDevice(Activity activity) {
        PathDirectoryWalker walker = new PathDirectoryWalker("_summary.tb");
        String pathRootDir = TrailbookFileUtilities.getInternalPathDirectory(activity);
        ArrayList<String> pathSummaryFileContents = walker.getPathFileContentsFromDevice(pathRootDir);
        for (String thisContent:pathSummaryFileContents) {
            PathSummary summary = gson.fromJson(thisContent, PathSummary.class);
            Log.d(Constants.TRAILBOOK_TAG, "loaded summary " + summary.getName() + ", " + summary.getId());

            Path p = new Path(summary.getId());
            p.setSummary(summary);
            ArrayList<String> segmentIds = loadSegmentIdsForPath(activity, summary.getId());
            for (String segId:segmentIds) {
                PathSegment segment = loadSegment(activity, segId);
                p.addSegment(segId);
                addNewSegment(segment);
                bus.post(new SegmentUpdatedEvent(segment));
            }
            p.setDownloaded(true);
            addPath(p);
        }
    }

    private void addNewSegment(PathSegment segment) {
        String id = segment.getId();
        if (mSegments.get(id) != null)
            mSegments.remove(id);

        mSegments.put(id, segment);
    }

    private PathSegment loadSegment(Context c, String segId) {
        try {
            ArrayList<LatLng> points = loadPoints(c, segId);
            HashMap<String, PointAttachedObject<Note>> notes = loadNotes(c, segId);
            PathSegment segment = new PathSegment(segId);
            segment.addPoints(points);
            segment.setNotes(notes);
            return segment;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private ArrayList<LatLng> loadPoints(Context c, String segId) throws IOException {
        File segmentPointsFile = TrailbookFileUtilities.getInternalSegmentPointsFile(c, segId);
        String pointsFileContents = FileUtils.readFileToString(segmentPointsFile);
        Type arrayLatLngType = new TypeToken<ArrayList<LatLng>>(){}.getType();
        ArrayList<LatLng> points = gson.fromJson(pointsFileContents, arrayLatLngType);
        return points;
    }

    private HashMap<String, PointAttachedObject<Note>> loadNotes(Context c, String segId) throws IOException {
        File segmentNotesFile = TrailbookFileUtilities.getInternalSegmentNotesFile(c, segId);
        String notesFileContents = FileUtils.readFileToString(segmentNotesFile);
        Type mapType = new TypeToken<HashMap<String, PointAttachedObject<Note>>>(){}.getType();
        HashMap<String, PointAttachedObject<Note>> notes = gson.fromJson(notesFileContents, mapType);
        return notes;
    }

    private ArrayList<String> loadSegmentIdsForPath(Context c, String pathId) {
        try {
            File pathSegmentListFile = TrailbookFileUtilities.getInternalPathSegmentListFile(c, pathId);
            String fileContents = FileUtils.readFileToString(pathSegmentListFile);
            Type arrayStringType = new TypeToken<ArrayList<String>>(){}.getType();
            ArrayList<String> segIdList =gson.fromJson(fileContents, arrayStringType);
            return segIdList;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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

    public String makeNewPath(String pathName, String segmentId) {
        String pathId = TrailbookPathUtilities.getNewPathId();
        Path p = new Path(pathId);
        PathSummary summary = p.getSummary();
        summary.setName(pathName);
        summary.setDescription("");

        p.addSegment(segmentId);

        addPath(p);
        return pathId;
    }

    public String makeNewSegment() {
        String segmentId = TrailbookPathUtilities.getNewSegmentId();
        addSegmentIfNeeded(segmentId);
        return segmentId;
    }

    public void addPointToSegment(String segmentId, String pathId, Location newLocation) {
        PathSegment s = getSegment(segmentId);

        LatLng point = new LatLng(newLocation.getLatitude(), newLocation.getLongitude());
        s.addPoint(point);
        updatePathSummaryStartAndEndPoints(point, getPath(pathId));

        bus.post(new SegmentUpdatedEvent(s));
    }

    private void updatePathSummaryStartAndEndPoints(LatLng point, Path path) {
        PathSummary summary = path.getSummary();
        summary.setEnd(point);
        if (path.getSummary().getStart() == null) {
            summary.setStart(point);
        }
    }

    public PointAttachedObject<Note> getNote(String noteId) {
        Collection<PathSegment> segmentColl = mSegments.values();
        for (PathSegment s: segmentColl) {
            HashMap<String,PointAttachedObject<Note>> notes = s.getPointNotes();
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

    public ArrayList<PathSegment> getSegmentsForPath(Path p) {
        ArrayList<PathSegment> segments = new ArrayList<PathSegment>();
        ArrayList<String> segIds = p.getSegmentIdList();
        for (String segId:segIds) {
            PathSegment s = getSegment(segId);
            segments.add(s);
        }

        return segments;
    }

    public HashMap<String, PointAttachedObject<Note>> getPointNotesForPath(String pathId) {
        HashMap<String, PointAttachedObject<Note>> allNotes = new HashMap<String, PointAttachedObject<Note>>();
        ArrayList<PathSegment> segments = getSegmentsForPath(getPath(pathId));
        for (PathSegment s:segments) {
            HashMap<String, PointAttachedObject<Note>> notes = s.getPointNotes();
            allNotes.putAll(notes);
        }
        return allNotes;
    }

    public void setSegmentIdsForPath(String pathId, ArrayList<String> segmentIds) {
        Path p = getPath(pathId);
        p.setSegmentIdList(segmentIds);
    }

    public void setSegmentPoints(String segmentId, ArrayList<LatLng> points) {
        addSegmentIfNeeded(segmentId);
        PathSegment s = mSegments.get(segmentId);
        s.setPoints(points);

        bus.post(new SegmentUpdatedEvent(s));
    }

    public void setSegmentNotes(String segmentId, HashMap<String,PointAttachedObject<Note>> notes) {
        addSegmentIfNeeded(segmentId);
        PathSegment s = mSegments.get(segmentId);
        s.setNotes(notes);
        bus.post(new SegmentUpdatedEvent(s));
    }

    public void addSegmentIfNeeded(String segmentId) {
        PathSegment s = mSegments.get(segmentId);
        if (s == null) {
            s = new PathSegment(segmentId);
            mSegments.put(segmentId,s);
        }
    }
}
