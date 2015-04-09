package com.trailbook.kole.services.database;

import com.google.gson.Gson;
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
import com.trailbook.kole.data.Path;
import com.trailbook.kole.data.PathSegment;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.data.TrailBookComment;
import com.trailbook.kole.helpers.NoteFactory;
import com.trailbook.kole.helpers.TrailbookFileUtilities;
import com.trailbook.kole.helpers.TrailbookPathUtilities;
import com.trailbook.kole.state_objects.TrailBookState;

import java.io.InputStream;
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
                populateDBConstantsFromRemoteServer();
                if (DBConstants.database == null || DBConstants.username == null || DBConstants.password == null)
                    return false;

                MongoCredential credential = MongoCredential.createMongoCRCredential(DBConstants.username, DBConstants.database, DBConstants.password);
                mongoClient = new MongoClient(new ServerAddress(DBConstants.serverAddress, DBConstants.port), Arrays.asList(credential));
                db = mongoClient.getDB(DBConstants.database);
                return true;
            } catch (Exception e) {
                return false;
            }
        } else {
            return true;
        }
    }

    private boolean isConnected() {
        try {
            Set<String> names = db.getCollectionNames();
            if (names == null || names.size()<1) {
                return false;
            } else {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private static void populateDBConstantsFromRemoteServer() {
        try {
            InputStream in = new java.net.URL(Constants.dbConnectConfigUrl).openStream();
            String jsonDbConstants = TrailbookFileUtilities.convertStreamToString(in);
            DBConstants.populateFromJsonString(jsonDbConstants);
        } catch (Exception e) {
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
        ArrayList<PointAttachedObject> paoObjects = getPointAttachedObjects(pathSummary.getObjectIdList());
        ArrayList<TrailBookComment> comments = getCommentsForPath(pathId);
        return new Path(pathSummary, segments, paoObjects, comments);
    }

    private ArrayList<PointAttachedObject> getPointAttachedObjects(ArrayList<String> paoIdList) {
        ArrayList<PointAttachedObject> paoObjects = new ArrayList<PointAttachedObject>();
        for (String paoId:paoIdList) {
            PointAttachedObject pao = getPointAttachedObject(paoId);
            if (pao != null)
                paoObjects.add(pao);
        }
        return paoObjects;
    }

    private PointAttachedObject getPointAttachedObject(String paoId) {
        if (!connect())
            return null;

        DBCollection paoCollection = db.getCollection(DBConstants.noteCollectionName);
        DBObject getPaoQuery = new BasicDBObject("_id", paoId);
        DBObject paObject = paoCollection.findOne(getPaoQuery);
        return getPointAttachedObjectFromDBObject(paObject);
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
        return getPathSummaries(null);
    }

    public ArrayList<PathSummary> getNewPathSummaries(ArrayList<String> pathsInCloudCache) {
        long lastRefreshedTime = TrailBookState.getLastRefreshedFromCloudTimeStamp();
        BasicDBObject query = new BasicDBObject("lastUpdatedTimestamp", new BasicDBObject("$gt", lastRefreshedTime))
                                        .append("_id", new BasicDBObject("$in", pathsInCloudCache));

        ArrayList<PathSummary> summaries = getPathSummaries(query);
        return summaries;
    }

    public ArrayList<PathSummary> getPathSummaries(DBObject query) {
        if (!connect())
            return null;

        ArrayList<PathSummary> paths = new ArrayList<PathSummary>();
        DBCollection pathsCollection = db.getCollection(DBConstants.pathCollectionName);
        DBCursor pathsCursor = null;
        if (query != null)
            pathsCursor = pathsCollection.find(query);
        else
            pathsCursor = pathsCollection.find();

        try {
            while (pathsCursor.hasNext()) {
                DBObject pathObject = pathsCursor.next();
                PathSummary path = null;
                try {
                    path = getPathFromDBObject(pathObject);
                    paths.add(path);
                } catch (Exception e) {
                }
            }
        } finally {
            pathsCursor.close();
        }
        return paths;
    }

    public ArrayList<PathSummary> getAllPathSummariesExcluding(ArrayList<String> excludeList) {
        if (excludeList == null)
            excludeList = new ArrayList<String>();

        BasicDBObject query = new BasicDBObject("_id", new BasicDBObject("$nin", excludeList));
        ArrayList<PathSummary> summaries = getPathSummaries(query);
        return summaries;
    }

    public boolean uploadPathContainer(Path pathContainer) {
        if (!connect())
            return false;
        try {
            uploadPath(pathContainer.summary);
            uploadSegments(pathContainer.segments);
            uploadNotes(pathContainer.paObjects);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void uploadPath(PathSummary summary) {
        if (!connect())
            throw new ConnectFailedException();

        String jsonPath = TrailbookPathUtilities.getPathSummaryJSONString(summary);
        DBCollection pathsCollection = db.getCollection(DBConstants.pathCollectionName);
        DBObject updatedPathObject = (DBObject) JSON.parse(jsonPath);
        DBObject existingPathQuery = new BasicDBObject("_id", summary.getId());
        pathsCollection.update(existingPathQuery, updatedPathObject, true, false, WriteConcern.ACKNOWLEDGED);
    }

    public void uploadSegment(PathSegment segment) {
        if (!connect())
            throw new ConnectFailedException();

        String jsonSegment = TrailbookPathUtilities.getSegmentJSONString(segment);
        DBCollection pathsCollection = db.getCollection(DBConstants.segmentCollectionName);
        DBObject updateSegmentObject = (DBObject) JSON.parse(jsonSegment);
        DBObject existingSegmentQuery = new BasicDBObject("_id", segment.getId());
        pathsCollection.update(existingSegmentQuery, updateSegmentObject, true, false, WriteConcern.ACKNOWLEDGED);
    }

    private void uploadSegments(ArrayList<PathSegment> segments) {
        for (PathSegment segment:segments) {
            uploadSegment(segment);
        }
    }

    private void uploadNote(PointAttachedObject note) {
        if (!connect())
            throw new ConnectFailedException();

        String jsonNote = NoteFactory.getJsonFromPointAttachedObject(note);
        DBCollection pathsCollection = db.getCollection(DBConstants.noteCollectionName);
        DBObject updateNoteObject = (DBObject) JSON.parse(jsonNote);
        DBObject existingNoteQuery = new BasicDBObject("_id", note.getId());
        pathsCollection.update(existingNoteQuery, updateNoteObject, true, false, WriteConcern.ACKNOWLEDGED);
    }

    private void uploadNotes(ArrayList<PointAttachedObject> notes) {
        for (PointAttachedObject note:notes) {
            uploadNote(note);
        }
    }

    private TrailBookComment getCommentFromDBObject(DBObject commentObject) {
        TrailBookComment comment = gson.fromJson(commentObject.toString(), TrailBookComment.class);
        return comment;
    }

    private PathSummary getPathFromDBObject(DBObject pathObject) {
        PathSummary path = gson.fromJson(pathObject.toString(), PathSummary.class);
        return path;
    }

    private PathSegment getSegmentFromDBObject(DBObject segmentObject) {
        PathSegment segment = gson.fromJson(segmentObject.toString(), PathSegment.class);
        return segment;
    }

    private PointAttachedObject getPointAttachedObjectFromDBObject(DBObject paoNoteObject) {
        PointAttachedObject paoNote = NoteFactory.getPointAttachedObjectFromJSONString(paoNoteObject.toString());
        return paoNote;
    }

    public void cloudDeletePath(PathSummary summary) {
        connect();
        deleteNotes(summary.getObjectIdList());
        deleteSegments(summary.getSegmentIdList());
        deletePath(summary.getId());
    }

    private void deleteNotes(ArrayList<String> objectIdList) {
        for (String paoId:objectIdList) {
            DBCollection paoCollection = db.getCollection(DBConstants.noteCollectionName);
            DBObject getPAOQuery = new BasicDBObject("_id", paoId);
            paoCollection.remove(getPAOQuery);
        }
    }

    private void deleteSegments(ArrayList<String> segmentIdList) {
        for (String segmentId:segmentIdList) {
            DBCollection paoCollection = db.getCollection(DBConstants.segmentCollectionName);
            DBObject getSegmentQuery = new BasicDBObject("_id", segmentId);
            paoCollection.remove(getSegmentQuery);
        }
    }

    private void deletePath(String pathId) {
        DBCollection paoCollection = db.getCollection(DBConstants.pathCollectionName);
        DBObject getPathQuery = new BasicDBObject("_id", pathId);
        paoCollection.remove(getPathQuery);
    }

    public void uploadComment(TrailBookComment comment) {
        if (!connect())
            throw new ConnectFailedException();

        String jsonComment = TrailbookPathUtilities.getCommentJsonString(comment);
        DBCollection commentsCollection = db.getCollection(DBConstants.generalCommentsCollectionName);
        DBObject updateCommentObject = (DBObject) JSON.parse(jsonComment);
        DBObject existingCommentQuery = new BasicDBObject("_id", comment.getId());
        commentsCollection.update(existingCommentQuery, updateCommentObject, true, false, WriteConcern.ACKNOWLEDGED);
    }

    public void uploadAttachedComment(PointAttachedObject paoComment) {
        if (!connect())
            throw new ConnectFailedException();

        String jsonComment = NoteFactory.getJsonFromPointAttachedObject(paoComment);
        DBCollection pathsCollection = db.getCollection(DBConstants.attachedCommentsCollectionName);
        DBObject updateCommentObject = (DBObject) JSON.parse(jsonComment);
        DBObject existingCommentQuery = new BasicDBObject("_id", paoComment.getId());
        pathsCollection.update(existingCommentQuery, updateCommentObject, true, false, WriteConcern.ACKNOWLEDGED);
    }

    public ArrayList<TrailBookComment> getCommentsForPath(String pathId) {
        if (!connect())
            return null;

        ArrayList<TrailBookComment> comments = new ArrayList<TrailBookComment>();
        DBCollection generalCommentsCollection = db.getCollection(DBConstants.generalCommentsCollectionName);
        DBObject getCommentsQuery = new BasicDBObject("pathId", pathId);
        DBCursor commentsCursor = generalCommentsCollection.find(getCommentsQuery);

        try {
            while (commentsCursor.hasNext()) {
                DBObject commentObject = commentsCursor.next();
                TrailBookComment comment = getCommentFromDBObject(commentObject);
                comments.add(comment);
            }
        } finally {
            commentsCursor.close();
        }

        return comments;
    }

    private class ConnectFailedException extends RuntimeException {
    }
}
