package com.trailbook.kole.location_processors;

import android.app.NotificationManager;
import android.content.Context;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.trailbook.kole.activities.R;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.Note;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.helpers.PreferenceUtilities;
import com.trailbook.kole.state_objects.PathManager;
import com.trailbook.kole.helpers.TrailbookPathUtilities;
import com.trailbook.kole.worker_fragments.LocationServicesFragment;

import java.util.Date;
import java.util.HashMap;

/**
 * Created by kole on 7/19/2014.
 */
public class PathFollowerLocationProcessor implements LocationServicesFragment.LocationProcessor {
    private static final int OFF_ROUTE_NOTIFICATION_ID = 1;
    private static final long ONE_MINUTE = 60000;
    private final NotificationCompat.Builder mOffRouteNotifyBuilder;
    private final NotificationCompat.Builder mApproachingNoteNotificationBuilder;
    NotificationManager mNotificationManager;
    Location mCurrentLocation;

    String mPathId;
    long mStrayFromPathAlertLastPlayedTime = 0;
    Context mContext;

    public PathFollowerLocationProcessor(String pathId, Context context) {
        mPathId=pathId;
        mContext=context;

        mOffRouteNotifyBuilder = new NotificationCompat.Builder(mContext)
                .setSmallIcon(R.drawable.trail_book_logo)
                .setContentTitle("Off Route Notification")
                .setContentText("You are off route.")
                .setOnlyAlertOnce(true)
                .setSound(getOffRouteSoundURI());
        mApproachingNoteNotificationBuilder = new NotificationCompat.Builder(mContext)
                .setSmallIcon(R.drawable.trail_book_logo)
                .setContentTitle("Trail Note Nearby")
                .setContentText("There is a path note nearby.")
                .setSound(getApproachingNoteSoundURI())
                .setOnlyAlertOnce(true);

        mNotificationManager = (NotificationManager)
                mContext.getSystemService(Context.NOTIFICATION_SERVICE);

//        removeAll();
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
            Log.d(Constants.TRAILBOOK_TAG, "PathFollowerLocationProcessor: Exception getting ring tone!", e);
            alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        return alertUri;
    }

    @Override
    public void process(Location newLocation) {
        Log.d(Constants.TRAILBOOK_TAG, "PathFollowerLocationProcessor: following " + mPathId + " "  + newLocation.toString() );
        processOffRouteNotification(newLocation);
        processApproachingNoteNotifications(newLocation);
    }

    public void processApproachingNoteNotifications(Location newLocation) {
        HashMap<String, PointAttachedObject<Note>> paoNotes = PathManager.getInstance().getPointNotesForPath(mPathId);
        for (PointAttachedObject<Note> paoNote:paoNotes.values()) {
            Note note = paoNote.getAttachment();
            double distanceToNote = TrailbookPathUtilities.getDistanceToNote(paoNote, newLocation);
            Log.d(Constants.TRAILBOOK_TAG, "PathFollowerLocationProcessor: distance to note " + note.getNoteID() + " : " + distanceToNote);
            if (distanceToNote < PreferenceUtilities.getNoteAlertDistanceInMeters(mContext)) {
                sendApproachingNoteNotification(note, distanceToNote);
            } else {
                cancelNotification(getNotificationId(note.getNoteID()));
            }
        }
    }

    private void processOffRouteNotification(Location newLocation) {
        double currentDistanceFromPath = TrailbookPathUtilities.getNearestDistanceFromPointToPath(TrailbookPathUtilities.locationToLatLon(newLocation), mPathId);
        Log.d(Constants.TRAILBOOK_TAG,"PathFollowerLocationProcessor: distance to path : " + currentDistanceFromPath );
        if (currentDistanceFromPath > PreferenceUtilities.getStrayFromPathTriggerDistanceInMeters(mContext) ) {
            if (!hasAlertBeenPlayedRecently()) {
                Log.d(Constants.TRAILBOOK_TAG, "Notify again.");
                mStrayFromPathAlertLastPlayedTime = new Date().getTime();
                cancelNotification(OFF_ROUTE_NOTIFICATION_ID);
            } else {
                Log.d(Constants.TRAILBOOK_TAG, "Upate distance on existing notification");
            }
            playStrayedFromPathAlert(currentDistanceFromPath);
            Log.d(Constants.TRAILBOOK_TAG, "PathFollowerLocationProcessor: sent off route notification.");
        } else {
            cancelNotification(OFF_ROUTE_NOTIFICATION_ID);
            Log.d(Constants.TRAILBOOK_TAG, "PathFollowerLocationProcessor: back on route.");
        }
    }

    public void cancelNotification(int notifyId) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nMgr = (NotificationManager) mContext.getSystemService(ns);
        nMgr.cancel(notifyId);
    }

    private void playStrayedFromPathAlert(double currentDistanceFromPath) {
        sendOffRouteNotification(currentDistanceFromPath);
        //Toast.makeText(mContext, "Strayed from path", Toast.LENGTH_SHORT).show();
        //todo: display this in the map view and change color of path.  remove the toast.
        Log.d(Constants.TRAILBOOK_TAG, "Strayed");
    }

    private void sendOffRouteNotification(double currentDistanceFromPath) {
        String notificationContent = String.format(mContext.getResources().getString(R.string.strayed_notification_content), PreferenceUtilities.getDistString(mContext, currentDistanceFromPath));
        mOffRouteNotifyBuilder.setContentText(notificationContent);
        updateOffRouteRingtone();
        mNotificationManager.notify(
                OFF_ROUTE_NOTIFICATION_ID,
                mOffRouteNotifyBuilder.build());
    }

    private void sendApproachingNoteNotification(Note note, double distance) {
        int notificationId = getNotificationId(note.getNoteID());
        Log.d(Constants.TRAILBOOK_TAG, "PathFollowerLocationProcessor: notificationid" + notificationId);
        Log.d(Constants.TRAILBOOK_TAG, "PathFollowerLocationProcessor: title: " + mContext.getString(R.string.note_notification_title));
        String notificationContent = String.format(mContext.getString(R.string.note_notification_title), PreferenceUtilities.getDistString(mContext, distance));
        mApproachingNoteNotificationBuilder.setContentTitle(notificationContent);
        mApproachingNoteNotificationBuilder.setContentText(note.getNoteContent());
        updateApproachingNoteRingtone();
        Log.d(Constants.TRAILBOOK_TAG,  "PathFollowerLocationProcessor: notification text: " + PreferenceUtilities.getDistString(mContext, distance) + ": " + note.getNoteContent());

        mNotificationManager.notify(
                notificationId,
                mApproachingNoteNotificationBuilder.build());
        Log.d(Constants.TRAILBOOK_TAG, "PathFollowerLocationProcessor: sent approaching note notification.");
    }

    private int getNotificationId(String noteID) {
        int id;
        //the last 9 digits should be safe to cast as an int
        try {
            String noteIdTrunc;
            if (noteID.length() > 9)
                noteIdTrunc = noteID.substring(noteID.length() - 9, noteID.length());
            else
                noteIdTrunc = noteID.replace('-', '0');

            Log.d(Constants.TRAILBOOK_TAG, "PathFollowerLocationProcessor: notification id for note " + noteID + ":" + noteIdTrunc);
            id = Integer.parseInt(noteIdTrunc);
        } catch (Exception e) {
            Log.d(Constants.TRAILBOOK_TAG, "error getting note id.", e);
            id = 10;
        }
        return id;
    }

    private boolean hasAlertBeenPlayedRecently() {
        long currentTime = new Date().getTime();
        long deltaMilliSeconds = currentTime - mStrayFromPathAlertLastPlayedTime;
        Log.d(Constants.TRAILBOOK_TAG, "Alert last played " + deltaMilliSeconds + " milliseconds ago.");
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
        mApproachingNoteNotificationBuilder.setSound(getApproachingNoteSoundURI());
    }

    public void removeAllNotifications() {
        mNotificationManager.cancelAll();
    }
}
