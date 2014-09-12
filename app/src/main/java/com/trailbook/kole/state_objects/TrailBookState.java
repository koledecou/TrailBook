package com.trailbook.kole.state_objects;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.maps.model.LatLngBounds;
import com.google.gson.Gson;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.trailbook.kole.activities.TrailBookActivity;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.events.LocationChangedEvent;
import com.trailbook.kole.helpers.ApplicationUtils;

/**
 * Created by kole on 9/8/2014.
 */
public class TrailBookState extends Application {
    public static final int MODE_SEARCH = 1;
    public static final int MODE_LEAD = 2;
    public static final int MODE_FOLLOW = 3;
    public static final String SAVED_MODE = "SAVED_MODE";
    private static final String SAVED_LOCATION = "SAVED_LOCATION";
    private static final String SAVED_ACTIVE_SEGMENT = "SAVED_ACTIVE_SEGMENT";
    private static final String SAVED_ACTIVE_PATH = "SAVED_ACTIVE_PATH";
    private static final String CLOUD_REFRESH_TS = "CLOUD_REFRESH_TS";

    private static int mMode = MODE_SEARCH;
    private static String mCurrentPathId;
    private static String mCurrentSegmentId;
    private static Location mCurrentLocation;
    private static SharedPreferences prefs;
    private static long mLastRefreshedFromCloudTimeStamp;
    private Bus bus;

    private static TrailBookState INSTANCE;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(Constants.TRAILBOOK_TAG, "TrailBookState: TrailBookState.onCreate()");
        INSTANCE = this;
        bus = BusProvider.getInstance();
        bus.register(this);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    public static TrailBookState getInstance(){
        return INSTANCE;
    }

    public static void setMode(int mode) {
        TrailBookState.mMode = mode;
        saveMode();
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
}
