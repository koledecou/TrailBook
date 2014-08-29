package com.trailbook.kole.data;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
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
    private transient boolean isDownloaded = false;

    HashMap<String, Note> pathNotes;
    ArrayList<String> segmentIds;
    PathSummary summary;

    private class NoPathIdException extends RuntimeException {}

    public Path(String id, String firstSegmentId) {
        summary = new PathSummary(id);
        segmentIds = new ArrayList<String>();
        segmentIds.add(firstSegmentId);
        pathNotes = new HashMap<String, Note>();
    }

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

    /*
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
*/
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
