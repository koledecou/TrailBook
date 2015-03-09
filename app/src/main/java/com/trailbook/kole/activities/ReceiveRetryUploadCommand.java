package com.trailbook.kole.activities;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.squareup.otto.Bus;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.location_processors.NotificationUtils;
import com.trailbook.kole.state_objects.BusProvider;

public class ReceiveRetryUploadCommand extends BroadcastReceiver {
    private static final String CLASS_NAME = "ReceiveRetryUploadCommand";

    public ReceiveRetryUploadCommand() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(Constants.TRAILBOOK_TAG, "ReceiveRetryUploadCommand: action received");
        String pathId = intent.getStringExtra(NotificationUtils.EXTRA_PATH_ID);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Bus bus = BusProvider.getInstance();
        bus.register(this);

        notificationManager.cancel(NotificationUtils.getNotificationId(pathId));
        //todo: restart upload
    }
}
