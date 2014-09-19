package com.trailbook.kole.location_processors;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.trailbook.kole.data.Constants;
import com.trailbook.kole.state_objects.TrailBookState;

public class ReceiveStartStopLocationUpdatesCommand extends BroadcastReceiver {
    public ReceiveStartStopLocationUpdatesCommand() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(Constants.TRAILBOOK_TAG, "ReceiveLocationUpdateCommand: action received");
        boolean isStopRequest = intent.getBooleanExtra(LocationProcessor.EXTRA_STOP, false);
        Log.d(Constants.TRAILBOOK_TAG, "ReceiveLocationUpdateCommand: is stop request:" +isStopRequest);
        if (isStopRequest) {
            TrailBookState.getInstance().switchToSearchMode();
        }
    }
}
