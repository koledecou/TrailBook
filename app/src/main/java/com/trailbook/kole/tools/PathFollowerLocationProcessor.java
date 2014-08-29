package com.trailbook.kole.tools;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.location.Location;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.squareup.otto.Produce;
import com.trailbook.kole.activities.R;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.Path;
import com.trailbook.kole.worker_fragments.LocationServicesFragment;

import java.util.Date;

/**
 * Created by kole on 7/19/2014.
 */
public class PathFollowerLocationProcessor implements LocationServicesFragment.LocationProcessor {
    private static final int OFF_ROUTE_NOTIFICATION_ID = 1;
    private static final long ONE_MINUTE = 60000;
    private final NotificationCompat.Builder mNotifyBuilder;
    NotificationManager mNotificationManager;
    Location mCurrentLocation;

    Path mPath;
    long mStrayFromPathAlertLastPlayedTime = 0;
    double mCurrentDistanceFromPath = 0;
    Context mContext;

    public PathFollowerLocationProcessor(String pathId, Context context) {
        mPath=PathManager.getInstance().getPath(pathId);
        mContext=context;

        mNotifyBuilder = new NotificationCompat.Builder(mContext)
                .setSmallIcon(R.drawable.trail_book_logo)
                .setContentTitle("Off Route Notification")
                .setContentText("You are off route.")
                .setSound(getSoundURI());
        mNotificationManager = (NotificationManager)
                mContext.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private Uri getSoundURI() {
        Uri alertUri;
        try {
            String ringTone = PreferenceManager.getDefaultSharedPreferences(mContext).getString("off_route_alert", null);
            alertUri = Uri.parse(ringTone);
        } catch (Exception e) {
            Log.d("RingToneException", "Exception getting ring tone!", e);
            alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        return alertUri;
    }

    @Override
    public void process(Location newLocation) {
        Log.d(Constants.TRAILBOOK_TAG, "following " + mPath.getId() + " "  + newLocation.toString() );
        mCurrentDistanceFromPath = TrailbookPathUtilities.getNearestDistanceFromPointToPath(TrailbookPathUtilities.locationToLatLon(newLocation), mPath);
        //TODO: move to shared preferences
        if (mCurrentDistanceFromPath > 50 ) {
            alertStrayedFromPath();
        }
    }

    private void alertStrayedFromPath() {
        if (!hasAlertBeenPlayedRecently())
            playAlert();

    }

    private void playAlert() {
        mStrayFromPathAlertLastPlayedTime = new Date().getTime();
                /*
        String ringTone = PreferenceManager.getDefaultSharedPreferences(mContext).getString("off_route_alert", null);
        try {
            Uri alertUri = Uri.parse(ringTone);
            Ringtone rt = RingtoneManager.getRingtone(mContext, alertUri);

            rt.play();
        } catch (Exception e) {
            Log.v("RingToneException", "Exception playing ring tone!");
        }
        */
        sendOffRouteNotification();
        Toast.makeText(mContext, "Strayed from path", Toast.LENGTH_SHORT).show();
        Log.d(Constants.TRAILBOOK_TAG, "Strayed");
    }

    private void sendOffRouteNotification() {
        mNotifyBuilder.setContentText("distance to path: " + String.format("%.0f", mCurrentDistanceFromPath) + " meters.");

        mNotificationManager.notify(
                OFF_ROUTE_NOTIFICATION_ID,
                mNotifyBuilder.build());
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
}
