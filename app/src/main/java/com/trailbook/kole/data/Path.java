package com.trailbook.kole.data;

import java.util.ArrayList;
import java.util.HashMap;


public class Path {
    private transient boolean isDownloaded = false;

    HashMap<String, Note> pathNotes;
    ArrayList<String> segmentIds;
    PathSummary summary;

    private class NoPathIdException extends RuntimeException {}

/*    public Path(String id, String ownerID) {
        summary = new PathSummary(id, ownerID);
        segmentIds = new ArrayList<String>();
        pathNotes = new HashMap<String, Note>();
    }*/

    public Path(String id) {
        summary = new PathSummary(id);
        segmentIds = new ArrayList<String>();
        pathNotes = new HashMap<String, Note>();
    }

    public void addSegment(String segmentId) {
        segmentIds.add(segmentId);
    }

    public void addPathNote (Note note, String noteId) {
        pathNotes.put(noteId, note);
    }

    public Note getPathNoteFromId(String noteId) {
        Note note = pathNotes.get(noteId);
        if (note != null) {
            return note;
        } else {
            return null;
        }
    }

    public void setSegmentIdList(ArrayList<String> segmentIds) {
        this.segmentIds = segmentIds;
    }

    public String getLastSegment() {
        if (segmentIds.size() >= 1)
            return segmentIds.get(segmentIds.size()-1);
        else
            return null;
    }

    public ArrayList<String> getSegmentIdList() {
        return segmentIds;
    }

    public PathSummary getSummary() {
        return summary;
    }

    public void setSummary(PathSummary summary) {
        this.summary = summary;
    }

    public void setDownloaded(boolean isDownloaded) {
        this.isDownloaded = isDownloaded;
    }

    public boolean isDownloaded() {
        return isDownloaded;
    }

    public String getId() {
        if (summary != null)
            return summary.getId();
        else
            throw new NoPathIdException();
    }
}
