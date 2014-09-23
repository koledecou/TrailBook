package com.trailbook.kole.data;

import com.google.android.gms.maps.model.LatLng;
import com.trailbook.kole.data.geo.GeoPoint;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Fistik on 7/2/2014.
 */
public class PathSummary {
    String name;
    String description;
    long lastUpdatedTimestamp;
    String ownerID;
    ArrayList<String> noteIds;
    ArrayList<String> segmentIds;

    //TODO: add objects for associated climbs, books, summary images, etc...

    String _id = "-1";
    GeoPoint startCoords = null;
    GeoPoint endCoords = null;

/*    public PathSummary(String id, String ownerID) {
        this.id=id;
        this.ownerID = ownerID;
    }*/

    public PathSummary(String id) {
        this._id=id;
        segmentIds = new ArrayList<String>();
        noteIds = new ArrayList<String>();
        startCoords = new GeoPoint();
        endCoords = new GeoPoint();
    }


    public void setSegmentIdList(ArrayList<String> segmentIds) {
        this.segmentIds = segmentIds;
    }


    public void setNoteIdList(ArrayList<String> noteIds) {
        this.noteIds = noteIds;
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
    public ArrayList<String> getNoteIdList() {
        return noteIds;
    }

    public void addSegment(String segmentId) {
        if (segmentId != null && !segmentIds.contains(segmentId))
            segmentIds.add(segmentId);
    }
    public void addNote(String noteId) {
        if (!noteIds.contains(noteId))
            noteIds.add(noteId);
    }

    public void setIDWithTimestamp()        { this._id = String.valueOf(new Date().getTime()); }
    public void setID(String id)            { this._id = id; }
    public void setStart(LatLng start)      {
        startCoords.setCoordinates(new double[]{start.longitude, start.latitude});
    }
    public void setEnd(LatLng end)          {
        endCoords.setCoordinates(new double[]{end.longitude, end.latitude});
    }

    public void setName(String name)        { this.name = name; }
    public void setDescription(String desc) { this.description = desc; }

    public String getName()                 { return name; }
    public String getDescription()          { return description; }
    public LatLng getStart() {
        return startCoords.toLatLng();
    }
    public LatLng getEnd() {
        return endCoords.toLatLng();
    }

    public String getId() {
        return _id;
    }
}
