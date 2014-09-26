package com.trailbook.kole.state_objects;

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
import com.trailbook.kole.events.MapObjectAddedEvent;
import com.trailbook.kole.events.PathDeletedEvent;
import com.trailbook.kole.events.PathReceivedEvent;
import com.trailbook.kole.events.PathSummariesReceivedEvent;
import com.trailbook.kole.events.PathSummaryAddedEvent;
import com.trailbook.kole.events.PathUpdatedEvent;
import com.trailbook.kole.events.SegmentDeletedEvent;
import com.trailbook.kole.events.SegmentUpdatedEvent;
import com.trailbook.kole.helpers.NoteFactory;
import com.trailbook.kole.helpers.PathDirectoryWalker;
import com.trailbook.kole.helpers.TrailbookFileUtilities;
import com.trailbook.kole.helpers.TrailbookPathUtilities;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

/**
 * Created by Fistik on 7/3/2014.
 */
public class PathManager {
    private static final PathManager INSTANCE = new PathManager();
    private static Hashtable<String,PathSummary> mPaths;
    private static Hashtable<String,PointAttachedObject<Note>> mNotes;
    private static Hashtable<String,PathSegment> mSegments;
    private static Bus bus;

    private Gson gson = new Gson();

    private PathManager() {
        mPaths = new Hashtable<String, PathSummary>();
        mSegments = new Hashtable<String, PathSegment>();
        mNotes = new Hashtable<String, PointAttachedObject<Note>>();
        bus = BusProvider.getInstance();
        bus.register(this);
    }

    public static PathManager getInstance() {
        return INSTANCE;
    }

    public static void addPathSummary(PathSummary summary) {
        String id = summary.getId();
        Log.d(Constants.TRAILBOOK_TAG, "adding path summary,"+id);
/*        Path2 path = mPaths.get(id);
        if (path==null) {
            Log.d(Constants.TRAILBOOK_TAG, "creating new path," + id);
            path = new Path2(id);
        }*/
        mPaths.put(id, summary);
        bus.post(new PathSummaryAddedEvent(summary));
    }

    public boolean noteBelongsToPath(String noteId, String pathId) {
        PathSummary summary = getPathSummary(pathId);
        ArrayList<String> noteIdsForPath = summary.getNoteIdList();
        if (noteIdsForPath != null && noteIdsForPath.contains(noteId))
            return true;
        else
            return false;
    }

    @Subscribe
    public void onPathSummariesReceivedEvent(PathSummariesReceivedEvent event){
        ArrayList<PathSummary> summaries = event.getPathSummaries();
        for (PathSummary summary:summaries) {
            //only add the path from the cloud if it's not stored locally.
            //todo: get last updated date and let user refresh if it's out of date.
            if (!isStoredLocally(summary.getId())) {
                addPathSummary(summary);
                savePathSummaryToTempFolder(summary);
            } else {
                Log.d(Constants.TRAILBOOK_TAG, "PathManager: path " + summary.getName() + " is local, not adding from cloud.");
            }
        }
    }

    private boolean isStoredLocally(String pathId) {
        return isPathInDirectory(pathId, new File(TrailbookFileUtilities.getInternalPathDirectory()));
    }

    public boolean isPathInCloudCache(String pathId) {
        return isPathInDirectory(pathId, new File(TrailbookFileUtilities.getInternalCacheDirectory()));
    }

    private boolean isPathInDirectory(String pathId, File rootPathDir) {
        if (rootPathDir == null)
            return false;

        String[] pathIds = rootPathDir.list();
        if (pathIds == null)
            return false;

        for (int i=0; i<pathIds.length; i++) {
            if (pathId.equals(pathIds[i])) {
                return true;
            }
        }

        return false;
    }

    @Subscribe
    public void onPathReceivedEvent(PathReceivedEvent event){
        Path path = event.getPath();
        ArrayList<PointAttachedObject<Note>> notes = path.notes;
        ArrayList<PathSegment> segments = path.segments;

        for (PointAttachedObject<Note> note:notes) {
            Log.d(Constants.TRAILBOOK_TAG, "PathManager: received note " + note.getAttachment().getNoteContent());
            mNotes.put(note.getId(), note);
            bus.post(new MapObjectAddedEvent(note));
        }

        for (PathSegment segment:segments) {
            Log.d(Constants.TRAILBOOK_TAG, "PathManager: received segment " + segment.getId());
            mSegments.put(segment.getId(), segment);
            saveSegment(segment, TrailBookState.getInstance());
            bus.post(new SegmentUpdatedEvent(segment));
        }

        savePath(path, TrailBookState.getInstance());
    }

    private void addPointsToSegment(ArrayList<LatLng> points, PathSegment thisSegment) {
        for (LatLng point:points) {
            thisSegment.addPoint(point);
        }
        bus.post(new SegmentUpdatedEvent(thisSegment));
    }

/*deleteme    public void addNoteToSegment(PathSegment s, PointAttachedObject<Note> paoNote) {
        Log.d(Constants.TRAILBOOK_TAG, "adding note: " + paoNote.getLocation() + "   " + paoNote.getAttachment().getNoteContent());
        s.addPointNote(paoNote);
        bus.post(new NoteAddedEvent(paoNote));
    }*/

/*deleteme    public Path2 getPath(String pathId) {
        return mPaths != null? mPaths.get(pathId) : null;
    }*/

    public PathSegment getSegment(String segmentId) {
        return mSegments!= null ? mSegments.get(segmentId) : null;
    }

    public PathSummary getPathSummary(String pathId) {
        return mPaths.get(pathId)==null?null:mPaths.get(pathId);
    }

    public void addPath(Path p) {
        addPathSummary(p.summary);
        addSegments(p.segments);
        addNotes(p.notes);
    }

    public void savePath(String pathId, Context c) {
        try {
            PathSummary summary = getPathSummary(pathId);
            savePathSummaryToLocalPaths(summary);
            saveSegments(pathId, c);
            saveNotes(getPointNotesForPath(pathId));
            bus.post(new PathUpdatedEvent(summary));
        } catch (Exception e) {
            Log.e(Constants.TRAILBOOK_TAG,"Error saving path:" + pathId, e);
        }
    }


    public void savePath(Path pathContainer, Context c) {
        PathSummary summary = pathContainer.summary;
        Log.d(Constants.TRAILBOOK_TAG, "Saving path:" + summary.getName() + ", id: " + summary.getId());
        ArrayList<PathSegment> segments = pathContainer.segments;
        ArrayList<PointAttachedObject<Note>> paoNotes = pathContainer.notes;
        try {
            savePathSummaryToLocalPaths(summary);
            saveSegments(segments, c);
            saveNotes(paoNotes);
            bus.post(new PathUpdatedEvent(summary));
        } catch (Exception e) {
            Log.e(Constants.TRAILBOOK_TAG,"Error saving path:" + summary.getId(), e);
        }
    }

    private PointAttachedObject<Note> loadNote(String noteId) {
        File noteFile = TrailbookFileUtilities.getInternalNoteFile(noteId);
        PointAttachedObject note = null;
        try {
            String noteFileContents = FileUtils.readFileToString(noteFile);
            Log.d(Constants.TRAILBOOK_TAG, getClass().getName() + "note file contents:" + noteFileContents);
            note = NoteFactory.getNoteFromJSONString(noteFileContents);
/*            Type paoNoteType = new TypeToken<PointAttachedObject<Note>>() {
            }.getType();
            note = gson.fromJson(noteFileContents, paoNoteType);
            note.updateAttachmentType();*/
        } catch (Exception e){
            Log.e(Constants.TRAILBOOK_TAG, "PathManager: error loading note " + noteId, e);
        }
        return note;
    }

    private void saveNotes(ArrayList<PointAttachedObject<Note>> paoNotes) {
        for (PointAttachedObject<Note> paoNote:paoNotes) {
            Log.d(Constants.TRAILBOOK_TAG, "PathManager: saving note: " + paoNote.getAttachment().getNoteContent());
            String noteJSON = TrailbookPathUtilities.getNoteJSONString(paoNote);
            File noteFile = TrailbookFileUtilities.getInternalNoteFile(paoNote.getId());
            try {
                FileUtils.write(noteFile, noteJSON);
            } catch (IOException e) {
                Log.e(Constants.TRAILBOOK_TAG, "PathManager: error saving note: " + paoNote.getId(), e);
            }
        }
    }

    private void saveSegment (PathSegment segment, Context c) {
        Log.d(Constants.TRAILBOOK_TAG, "Saving segment " + segment.getId());
        saveSegmentPoints(segment, c);
    }

    public void savePathSummaryToLocalPaths(PathSummary summary) {
        File localPathSummaryFile = TrailbookFileUtilities.getInternalPathSummaryFile(summary.getId());
        savePathSummary(summary, localPathSummaryFile);
    }

    public void savePathSummaryToTempFolder(PathSummary summary) {
        File tempPathSummaryFile = TrailbookFileUtilities.getCachedPathSummaryFile(summary.getId());
        savePathSummary(summary, tempPathSummaryFile);
    }

    public void savePathSummary(PathSummary summary,  File pathSummaryFile) {
        String pathSummaryJSON = TrailbookPathUtilities.getPathSummaryJSONString(summary);
        Log.d(Constants.TRAILBOOK_TAG, "saving summary: " + pathSummaryJSON);

        try {
            FileUtils.write(pathSummaryFile, pathSummaryJSON);
        } catch (IOException e) {
            Log.e(Constants.TRAILBOOK_TAG, "Error saving path " + summary.getId(), e);
        }
    }

/*deleteme    public void savePathSummary (String pathId, Context c) {
        Log.d(Constants.TRAILBOOK_TAG, "Saving " + pathId);
        Path2 summary = getPathSummary(pathId);
        if (summary == null)
            return;

        savePathSummary(summary, c);
    }*/

    public void saveSegments(String pathId, Context c) {
        PathSummary summary = getPathSummary(pathId);
        ArrayList<String> segmentIds = summary.getSegmentIdList();
        for (String id:segmentIds) {
            saveSegment(id, c);
        }
    }

    public void saveSegment(String segmentId, Context c) {
        Log.d(Constants.TRAILBOOK_TAG, "Saving segment " + segmentId);
        saveSegmentPoints(getSegment(segmentId), c);
    }

    public void saveSegmentPoints(PathSegment segment, Context c) {
        String segmentPointsString = TrailbookPathUtilities.getSegmentPointsJSONString(segment);
        Log.d(Constants.TRAILBOOK_TAG, "saving points: " + segmentPointsString);
        File segmentPointsFile = TrailbookFileUtilities.getInternalSegmentPointsFile(segment.getId());
        try {
            FileUtils.write(segmentPointsFile, segmentPointsString);
        } catch (IOException e) {
            Log.e(Constants.TRAILBOOK_TAG, "Error saving segment points", e);
        }
    }

    public void loadPathFromDevice(Context c, String pathId) {
        File summaryFile = TrailbookFileUtilities.getInternalPathSummaryFile(pathId);
        String summaryFileContents = null;
        PathSummary summary = null;
        try {
            summaryFileContents = FileUtils.readFileToString(summaryFile);
            summary = getSummaryFromString(summaryFileContents);
        } catch (Exception e) {
            Log.e(Constants.TRAILBOOK_TAG, "PathManager Error: can't load summary file for path " + pathId);
            return;
        }
        bus.post(new PathSummaryAddedEvent(summary));
        loadPathFromSummary(summary);
    }

    private PathSummary getSummaryFromString(String summaryFileContents) {
        PathSummary summary = gson.fromJson(summaryFileContents, PathSummary.class);
        Log.d(Constants.TRAILBOOK_TAG, "loaded summary " + summary.getName() + ", " + summary.getId());
        return summary;
    }

    private void loadPathFromSummary(PathSummary summary) {
        ArrayList<String> segmentIds = summary.getSegmentIdList();
        for (String segId : segmentIds) {
            try {
                PathSegment segment = loadSegment(segId);
                addNewSegment(segment);
                bus.post(new SegmentUpdatedEvent(segment));
            } catch (Exception e) {
                Log.e(Constants.TRAILBOOK_TAG, "Error loading segment id: " + segId, e);
            }
        }

        ArrayList<String> noteIds = summary.getNoteIdList();
        for (String noteId: noteIds) {
            PointAttachedObject<Note> paoNote = loadNote(noteId);
            if (paoNote != null) {
                Log.d(Constants.TRAILBOOK_TAG, "PathManager: loaded note:" + paoNote.getAttachment().getNoteContent());
                mNotes.put(paoNote.getId(), paoNote);
                bus.post(new MapObjectAddedEvent(paoNote));
            }
        }
//deleteme        p.setDownloaded(true);
        addPathSummary(summary);
    }

    public void loadPathsFromDevice() {
        ArrayList<PathSummary> summaries = loadSummariesFromDevice(TrailbookFileUtilities.getInternalPathDirectory());
        for (PathSummary summary:summaries) {
            bus.post(new PathSummaryAddedEvent(summary));
            loadPathFromSummary(summary);
        }
    }

    public void loadCachedPathSummaries() {
        ArrayList<PathSummary> summaries = loadSummariesFromDevice(TrailbookFileUtilities.getInternalCacheDirectory());
        for (PathSummary summary:summaries) {
            bus.post(new PathSummaryAddedEvent(summary));
        }
    }

    public ArrayList<PathSummary> loadSummariesFromDevice(String pathRootDir) {
        ArrayList<PathSummary> summaries = new ArrayList<PathSummary>();
        PathDirectoryWalker walker = new PathDirectoryWalker("_summary.tb");

        ArrayList<String> pathSummaryFileContents = walker.getPathFileContentsFromDevice(pathRootDir);
        for (String thisContent:pathSummaryFileContents) {
            try {
                PathSummary summary = getSummaryFromString(thisContent);
                Log.d(Constants.TRAILBOOK_TAG, "Note Ids for " + summary.getName() + ":" + summary.getNoteIdList());
                summaries.add(summary);
            } catch (Exception e) {
                Log.e(Constants.TRAILBOOK_TAG, "Error loading path.", e);
            }
        }
        return summaries;
    }

    private void addNewSegment(PathSegment segment) {
        String id = segment.getId();
        if (mSegments.get(id) != null)
            mSegments.remove(id);

        mSegments.put(id, segment);
    }

    public PathSegment loadSegment(String segId) {
        try {
            ArrayList<LatLng> points = loadPoints(segId);
            PathSegment segment = new PathSegment(segId);
            segment.addPoints(points);
            return segment;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private ArrayList<LatLng> loadPoints(String segId) throws IOException {
        File segmentPointsFile = TrailbookFileUtilities.getInternalSegmentPointsFile(segId);
        String pointsFileContents = FileUtils.readFileToString(segmentPointsFile);
        Type arrayLatLngType = new TypeToken<ArrayList<LatLng>>(){}.getType();
        ArrayList<LatLng> points = gson.fromJson(pointsFileContents, arrayLatLngType);
        return points;
    }

    private void addNotes(ArrayList<PointAttachedObject<Note>> notes) {
        for (PointAttachedObject<Note> note:notes) {
            addNote(note);
        }
    }

    private void addNote(PointAttachedObject<Note> note) {
        String id = note.getId();
        if (mNotes != null)
            mNotes.put(id, note);
        bus.post(new MapObjectAddedEvent(note));
    }

    private void addSegments(ArrayList<PathSegment> segments) {
        for (PathSegment s:segments) {
            addSegment(s);
        }
    }

    private void addSegment(PathSegment s) {
        String id = s.getId();
        if (mSegments.get(id) ==  null)
            mSegments.put(id, s);
        bus.post(new SegmentUpdatedEvent(s));
    }

    private void saveSegments(ArrayList<PathSegment> segments, Context c) {
        for (PathSegment s:segments) {
            saveSegment(s, c);
        }
    }

    public ButtonActions getButtonActions(String pathId) {
        ButtonActions actions = new ButtonActions();

        PathSummary p = getPathSummary(pathId);
        if (!isStoredLocally(pathId)) {
            actions.mCanFollowPath = false;
            actions.mCanDownloadPath = true;
        } else {
            actions.mCanFollowPath = true;
            actions.mCanDownloadPath = false;
        }

        return actions;
    }

    public String makeNewPath(String pathName, String segmentId, String ownerId) {
        String pathId = TrailbookPathUtilities.getNewPathId();
        PathSummary summary = new PathSummary(pathId);
        summary.setName(pathName);
        summary.setDescription("");

        summary.addSegment(segmentId);

        addPathSummary(summary);
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
        updatePathSummaryStartAndEndPoints(point, getPathSummary(pathId));

        bus.post(new SegmentUpdatedEvent(s));
    }

    private void updatePathSummaryStartAndEndPoints(LatLng point, PathSummary summary) {
//deleteme        PathSummary summary = path.getSummary();
        summary.setEnd(point);
        if (summary.getStart() == null) {
            summary.setStart(point);
        }
    }

    public PointAttachedObject<Note> getNote(String noteId) {
/*        Collection<PathSegment> segmentColl = mSegments.values();
        for (PathSegment s: segmentColl) {
            HashMap<String,PointAttachedObject<Note>> notes = s.getPointNotes();
            PointAttachedObject<Note> paoNote = notes.get(noteId);
            if (paoNote != null)
                return paoNote;
        }*/
        return mNotes.get(noteId);
    }

    public boolean hasDownloadedPaths() {
        ArrayList<String> downloadedPathIds = new ArrayList<String>();
        for (PathSummary p:mPaths.values()) {
            if (isStoredLocally(p.getId()))
                return true;
        }

        return false;
    }

    public ArrayList<String> getDownloadedPathIds() {
        ArrayList<String> downloadedPathIds = new ArrayList<String>();
        for (PathSummary p:mPaths.values()) {
            if (isStoredLocally(p.getId()))
                downloadedPathIds.add(p.getId());
        }
        return downloadedPathIds;
    }

    public ArrayList<PathSummary> getDownloadedPathSummaries() {
        ArrayList<PathSummary> downloadedPathSummaries = new ArrayList<PathSummary>();
        for (PathSummary p:mPaths.values()) {
            if (isStoredLocally(p.getId()))
                downloadedPathSummaries.add(p);
        }
        return downloadedPathSummaries;
    }

    public ArrayList<PathSegment> getSegmentsForPath(String pathId) {
        PathSummary p = getPathSummary(pathId);
        if (p == null)
            return null;

        ArrayList<PathSegment> segments = new ArrayList<PathSegment>();
        ArrayList<String> segIds = p.getSegmentIdList();
        if (segIds == null || segIds.size()<1) {
            Log.d(Constants.TRAILBOOK_TAG, "PathManager: no segments in path");
            return null;
        }

        for (String segId:segIds) {
            PathSegment s = getSegment(segId);
            if (s != null)
                segments.add(s);
        }

        return segments;
    }

    public ArrayList<PointAttachedObject<Note>> getPointNotesForPath(String pathId) {
        Log.d(Constants.TRAILBOOK_TAG, "PathManager: getting notes for path " + pathId);
        ArrayList<PointAttachedObject<Note>> allNotes = new ArrayList<PointAttachedObject<Note>>();
        PathSummary summary = getPathSummary(pathId);
        ArrayList<String> noteIds = summary.getNoteIdList();
        for (String noteId:noteIds) {
            PointAttachedObject<Note> thisNote = getNote(noteId);
            allNotes.add(thisNote);
        }
        return allNotes;
    }

    public void addSegmentIfNeeded(String segmentId) {
        PathSegment s = mSegments.get(segmentId);
        if (s == null) {
            s = new PathSegment(segmentId);
            mSegments.put(segmentId,s);
        }
    }

    public LatLng getStartCoordsForPath(String pathId) {
        PathSummary summary = getPathSummary(pathId);
        if (summary == null)
            return null;

        return summary.getStart();
    }

    public void deletePath(String pathId, Context c) {
        try {
            deleteSegments(pathId, c);
            deletePaths(pathId, c);
        } catch (Exception e) {
            Log.e(Constants.TRAILBOOK_TAG,"Error saving path:" + pathId, e);
        }
    }

    public void deleteSegments(String pathId, Context c) {
        PathSummary p = getPathSummary(pathId);
        ArrayList<String> segmentIds = p.getSegmentIdList();
        for (String segmentId:segmentIds) {
            if (!isSegmentUsedByPathsOtherThan(pathId, segmentId)) {
                PathSegment segment = getSegment(segmentId);
                if (segment == null) {
                    Log.e(Constants.TRAILBOOK_TAG, "Can't find segment to delete:" + segmentId);
                    continue;
                }

                deleteSegment(segmentId, c);
                bus.post(new SegmentDeletedEvent(segment));
                mSegments.remove(segmentId);
            }
        }
    }

    private boolean isSegmentUsedByPathsOtherThan(String pathId, String segmentId) {
        ArrayList<String> pathIds = getPathsContainingSegment(segmentId);
        pathIds.remove(pathId);
        if (pathIds.size()>0)
            return true;
        else
            return false;
    }

    private ArrayList<String> getPathsContainingSegment(String segmentId) {
        ArrayList<String> pathsWithSegment = new ArrayList<String>();
        for (PathSummary p:mPaths.values()) {
            if (p.getSegmentIdList().contains(segmentId))
                pathsWithSegment.add(p.getId());
        }
        return pathsWithSegment;
    }

    public void deleteSegment(String segmentId, Context c) {
        Log.d(Constants.TRAILBOOK_TAG, "delete segment " + segmentId);
        File segmentDir = new File(TrailbookFileUtilities.getInternalSegmentDirectory(segmentId));
        try {
            FileUtils.deleteDirectory(segmentDir);
            Log.d(Constants.TRAILBOOK_TAG, "Deleted Segment:" + segmentDir);
        } catch (IOException e) {
            Log.e(Constants.TRAILBOOK_TAG, "Cannot delete segment:" + segmentId, e);
        }
    }

    public void deletePaths (String pathId, Context c) {
        String pathDir = TrailbookFileUtilities.getInternalPathDirectory(pathId);
        try {
            FileUtils.deleteDirectory(new File(pathDir));
            Log.d(Constants.TRAILBOOK_TAG, "Deleted Path:" + pathDir);
        } catch (IOException e) {
            Log.e(Constants.TRAILBOOK_TAG, "Cannot delete Path:" + pathId, e);
        }
        bus.post(new PathDeletedEvent(getPathSummary(pathId)));
        mPaths.remove(pathId);
    }

    public void addNoteToPath(String pathId, PointAttachedObject<Note> paoNote) {
        PathSummary summary = getPathSummary(pathId);
        Log.d(Constants.TRAILBOOK_TAG, "adding note: " + paoNote.getLocation() + "   " + paoNote.getAttachment().getNoteContent());
        summary.addNote(paoNote.getId());
        mNotes.put(paoNote.getId(), paoNote);
        bus.post(new MapObjectAddedEvent(paoNote));
    }

    public Collection<PathSummary> getAllSummaries() {
        return mPaths.values();
    }

    public boolean doesSummaryWithNameAlreadyExist(String name) {
        Collection<PathSummary> summaries = getAllSummaries();
        for (PathSummary summary:summaries) {
            String compareName = summary.getName();
            if (name.equalsIgnoreCase(compareName))
                return true;
        }
        return false;
    }
}
