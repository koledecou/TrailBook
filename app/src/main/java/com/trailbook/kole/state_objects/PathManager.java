package com.trailbook.kole.state_objects;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.trailbook.kole.data.ButtonActions;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.KeyWord;
import com.trailbook.kole.data.KeyWordDAO;
import com.trailbook.kole.data.KeyWordGroup;
import com.trailbook.kole.data.Path;
import com.trailbook.kole.data.PathGroup;
import com.trailbook.kole.data.PathSegment;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.data.TrailBookComment;
import com.trailbook.kole.events.MapObjectAddedEvent;
import com.trailbook.kole.events.PathDeletedEvent;
import com.trailbook.kole.events.PathReceivedEvent;
import com.trailbook.kole.events.PathSummariesReceivedFromCloudEvent;
import com.trailbook.kole.events.PathSummaryAddedEvent;
import com.trailbook.kole.events.PathUpdatedEvent;
import com.trailbook.kole.events.PointAttachedObjectDeletedEvent;
import com.trailbook.kole.events.SegmentDeletedEvent;
import com.trailbook.kole.events.SegmentUpdatedEvent;
import com.trailbook.kole.helpers.NoteFactory;
import com.trailbook.kole.helpers.TrailBookDirectoryWalker;
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
    private static Hashtable<String,PointAttachedObject> mPointAttachedObjects;
    private static Hashtable<String,PathSegment> mSegments;
    private static Hashtable<String,TrailBookComment> mPathComments;
    private static Hashtable<String, PathGroup> mGroups;
    private static Bus bus;

    private static Gson gson = new Gson();

    private PathManager() {
        mPaths = new Hashtable<String, PathSummary>();
        mSegments = new Hashtable<String, PathSegment>();
        mPathComments = new Hashtable<String, TrailBookComment>();
        mPointAttachedObjects = new Hashtable<String, PointAttachedObject>();
        bus = BusProvider.getInstance();
        bus.register(this);
    }

    public static PathManager getInstance() {
        return INSTANCE;
    }

    public static void addPathSummary(PathSummary summary) {
        String id = summary.getId();
        Log.d(Constants.TRAILBOOK_TAG, "adding path summary,"+id);
        mPaths.put(id, summary);
        hashKeywords(summary);
    }

    public static void hashKeywords(PathSummary summary) {
        Log.d(Constants.TRAILBOOK_TAG, "PathManager: hashing key words for " + summary.getName());
        KeyWordGroup keyWords = summary.getKeyWordGroup();
        KeyWordDAO keyWordDAO = new KeyWordDAO(TrailBookState.getInstance());
        keyWordDAO.open();

        try {
            String pathId = summary.getId();
            String pathName = summary.getName();
            if (keyWords != null && keyWords.climbs != null) {
                for (String keyWord : keyWords.climbs) {
                    Log.d(Constants.TRAILBOOK_TAG, "PathManager: hashing climb key word " + keyWord + " for " + summary.getName());
                    KeyWord climbWord = new KeyWord(KeyWord.CLIMB, keyWord, pathId, pathName);
                    keyWordDAO.insertUpdateKeyWord(climbWord);
                }
            }
            if (keyWords != null && keyWords.crags != null) {
                for (String keyWord : keyWords.crags) {
                    Log.d(Constants.TRAILBOOK_TAG, "PathManager: hashing crag key word " + keyWord + " for " + summary.getName());
                    KeyWord cragWord = new KeyWord(KeyWord.CRAG, keyWord, pathId, pathName);
                    keyWordDAO.insertUpdateKeyWord(cragWord);
                }
            }
            if (keyWords != null && keyWords.regions != null) {
                for (String keyWord : keyWords.regions) {
                    Log.d(Constants.TRAILBOOK_TAG, "PathManager: hashing regions key word " + keyWord + " for " + summary.getName());
                    KeyWord regionWord = new KeyWord(KeyWord.REGION, keyWord, pathId, pathName);
                    keyWordDAO.insertUpdateKeyWord(regionWord);
                }
            }
        }catch (Exception e) {
            Log.e(Constants.TRAILBOOK_TAG, "PathManager: error hashing key words ", e);
        } finally {
            keyWordDAO.close();
        }

    }


/*
    public static void saveKeyWordHash() {
        Log.d(Constants.TRAILBOOK_TAG, "PathManager: saving key word hash");
        String keyHashJSON = TrailbookPathUtilities.getKeyHashJSON(mKeyWordHashes);
        String keyHashFile = TrailbookFileUtilities.getKeyHashFileName();
        try {
            FileUtils.write(new File(keyHashFile), keyHashJSON);
        } catch (IOException e) {
            Log.e(Constants.TRAILBOOK_TAG, "Error saving keys", e);
        }
    }
*/
/*
    public static void loadKeyWordHash() {
        Log.d(Constants.TRAILBOOK_TAG, "PathManager: loading key word hash");
        String keyHashFile = TrailbookFileUtilities.getKeyHashFileName();
        try {
            String fileContents = FileUtils.readFileToString(new File(keyHashFile));
            mKeyWordHashes = gson.fromJson(fileContents, KeyWordHashCollection.class);
        } catch (Exception e) {
            Log.e(Constants.TRAILBOOK_TAG, "Error getting keys", e);
        }
    }*/


    public boolean objectBelongsToPath(String objectId, String pathId) {
        PathSummary summary = getPathSummary(pathId);
        ArrayList<String> objectIdsForPath = summary.getObjectIdList();
        if (objectIdsForPath != null && objectIdsForPath.contains(objectId))
            return true;
        else
            return false;
    }

    @Subscribe
    public void onPathSummariesReceivedEvent(PathSummariesReceivedFromCloudEvent event){
        ArrayList<PathSummary> summaries = event.getPathSummaries();
        for (PathSummary summary:summaries) {
            //only add the path from the cloud if it's not stored locally.
            //todo: get last updated date and let user refresh if it's out of date.
            if (!isStoredLocally(summary.getId())) {
                addPathSummary(summary);
                savePathSummaryToCloudCache(summary);
            } else {
                Log.d(Constants.TRAILBOOK_TAG, "PathManager: path " + summary.getName() + " is local, not adding from cloud.");
            }
        }
    }

    public boolean isStoredLocally(String pathId) {
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

    public void onPathReceived(Path path){
        ArrayList<PointAttachedObject> paObjects = path.paObjects;
        ArrayList<PathSegment> segments = path.segments;
        ArrayList<TrailBookComment> comments = path.comments;

        for (PointAttachedObject pao:paObjects) {
            Log.d(Constants.TRAILBOOK_TAG, "PathManager: received pao " + pao.getAttachment().toString());
            mPointAttachedObjects.put(pao.getId(), pao);
        }

        for (PathSegment segment:segments) {
            Log.d(Constants.TRAILBOOK_TAG, "PathManager: received segment " + segment.getId());
            mSegments.put(segment.getId(), segment);
            saveSegment(segment);
        }

        for (TrailBookComment comment:comments) {
            Log.d(Constants.TRAILBOOK_TAG, "PathManager: recieved comment " + comment.getShortContent());
            mPathComments.put(comment.getId(), comment);
            saveComment(comment);
        }

        savePath(path);
    }

    @Subscribe
    public void onPathReceivedEvent(PathReceivedEvent event){
        Path path = event.getPath();
        postPathReceivedEvents(path);
    }

    private void postPathReceivedEvents(Path path) {
        ArrayList<PointAttachedObject> paObjects = path.paObjects;
        ArrayList<PathSegment> segments = path.segments;

        for (PointAttachedObject pao:paObjects) {
            bus.post(new MapObjectAddedEvent(pao));
        }

        for (PathSegment segment:segments) {
            bus.post(new SegmentUpdatedEvent(segment));
        }
        bus.post(new PathUpdatedEvent(path.summary));
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

    public Path getPath(String pathId) {
        PathSummary summary = getPathSummary(pathId);
        ArrayList<PathSegment> segments = getSegmentsForPath(pathId);
        ArrayList<PointAttachedObject> paObjects = getPointObjectsForPath(pathId);
        Path pathContainer = new Path(summary, segments, paObjects);
        return pathContainer;
    }

    public PathSummary getPathSummary(String pathId) {
        if (pathId == null)
            return null;
        else
            return mPaths.get(pathId);
    }

    public void addPath(Path p) {
        addPathSummary(p.summary);
        addSegments(p.segments);
        addPointObjects(p.paObjects);
    }

    public void savePath(String pathId) {
        try {
            PathSummary summary = getPathSummary(pathId);
            savePathSummaryToLocalPaths(summary);
            saveSegments(pathId);
            savePointObjects(getPointObjectsForPath(pathId));
            bus.post(new PathUpdatedEvent(summary));
        } catch (Exception e) {
            Log.e(Constants.TRAILBOOK_TAG,"Error saving path:" + pathId, e);
        }
    }


    public void savePath(Path pathContainer) {
        PathSummary summary = pathContainer.summary;
        Log.d(Constants.TRAILBOOK_TAG, "Saving path:" + summary.getName() + ", id: " + summary.getId());
        ArrayList<PathSegment> segments = pathContainer.segments;
        ArrayList<PointAttachedObject> paObjects = pathContainer.paObjects;
        try {
            savePathSummaryToLocalPaths(summary);
            saveSegments(segments);
            savePointObjects(paObjects);
        } catch (Exception e) {
            Log.e(Constants.TRAILBOOK_TAG,"Error saving path:" + summary.getId(), e);
        }
    }

    private PointAttachedObject loadPointAttachedObject(String paoId) {
        File paoFile = TrailbookFileUtilities.getInternalPAOFile(paoId);
        PointAttachedObject paObject = null;
        try {
            String paoFileContents = FileUtils.readFileToString(paoFile);
            Log.v(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + " pao file contents:" + paoFileContents);
            paObject = NoteFactory.getPointAttachedObjectFromJSONString(paoFileContents);
/*            Type paoNoteType = new TypeToken<PointAttachedObject<Note>>() {
            }.getType();
            note = gson.fromJson(noteFileContents, paoNoteType);
            note.updateAttachmentType();*/
        } catch (Exception e){
            Log.e(Constants.TRAILBOOK_TAG, "PathManager: error loading note " + paoId, e);
        }
        return paObject;
    }

    private void savePointObjects(ArrayList<PointAttachedObject> paObjects) {
        for (PointAttachedObject paObject:paObjects) {
            Log.d(Constants.TRAILBOOK_TAG, "PathManager: saving note: " + paObject.getAttachment().toString());
            String paoJSON = NoteFactory.getJsonFromPointAttachedObject(paObject);
            File paoFile = TrailbookFileUtilities.getInternalPAOFile(paObject.getId());
            try {
                FileUtils.write(paoFile, paoJSON);
            } catch (IOException e) {
                Log.e(Constants.TRAILBOOK_TAG, "PathManager: error saving note: " + paObject.getId(), e);
            }
        }
    }

    public void saveComment (TrailBookComment comment) {
        Log.d(Constants.TRAILBOOK_TAG, "Saving comment " + comment.getId());
        String commentJsonString = TrailbookPathUtilities.getCommentJsonString(comment);
        File commentsFile = TrailbookFileUtilities.getInternalCommentFile(comment.getPathId(), comment.getId());
        try {
            FileUtils.write(commentsFile, commentJsonString);
        } catch (IOException e) {
            Log.e(Constants.TRAILBOOK_TAG, "Error saving segment points", e);
        }
    }


    private void saveSegment (PathSegment segment) {
        Log.d(Constants.TRAILBOOK_TAG, "Saving segment " + segment.getId());
        saveSegmentPoints(segment);
    }

    public void savePathSummaryToLocalPaths(PathSummary summary) {
        File localPathSummaryFile = TrailbookFileUtilities.getInternalPathSummaryFile(summary.getId());
        savePathSummary(summary, localPathSummaryFile);
    }

    public void savePathSummaryToCloudCache(PathSummary summary) {
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

    public void saveSegments(String pathId) {
        PathSummary summary = getPathSummary(pathId);
        ArrayList<String> segmentIds = summary.getSegmentIdList();
        for (String id:segmentIds) {
            saveSegment(id);
        }
    }

    public void saveSegment(String segmentId) {
        Log.d(Constants.TRAILBOOK_TAG, "Saving segment " + segmentId);
        saveSegmentPoints(getSegment(segmentId));
    }

    public void saveSegmentPoints(PathSegment segment) {
        String segmentPointsString = TrailbookPathUtilities.getSegmentPointsJSONString(segment);
        Log.d(Constants.TRAILBOOK_TAG, "saving points: " + segmentPointsString);
        File segmentPointsFile = TrailbookFileUtilities.getInternalSegmentPointsFile(segment.getId());
        try {
            FileUtils.write(segmentPointsFile, segmentPointsString);
        } catch (IOException e) {
            Log.e(Constants.TRAILBOOK_TAG, "Error saving segment points", e);
        }
    }

    public Path loadPathFromDevice(String pathId) {
        File summaryFile = TrailbookFileUtilities.getInternalPathSummaryFile(pathId);
        String summaryFileContents = null;
        PathSummary summary = null;
        try {
            summaryFileContents = FileUtils.readFileToString(summaryFile);
            summary = getSummaryFromString(summaryFileContents);
            addPathSummary(summary);
        } catch (Exception e) {
            Log.e(Constants.TRAILBOOK_TAG, "PathManager Error: can't load summary file for path " + pathId + " " + summary.getName(), e);
            return null;
        }

        return loadPathFromSummary(summary);
    }

    private PathSummary getSummaryFromString(String summaryFileContents) {
        PathSummary summary = gson.fromJson(summaryFileContents, PathSummary.class);
        Log.d(Constants.TRAILBOOK_TAG, "loaded summary " + summary.getName() + ", " + summary.getId());
        return summary;
    }

    public Path loadPathFromSummary(PathSummary summary) {
        ArrayList<String> segmentIds = summary.getSegmentIdList();
        for (String segId : segmentIds) {
            try {
                PathSegment segment = loadSegment(segId);
                addNewSegment(segment);

            } catch (Exception e) {
                Log.e(Constants.TRAILBOOK_TAG, "Error loading segment id: " + segId, e);
            }
        }

        ArrayList<String> paoIds = summary.getObjectIdList();
        for (String paoId: paoIds) {
            PointAttachedObject pao = loadPointAttachedObject(paoId);
            if (pao != null) {
                Log.d(Constants.TRAILBOOK_TAG, "PathManager: loaded note:" + pao.getAttachment().toString());
                mPointAttachedObjects.put(pao.getId(), pao);

            }
        }

        ArrayList<TrailBookComment> comments = loadCommentsForPath(summary.getId());

        String pathId = summary.getId();
        Path path = new Path(summary, getSegmentsForPath(pathId), getPointObjectsForPath(pathId), comments);
        return path;
    }

    public ArrayList<TrailBookComment> loadCommentsForPath(String pathId) {
        ArrayList<TrailBookComment> comments = new ArrayList<TrailBookComment>();
        String pathCommentDir = TrailbookFileUtilities.getInternalCommentsDirectory(pathId);
        TrailBookDirectoryWalker walker = new TrailBookDirectoryWalker("_comment.tb");

        ArrayList<String> commentsJsonStrings = walker.getFileContentsFromDevice(pathCommentDir);
        for (String thisContent:commentsJsonStrings) {
            try {
                TrailBookComment comment = gson.fromJson(thisContent, TrailBookComment.class);
                comments.add(comment);
                addPathComment(comment);
                Log.d(Constants.TRAILBOOK_TAG, "PathManager: loaded comment: " + comment.getShortContent());
            }catch (Exception e) {
                Log.e(Constants.TRAILBOOK_TAG, "Error loading path.", e);
            }
        }
        return comments;
    }

    public ArrayList<PathSummary> loadSummariesFromDevice(String pathRootDir) {
        ArrayList<PathSummary> summaries = new ArrayList<PathSummary>();
        TrailBookDirectoryWalker walker = new TrailBookDirectoryWalker("_summary.tb");

        ArrayList<String> pathSummaryFileContents = walker.getFileContentsFromDevice(pathRootDir);
        for (String thisContent:pathSummaryFileContents) {
            try {
                PathSummary summary = getSummaryFromString(thisContent);
                Log.d(Constants.TRAILBOOK_TAG, "Note Ids for " + summary.getName() + ":" + summary.getObjectIdList());
                summaries.add(summary);
                mPaths.put(summary.getId(), summary);
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

    private void addPointObjects(ArrayList<PointAttachedObject> paObjects) {
        for (PointAttachedObject pao:paObjects) {
            addPao(pao);
        }
    }

    private void addPao(PointAttachedObject pao) {
        String id = pao.getId();
        if (mPointAttachedObjects != null)
            mPointAttachedObjects.put(id, pao);
        bus.post(new MapObjectAddedEvent(pao));
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

    private void saveSegments(ArrayList<PathSegment> segments) {
        for (PathSegment s:segments) {
            saveSegment(s);
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
        bus.post(new PathSummaryAddedEvent(summary));
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

    public PointAttachedObject getPointAttachedObject(String objectId) {
/*        Collection<PathSegment> segmentColl = mSegments.values();
        for (PathSegment s: segmentColl) {
            HashMap<String,PointAttachedObject<Note>> notes = s.getPointNotes();
            PointAttachedObject<Note> paoNote = notes.get(noteId);
            if (paoNote != null)
                return paoNote;
        }*/
        return mPointAttachedObjects.get(objectId);
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

    public ArrayList<PointAttachedObject> getPointObjectsForPath(String pathId) {
        Log.d(Constants.TRAILBOOK_TAG, "PathManager: getting notes for path " + pathId);
        ArrayList<PointAttachedObject> allObjects = new ArrayList<PointAttachedObject>();
        PathSummary summary = getPathSummary(pathId);
        ArrayList<String> objectIds = summary.getObjectIdList();
        for (String objectId:objectIds) {
            PointAttachedObject thisObject = getPointAttachedObject(objectId);
            allObjects.add(thisObject);
        }
        return allObjects;
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
            deletePointAttachedObjects(pathId, c);
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

    public void deletePointAttachedObjects(String pathId, Context c) {
        ArrayList<PointAttachedObject> pointAttachedObjects = getPointObjectsForPath(pathId);
        if (pointAttachedObjects != null) {
            for (PointAttachedObject pao : pointAttachedObjects) {
                String paoId = pao.getId();
                File paoFile = TrailbookFileUtilities.getInternalPAOFile(paoId);
                Log.d(Constants.TRAILBOOK_TAG, "deleteing note file: " + paoFile);
                try {
                    FileUtils.forceDelete(paoFile);
                } catch (IOException e) {
                    Log.e(Constants.TRAILBOOK_TAG, "Cannot delete note file:" + paoFile, e);
                }
                deleteImages(pao);
            }
        }
    }

    private void deleteImages(PointAttachedObject pao) {
        ArrayList<String> imageFileNames = pao.getAttachment().getImageFileNames();
        if (imageFileNames == null || imageFileNames.size()<1)
            return;

        for (String fileName:imageFileNames) {
            File file = TrailbookFileUtilities.getInternalImageFile(fileName);
            try {
                Log.d(Constants.TRAILBOOK_TAG, "deleteing image file: " + file);
                FileUtils.forceDelete(file);
            } catch (IOException e) {
                Log.e(Constants.TRAILBOOK_TAG, "Cannot delete image file:" + file, e);
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

    public void addPointAttachedObjectToPath(String pathId, PointAttachedObject paObject) {
        PathSummary summary = getPathSummary(pathId);
        Log.d(Constants.TRAILBOOK_TAG, "adding pao: " + paObject.getLocation() + "   " + paObject.getAttachment().toString());
        summary.addPao(paObject.getId());
        mPointAttachedObjects.put(paObject.getId(), paObject);
        bus.post(new MapObjectAddedEvent(paObject));
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

    public void deletePaoFromPath(String pathId, String paoId) {
        //just in case other paths have the same note, don't delete it just remove it from this path.
        PathSummary summary = getPathSummary(pathId);
        summary.removePao(paoId);
        bus.post(new PointAttachedObjectDeletedEvent(paoId));
    }

    public void deletePathFromCloudCache(String pathId) {
        File cachedSummaryDir = new File(TrailbookFileUtilities.getCacheDirectoryForPath(pathId));
        try {
            FileUtils.deleteDirectory(cachedSummaryDir);
        } catch (IOException e) {
            Log.e(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ": error deleting cached path " + pathId, e);
        }
    }

    public ArrayList<String> getPathsWithinBounds(LatLngBounds bounds) {
        ArrayList<String> summariesWithinBounds = new ArrayList<String>();
        for (PathSummary summary:mPaths.values()) {
            if (bounds.contains(summary.getEnd()) ) {
                summariesWithinBounds.add(summary.getId());
            }
        }
        return summariesWithinBounds;
    }

    public PathSegment getMainSegmentForPath(String pathId) {
        ArrayList<PathSegment> segments = getSegmentsForPath(pathId);
        if (segments != null && segments.size()>0)
            return segments.get(segments.size()-1);
        else
            return null;
    }

    public String addNewSegmentToPath(String pathId) {
        String segmentId = makeNewSegment();
        PathSummary summary = getPathSummary(pathId);
        summary.addSegment(segmentId);
        return segmentId;
    }

    public ArrayList<String> getTags(String pathId) {
        //todo: implement tags
        return  new ArrayList<String>();
    }

    public ArrayList<TrailBookComment> getComments(String pathId) {
        ArrayList<TrailBookComment> comments = new ArrayList<TrailBookComment>();
        for (TrailBookComment comment:mPathComments.values()) {
            if (comment.getPathId().equals(pathId))
                comments.add(comment);
        }
        return comments;
    }

    public void addPathComment(TrailBookComment comment) {
        mPathComments.put(comment.getId(), comment);
    }

    public PathGroup getGroup(String groupId) {
        return mGroups.get(groupId);
    }

    public void addGroup(PathGroup group) {
        mGroups.put(group.groupId, group);
    }

    public void removeCloudCache(String pathId) {
        File cacheFile = TrailbookFileUtilities.getCachedPathSummaryFile(pathId);
        try {
            FileUtils.forceDelete(cacheFile);
        } catch (IOException e) {
            Log.e(Constants.TRAILBOOK_TAG, "PathManager: failed to delete cache file:" +cacheFile, e );
        }
    }
}
