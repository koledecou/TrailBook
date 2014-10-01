package com.trailbook.kole.state_objects;

import android.app.Application;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.Path;
import com.trailbook.kole.data.PathSegment;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.events.LocationChangedEvent;
import com.trailbook.kole.events.MapObjectAddedEvent;
import com.trailbook.kole.events.ModeChangedEvent;
import com.trailbook.kole.events.PathSummaryAddedEvent;
import com.trailbook.kole.events.SegmentUpdatedEvent;
import com.trailbook.kole.helpers.ApplicationUtils;
import com.trailbook.kole.location_processors.BackgroundLocationService;
import com.trailbook.kole.location_processors.LocationProcessor;
import com.trailbook.kole.location_processors.PathLeaderLocationProcessor;
import com.trailbook.kole.location_processors.TrailBookLocationReceiver;

/**
 * Created by kole on 9/8/2014.
 */
public class TrailBookState extends Application {
    public static final int MODE_SEARCH = 1;
    public static final int MODE_LEAD = 2;
    public static final int MODE_FOLLOW = 3;
    public static final int MODE_EDIT = 4;

    public static final String SAVED_MODE = "SAVED_MODE";
    private static final String SAVED_LOCATION = "SAVED_LOCATION";
    private static final String SAVED_ACTIVE_SEGMENT = "SAVED_ACTIVE_SEGMENT";
    private static final String SAVED_ACTIVE_PATH = "SAVED_ACTIVE_PATH";
    private static final String CLOUD_REFRESH_TS = "CLOUD_REFRESH_TS";

    private static int mMode = MODE_SEARCH;
    private static String mCurrentPathId;
    private static String mCurrentSegmentId;
    private static Location mCurrentLocation;
    private static PathManager mPathManager;
    private static SharedPreferences prefs;
    private static long mLastRefreshedFromCloudTimeStamp;
    private static LocationProcessor locationProcessor;
    private Bus bus;

    private static TrailBookState INSTANCE;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(Constants.TRAILBOOK_TAG, "TrailBookState: TrailBookState.onCreate()");
        INSTANCE = this;
        bus = BusProvider.getInstance();
        bus.register(this);

        mPathManager = PathManager.getInstance();

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    public static TrailBookState getInstance(){
        return INSTANCE;
    }

    public static void setMode(int mode) {
        TrailBookState.mMode = mode;
        saveMode();
    }

    public static PathManager getPathManager() {
        return mPathManager;
    }

    public static long getLastRefreshedFromCloudTimeStamp() {
        return mLastRefreshedFromCloudTimeStamp;
    }

    public static void resetLastRefreshedFromCloudTimeStamp() {
        mLastRefreshedFromCloudTimeStamp = ApplicationUtils.getCurrentTimeStamp();
        saveLastRefreshedFromCloudTimeStamp();
    }

    private static void saveLastRefreshedFromCloudTimeStamp() {
        SharedPreferences.Editor editor = prefs.edit();
        Log.d(Constants.TRAILBOOK_TAG, "TrailBookState: saving time," + mLastRefreshedFromCloudTimeStamp);
        editor.putLong(CLOUD_REFRESH_TS, mLastRefreshedFromCloudTimeStamp);
        editor.commit();
    }

    private static void restoreLastRefreshedFromCloudTimeStamp() {
        mLastRefreshedFromCloudTimeStamp = prefs.getLong(CLOUD_REFRESH_TS, 0l);
        Log.d(Constants.TRAILBOOK_TAG, "TrailBookState: restored time," + mLastRefreshedFromCloudTimeStamp);
    }

    private static void saveMode() {
        SharedPreferences.Editor editor = prefs.edit();
        Log.d(Constants.TRAILBOOK_TAG, "TrailBookState: saving mode," + mMode);
        editor.putInt(SAVED_MODE, mMode);
        editor.commit();
    }

    public static int getMode() {
        return mMode;
    }

    public Context getContext() {
        return this;
    }

    public static String getActivePathId() {
        return mCurrentPathId;
    }

    public static void setActivePathId(String currentPathId) {
        mCurrentPathId = currentPathId;
        saveActivePathId();
    }

    public static String getActiveSegmentId() {
        return mCurrentSegmentId;
    }

    public static void setActiveSegmentId(String currentSegmentId) {
        mCurrentSegmentId = currentSegmentId;
        saveActiveSegmentId();
    }

    public static void saveActiveSegmentId() {
        SharedPreferences.Editor editor = prefs.edit();
        Log.d(Constants.TRAILBOOK_TAG, "TrailBookState: saving active segment," + mCurrentSegmentId);
        editor.putString(SAVED_ACTIVE_SEGMENT, mCurrentSegmentId);
        editor.commit();
    }

    public void restoreActivePath() {
        Log.d(Constants.TRAILBOOK_TAG, "TrailBookState: current path is " + mPathManager.getPathSummary(mCurrentPathId));
        if (mPathManager.getPathSummary(mCurrentPathId) == null) {
            Log.d(Constants.TRAILBOOK_TAG, "TrailBookState: re-loading active path: " + mCurrentPathId);
            //doing this sycronously because it is critical that we have the path in time.
            Path path = mPathManager.loadPathFromDevice( mCurrentPathId);
            postEventsForAddedPath(path);
        }
    }

    private void postEventsForAddedPath(Path p) {
        Log.d(Constants.TRAILBOOK_TAG, "TrailBookState: posting summary:" + p.summary.getName());
        Log.d(Constants.TRAILBOOK_TAG, "TrailBookState: posting segments:" + p.segments.size());
        Log.d(Constants.TRAILBOOK_TAG, "TrailBookState: posting paobjects:" + p.paObjects.size());
        bus.post(new PathSummaryAddedEvent(p.summary));
        for (PathSegment segment:p.segments) {
            bus.post(new SegmentUpdatedEvent(segment));
        }

        for (PointAttachedObject pao:p.paObjects) {
            bus.post(new MapObjectAddedEvent(pao));
        }
    }

    public static void restoreActiveSegmentId() {
        mCurrentSegmentId = prefs.getString(SAVED_ACTIVE_SEGMENT, null);
        Log.d(Constants.TRAILBOOK_TAG, "TrailBookState: restored active segment," + mCurrentSegmentId);
    }

    public static void saveActivePathId() {
        SharedPreferences.Editor editor = prefs.edit();
        Log.d(Constants.TRAILBOOK_TAG, "TrailBookState: saving active path," + mCurrentPathId);
        editor.putString(SAVED_ACTIVE_PATH, mCurrentPathId);
        editor.commit();
    }

    public static void restoreActivePathId() {
        mCurrentPathId = prefs.getString(SAVED_ACTIVE_PATH, null);
        Log.d(Constants.TRAILBOOK_TAG, "TrailBookState: restored active path," + mCurrentPathId);
    }


    public static Location getCurrentLocation() {
        restoreSavedLocIfNeeded();
        return mCurrentLocation;
    }

    private static void restoreSavedLocIfNeeded() {
        if (mCurrentLocation == null) {
            Gson gson = new Gson();
            String jsonLastLocation = prefs.getString(SAVED_LOCATION, null);
            if (jsonLastLocation != null) {
                Log.d(Constants.TRAILBOOK_TAG, "TrailBookState: restored json location," + jsonLastLocation);
                mCurrentLocation = gson.fromJson(jsonLastLocation, Location.class);
            }
        }
    }

    public static void setCurrentLocation(Location l) {
        mCurrentLocation = l;
    }

    @Subscribe
    public void onLocationChangedEvent(LocationChangedEvent event){
        mCurrentLocation = event.getLocation();

        SharedPreferences.Editor editor = prefs.edit();
        String jsonLocation = getJsonLocation(mCurrentLocation);
        Log.d(Constants.TRAILBOOK_TAG, "TrailBookState: saving json location," + jsonLocation);
        editor.putString(SAVED_LOCATION, jsonLocation);
        editor.commit();
    }

    private String getJsonLocation(Location l) {
        Gson gson = new Gson();
        String jsonLocation = gson.toJson(l);
        return jsonLocation;
    }

    public static void restoreMode() {
        mMode = prefs.getInt(SAVED_MODE, MODE_SEARCH);
        Log.d(Constants.TRAILBOOK_TAG, "TrailBookState: restored mode," + mMode);
    }

    public static void restoreState() {
        restoreActivePathId();
        restoreActiveSegmentId();

        restoreMode();
        restoreSavedLocIfNeeded();
        restoreLastRefreshedFromCloudTimeStamp();
    }

    public static void setLocationProcessor(LocationProcessor processor) {
        locationProcessor = processor;
    }

    public static LocationProcessor getLocationProcessor() {
        return locationProcessor;
    }

    public void startLocationUpdates() {
        Log.d(Constants.TRAILBOOK_TAG, "TrailBookState: startLocationUpdates()");
        ComponentName comp = new ComponentName(this.getPackageName(), BackgroundLocationService.class.getName());
        ComponentName service = startService(new Intent().setComponent(comp));
        enableLocationReceiver();

        if (null == service){
            // something really wrong here
            Log.e(Constants.TRAILBOOK_TAG, "Could not start service " + comp.toString());
        }
    }

    public void stopLocationUpdates() {
        disableLocationReceiver();
        ComponentName comp = new ComponentName(this.getPackageName(), BackgroundLocationService.class.getName());
        stopService(new Intent().setComponent(comp));
        removeAllNotificaions();
    }

    public void enableLocationReceiver(){
        ComponentName receiver = new ComponentName(this, TrailBookLocationReceiver.class);
        PackageManager pm = this.getPackageManager();
        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
        Log.d(Constants.TRAILBOOK_TAG, "enabled location reciever");
    }

    public void disableLocationReceiver(){
        ComponentName receiver = new ComponentName(this, TrailBookLocationReceiver.class);
        PackageManager pm = this.getPackageManager();
        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
        Log.d(Constants.TRAILBOOK_TAG, "disabled location reciever");
    }

    public void switchToSearchMode() {
        int oldMode = mMode;
        mCurrentPathId=null;
        mCurrentSegmentId=null;
        stopLocationUpdates();

        if (locationProcessor != null) {
            setLocationProcessor(null);
        }

        setMode(MODE_SEARCH);
        bus.post(new ModeChangedEvent(oldMode, MODE_SEARCH));
    }

    private void removeAllNotificaions() {
        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    public void switchToLeadMode(String pathId, String segmentId) {
        Log.d(Constants.TRAILBOOK_TAG, "TrailBookState: switchingToLeadMode " + pathId + "," + segmentId);
        int oldMode = mMode;
        mCurrentPathId=pathId;
        mCurrentSegmentId=segmentId;
        setLocationProcessor(new PathLeaderLocationProcessor(
                this,
                segmentId,
                pathId));

        startLocationUpdates();

        setMode(MODE_LEAD);
        bus.post(new ModeChangedEvent(oldMode, MODE_LEAD));
    }

    public void switchToEditMode(String pathId) {
        int oldMode = mMode;
        setActivePathId(pathId);
        setActiveSegmentId(null);
        stopLocationUpdates();
        if (locationProcessor != null) {
            setLocationProcessor(null);
        }

        setMode(MODE_EDIT);
        bus.post(new ModeChangedEvent(oldMode, MODE_EDIT));
    }

    public void resumeLeadingActivePath(boolean restoreState) {
        if (restoreState)
            restoreActivePath();

        switchToLeadMode(mCurrentPathId, mCurrentSegmentId);
    }

    public static String getCurrentUserId() {
        return "-1";
    }
}
