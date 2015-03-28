package com.trailbook.kole.location_processors;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.widget.RemoteViews;

import com.squareup.otto.Bus;
import com.trailbook.kole.activities.R;
import com.trailbook.kole.events.RefreshMessageEvent;
import com.trailbook.kole.state_objects.BusProvider;
import com.trailbook.kole.state_objects.PathManager;
import com.trailbook.kole.state_objects.TrailBookState;

public class ReceiveStartStopLocationUpdatesCommand extends BroadcastReceiver {
    public ReceiveStartStopLocationUpdatesCommand() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String pathId = TrailBookState.getActivePathId();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Bus bus = BusProvider.getInstance();
        bus.register(this);

        boolean isStopRequest = intent.getBooleanExtra(NotificationUtils.EXTRA_STOP, false);
        boolean isPauseRequest = intent.getBooleanExtra(NotificationUtils.EXTRA_PAUSE, false);
        boolean isResumeRequest = intent.getBooleanExtra(NotificationUtils.EXTRA_RESUME, false);
        if (isStopRequest) {
            TrailBookState.getInstance().switchToSearchMode();
        } else if (isPauseRequest) {
            TrailBookState.getInstance().stopLocationUpdates();
            RemoteViews remoteViews = NotificationUtils.getNotificationRemoteView(context);

            remoteViews.setViewVisibility(R.id.fn_button_pause, View.GONE);
            remoteViews.setViewVisibility(R.id.fn_button_resume, View.VISIBLE);
            String title = context.getString(R.string.paused_title);
            String content = context.getString(R.string.paused_content);
            NotificationCompat.Builder builder = NotificationUtils.createListeningNotifyBuilder(context, title, content, pathId, remoteViews);

            notificationManager.notify(
                    LocationProcessor.LISTENING_NOTIFICATION_ID,
                    builder.build());

            bus.post(new RefreshMessageEvent());
        } else if (isResumeRequest) {
            RemoteViews remoteViews = NotificationUtils.getNotificationRemoteView(context);
            remoteViews.setViewVisibility(R.id.fn_button_pause, View.VISIBLE);
            remoteViews.setViewVisibility(R.id.fn_button_resume, View.GONE);
            String newSegmentId = PathManager.getInstance().addNewSegmentToPath(pathId);
            TrailBookState.setActiveSegmentId(newSegmentId);
            TrailBookState.getInstance().resumeLeadingActivePath(true);
            //wait for location?
            String title = String.format(context.getString(R.string.leading_trail_title), PathManager.getInstance().getPathSummary(pathId).getName());
            String content = context.getString(R.string.leading_trail_notification_content);
            NotificationCompat.Builder builder = NotificationUtils.createListeningNotifyBuilder(context, title, content, pathId, remoteViews);
            notificationManager.notify(
                    LocationProcessor.LISTENING_NOTIFICATION_ID,
                    builder.build());

            bus.post(new RefreshMessageEvent());
        }
    }
}
