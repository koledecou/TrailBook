package com.trailbook.kole.events;

import com.google.android.gms.common.ConnectionResult;

/**
 * Created by Fistik on 7/17/2014.
 */
public class LocationServiceConnectionFailedEvent {
    private ConnectionResult connectionResult;

    public LocationServiceConnectionFailedEvent(ConnectionResult connectionResult) {
        this.connectionResult = connectionResult;
    }

    public ConnectionResult getConnectionResult() {
        return connectionResult;
    }
}
