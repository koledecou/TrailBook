package com.trailbook.kole.location_processors;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.trailbook.kole.activities.R;
import com.trailbook.kole.activities.TrailBookActivity;
import com.trailbook.kole.data.Constants;

/**
 * Created by kole on 9/15/2014.
 */
public abstract class LocationProcessor {
    public static final int LISTENING_NOTIFICATION_ID = 2;
    public static final String EXTRA_STOP = "EXTRA_STOP" ;
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

    NotificationCompat.Builder createListeningNotifyBuilder (String title, String content) {
        RemoteViews followingNotificationView = new RemoteViews(mContext.getPackageName(), R.layout.following_notification);
        Intent stopButtonIntent = new Intent(mContext, ReceiveStartStopLocationUpdatesCommand.class);
        stopButtonIntent.putExtra(EXTRA_STOP, true);
        followingNotificationView.setOnClickPendingIntent(R.id.fn_button_stop, PendingIntent.getBroadcast(mContext, 0, stopButtonIntent, 0));


        followingNotificationView.setTextViewText(R.id.fn_text_title, title);
        followingNotificationView.setTextViewText(R.id.fn_text_content, content);

        return new NotificationCompat.Builder(mContext)
                .setSmallIcon(R.drawable.trail_book_logo)
                .setContent(followingNotificationView)
                .setContentIntent(getListeningNotificationPendingIntent())
                .setOngoing(true);
    }


    private PendingIntent getListeningNotificationPendingIntent() {
        Intent resultIntent = new Intent(mContext, TrailBookActivity.class);
        resultIntent.setAction("android.intent.action.MAIN");
        resultIntent.addCategory("android.intent.category.LAUNCHER");

        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        mContext,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        return resultPendingIntent;
    }

    public abstract void process(Location newLocation);
}
