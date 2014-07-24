package com.trailbook.kole.data;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.annotations.Expose;
import com.trailbook.kole.tools.TrailbookPathUtilities;


public class Path {
    private static final int MIN_DISTANCE_BETWEEN_POINTS=3; //distance in meters
    private static final int MAX_DISTANCE_BETWEEN_POINTS = 1000;
    private transient boolean isDownloaded = false;

    ArrayList<LatLng> points;
    HashMap<String, PointAttachedObject<Note>> pointNotes;
    HashMap<String, Note> pathNotes;
    PathSummary summary;

    private class NoPathIdException extends RuntimeException {}

    public Path(String id) {
        summary = new PathSummary(id);
        points = new ArrayList<LatLng>();
        pointNotes = new HashMap<String, PointAttachedObject<Note>>();
        pathNotes = new HashMap<String, Note>();
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
                Log.d("trailbook", "pathid: " + getSummary().getId() + " dist: " +  String.valueOf(delta) + " index: " + points.size());
            }
            if (delta>MIN_DISTANCE_BETWEEN_POINTS && delta < MAX_DISTANCE_BETWEEN_POINTS)
                points.add(newPoint);
        }
    }

    public void addNote (LatLng p, Note note, String noteId) {
        PointAttachedObject<Note> paoNote = new PointAttachedObject(p, note);
        pointNotes.put(noteId, paoNote);
    }

    public void addPathNote (Note note, String noteId) {
        pathNotes.put(noteId, note);
    }

    public Note getNoteFromId(String noteId) {
        PointAttachedObject<Note> paoNote = pointNotes.get(noteId);
        if (paoNote != null) {
            return (Note)paoNote.getAttachment();
        }

        Note pathNote = pathNotes.get(noteId);
        if (pathNote != null) {
            return pathNote;
        }

        return null;
    }

    public LatLng getStartCoords() {
        if (points.size()<1)
            return null;

        LatLng start = points.get(0);
        return start;
    }

    public LatLng getEndCoords() {
        if (points.size()<1)
            return null;

        LatLng end = points.get(points.size()-1);
        return end;
    }

    public void setStartAndEnd() {
        summary.setStart(getStartCoords());
        summary.setEnd(getEndCoords());
    }

    public void addPoints(ArrayList<LatLng> newPoints) {
        points.addAll(newPoints);
    }

    public PathSummary getSummary() {
        return summary;
    }

    public ArrayList<LatLng> getPoints() {
        return points;
    }

    public void setSummary(PathSummary summary) {
        this.summary = summary;
    }

    public void addPointNote(PointAttachedObject<Note> paoNote) {
        this.pointNotes.put(paoNote.getAttachment().getNoteID(), paoNote);
    }

    public HashMap<String,PointAttachedObject<Note>> getPointNotes() {
        return pointNotes;
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
        throw new NoPathIdException();
    }
}
