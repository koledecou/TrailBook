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
import android.view.ViewConfiguration;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.trailbook.kole.activities.R;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.Path;
import com.trailbook.kole.data.PathSegment;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.data.User;
import com.trailbook.kole.events.LocationChangedEvent;
import com.trailbook.kole.events.MapObjectAddedEvent;
import com.trailbook.kole.events.ModeChangedEvent;
import com.trailbook.kole.events.PathSummaryAddedEvent;
import com.trailbook.kole.events.SegmentUpdatedEvent;
import com.trailbook.kole.fragments.TrailBookMapFragment;
import com.trailbook.kole.helpers.ApplicationUtils;
import com.trailbook.kole.location_processors.BackgroundLocationService;
import com.trailbook.kole.location_processors.LocationProcessor;
import com.trailbook.kole.location_processors.PathLeaderLocationProcessor;
import com.trailbook.kole.location_processors.TrailBookLocationReceiver;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import java.lang.reflect.Field;


@ReportsCrashes(formKey = "", // will not be used
        mailTo = "kole.decou@gmail.com",
        mode = ReportingInteractionMode.TOAST,
        resToastText = R.string.crash_toast_text)

public class TrailBookState extends Application {
    public static final int MODE_SEARCH = 1;
    public static final int MODE_LEAD = 2;
    public static final int MODE_FOLLOW = 3;
    public static final int MODE_EDIT = 4;

    public static final String SAVED_MODE = "SAVED_MODE";
    private static final String SAVED_LOCATION = "SAVED_LOCATION";
    private static final String SAVED_ACTIVE_SEGMENT = "SAVED_ACTIVE_SEGMENT";
    private static final String SAVED_ACTIVE_PATH = "SAVED_ACTIVE_PATH";
    private static final String SAVED_USER_ID = "SAVED_USER_ID";
    private static final String CLOUD_REFRESH_TS = "CLOUD_REFRESH_TS";
    private static final String SAVED_USER_NAME = "SAVED_USER_NAME" ;
    private static final String SAVED_PROFILE_PIC_URL = "SAVED_USER_PROFILE_PIC";
    private static final String SAVED_GOOD_TO_SAVE_LOCATIONS = "SAVED_GOOD_TO_SAVE_LOCATIONS";
    public static final String NO_START_PATH = "NONE";
    private static final String ZOOM_TO_ID = "ZOOM_TO_ID";
    private static final String SAVED_ZOOM_LEVEL = "ZOOM";
    private static final String SAVED_SELECTED_LOCATION = "SELECTED_LOC";
    private static final String SAVED_MAP_CENTER = "MAP_CENTER";

    private static int mMode = MODE_SEARCH;
    private static String mCurrentPathId;
    private static String mCurrentSegmentId;
    private static Location mCurrentLocation;
    private static PathManager mPathManager;
    private static SharedPreferences prefs;
    private static long mLastRefreshedFromCloudTimeStamp = 0;
    private static String mZoomToPathId = NO_START_PATH;
    private static LocationProcessor locationProcessor;
    private static User mUser;
    private static int mConsecutiveGoodLocations = 0;
    private static boolean mAlreadyGotEnoughGoodLocations = false;
    private Bus bus;

    private static boolean mIsListeningToLocationUpdates = false;

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
        ACRA.init(this);
        forceOverflowMenu();
    }

    private void forceOverflowMenu() {
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if(menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception ex) {
            // Ignore
        }
    }

    public static TrailBookState getInstance(){
        return INSTANCE;
    }

    public static void setMode(int mode) {
        TrailBookState.mMode = mode;
        saveMode();
    }

    public static void setZoomToPathId(String zoomToPathId) {
        SharedPreferences.Editor editor = prefs.edit();
        Log.d(Constants.TRAILBOOK_TAG, "TrailBookState: setting zoom to path id: " + zoomToPathId);
        editor.putString(ZOOM_TO_ID, zoomToPathId);
        editor.commit();
        //TrailBookState.mZoomToPathId = zoomToPathId;
    }

    public static void resetZoomToPathId() {
        SharedPreferences.Editor editor = prefs.edit();
        Log.d(Constants.TRAILBOOK_TAG, "TrailBookState: setting zoom to path id none: " + NO_START_PATH);
        editor.putString(ZOOM_TO_ID, NO_START_PATH);
        editor.commit();
        //TrailBookState.mZoomToPathId = NO_START_PATH;
    }

    public static String getZoomToPathId() {
        return prefs.getString(ZOOM_TO_ID, NO_START_PATH);
        //return mZoomToPathId;
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

        mAlreadyGotEnoughGoodLocations = prefs.getBoolean(SAVED_GOOD_TO_SAVE_LOCATIONS, false);
    }

    public static void setCurrentLocation(Location l) {
        mCurrentLocation = l;
    }

    @Subscribe
    public void onLocationChangedEvent(LocationChangedEvent event){
        mCurrentLocation = event.getLocation();
        if (!mAlreadyGotEnoughGoodLocations) {
            updateIsReadyToRecordLocationsState(event.getLocation());
        }

        SharedPreferences.Editor editor = prefs.edit();
        String jsonLocation = getJsonLocation(mCurrentLocation);
        //Log.d(Constants.TRAILBOOK_TAG, "TrailBookState: saving json location," + jsonLocation);
        editor.putString(SAVED_LOCATION, jsonLocation);
        editor.commit();
    }

    private static String getJsonFromPoint(LatLng point) {
        Gson gson = new Gson();
        String jsonCenter = gson.toJson(point);
        return jsonCenter;
    }

    public static LatLng getMapCenterPoint() {
        Gson gson = new Gson();
        String defaultCenterPointJson = gson.toJson(TrailBookMapFragment.DEFAULT_MAP_CENTER, LatLng.class);
        String centerPointJson = prefs.getString(SAVED_MAP_CENTER, defaultCenterPointJson);
        LatLng center = gson.fromJson(centerPointJson, LatLng.class);
        Log.d(Constants.TRAILBOOK_TAG, "TrailbookState: got center," + center);
        return center;
    }

    public static void saveMapCenterPoint(LatLng center) {
        SharedPreferences.Editor editor = prefs.edit();
        String jsonCenter = getJsonFromPoint(center);
        editor.putString(SAVED_MAP_CENTER, jsonCenter);
        editor.commit();
        Log.d(Constants.TRAILBOOK_TAG, "TrailbookState: Saved center " + jsonCenter);
    }

    public static float getMapZoomLevel() {
        float zoomLevel = prefs.getFloat(SAVED_ZOOM_LEVEL, TrailBookMapFragment.DEFAULT_ZOOM_LEVEL);
        return zoomLevel;
    }

    public static LatLng getSelectedMapLocation() {
        Gson gson = new Gson();
        String jsonSelectedLocation = prefs.getString(SAVED_SELECTED_LOCATION, null);
        if (jsonSelectedLocation != null && TrailBookState.getMode()==TrailBookState.MODE_EDIT) {
            return gson.fromJson(jsonSelectedLocation, LatLng.class);
        } else {
            return  null;
        }
    }

    public static void saveSelectedMapLocation(LatLng selectedLoc) {
        SharedPreferences.Editor editor = prefs.edit();
        String jsonSelectedLocation = getJsonFromPoint(selectedLoc);
        editor.putString(SAVED_SELECTED_LOCATION, jsonSelectedLocation);
        editor.commit();
    }

    public static void saveMapZoomLevel(float zoomLevel) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat(SAVED_ZOOM_LEVEL, zoomLevel);
        Log.d(Constants.TRAILBOOK_TAG, "TrailBookMapFragment: Saved zoom level " + zoomLevel);
        editor.commit();
    }

    public void updateIsReadyToRecordLocationsState(Location location) {
        if (TrailBookState.alreadyGotEnoughGoodLocations())
            return;

        if (location.hasAccuracy()) {
            float accuracy = location.getAccuracy();
            if (accuracy < Constants.MIN_ACCURACY_TO_START_LEADING) {
                incrementConsecutiveGoodLocations();
                Log.d(Constants.TRAILBOOK_TAG, "TrailbookPathUtilities.isAccurateEnoughToRecordUpdateEvents: got good location: " + location.getAccuracy());
                Log.d(Constants.TRAILBOOK_TAG, "TrailbookPathUtilities.isAccurateEnoughToRecordUpdateEvents: consecutive good locations: " + TrailBookState.getConsecutiveGoodLocations());
                if (TrailBookState.getConsecutiveGoodLocations() >= Constants.MIN_CONNSECUTIVE_GOOD_LOCATIONS_TO_LEAD) {
                    setAlreadGotEnoughGoodLocations(true);
                } else
                    setAlreadGotEnoughGoodLocations(false);
            } else {
                Log.d(Constants.TRAILBOOK_TAG, "TrailbookPathUtilities.isAccurateEnoughToRecordUpdateEvents: location not accurate enough: " + location.getAccuracy());
                TrailBookState.resetConsecutiveGoodLocations();
            }
        } else {
            Log.d(Constants.TRAILBOOK_TAG, "TrailbookPathUtilities.isAccurateEnoughToRecordUpdateEvents: no accuracy on device");
            setAlreadGotEnoughGoodLocations(true);
        }
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

    private static void restoreUser() {
        mUser = new User();
        mUser.userId = prefs.getString(SAVED_USER_ID, "-1");
        mUser.userName = prefs.getString(SAVED_USER_NAME, "");
        mUser.profilePhotoUrl = prefs.getString(SAVED_PROFILE_PIC_URL, "");
        Log.d(Constants.TRAILBOOK_TAG, "TrailBookState: restored user id," + mUser.userId);
    }

    public static void restoreState() {
        restoreUser();
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
        mIsListeningToLocationUpdates = true;
    }

    public void stopLocationUpdates() {
        disableLocationReceiver();
        ComponentName comp = new ComponentName(this.getPackageName(), BackgroundLocationService.class.getName());
        stopService(new Intent().setComponent(comp));
        mIsListeningToLocationUpdates = false;
        resetConsecutiveGoodLocations();
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
        removeAllNotificaions();
        if (locationProcessor != null) {
            setLocationProcessor(null);
        }

        setMode(MODE_SEARCH);
        bus.post(new ModeChangedEvent(oldMode, MODE_SEARCH));
    }

    public void removeAllNotificaions() {
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
        removeAllNotificaions();
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

    public static User getCurrentUser() {
        return mUser;
    }

    public void setUser(User user) {
        mUser = user;
        SharedPreferences.Editor editor = prefs.edit();
        Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ": saving user id " + mUser.userId);
        editor.putString(SAVED_USER_ID, mUser.userId);
        editor.putString(SAVED_USER_NAME, mUser.userName);
        editor.putString(SAVED_PROFILE_PIC_URL, mUser.profilePhotoUrl);
        editor.commit();
    }

    public static boolean isListeningToLocationUpdates() {
        return mIsListeningToLocationUpdates;
    }

    public static void incrementConsecutiveGoodLocations() {
        mConsecutiveGoodLocations++;
    }

    public static int getConsecutiveGoodLocations() {
        return mConsecutiveGoodLocations;
    }

    public static void resetConsecutiveGoodLocations() {
        mConsecutiveGoodLocations = 0;
        TrailBookState.getInstance().setAlreadGotEnoughGoodLocations(false);
    }

    public static boolean alreadyGotEnoughGoodLocations() {
        return mAlreadyGotEnoughGoodLocations;
    }

    public void setAlreadGotEnoughGoodLocations(boolean gotEnough) {
        this.mAlreadyGotEnoughGoodLocations = gotEnough;

        SharedPreferences.Editor editor = prefs.edit();
        Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ":setAlreadGotEnoughGoodLocations " + gotEnough);
        editor.putBoolean(SAVED_GOOD_TO_SAVE_LOCATIONS, gotEnough);
        editor.commit();
    }
}
