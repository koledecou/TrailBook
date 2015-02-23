package com.trailbook.kole.data;

import java.util.ArrayList;

/**
 * Created by kole on 1/27/2015.
 */
public class KeyWordGroup {
    public ArrayList<String> climbs;
    public ArrayList<String> crags;
    public ArrayList<String> regions;

    public KeyWordGroup(ArrayList<String> climbs, ArrayList<String> crags, ArrayList<String> regions) {
        this.climbs = climbs;
        this.crags = crags;
        this.regions = regions;
    }

    public KeyWordGroup() {
        climbs = new ArrayList<String>();
        crags = new ArrayList<String>();
        regions = new ArrayList<String>();
    }

    public void addClimb(String climb) {
        climb = climb.trim();
        if (climbs == null)
            climbs = new ArrayList<String>();

        if (!climbs.contains(climb))
            climbs.add(climb);
    }

    public void addCrag(String crag) {
        crag = crag.trim();
        if (crags == null)
            crags = new ArrayList<String>();

        crags.add(crag);
    }

    public void addRegion(String region) {
        region = region.trim();
        if (regions == null)
            regions = new ArrayList<String>();

        regions.add(region);
    }

    public void removeClimb(String climb) {
        climb = climb.trim();
        if (climbs != null) {
            climbs.remove(climb);
        }
    }

    public void removeCrag(String crag) {
        //crag = crag.trim();
        if (crags != null) {
            crags.remove(crag);
        }
    }

    public void removeRegion(String region) {
        region = region.trim();
        if (regions != null) {
            regions.remove(region);
        }
    }
}
