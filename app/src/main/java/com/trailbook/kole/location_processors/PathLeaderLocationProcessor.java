package com.trailbook.kole.location_processors;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import com.trailbook.kole.activities.R;
import com.trailbook.kole.activities.TrailBookActivity;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.state_objects.PathManager;
import com.trailbook.kole.state_objects.TrailBookState;

/**
 * Created by kole on 7/20/2014.
 */
public class PathLeaderLocationProcessor extends LocationProcessor {
    private static final float MIN_ACCURACY = 50; //don't record points that might be worse than 160 ft
    PathManager mPathManager;
    String mSegmentId;
    String mPathId;
    Location mLastLocation = null;
    private NotificationCompat.Builder mListeningNotifyBuilder;

    public PathLeaderLocationProcessor(Context context, String segmentId, String pathId) {
        super(context);

        this.mSegmentId = segmentId;
        this.mPathId = pathId;
        mPathManager = PathManager.getInstance();
        RemoteViews leaderNotificationView = NotificationUtils.getLeadingNotificationRemoteView(context);
        String title = String.format(mContext.getString(R.string.leading_trail_title), PathManager.getInstance().getPathSummary(mPathId).getName());
        String content = mContext.getString(R.string.leading_trail_notification_content);
        mListeningNotifyBuilder = NotificationUtils.createListeningNotifyBuilder(context, title, content, mPathId, leaderNotificationView);

        sendListeningNotification();
    }

    @Override
    public void process(Location newLocation) {
        if (mLastLocation == null)
            mLastLocation = newLocation;
        else {
            if (mLastLocation.distanceTo(newLocation) < Constants.MIN_DISTANCE_BETWEEN_POINTS)
                return;
        }
        if (isGoodStateToRecord(newLocation)) {
            mPathManager.addPointToSegment(mSegmentId, mPathId, newLocation);
            mPathManager.savePath(mPathId);
        }

        mLastLocation = newLocation;
    }

    private boolean isGoodStateToRecord(Location newLocation) {
        if (!TrailBookState.alreadyGotEnoughGoodLocations())
            return false;
        if (mPathManager.getSegment(mSegmentId) == null ||  mPathManager.getPathSummary(mPathId) == null)
            return false;
        if (newLocation.hasAccuracy() && newLocation.getAccuracy() > MIN_ACCURACY)
            return false;

        return true;
    }

    private void sendListeningNotification() {
        PathSummary p = PathManager.getInstance().getPathSummary(mPathId);
        String notificationContent = String.format(mContext.getResources().getString(R.string.leading_trail_title), p.getName());
        mListeningNotifyBuilder.setContentTitle(notificationContent);
        mListeningNotifyBuilder.setContentText("there will be distance info and an off button here");
        mListeningNotifyBuilder.setContentIntent(getListeningNotificationPendingIntent());
        mNotificationManager.notify(
                LISTENING_NOTIFICATION_ID,
                mListeningNotifyBuilder.build());
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
}
