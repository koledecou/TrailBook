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


public class Path {
    public static final int MIN_DISTANCE_BETWEEN_POINTS=3; //distance in meters
    ArrayList<LatLng> points;
    HashMap<String, PointAttachedObject> notes;
    HashMap<String, Note> pathNotes;
    PathSummary summary;

    public Path(String id) {
        summary = new PathSummary(id);
        points = new ArrayList<LatLng>();
        notes = new HashMap<String, PointAttachedObject>();
        pathNotes = new HashMap<String, Note>();
    }

    public void addPoint(LatLng newPoint) {
        //only add it if it's far enough away from the last one.
        if (points.size() < 1){
            points.add(newPoint);
        }else{
            LatLng last = points.get(points.size() - 1);
            float[] results = null;
            Location.distanceBetween(last.latitude, last.longitude, newPoint.latitude, newPoint.longitude, results);
            if (results[0]>MIN_DISTANCE_BETWEEN_POINTS)
                points.add(newPoint);
        }
    }

    public void addNote (LatLng p, Note note, String noteId) {
        PointAttachedObject paoNote = new PointAttachedObject(p, note);
        notes.put(noteId, paoNote);
    }

    public void addPathNote (Note note, String noteId) {
        pathNotes.put(noteId, note);
    }

    public Note getNoteFromId(String noteId) {
        PointAttachedObject paoNote = notes.get(noteId);
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
}
