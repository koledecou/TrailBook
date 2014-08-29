package com.trailbook.kole.worker_fragments;



import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.app.Fragment;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.squareup.otto.Bus;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.events.LocationChangedEvent;
import com.trailbook.kole.events.LocationServiceDisconnectedEvent;
import com.trailbook.kole.tools.BusProvider;

public class LocationServicesFragment extends Fragment implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        LocationListener {

    private Location mCurrentLocation;

    public interface LocationProcessor {
        public void process(Location newLocation);
    }

    private static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 1;
    private static final String WAIT_LOCK_ID = "TRAILBOOK_WAITLOCK";
    private Bus mBus = BusProvider.getInstance();

    private static final int MILLISECONDS_PER_SECOND = 1000;
    public static final int UPDATE_INTERVAL_IN_SECONDS = 0;
    private static final long UPDATE_INTERVAL = MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;
    private static final int FASTEST_INTERVAL_IN_SECONDS = 0;
    private static final long FASTEST_INTERVAL = MILLISECONDS_PER_SECOND * FASTEST_INTERVAL_IN_SECONDS;
    private LocationRequest mLocationRequest;
    private LocationClient mLocationClient;
    private SharedPreferences mPrefs;
    private Activity mParent;
    private SharedPreferences.Editor mEditor;
    private boolean mUpdatesRequested;
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;
    private LocationProcessor mLocationProcessor;

    public LocationServicesFragment() {
        super();

        mBus = BusProvider.getInstance();
        mBus.register(this);
    }

    /**
     * Fragment initialization. We want to be retained.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
        setRetainInstance(true);
        mPowerManager = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
        mParent = getActivity();
        initializeSharedPreferences();
        
        if (!userHasDisabledGPSLocationServices())
            promptToEnableLocationServices();

        initializeLocationClient();
    }

    private void initializeSharedPreferences() {
        mPrefs = mParent.getSharedPreferences("SharedPreferences",
                Context.MODE_PRIVATE);
        mEditor = mPrefs.edit();
    }

    private boolean userHasDisabledGPSLocationServices() {
        LocationManager manager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            return false;
        else
            return true;
    }

    private void promptToEnableLocationServices() {
        Toast.makeText(getActivity(), "Enable location services for accurate data", Toast.LENGTH_SHORT).show();
    }

    private void initializeLocationClient() {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

        mLocationClient = new LocationClient(mParent, this, this);
        mLocationClient.connect();
        // Start with updates turned off
        mUpdatesRequested = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mPrefs.contains("UPDATES_ON")) {
            setUpdatesToSavedValue();
        } else {
            stopUpdates();
        }
    }

    private void setUpdatesToSavedValue() {
        mUpdatesRequested =
                mPrefs.getBoolean("UPDATES_ON", false);
    }

    /**
     * This is called when the fragment is going away.  It is NOT called
     * when the fragment is being propagated between activity instances.
     */
    @Override
    public void onDestroy() {
        mBus.unregister(this);
        stopUpdates();

        super.onDestroy();
    }

    /**
     * This is called right before the fragment is detached from its
     * current activity instance.
     */
    @Override
    public void onDetach() {
        saveUpdateState(mUpdatesRequested);
        super.onDetach();
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (mUpdatesRequested)
            mLocationClient.requestLocationUpdates(mLocationRequest, this);
    }

    @Override
    public void onDisconnected() {
        mBus.post(new LocationServiceDisconnectedEvent());
    }

    @Override
    public void onLocationChanged(Location l) {
        Log.d(Constants.TRAILBOOK_TAG, "location changed: " + l.toString());
        mCurrentLocation = l;
        if (mLocationProcessor != null)
            mLocationProcessor.process(l);

        mBus.post(new LocationChangedEvent(l));
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        mParent,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                * Thrown if Google Play services canceled the original
                * PendingIntent
                */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            Toast.makeText(mParent, "Can't connect to location services.", Toast.LENGTH_SHORT).show();
        }
    }

    public void startUpdates(LocationProcessor locationProcessor) {
        mLocationProcessor = locationProcessor;
        saveUpdateState(true);
        mLocationClient.requestLocationUpdates(mLocationRequest, this);
        keepListeningWhileAsleep();
    }

    private void saveUpdateState(boolean flag) {
        mEditor.putBoolean("UPDATES_ON", flag);
        mEditor.commit();
    }

    private void keepListeningWhileAsleep() {
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAIT_LOCK_ID);
        Log.d(Constants.TRAILBOOK_TAG, "Aquiring lock");
    }

    public void stopUpdates() {
        Log.d(Constants.TRAILBOOK_TAG, "stopping location updates");
        mUpdatesRequested = false;
        saveUpdateState(false);
        if (mLocationClient != null && mLocationClient.isConnected())
            mLocationClient.removeLocationUpdates(this);

        dontListeningWhileAsleep();
    }

    private void dontListeningWhileAsleep() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            Toast.makeText(getActivity(), "releasing waitlock", Toast.LENGTH_SHORT).show();
            mWakeLock.release();
            Log.d(Constants.TRAILBOOK_TAG, "Releasing lock");
        }
    }
}
