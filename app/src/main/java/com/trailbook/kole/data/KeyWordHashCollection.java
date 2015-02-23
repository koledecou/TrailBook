package com.trailbook.kole.data;

import java.util.HashMap;

/**
 * Created by kole on 2/10/2015.
 */
public class KeyWordHashCollection {
    HashMap<String, String> climbs;
    HashMap<String, String> crags;
    HashMap<String, String> regions;

    public KeyWordHashCollection() {
        climbs = new HashMap<String, String>();
        crags = new HashMap<String, String>();
        regions = new HashMap<String, String>();
    }

    public void addClimb(String keyWord, String pathId) {
        climbs.put(keyWord, pathId);
    }

    public void addCrag(String keyWord, String pathId) {
        crags.put(keyWord, pathId);
    }

    public void addRegion(String keyWord, String pathId) {
        regions.put(keyWord, pathId);
    }
}
