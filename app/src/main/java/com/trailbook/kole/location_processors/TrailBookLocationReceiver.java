package com.trailbook.kole.location_processors;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

import com.google.android.gms.location.LocationClient;
import com.squareup.otto.Bus;
import com.trailbook.kole.events.LocationChangedEvent;
import com.trailbook.kole.state_objects.BusProvider;
import com.trailbook.kole.state_objects.TrailBookState;

/**
 * Created by kole on 9/14/2014.
 */
public class TrailBookLocationReceiver extends BroadcastReceiver {
    Bus mBus;

    public TrailBookLocationReceiver() {
        super();

        mBus = BusProvider.getInstance();
        mBus.register(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Location location = (Location) intent.getExtras().get(LocationClient.KEY_LOCATION_CHANGED);
        if (location != null) {
            //Log.d(Constants.TRAILBOOK_TAG, "TrailBookLocationReceiver: New Location received:" + location);
            mBus.post(new LocationChangedEvent(location));
            LocationProcessor processor = TrailBookState.getLocationProcessor();
            if (processor != null)
                processor.process(location);
        }
    }
}
