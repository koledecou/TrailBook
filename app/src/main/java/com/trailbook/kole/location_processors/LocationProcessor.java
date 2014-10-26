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

/*    NotificationCompat.Builder createListeningNotifyBuilder(String title, String content, RemoteViews notificationRemoteView) {
        Intent stopButtonIntent = new Intent(mContext, ReceiveStartStopLocationUpdatesCommand.class);
        stopButtonIntent.putExtra(EXTRA_STOP, true);

        Intent pauseButtonIntent = new Intent(mContext, ReceiveStartStopLocationUpdatesCommand.class);
        pauseButtonIntent.putExtra(EXTRA_PAUSE, true);

        Intent resumeButtonIntent = new Intent(mContext, ReceiveStartStopLocationUpdatesCommand.class);
        resumeButtonIntent.putExtra(EXTRA_RESUME, true);

        notificationRemoteView.setOnClickPendingIntent(R.id.fn_button_stop, PendingIntent.getBroadcast(mContext, RC_STOP, stopButtonIntent, 0));
        notificationRemoteView.setOnClickPendingIntent(R.id.fn_button_resume, PendingIntent.getBroadcast(mContext, RC_RESUME, resumeButtonIntent, 0));
        notificationRemoteView.setOnClickPendingIntent(R.id.fn_button_pause, PendingIntent.getBroadcast(mContext, RC_PAUSE, pauseButtonIntent, 0));

        notificationRemoteView.setTextViewText(R.id.fn_text_title, title);
        notificationRemoteView.setTextViewText(R.id.fn_text_content, content);

        return new NotificationCompat.Builder(mContext)
                .setSmallIcon(R.drawable.trail_book_logo)
                .setContent(notificationRemoteView)
                .setContentIntent(getListeningNotificationPendingIntent())
                .setOngoing(true);
    }*/

/*    protected RemoteViews getNotificationRemoteView() {
        RemoteViews notificationView = new RemoteViews(mContext.getPackageName(), R.layout.active_path_notification);
        return notificationView;
    }*/

/*    private PendingIntent getListeningNotificationPendingIntent() {
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
    }*/

    public abstract void process(Location newLocation);
}
