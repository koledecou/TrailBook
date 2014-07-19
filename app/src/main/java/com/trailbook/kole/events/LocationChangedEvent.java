package com.trailbook.kole.events;

import android.location.Location;

/**
 * Created by Fistik on 7/17/2014.
 */
public class LocationChangedEvent {
    private final Location mLocation;

    public LocationChangedEvent(Location location) {
        mLocation = location;
    }

    public Location getLocation() {
        return mLocation;
    }
}
