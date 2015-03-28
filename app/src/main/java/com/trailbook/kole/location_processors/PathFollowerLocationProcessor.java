package com.trailbook.kole.location_processors;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.widget.RemoteViews;

import com.trailbook.kole.activities.ApproachingObjectNotificationReceiverActivity;
import com.trailbook.kole.activities.R;
import com.trailbook.kole.activities.TrailBookActivity;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.helpers.PreferenceUtilities;
import com.trailbook.kole.helpers.TrailbookPathUtilities;
import com.trailbook.kole.state_objects.PathManager;
import com.trailbook.kole.state_objects.TrailBookState;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by kole on 7/19/2014.
 */
public class PathFollowerLocationProcessor extends LocationProcessor {
    private static final int OFF_ROUTE_NOTIFICATION_ID = 1;

    private static final long ONE_MINUTE = 60000;
    public static final String EXTRA_OBJECT_ID = "EXTRA_OBJECT_ID";

    private NotificationCompat.Builder mOffRouteNotifyBuilder;
    private NotificationCompat.Builder mApproachingPointObjectNotificationBuilder;

    Location mCurrentLocation;

    String mPathId;
    long mStrayFromPathAlertLastPlayedTime = 0;

    public PathFollowerLocationProcessor(String pathId, Context context) {
        super(context);

        mPathId=pathId;
        createNotificationBuilders();

        sendListeningNotification();
    }

    private void createNotificationBuilders() {
        mOffRouteNotifyBuilder = createOffRouteNotifyBuilder();
        mApproachingPointObjectNotificationBuilder = createApproachingNoteNotifyBuilder();
//        mListeningNotifyBuilder = createListeningNotifyBuilder();
        RemoteViews followerNotificationView = NotificationUtils.getFollowingNotificationRemoteView(mContext);
        String title = String.format(mContext.getString(R.string.following_trail_title), PathManager.getInstance().getPathSummary(mPathId).getName());
        String content = mContext.getString(R.string.following_trail_notification_content);
        mListeningNotifyBuilder = NotificationUtils.createListeningNotifyBuilder(mContext, title, content, mPathId, followerNotificationView);
//        TrailBookState.setListeningNotifyBuilder(mListeningNotifyBuilder);
    }

/*
    private NotificationCompat.Builder createListeningNotifyBuilder() {
        PathSummary p = PathManager.getInstance().getPathSummary(mPathId);
        String title = String.format(mContext.getString(R.string.following_trail_title), p.getName());

        return super.createListeningNotifyBuilder(title, mContext.getString(R.string.following_trail_notification_content), getNotificationRemoteView());
    }
*/

    protected RemoteViews getNotificationRemoteView() {
        RemoteViews followingNotificationView = new RemoteViews(mContext.getPackageName(), R.layout.active_path_notification);
        followingNotificationView.setViewVisibility(R.id.fn_button_pause, View.GONE);
        return followingNotificationView;
    }

    private NotificationCompat.Builder createApproachingNoteNotifyBuilder() {
        return new NotificationCompat.Builder(mContext)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle("Trail Note Nearby")
                .setContentText("There is a path note nearby.")
                .setSound(getApproachingNoteSoundURI())
                .setOnlyAlertOnce(true);
    }

    private NotificationCompat.Builder createOffRouteNotifyBuilder() {
       return new NotificationCompat.Builder(mContext)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle("Off Route Notification")
                .setContentText("You are off route.")
                .setOnlyAlertOnce(true)
                .setSound(getOffRouteSoundURI());
    }

    private Uri getOffRouteSoundURI() {
        return getSoundURI(PreferenceManager.getDefaultSharedPreferences(mContext).getString("off_route_alert", null));
    }

    private Uri getApproachingNoteSoundURI() {
        return getSoundURI(PreferenceManager.getDefaultSharedPreferences(mContext).getString("ringtoneClose", null));
    }

    private Uri getSoundURI(String ringTone) {
        Uri alertUri;
        try {
            alertUri = Uri.parse(ringTone);
        } catch (Exception e) {
            alertUri = Uri.parse("android.resource://"
                    + TrailBookState.getInstance().getPackageName() + "/" + R.raw.knock);
            //alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        return alertUri;
    }

    @Override
    public void process(Location newLocation) {
        try {
            processOffRouteNotification(newLocation);
        } catch (Exception e) {
        }
        try {
            processApproachingObjectNotifications(newLocation);
        } catch (Exception e) {
        }
    }

    public void processApproachingObjectNotifications(Location newLocation) {
        ArrayList<PointAttachedObject> pointObjects = PathManager.getInstance().getPointObjectsForPath(mPathId);
        for (PointAttachedObject paObject:pointObjects) {
            double distanceToNote = TrailbookPathUtilities.getDistanceToNote(paObject, newLocation);
            if (distanceToNote < PreferenceUtilities.getNoteAlertDistanceInMeters(mContext)) {
                sendApproachingObjectNotification(paObject.getId(), paObject, distanceToNote);
            } else {
                cancelNotification(NotificationUtils.getNotificationId(paObject.getId()));
            }
        }
    }

    private void processOffRouteNotification(Location newLocation) {
        double currentDistanceFromPath = TrailbookPathUtilities.getNearestDistanceFromPointToPath(TrailbookPathUtilities.locationToLatLon(newLocation), mPathId);
        if (currentDistanceFromPath > PreferenceUtilities.getStrayFromPathTriggerDistanceInMeters(mContext) ) {
            if (!hasAlertBeenPlayedRecently()) {
                mStrayFromPathAlertLastPlayedTime = new Date().getTime();
                cancelNotification(OFF_ROUTE_NOTIFICATION_ID);
            }
            playStrayedFromPathAlert(currentDistanceFromPath);
        } else {
            cancelNotification(OFF_ROUTE_NOTIFICATION_ID);
        }
    }

    public void cancelNotification(int notifyId) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nMgr = (NotificationManager) mContext.getSystemService(ns);
        nMgr.cancel(notifyId);
    }

    private void playStrayedFromPathAlert(double currentDistanceFromPath) {
        sendOffRouteNotification(currentDistanceFromPath);
        //todo: display this in the map view and change color of path.  remove the toast.
    }

    private void sendListeningNotification() {
        mNotificationManager.notify(
                LISTENING_NOTIFICATION_ID,
                mListeningNotifyBuilder.build());
    }

    private void sendOffRouteNotification(double currentDistanceFromPath) {
        String notificationContent = String.format(mContext.getResources().getString(R.string.strayed_notification_content), PreferenceUtilities.getDistString(mContext, currentDistanceFromPath));
        mOffRouteNotifyBuilder.setContentText(notificationContent);
        mOffRouteNotifyBuilder.setContentIntent(getOffRouteNotificationPendingIntent());
        updateOffRouteRingtone();
        mNotificationManager.notify(
                OFF_ROUTE_NOTIFICATION_ID,
                mOffRouteNotifyBuilder.build());
    }

    private void sendApproachingObjectNotification(String noteId, PointAttachedObject paObject, double distance) {
        int notificationId = NotificationUtils.getNotificationId(noteId);
        //todo: change this for other types
        String notificationContent = String.format(mContext.getString(R.string.note_notification_title), PreferenceUtilities.getDistString(mContext, distance));
        mApproachingPointObjectNotificationBuilder.setContentTitle(notificationContent);
        mApproachingPointObjectNotificationBuilder.setContentText(paObject.getAttachment().getNotificationString());
        mApproachingPointObjectNotificationBuilder.setContentIntent(getApproachingObjectNotificationPendingIntent(noteId, notificationId));
        updateApproachingNoteRingtone();

        mNotificationManager.notify(
                notificationId,
                mApproachingPointObjectNotificationBuilder.build());
    }

    private boolean hasAlertBeenPlayedRecently() {
        long currentTime = new Date().getTime();
        long deltaMilliSeconds = currentTime - mStrayFromPathAlertLastPlayedTime;
        if (mStrayFromPathAlertLastPlayedTime == 0 || (deltaMilliSeconds > ONE_MINUTE) ){
            return false;
        } else {
            return  true;
        }
    }

    private void updateOffRouteRingtone() {
        mOffRouteNotifyBuilder.setSound(getOffRouteSoundURI());
    }

    private void updateApproachingNoteRingtone() {
        mApproachingPointObjectNotificationBuilder.setSound(getApproachingNoteSoundURI());
    }

    private PendingIntent getApproachingObjectNotificationPendingIntent(String objectId, int notificationId) {
        Intent resultIntent = new Intent(mContext, ApproachingObjectNotificationReceiverActivity.class);
        resultIntent.putExtra(EXTRA_OBJECT_ID, objectId);
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // Because clicking the notification launches a new ("special") activity,
        // there's no need to create an artificial back stack.
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        mContext,
                        notificationId,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        return resultPendingIntent;
    }

    private PendingIntent getOffRouteNotificationPendingIntent() {
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
