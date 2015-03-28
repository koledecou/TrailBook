package com.trailbook.kole.location_processors;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.widget.RemoteViews;

import com.trailbook.kole.activities.R;
import com.trailbook.kole.activities.TrailBookActivity;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.state_objects.PathManager;

/**
 * Created by kole on 10/10/2014.
 */
public class NotificationUtils {
    public static final String EXTRA_STOP = "EXTRA_STOP" ;
    public static final String EXTRA_RESUME = "EXTRA_RESUME";
    public static final String EXTRA_PAUSE = "EXTRA_PAUSE";
    public static final String EXTRA_PATH_ID = "EXTRA_PATH_ID";

    public static final int RC_STOP = 0;
    public static final int RC_RESUME = 1;
    public static final int RC_PAUSE = 2;
    public static final int RC_RETRY = 3;

    public static NotificationCompat.Builder createListeningNotifyBuilder(Context context, String title, String content, String pathId, RemoteViews notificationRemoteView) {
        PathSummary p = PathManager.getInstance().getPathSummary(pathId);

        Intent stopButtonIntent = new Intent(context, ReceiveStartStopLocationUpdatesCommand.class);
        stopButtonIntent.putExtra(EXTRA_STOP, true);

        Intent pauseButtonIntent = new Intent(context, ReceiveStartStopLocationUpdatesCommand.class);
        pauseButtonIntent.putExtra(EXTRA_PAUSE, true);

        Intent resumeButtonIntent = new Intent(context, ReceiveStartStopLocationUpdatesCommand.class);
        resumeButtonIntent.putExtra(EXTRA_RESUME, true);

        notificationRemoteView.setOnClickPendingIntent(R.id.fn_button_stop, PendingIntent.getBroadcast(context, RC_STOP, stopButtonIntent, 0));
        notificationRemoteView.setOnClickPendingIntent(R.id.fn_button_resume, PendingIntent.getBroadcast(context, RC_RESUME, resumeButtonIntent, 0));
        notificationRemoteView.setOnClickPendingIntent(R.id.fn_button_pause, PendingIntent.getBroadcast(context, RC_PAUSE, pauseButtonIntent, 0));

        notificationRemoteView.setTextViewText(R.id.fn_text_title, title);
        notificationRemoteView.setTextViewText(R.id.fn_text_content, content);

        return new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.notification)
                .setContent(notificationRemoteView)
                .setContentIntent(getListeningNotificationPendingIntent(context))
                .setOngoing(true);
    }

    public static NotificationCompat.Builder createUploadFailedNotifyBuilder(Context context, String pathId, String content) {
        RemoteViews notificationRemoteView = getUploadFailedNotificationRemoteView(context);
        PathSummary summary = PathManager.getInstance().getPathSummary(pathId);
        String title = summary.getName();
/*
        Intent retryButtonIntent = new Intent(context, ReceiveRetryUploadCommand.class);
        retryButtonIntent.putExtra(EXTRA_PATH_ID, pathId);

        notificationRemoteView.setOnClickPendingIntent(R.id.retry_button, PendingIntent.getBroadcast(context, RC_RETRY, retryButtonIntent, 0));
*/

        notificationRemoteView.setTextViewText(R.id.fn_text_title, title);
        notificationRemoteView.setTextViewText(R.id.fn_text_content, content);

        return new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.notification)
                .setContent(notificationRemoteView)
                .setContentIntent(getListeningNotificationPendingIntent(context));
    }

    public static RemoteViews getLeadingNotificationRemoteView(Context context) {
        RemoteViews leadingNotificationView = new RemoteViews(context.getPackageName(), R.layout.active_path_notification);
        leadingNotificationView.setViewVisibility(R.id.fn_button_resume, View.GONE);
        return leadingNotificationView;
    }

    public static RemoteViews getFollowingNotificationRemoteView(Context context) {
        RemoteViews followingNotificationView = new RemoteViews(context.getPackageName(), R.layout.active_path_notification);
        followingNotificationView.setViewVisibility(R.id.fn_button_pause, View.GONE);
        followingNotificationView.setViewVisibility(R.id.fn_button_resume, View.GONE);
        return followingNotificationView;
    }

    public static PendingIntent getListeningNotificationPendingIntent(Context context) {
        Intent resultIntent = new Intent(context, TrailBookActivity.class);
        resultIntent.setAction("android.intent.action.MAIN");
        resultIntent.addCategory("android.intent.category.LAUNCHER");

        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        context,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        return resultPendingIntent;
    }

    public static RemoteViews getNotificationRemoteView(Context context) {
        RemoteViews followingNotificationView = new RemoteViews(context.getPackageName(), R.layout.active_path_notification);
        return followingNotificationView;
    }

    public static RemoteViews getUploadFailedNotificationRemoteView(Context context) {
        RemoteViews uploadFailedView = new RemoteViews(context.getPackageName(), R.layout.upload_failed_notification);
        return uploadFailedView;
    }

    public static int getNotificationId(String objectID) {
        int id;
        //the last 9 digits should be safe to cast as an int
        try {
            String objectIdTrunc;
            if (objectID.length() > 9) {
                objectIdTrunc = objectID.substring(objectID.length() - 9, objectID.length());
            } else {
                objectIdTrunc = objectID.replace('-', '0');
            }

            id = Integer.parseInt(objectIdTrunc);
        } catch (Exception e) {
            id = 10;
        }
        return id;
    }
}
