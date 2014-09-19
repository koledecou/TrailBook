package com.trailbook.kole.location_processors;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.trailbook.kole.activities.R;
import com.trailbook.kole.activities.TrailBookActivity;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.Path;
import com.trailbook.kole.state_objects.PathManager;

/**
 * Created by kole on 7/20/2014.
 */
public class PathLeaderLocationProcessor extends LocationProcessor {
    private static final float MIN_ACCURACY = 60; //don't record points that might be worse than 200 ft
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
        mListeningNotifyBuilder = createListeningNotifyBuilder();

        sendListeningNotification();
    }

    private NotificationCompat.Builder createListeningNotifyBuilder() {
        Path p = PathManager.getInstance().getPath(mPathId);
        String title = String.format(mContext.getString(R.string.leading_trail_title), p.getSummary().getName());

        return super.createListeningNotifyBuilder(title, mContext.getString(R.string.following_trail_notification_content));
    }

    @Override
    public void process(Location newLocation) {
        if (mLastLocation == null)
            mLastLocation = newLocation;
        else {
            if (mLastLocation.distanceTo(newLocation) < Constants.MIN_DISTANCE_BETWEEN_POINTS)
                return;
        }
        Log.d(Constants.TRAILBOOK_TAG, "adding point to segment " + mSegmentId + " "  + newLocation.toString() );
        Log.d(Constants.TRAILBOOK_TAG, "PathLeaderLocationProcessor: accuracy is " + newLocation.getAccuracy());
        if (!newLocation.hasAccuracy() || newLocation.getAccuracy() < MIN_ACCURACY &&
                mPathManager.getSegment(mSegmentId) != null &&
                mPathManager.getPath(mPathId) != null) {
            mPathManager.addPointToSegment(mSegmentId, mPathId, newLocation);
            mPathManager.savePath(mPathId, mContext);
        }

        mLastLocation = newLocation;
    }

    private void sendListeningNotification() {
        Path p = PathManager.getInstance().getPath(mPathId);
        String notificationContent = String.format(mContext.getResources().getString(R.string.leading_trail_title), p.getSummary().getName());
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
