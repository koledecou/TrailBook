package com.trailbook.kole.data;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.annotations.Expose;

import java.util.Date;
import java.util.Hashtable;

/**
 * Created by Fistik on 7/2/2014.
 */
public class PathSummary {
    String name;
    String keyWordList; //climb names and crags
    String creator;
    String category;
    String password;
    String description;
    String startDescription;
    String destDescription;
    String groupId;

    //TODO: add objects for associated climbs, books, summary images, etc...

    String id = "-1";
    LatLng start;
    LatLng end;

    public PathSummary(String id) {
        this.id=id;
    }

    public void setIDWithTimestamp()        { this.id = String.valueOf(new Date().getTime()); }
    public void setID(String id)            { this.id = id; }
    public void setStart(LatLng start)      { this.start = start; }
    public void setEnd(LatLng end)          { this.end = end; }

    public void setName(String name)        { this.name = name; }
    public void setDescription(String desc) { this.description = desc; }

    public String getName()                 { return name; }
    public String getDescription()          { return description; }
    public LatLng getStart() { return start; }
    public LatLng getEnd() { return end; }

    public String getId() {
        return id;
    }
}
