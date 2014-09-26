package com.trailbook.kole.services.database;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.util.JSON;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.Note;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.data.Path;
import com.trailbook.kole.data.PathSegment;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.helpers.TrailbookPathUtilities;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

/**
 * Created by kole on 9/19/2014.
 */
public class TrailbookRemoteDatabase {
    private static TrailbookRemoteDatabase INSTANCE = new TrailbookRemoteDatabase();
    private static MongoClient mongoClient;
    private static DB db;
    Gson gson = new Gson();

    private TrailbookRemoteDatabase() {
    }

    public static TrailbookRemoteDatabase getInstance() {
        return INSTANCE;
    }

    private boolean connect() {
        if (mongoClient == null || !isConnected()) {
            try {
                MongoCredential credential = MongoCredential.createMongoCRCredential(DBConstants.username, DBConstants.database, DBConstants.password);
                mongoClient = new MongoClient(new ServerAddress(DBConstants.serverAddress, DBConstants.port), Arrays.asList(credential));
                db = mongoClient.getDB(DBConstants.database);
                Log.d(Constants.TRAILBOOK_TAG, "Mongo: connected to " + db.toString());
                return true;
            } catch (Exception e) {
                Log.e(Constants.TRAILBOOK_TAG, "Mongo: error connecting to trailbook db", e);
                return false;
            }
        } else {
            Log.d(Constants.TRAILBOOK_TAG, "Mongo: already connected");
            return true;
        }
    }

    private boolean isConnected() {
        try {
            Set<String> names = db.getCollectionNames();
            if (names == null || names.size()<1) {
                Log.e(Constants.TRAILBOOK_TAG, "Mongo: can't connect to db ");
                return false;
            } else {
                Log.d(Constants.TRAILBOOK_TAG, "Mongo: already connected");
                return true;
            }
        } catch (Exception e) {
            Log.e(Constants.TRAILBOOK_TAG, "Mongo: can't connect to db ", e);
            return false;
        }
    }

    public Path getPath(String pathId) {
        if (!connect())
            return null;

        DBCollection pathsCollection = db.getCollection(DBConstants.pathCollectionName);
        DBObject getPathSummaryQuery = new BasicDBObject("_id", pathId);
        DBObject pathObject = pathsCollection.findOne(getPathSummaryQuery);
        PathSummary pathSummary = getPathFromDBObject(pathObject);
        ArrayList<PathSegment> segments = getSegments(pathSummary.getSegmentIdList());
        ArrayList<PointAttachedObject<Note>> paoNotes = getNotes(pathSummary.getNoteIdList());
        return new Path(pathSummary, segments, paoNotes);
    }

    private ArrayList<PointAttachedObject<Note>> getNotes(ArrayList<String> noteIdList) {
        ArrayList<PointAttachedObject<Note>> paoNotes = new ArrayList<PointAttachedObject<Note>>();
        for (String noteId:noteIdList) {
            PointAttachedObject<Note> paoNote = getNote(noteId);
            if (paoNote != null)
                paoNotes.add(paoNote);
        }
        return paoNotes;
    }

    private PointAttachedObject<Note> getNote(String noteId) {
        if (!connect())
            return null;

        DBCollection notesCollection = db.getCollection(DBConstants.noteCollectionName);
        DBObject getNoteQuery = new BasicDBObject("_id", noteId);
        DBObject noteObject = notesCollection.findOne(getNoteQuery);
        return getPAONoteFromDBObject(noteObject);
    }

    private ArrayList<PathSegment> getSegments(ArrayList<String> segmentIdList) {
        ArrayList<PathSegment> segments = new ArrayList<PathSegment>();
        for (String segmentId:segmentIdList) {
            PathSegment segment = getSegment(segmentId);
            if (segment != null)
                segments.add(segment);
        }
        return segments;
    }

    private PathSegment getSegment(String segmentId) {
        if (!connect())
            return null;

        DBCollection segmentsCollection = db.getCollection(DBConstants.segmentCollectionName);
        DBObject getSegmentQuery = new BasicDBObject("_id", segmentId);
        DBObject segmentObject = segmentsCollection.findOne(getSegmentQuery);
        return getSegmentFromDBObject(segmentObject);
    }

    public ArrayList<PathSummary> getAllPaths() {
        if (!connect())
            return null;

        ArrayList<PathSummary> paths = new ArrayList<PathSummary>();
        DBCollection pathsCollection = db.getCollection(DBConstants.pathCollectionName);
        DBCursor pathsCursor = pathsCollection.find();
        Log.d(Constants.TRAILBOOK_TAG, "Mongo: number of paths is " + pathsCollection.getCount());
        try {
            while (pathsCursor.hasNext()) {
                DBObject pathObject = pathsCursor.next();
                Log.d(Constants.TRAILBOOK_TAG, "Mongo: got path " + pathObject);
                PathSummary path = getPathFromDBObject(pathObject);
                paths.add(path);
                Log.d(Constants.TRAILBOOK_TAG, "Mongo: segmentd for " + path.getName() + ":" + path.getSegmentIdList());
            }
        } finally {
            pathsCursor.close();
        }
        return paths;
    }

    public boolean uploadPathContainer(Path pathContainer) {
        if (!connect())
            return false;
        try {
            uploadPath(pathContainer.summary);
            uploadSegments(pathContainer.segments);
            uploadNotes(pathContainer.notes);
            return true;
        } catch (Exception e) {
            Log.e(Constants.TRAILBOOK_TAG, "Mongo: failed to insert/update path " + pathContainer.summary.getName(), e);
            return false;
        }
    }

    public void uploadPath(PathSummary summary) {
        if (!connect())
            throw new ConnectFailedException();

        String jsonPath = TrailbookPathUtilities.getPathSummaryJSONString(summary);
        DBCollection pathsCollection = db.getCollection(DBConstants.pathCollectionName);
        Log.d(Constants.TRAILBOOK_TAG, "Mongo: inserting path: " + jsonPath);
        DBObject updatedPathObject = (DBObject) JSON.parse(jsonPath);
        DBObject existingPathQuery = new BasicDBObject("_id", summary.getId());
        pathsCollection.update(existingPathQuery, updatedPathObject, true, false, WriteConcern.ACKNOWLEDGED);
    }

    public void uploadSegment(PathSegment segment) {
        if (!connect())
            throw new ConnectFailedException();

        String jsonSegment = TrailbookPathUtilities.getSegmentJSONString(segment);
        DBCollection pathsCollection = db.getCollection(DBConstants.segmentCollectionName);
        Log.d(Constants.TRAILBOOK_TAG, "Mongo: inserting segment: " + jsonSegment);
        DBObject updateSegmentObject = (DBObject) JSON.parse(jsonSegment);
        DBObject existingSegmentQuery = new BasicDBObject("_id", segment.getId());
        pathsCollection.update(existingSegmentQuery, updateSegmentObject, true, false, WriteConcern.ACKNOWLEDGED);
    }

    private void uploadSegments(ArrayList<PathSegment> segments) {
        Log.d(Constants.TRAILBOOK_TAG, "Mongo:" + segments);
        for (PathSegment segment:segments) {
            uploadSegment(segment);
        }
    }

    private void uploadNote(PointAttachedObject<Note> note) {
        if (!connect())
            throw new ConnectFailedException();

        String jsonNote = TrailbookPathUtilities.getNoteJSONString(note);
        DBCollection pathsCollection = db.getCollection(DBConstants.noteCollectionName);
        Log.d(Constants.TRAILBOOK_TAG, "Mongo: inserting note: " + jsonNote);
        DBObject updateNoteObject = (DBObject) JSON.parse(jsonNote);
        DBObject existingNoteQuery = new BasicDBObject("_id", note.getId());
        pathsCollection.update(existingNoteQuery, updateNoteObject, true, false, WriteConcern.ACKNOWLEDGED);
    }

    private void uploadNotes(ArrayList<PointAttachedObject<Note>> notes) {
        for (PointAttachedObject<Note> note:notes) {
            uploadNote(note);
        }
    }

    private PathSummary getPathFromDBObject(DBObject pathObject) {
        try {
            PathSummary path = gson.fromJson(pathObject.toString(), PathSummary.class);
            Log.d(Constants.TRAILBOOK_TAG, "Mongo: got path:" + path.getName());
            Log.d(Constants.TRAILBOOK_TAG, "Mongo: path segments:" + path.getSegmentIdList());
            Log.d(Constants.TRAILBOOK_TAG, "Mongo: path notes:" + path.getNoteIdList());
            return path;
        } catch (Exception e) {
            Log.e(Constants.TRAILBOOK_TAG, "Mongo: exception getting path", e);
            return null;
        }
    }

    private PathSegment getSegmentFromDBObject(DBObject segmentObject) {
        try {
            PathSegment segment = gson.fromJson(segmentObject.toString(), PathSegment.class);
            Log.d(Constants.TRAILBOOK_TAG, "Mongo: got segment:" + segment);
            Log.d(Constants.TRAILBOOK_TAG, "Mongo: segment points:" + segment.getPoints());
            return segment;
        } catch (Exception e) {
            Log.e(Constants.TRAILBOOK_TAG, "Mongo: exception getting segment", e);
            return null;
        }
    }

    private PointAttachedObject<Note> getPAONoteFromDBObject(DBObject paoNoteObject) {
        try {
            Type poaNoteType = new TypeToken<PointAttachedObject<Note>>(){}.getType();
            PointAttachedObject<Note> paoNote = gson.fromJson(paoNoteObject.toString(), poaNoteType);
            Log.d(Constants.TRAILBOOK_TAG, "Mongo: got paoNote:" + paoNote.getLocation() + " content:" + paoNote.getAttachment().getNoteContent() + " image: " + paoNote.getAttachment().getImageFileName());
            return paoNote;
        } catch (Exception e) {
            Log.e(Constants.TRAILBOOK_TAG, "Mongo: exception getting paoNote", e);
            return null;
        }
    }

    private class ConnectFailedException extends RuntimeException {
    }
}
