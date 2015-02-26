package com.trailbook.kole.location_processors;

import android.app.NotificationManager;
import android.content.Context;
import android.location.Location;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.trailbook.kole.data.Constants;

/**
 * Created by kole on 9/15/2014.
 */
public abstract class LocationProcessor {
    public static final int LISTENING_NOTIFICATION_ID = 2;

    NotificationManager mNotificationManager;
    NotificationCompat.Builder mListeningNotifyBuilder;

    Context mContext;

    public LocationProcessor(Context context) {
        mContext = context;
        mNotificationManager = (NotificationManager)
                mContext.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void removeAllNotifications() {
        Log.d(Constants.TRAILBOOK_TAG, "LocationProcessor: removing all notifications");
        mNotificationManager.cancelAll();
    }

    public abstract void process(Location newLocation);
}
