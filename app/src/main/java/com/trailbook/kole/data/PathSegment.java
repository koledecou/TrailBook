package com.trailbook.kole.data;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.trailbook.kole.tools.TrailbookPathUtilities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * Created by kole on 8/18/2014.
 */
public class PathSegment {
    String segmentId;
    ArrayList<LatLng> points;
    HashMap<String, PointAttachedObject<Note>> pointNotes;

    public PathSegment(String id) {
        points = new ArrayList<LatLng>();
        pointNotes = new HashMap<String, PointAttachedObject<Note>>();
        this.segmentId = id;
    }

    public void addPoint(LatLng newPoint) {
        //only add it if it's far enough away from the last one.
        // If it's too far away it's a bad point.
        if (points.size() < 1){
            points.add(newPoint);
        }else{
            LatLng last = points.get(points.size() - 1);
            float delta = TrailbookPathUtilities.getDistanceInMeters(last, newPoint);
            if (delta > 200) {
                Log.d("trailbook", "segmentId: " + segmentId + " dist: " + String.valueOf(delta) + " index: " + points.size());
            }
            if (delta>Constants.MIN_DISTANCE_BETWEEN_POINTS && delta < Constants.MAX_DISTANCE_BETWEEN_POINTS)
                points.add(newPoint);
        }
    }

    public void addNote(String noteId, PointAttachedObject<Note> paoNote) {
        pointNotes.put(noteId, paoNote);
    }

    public Note getNoteFromId(String noteId) {
        PointAttachedObject<Note> paoNote = pointNotes.get(noteId);
        if (paoNote != null) {
            return (Note)paoNote.getAttachment();
        }

        return null;
    }

    public ArrayList<Note> getNotes() {
        ArrayList<Note> notes = new ArrayList<Note>();
        Collection<PointAttachedObject<Note>> paoNotes = getPointNotes().values();
        for (PointAttachedObject<Note> paoNote : paoNotes) {
            notes.add(paoNote.getAttachment());
        }
        return notes;
    }

    public void addPoints(ArrayList<LatLng> newPoints) {
        points.addAll(newPoints);
    }

    public ArrayList<LatLng> getPoints() {
        return points;
    }

    public void addPointNote(PointAttachedObject<Note> paoNote) {
        this.pointNotes.put(paoNote.getAttachment().getNoteID(), paoNote);
    }

    public HashMap<String,PointAttachedObject<Note>> getPointNotes() {
        return pointNotes;
    }

    public String getId() {
        return segmentId;
    }

    public void removePoints() {
        points = new ArrayList<LatLng>();
    }

    public void setNotes(HashMap<String, PointAttachedObject<Note>> notes) {
        this.pointNotes = notes;
    }

    public void setPoints(ArrayList<LatLng> points) {
        this.points = points;
    }
}
