package com.trailbook.kole.location_processors;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.trailbook.kole.data.Constants;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * BackgroundLocationService used for tracking user location in the background.
 *
 * @author cblack
 */
public class BackgroundLocationService extends Service implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    IBinder mBinder = new LocalBinder();

    private LocationClient mLocationClient;
    private LocationRequest mLocationRequest;
    // Flag that indicates if a request is underway.
    private boolean mInProgress;

    private Boolean servicesAvailable = false;

    public class LocalBinder extends Binder {
        public BackgroundLocationService getServerInstance() {
            return BackgroundLocationService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();


        mInProgress = false;
        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(Constants.UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(Constants.FASTEST_INTERVAL);

        servicesAvailable = servicesConnected();

        /*
         * Create a new location client, using the enclosing class to
         * handle callbacks.
         */
        mLocationClient = new LocationClient(this, this, this);
    }

    private boolean servicesConnected() {
        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            return true;
        } else {
            return false;
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        boolean isStopCommand = false;
        boolean isPauseCommand = false;
        if (intent != null) {
            isStopCommand = intent.getBooleanExtra(NotificationUtils.EXTRA_STOP, false);
            isPauseCommand = intent.getBooleanExtra(NotificationUtils.EXTRA_PAUSE, false);
        }

        Log.d(Constants.TRAILBOOK_TAG, "BackgroundLocationService: " + (isStopCommand ? "Stop" : "Start") + " Command");
        if (isStopCommand || isPauseCommand) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (!servicesAvailable || mLocationClient.isConnected() || mInProgress) {
            Log.d(Constants.TRAILBOOK_TAG, "BackgroundLocationService: Already connected.");
            return START_STICKY;
        }

        setUpLocationClientIfNeeded();
        if (!mLocationClient.isConnected() || !mLocationClient.isConnecting() && !mInProgress) {
            Log.d(Constants.TRAILBOOK_TAG, "BackgroundLocationService: Connecting to location client");
//            appendLog(DateFormat.getDateTimeInstance().format(new Date()) + ": Started", Constants.LOG_FILE);
            mInProgress = true;
            mLocationClient.connect();
        }

        return START_STICKY;
    }

    /*
     * Create a new location client, using the enclosing class to
     * handle callbacks.
     */
    private void setUpLocationClientIfNeeded() {
        if (mLocationClient == null)
            mLocationClient = new LocationClient(this, this, this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public String getTime() {
        SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return mDateFormat.format(new Date());
    }

    @Override
    public void onDestroy() {
        disconnectClient();

        Log.d(Constants.TRAILBOOK_TAG, "BackgroundLocationService: Stopped");
        super.onDestroy();
    }


    /*
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    @Override
    public void onConnected(Bundle bundle) {
        PendingIntent locationIntent = getLocationReceiverPendingIntent();
        Log.d(Constants.TRAILBOOK_TAG, "BackgroundLocationService: registering th location reciever");
        mLocationClient.requestLocationUpdates(mLocationRequest, locationIntent);
    }

    private PendingIntent getLocationReceiverPendingIntent() {
        Intent intent = new Intent(this, TrailBookLocationReceiver.class);
        return PendingIntent.getBroadcast(getApplicationContext(), 14872, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    /*
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
    @Override
    public void onDisconnected() {
        disconnectClient();
    }

    private void disconnectClient() {
        mInProgress = false;
        if (mLocationClient != null && mLocationClient.isConnected()) {
            PendingIntent locationIntent = getLocationReceiverPendingIntent();
            Log.d(Constants.TRAILBOOK_TAG, "BackgroundLocationService: removing location updates.");
            mLocationClient.removeLocationUpdates(locationIntent);
        }

        if (servicesAvailable && mLocationClient != null) {
            // Destroy the current location client
            if (mLocationClient.isConnected()) {
                Log.d(Constants.TRAILBOOK_TAG, "BackgroundLocationService: client disconnected.");
                mLocationClient.disconnect();
            }

            mLocationClient = null;
        }
    }

    /*
     * Called by Location Services if the attempt to
     * Location Services fails.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        mInProgress = false;
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            // If no resolution is available, display an error dialog
        } else {

        }
    }


}