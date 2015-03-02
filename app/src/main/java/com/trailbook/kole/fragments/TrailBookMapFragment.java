package com.trailbook.kole.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.trailbook.kole.activities.R;
import com.trailbook.kole.activities.TrailBookActivity;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.PathSegment;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.events.FilterChangedEvent;
import com.trailbook.kole.events.LocationChangedEvent;
import com.trailbook.kole.events.MapObjectAddedEvent;
import com.trailbook.kole.events.ModeChangedEvent;
import com.trailbook.kole.events.PathDeletedEvent;
import com.trailbook.kole.events.PathDetailRequestEvent;
import com.trailbook.kole.events.PathReceivedEvent;
import com.trailbook.kole.events.PathSummariesReceivedFromCloudEvent;
import com.trailbook.kole.events.PathSummaryAddedEvent;
import com.trailbook.kole.events.PointAttachedObjectDeletedEvent;
import com.trailbook.kole.events.RefreshMessageEvent;
import com.trailbook.kole.events.SegmentDeletedEvent;
import com.trailbook.kole.events.SegmentUpdatedEvent;
import com.trailbook.kole.events.ZoomRequestEvent;
import com.trailbook.kole.fragments.point_attched_object_view.PointAttachedObjectView;
import com.trailbook.kole.helpers.ApplicationUtils;
import com.trailbook.kole.helpers.MapUtilities;
import com.trailbook.kole.helpers.NoteFactory;
import com.trailbook.kole.helpers.PreferenceUtilities;
import com.trailbook.kole.helpers.TrailbookPathUtilities;
import com.trailbook.kole.state_objects.BusProvider;
import com.trailbook.kole.state_objects.PathManager;
import com.trailbook.kole.state_objects.TrailBookState;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Set;

/**
 * Created by Fistik on 6/30/2014.
 */
public class TrailBookMapFragment extends MapFragment implements GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerDragListener, View.OnClickListener, GoogleMap.OnMapLoadedCallback, GoogleMap.OnMapLongClickListener {
    private static final String LOG_CLASS_NAME = "TrailBookMapFragment";
    private static final float THICK = 10;
    private static final float MEDIUM = 6;
    private static final String SAVED_ZOOM_LEVEL = "ZOOM";
    private static final String SAVED_SELECTED_LOCATION = "SELECTED_LOC";
    private static final String SAVED_MAP_CENTER = "MAP_CENTER";
    private static final String SAVED_MAP_LOADED_STATE = "MAP_LOADED";

    private static final float DEFAULT_ZOOM_LEVEL = 16;
    private static final LatLng DEFAULT_MAP_CENTER = new LatLng( 34.8326509,-111.7693473);
    private PathDetailsView mDetailView;

    private SharedPreferences savedStatePrefs;
    private LatLng mSelectedLocation = null;
    private boolean mMapLoaded = false;

    public boolean isMapLoaded() {
        if (mMap != null && mMapLoaded)
            return true;
        else
            return false;
    }

    public LatLngBounds getBounds() {
        if (mMap != null)
            return mMap.getProjection().getVisibleRegion().latLngBounds;
        else
            return null;
    }

    public void hideEndMarker(String pathId) {
        Marker endMarker = mEndMarkers.get(pathId);
        endMarker.setVisible(false);
    }

    public enum MarkerType {START,END,NOTE,POINT,UNKNOWN}

    private Bus bus;
    private LinkedList mEventQueue;
    private LinkedList mWaitingForViewEventQueue;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private float mZoomLevel = DEFAULT_ZOOM_LEVEL;
    private LatLng mCenterPoint = DEFAULT_MAP_CENTER;


    private PathManager mPathManager;
    private BidiMap<String, Marker> mStartMarkers;
    private BidiMap<String, Marker> mEndMarkers;
    private BidiMap<String, Polyline> mPathPolylines;
    private SlidingUpPanelLayout mSlidingPanel;
    private BidiMap<String,Marker> mPaoMarkers;
    private Marker mSelectedPointMarker;

    /**
     * Listener interface to tell when the map is ready
     */
    public static interface OnMapReadyListener {
        void onMapReady();
    }

    public TrailBookMapFragment() {
        super();
        createMapObjects();
        initializeBus();
        initializeQueues();
        initializePathManager();
        setArguments(new Bundle());
    }

/*    public void setParentActivity(TrailBookActivity parentActivity) {
        this.parentActivity = parentActivity;
    }*/

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUpMapIfNeeded();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(Constants.TRAILBOOK_TAG, "TrailBookMapFragment: onActivityCreated");
    }

    @Override
    public void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        setMapTypeToUserPreference();
    }

/*
    private void zoomToPath(String requestedPathId) {

        Log.d(Constants.TRAILBOOK_TAG, " requested zoom to " + requestedPathId);
        if (requestedPathId != null && !requestedPathId.equals(TrailBookState.NO_START_PATH)) {
            Log.d(Constants.TRAILBOOK_TAG, " zooming to " + requestedPathId);
            TrailBookState.resetZoomToPathId();
            ZoomRequestEvent event = new ZoomRequestEvent(requestedPathId);
            bus.post(event);
            showPathSummary(requestedPathId);
        }
    }
*/

    private void setMapTypeToUserPreference() {
        int mapPreference = PreferenceUtilities.getMapType(getActivity());
        Log.d(Constants.TRAILBOOK_TAG, LOG_CLASS_NAME + ": map preference:" + mapPreference);
        if (mMap != null) {
            mMap.setMapType(mapPreference);
        }
    }

    private void restoreState(Bundle savedInstanceState) {
        Log.d(Constants.TRAILBOOK_TAG, "restoring map");
        savedStatePrefs = getActivity().getPreferences(Context.MODE_PRIVATE);
        Gson gson = new Gson();
//deleteme        String startBoundsJson = gson.toJson(startBounds, LatLngBounds.class);
//        String jsonLastBounds = savedStatePrefs.getString(SAVED_LAT_LNG_BOUNDS_GSON, startBoundsJson);

        String defaultCenterPointJson = gson.toJson(DEFAULT_MAP_CENTER, LatLng.class);
        String centerPointJson = savedStatePrefs.getString(SAVED_MAP_CENTER, defaultCenterPointJson);
        mCenterPoint = gson.fromJson(centerPointJson, LatLng.class);
        Log.d(Constants.TRAILBOOK_TAG, "TrailBookMapFragment: restored center:" + mCenterPoint);

        mZoomLevel = savedStatePrefs.getFloat(SAVED_ZOOM_LEVEL, DEFAULT_ZOOM_LEVEL);
        Log.d(Constants.TRAILBOOK_TAG, LOG_CLASS_NAME + ": restored zoom level "+mZoomLevel);

        String jsonSelectedLocation = savedStatePrefs.getString(SAVED_SELECTED_LOCATION, null);
        if (jsonSelectedLocation != null && TrailBookState.getMode()==TrailBookState.MODE_EDIT) {
            mSelectedLocation = gson.fromJson(jsonSelectedLocation, LatLng.class);
        }

        mMapLoaded = savedStatePrefs.getBoolean(SAVED_MAP_LOADED_STATE, false);
    }

    public void refreshSegmentsForActivePath() {
        String pathId = TrailBookState.getActivePathId();
        ArrayList<PathSegment> segments = mPathManager.getSegmentsForPath(pathId);
        for (PathSegment s:segments) {
            addSegmentToMap(s);
        }
    }

    @Override
    public void onMapLoaded() {
        mMapLoaded = true;

        drawLoadedPaths();
        processQueuedEvents();
        if (TrailBookState.getMode() == TrailBookState.MODE_EDIT && mSelectedLocation != null)
            putSelectedMarkerOnMap(mSelectedLocation);

        displayMessage();
//        Toast.makeText(TrailBookState.getInstance(), "TrailBookMapFragment.onMapLoaded()", Toast.LENGTH_SHORT).show();
    }

    private void displayMessage() {
        if (TrailBookState.getMode() == TrailBookState.MODE_EDIT) {
            PathSummary summary = mPathManager.getPathSummary(TrailBookState.getActivePathId());
            String message = String.format(getString(R.string.editing_message), summary.getName());
            displayMessage(message);
        } else if (TrailBookState.getMode() == TrailBookState.MODE_LEAD) {
            String message;
            if (TrailBookState.isListeningToLocationUpdates()) {
                PathSummary summary = mPathManager.getPathSummary(TrailBookState.getActivePathId());
                message = String.format(getString(R.string.leading_message), summary.getName());
            } else {
                message = getString(R.string.location_updates_paused);
            }
            displayMessage(message);
        } else if (TrailBookState.getMode() == TrailBookState.MODE_FOLLOW) {
            PathSummary summary = mPathManager.getPathSummary(TrailBookState.getActivePathId());
            String message = String.format(getString(R.string.following_message), summary.getName());
            displayMessage(message);
        } else {
            hideMapMessage();
        }
    }

    public void displayMessage(String message) {
        TextView tvMapMessage = (TextView)(getActivity().findViewById(R.id.map_tv_message));
        if (tvMapMessage == null) {
            return;
        }
        tvMapMessage.setVisibility(View.VISIBLE);
        tvMapMessage.setText(message);
    }

    private void createMapObjects() {
        mStartMarkers = new DualHashBidiMap<String, Marker>();
        mEndMarkers = new DualHashBidiMap<String, Marker>();
        mPaoMarkers = new DualHashBidiMap<String, Marker>();
        mPathPolylines = new DualHashBidiMap<String, Polyline>();
    }

    private void changePathToSelected(String pathId) {
        BitmapDescriptor bmp;
        Marker end = mEndMarkers.get(pathId);
        if (mPathManager.isStoredLocally(pathId))
            bmp = BitmapDescriptorFactory.fromResource(R.drawable.path_marker_selected);
        else
            bmp = BitmapDescriptorFactory.fromResource(R.drawable.path_marker_selected_cloud);

        if (end != null) {
            end.setIcon(bmp);
        }

        setPathColor(pathId, getResources().getColor(R.color.OrangeRed));
    }

    private void setPathColor(String pathId, int color) {
        ArrayList<PathSegment> segments = mPathManager.getSegmentsForPath(pathId);
        for (PathSegment s:segments) {
            Polyline line = mPathPolylines.get(s.getId());
            if (line != null) {
                line.setVisible(true);
                line.setColor(color);
            }
        }
    }

    private void changeAllPathsToUnSelected() {
        BitmapDescriptor bmp;

        MapIterator<String, Marker> endMarkers = mEndMarkers.mapIterator();
        while (endMarkers.hasNext()) {
            endMarkers.next();
            Marker m = endMarkers.getValue();
            String pathId = mEndMarkers.getKey(m);
            if (mPathManager.isStoredLocally(pathId))
                bmp = BitmapDescriptorFactory.fromResource(R.drawable.path_marker);
            else
                bmp = BitmapDescriptorFactory.fromResource(R.drawable.path_marker_cloud);

            m.setIcon(bmp);
        }

        MapIterator<String, Polyline> lines = mPathPolylines.mapIterator();
        while (lines.hasNext()) {
            lines.next();
            Polyline line = lines.getValue();
            line.setVisible(true);
            line.setColor(getResources().getColor(R.color.DarkOrange));
        }
    }

    private void initializePathManager() {
        if (mPathManager == null)
            mPathManager = PathManager.getInstance();
    }

    private void initializeBus() {
        if (bus == null) {
            bus = BusProvider.getInstance();
            bus.register(this);
        }
    }

    private void initializeQueues() {
        mEventQueue = new LinkedList();
        mWaitingForViewEventQueue = new LinkedList();
    }

    @Override
    public void onDestroy () {
        bus.unregister(this);
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater arg0, ViewGroup arg1, Bundle savedInstanceState) {
        View v = super.onCreateView(arg0, arg1, savedInstanceState);
        Bundle savedState = getArguments();
        if (savedState != null)
            restoreState(savedState);
        Log.d(Constants.TRAILBOOK_TAG, "getting map");
        mMap = this.getMap();
        if (mMap == null) {
            Log.e(Constants.TRAILBOOK_TAG, "error getting map");
            //todo: create no google maps dialog
        }
        setUpMapIfNeeded();
        hideMapMessage();
        mSlidingPanel = (SlidingUpPanelLayout) getActivity().findViewById(R.id.main_panel);
        processWaitingForViewEvents();

        Log.d(Constants.TRAILBOOK_TAG, "TrailBookMapFragment Mode:" + TrailBookState.getMode());
        return v;
    }

    private void drawLoadedPaths() {
        Log.d(Constants.TRAILBOOK_TAG, LOG_CLASS_NAME + ": drawLoadedPaths()");
        ArrayList<PathSummary> summaries = mPathManager.getDownloadedPathSummaries();
        for (PathSummary summary:summaries) {
            addPathToMap(summary);
        }
        hideNonActivePathsIfNeeded();
    }

    private void addPathToMap(PathSummary summary) {
        addPathSummaryToMap(summary);
        ArrayList<PathSegment> segments = mPathManager.getSegmentsForPath(summary.getId());
        for (PathSegment segment : segments) {
            addSegmentToMap(segment);
        }
    }

    private boolean shouldDisplayPath(PathSummary summary) {
        if (TrailbookPathUtilities.isPathInFilter(summary, TrailbookPathUtilities.getFilters())) {
            Log.v(Constants.TRAILBOOK_TAG, "TrailBookMapFragment: showing " + summary.getName());
            return true;
        } else {
            Log.v(Constants.TRAILBOOK_TAG, "TrailBookMapFragment: not showing " + summary.getName());
            return false;
        }
    }

    public void hideMapMessage() {
        if (isAdded()) {
            TextView tvMapMessage = (TextView) (getActivity().findViewById(R.id.map_tv_message));
            if (tvMapMessage != null) {
                tvMapMessage.setVisibility(View.INVISIBLE);
            }

            TextView tvTopMapMessage = (TextView) (getActivity().findViewById(R.id.map_tv_top_message));
            if (tvTopMapMessage != null) {
                tvTopMapMessage.setVisibility(View.INVISIBLE);
            }
        }
    }
    
/*    public void hideProgressBar() {
        if (isAdded()) {
            getActivity().setProgressBarIndeterminateVisibility(false);
        }
    }

    public void updateProgressBar(int percentComplete) {
        if (isAdded()) {
            getActivity().setProgressBarIndeterminateVisibility(true);
        }
    }*/

    public void removeSelectedMarker() {
        Log.d(Constants.TRAILBOOK_TAG, LOG_CLASS_NAME + ": removing selected marker");
        if (mSelectedPointMarker != null) {
            mSelectedPointMarker.remove();
        }
        mSelectedLocation = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        collapseSlidingPanelIfExpanded();
        changeAllPathsToUnSelected();

        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        Log.d(Constants.TRAILBOOK_TAG, "saving map fragment instance state");

        saveCenterPoint(editor);
        saveZoomLevel(editor);
        saveSelectedLocation(editor);
        saveMapLoaded(editor);

        editor.commit();
    }

    private void saveMapLoaded(SharedPreferences.Editor editor) {
        editor.putBoolean(SAVED_MAP_LOADED_STATE, mMapLoaded);
    }

    private void saveZoomLevel(SharedPreferences.Editor editor) {
        mZoomLevel = mMap.getCameraPosition().zoom;
        editor.putFloat(SAVED_ZOOM_LEVEL, mZoomLevel);
        Log.d(Constants.TRAILBOOK_TAG, "TrailBookMapFragment: Saved zoom level " + mZoomLevel);
    }

    private void saveSelectedLocation(SharedPreferences.Editor editor) {
        Gson gson = new Gson();
        String jsonSelectedLocation = gson.toJson(mSelectedLocation, LatLng.class);
        editor.putString(SAVED_SELECTED_LOCATION, jsonSelectedLocation);
    }

    private boolean isBoundsValid(String jsonBounds) {
        return !jsonBounds.contains("\"latitude\":0.0,\"longitude\":0.0");
    }

    private void saveCenterPoint(SharedPreferences.Editor editor) {
        String jsonCenter = getJsonCenter();
        editor.putString(SAVED_MAP_CENTER, jsonCenter);
        Log.d(Constants.TRAILBOOK_TAG, "TrailBookMapFragment: Saved ceter " + jsonCenter);
    }

    private String getJsonCenter() {
//        mBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
        mCenterPoint = mMap.getCameraPosition().target;
        Gson gson = new Gson();
        String jsonCenter = gson.toJson(mCenterPoint);
        Log.d(Constants.TRAILBOOK_TAG, "TrailBookMapFragment: center," + jsonCenter);
        return jsonCenter;
    }

    public void showOnlyPath(String pathId) {
        hideAllPaths();

        ArrayList<PointAttachedObject> paObjects = mPathManager.getPointObjectsForPath(pathId);
        showPointObjects(paObjects);
        showLinesForPath(pathId);
        showStartPointForPath(pathId);
        showEndPointForPath(pathId);
    }

    public void showStartPointForPath(String pathId) {
        Marker m = mStartMarkers.get(pathId);
        if (m != null) {
            m.remove();
            mStartMarkers.remove(pathId);
        }

        addStartMarkerToMap(mPathManager.getPathSummary(pathId));
    }

    public void showEndPointForPath(String pathId) {
        Marker m = mEndMarkers.get(pathId);
        if (m != null) {
            m.remove();
            mEndMarkers.remove(pathId);
        }

        addEndMarkerToMap(mPathManager.getPathSummary(pathId));
    }

    private void showLinesForPath(String pathId) {
        ArrayList<PathSegment> segments = mPathManager.getSegmentsForPath(pathId);
        for (PathSegment s:segments) {
            Polyline line = mPathPolylines.get(s.getId());
            if (line != null)
                line.setVisible(true);
            else
                addSegmentToMap(s);
        }
    }

    public void showAllPaths() {
        setVisibilityForAllEndMarkers(true);
        setVisibilityForAllLines(true);
    }

    public void hideAllPaths() {
        setVisibilityForAllStartMarkers(false);
        setVisibilityForAllEndMarkers(false);
        setVisibilityForAllLines(false);
        setVisibilityForAllNoteMarkers(false);
        if (TrailBookState.getMode() != TrailBookState.MODE_EDIT)
            removeSelectedMarker();
    }

    public void hideNonActivePathsIfNeeded() {
        if (TrailBookState.getMode() == TrailBookState.MODE_LEAD
                || TrailBookState.getMode() == TrailBookState.MODE_EDIT
                || TrailBookState.getMode() == TrailBookState.MODE_FOLLOW) {
            showOnlyPath(TrailBookState.getActivePathId());
        }
    }

    public void setVisibilityForAllLines(boolean show) {
        if (mPathPolylines == null)
            return;

        Set<Polyline> lines = mPathPolylines.values();
        for (Polyline line:lines) {
            line.setVisible(show);
        }
    }

    public void setVisibilityForAllStartMarkers(boolean show) {
        if (mStartMarkers == null)
            return;

        Set<Marker> startMarkers = mStartMarkers.values();
        for (Marker m:startMarkers) {
            m.setVisible(show);
        }
    }

    public void setVisibilityForAllEndMarkers(boolean show) {
        if (mEndMarkers == null)
            return;

        Set<Marker> endMarkers = mEndMarkers.values();
        for (Marker m:endMarkers) {
            m.setVisible(show);
        }
    }

    public void setVisibilityForAllNoteMarkers(boolean show) {
        if (mPaoMarkers == null)
            return;

        Set<Marker> noteMarkers = mPaoMarkers.values();
        ArrayList<String> removedNoteIds = new ArrayList<String>();
        for (Marker m:noteMarkers) {
            String noteId = mPaoMarkers.getKey(m);
            removedNoteIds.add(noteId);
            m.setVisible(show);
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {
        Log.d(Constants.TRAILBOOK_TAG, "Clicked map, " + latLng.latitude + "," + latLng.longitude);
        collapseSlidingPanelIfExpanded();
        changeAllPathsToUnSelected();

        if (TrailBookState.getMode() == TrailBookState.MODE_EDIT) {
            LatLng closestPointOnPath = TrailbookPathUtilities.getNearestPointOnPath(latLng, TrailBookState.getActivePathId());
            mSelectedLocation = closestPointOnPath;
            putSelectedMarkerOnMap(closestPointOnPath);
        }
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        Log.d(Constants.TRAILBOOK_TAG, "Long Clicked map, " + latLng.latitude + "," + latLng.longitude);
    }

    private void putSelectedMarkerOnMap(LatLng point) {
        if (mSelectedPointMarker != null)
            mSelectedPointMarker.remove();

        if (point != null) {
            Log.d(Constants.TRAILBOOK_TAG, LOG_CLASS_NAME + ": putting selected marker on map:" + point);
            mSelectedPointMarker = mMap.addMarker(new MarkerOptions()
                            .position(point)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.point_select_2))
                            .anchor(.5f, .5f)
            );
            mSelectedPointMarker.setDraggable(true);
        }
    }

    private void collapseSlidingPanelIfExpanded() {
        if (mSlidingPanel != null && mSlidingPanel.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED)
            mSlidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        MarkerType markerType = getMarkerType(marker);
        if (markerType == MarkerType.START) {
            String pathId = mStartMarkers.getKey(marker);
            return showPathSummary(pathId);
        } else if (markerType == MarkerType.END) {
            String pathId = mEndMarkers.getKey(marker);
            return showPathSummary(pathId);
        }  else if (markerType == MarkerType.NOTE) {
            String paoId = mPaoMarkers.getKey(marker);
            return showPAObject(paoId);
        }
        return false;
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
        Log.d(Constants.TRAILBOOK_TAG, "Starting drag");
    }

    @Override
    public void onMarkerDrag(Marker marker) {
        Log.d(Constants.TRAILBOOK_TAG, "dragging");
        String pathId = TrailBookState.getActivePathId();
        LatLng newLoc = marker.getPosition();

        MarkerType type = getMarkerType(marker);
        if (type == MarkerType.POINT && mSelectedLocation != null) {
            TrailbookPathUtilities.movePoint(pathId, mSelectedLocation, newLoc);
            mSelectedLocation = newLoc;
            refreshSegmentsForActivePath();
        } else if (type == MarkerType.NOTE) {
            String paoId = mPaoMarkers.getKey(marker);
            TrailbookPathUtilities.moveNote(pathId, paoId, newLoc);
        }
        mPathManager.savePath(pathId);
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        Log.d(Constants.TRAILBOOK_TAG, "Ending drag");
    }

    @Override
    public void onClick(View view) {
        if (isAdded()) {
            Log.d(Constants.TRAILBOOK_TAG, "View clicked:" + view.getId() + ", " + view.getTag());
            if (view.getId() == R.id.nv_small_note_layout || view.getId() == R.id.vc_small_climb_layout || view.getId() == R.id.vn_button_expand) {
                Log.d(Constants.TRAILBOOK_TAG, "Note clicked");
                collapseSlidingPanelIfExpanded();
                changeAllPathsToUnSelected();

                ((TrailBookActivity) getActivity()).showFullObject(((PointAttachedObjectView) view).getPaoId());
            }
        }
    }

    private boolean showPAObject(String paoId) {
        if (paoId == null) {
            Log.d(Constants.TRAILBOOK_TAG, "Null Path ID");
            return true;
        }
        paoId = paoId.substring(0,paoId.length());

        if (mSlidingPanel != null) {
/*
            int height = getFullWindowHeight();
            mSlidingPanel.setPanelHeight(height/2);
            mSlidingPanel.requestLayout();
*/
            expandSlidingPanelIfCollapsed();
            PointAttachedObjectView v = NoteFactory.getPaoSmallView(PathManager.getInstance().getPointAttachedObject(paoId));
            v.setOnClickListener(this);
            addViewToSlidingUpPanel(v);
        }
        return false;
    }

    private void expandSlidingPanelIfCollapsed() {
        Log.d(Constants.TRAILBOOK_TAG, LOG_CLASS_NAME + " " + mSlidingPanel.getPanelState());
        Log.d(Constants.TRAILBOOK_TAG, LOG_CLASS_NAME + " expanding panel");
        mSlidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
    }

    private int getFullWindowHeight() {
        DisplayMetrics dm = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
        return dm.heightPixels;
    }

    public boolean showPathSummary(String pathId) {
        if (pathId == null) {
            Log.d(Constants.TRAILBOOK_TAG, LOG_CLASS_NAME + " Null Path ID");
            return true;
        }

        if (mSlidingPanel != null) {
            Log.d(Constants.TRAILBOOK_TAG, LOG_CLASS_NAME + " showing sliding panel for " + pathId);
            mDetailView = getPathDetailsView(pathId);
            registerForContextMenu(mDetailView.getMoreButton());
            addViewToSlidingUpPanel(mDetailView);
            expandSlidingPanelIfCollapsed();
            changeAllPathsToUnSelected();
            changePathToSelected(pathId);
        } else {
            Log.d(Constants.TRAILBOOK_TAG, LOG_CLASS_NAME + " no sliding panel yet. ");
            Log.d(Constants.TRAILBOOK_TAG, LOG_CLASS_NAME + ":queueing onPathDetailRequestEvent");
            queueEventWaitingForView(new PathDetailRequestEvent(pathId));
        }

        return false;
    }

    private void addViewToSlidingUpPanel(View v) {
        if (isAdded()) {
            LinearLayout panelContainer = (LinearLayout) getActivity().findViewById(R.id.details_panel_container);
            panelContainer.removeAllViews();
            panelContainer.addView(v);
        }
    }

    private PathDetailsView getPathDetailsView(String pathId) {
//        PathDetailsView pv = (PathDetailsView) getActivity().getLayoutInflater().inflate(R.layout.path_details, null);
        PathDetailsView pv = new PathDetailsView(getActivity());
        pv.setPathId(pathId);
        pv.setActionListener((PathDetailsActionListener) getActivity());
        return pv;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,ContextMenu.ContextMenuInfo menuInfo) {
        Log.d(Constants.TRAILBOOK_TAG, "TrailBookMapFragment creating context menu");
        if (mDetailView != null) {
            Log.d(Constants.TRAILBOOK_TAG, "context menu for " + v.getId());
            if (v.getId() == mDetailView.getMoreButton().getId()) {
                PathSummary summary = mPathManager.getPathSummary(mDetailView.getPathId());
                if (summary != null) {
                    menu.setHeaderTitle(summary.getName());
                    ApplicationUtils.addPathActionMenuItems(menu, summary.getId());
                }
            }
        }

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        String pathId = mDetailView.getPathId();
        Log.d(Constants.TRAILBOOK_TAG, "TrailBookMapFragment: item clicked for path:" + pathId);
        TrailBookActivity actionListener = (TrailBookActivity)getActivity();
        actionListener.processMenuAction(pathId, item);
        return true;
    }



    private MarkerType getMarkerType(Marker marker) {
        //TODO: refactor me
        String id = mStartMarkers.getKey(marker);
        if (id != null) {
            return MarkerType.START;
        }
        id = mEndMarkers.getKey(marker);
        if (id != null) {
            return MarkerType.END;
        }
        id = mPaoMarkers.getKey(marker);
        if (id != null) {
            return MarkerType.NOTE;
        }

        if (mSelectedPointMarker != null && marker.getId().equalsIgnoreCase(mSelectedPointMarker.getId()))
            return MarkerType.POINT;

        return MarkerType.UNKNOWN;
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p>
     * If it isn't installed {@link com.google.android.gms.maps.SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have a map to set up.
        if (mMap != null) {
            setUpMap();
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        Log.d(Constants.TRAILBOOK_TAG, "Setting up the map");
        //center the map on the current location
        mMap.setOnMapLoadedCallback(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMarkerDragListener(this);
        mMap.setOnMapClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.setMyLocationEnabled(true);
        //mMap.setInfoWindowAdapter(new MapPointSelectedAdaptor(getActivity().getLayoutInflater()));
        setMapTypeToUserPreference();

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(mCenterPoint)
                .zoom(mZoomLevel)
                .build();                   // Creates a CameraPosition from the builder
        Log.d(Constants.TRAILBOOK_TAG, LOG_CLASS_NAME + ": zooming to " + mCenterPoint + " at zoom level " + mZoomLevel);
        String requestedPathId = TrailBookState.getZoomToPathId();
        TrailBookState.resetZoomToPathId();
        if (requestedPathId == null || requestedPathId.equals(TrailBookState.NO_START_PATH)) {
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        } else {
            zoomToPath(requestedPathId);
            requestShowPathDetails(requestedPathId);
        }
    }

    private void requestShowPathDetails(String launchedPathId) {
        PathDetailRequestEvent event = new PathDetailRequestEvent(launchedPathId);
        bus.post(event);
    }

    public static TrailBookMapFragment newInstance() {
        TrailBookMapFragment fragment = new TrailBookMapFragment();
        return fragment;
    }


    private void removeStartMarker(String pathId) {
        Marker startMarker = mStartMarkers.get(pathId);
        if (startMarker != null) {
            startMarker.remove();
            mStartMarkers.remove(pathId);
        }
    }

    private void removeEndMarker(String pathId) {
        Marker endMarker = mEndMarkers.get(pathId);
        if (endMarker != null) {
            endMarker.remove();
            mEndMarkers.remove(pathId);
        }
    }

    private void updateEndMarkerToLocal(String pathId) {
        Marker endMarker = mEndMarkers.get(pathId);
        if (endMarker != null) {
            BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.path_marker);
            endMarker.setIcon(icon);
        }
    }

    private void removePaoFromMap(String paoId) {
        Marker paoMarker = mPaoMarkers.get(paoId);
        if (paoMarker != null) {
            Log.d(Constants.TRAILBOOK_TAG, LOG_CLASS_NAME + ": removing pao " + paoId);
            paoMarker.remove();
        }
    }

    private void removeSegmentFromMap(PathSegment segment) {
        removePoints(segment);
    }

    private void removePoints(PathSegment s) {
        String id = s.getId();
        Polyline line = mPathPolylines.get(id);
        if (line != null) {
            line.remove();
            mPathPolylines.remove(id);
        }
    }

    private void addSegmentToMap(PathSegment segment) {
        if (segment.getPoints() != null)
            addPoints(segment, getPolylineOptions(segment));
    }

    private void showPointObjects(ArrayList<PointAttachedObject> pointObjects) {
        for (PointAttachedObject pointObject:pointObjects) {
            if (pointObject != null)
                showPointObject(pointObject);
        }
    }

    private void showPointObject(PointAttachedObject pointObject) {
        String id = pointObject.getId();
        Marker m = mPaoMarkers.get(id);
        if (m != null)
            m.setVisible(true);
        else
            addPointOjbect(pointObject, NoteFactory.getUnelectedIconId(pointObject.getAttachment().getType()));
    }

    private PolylineOptions getPolylineOptions(PathSegment segment) {
        PolylineOptions o = new PolylineOptions();
//        o.color(R.color.Cornsilk);
        o.width(THICK);

        return o;
    }

    private void addPointOjbect(PointAttachedObject paoObject, int iconId) {
        String objectId = paoObject.getId();
        Marker currentMarker = mPaoMarkers.get(objectId);
        if (currentMarker == null){
            Log.d(Constants.TRAILBOOK_TAG, LOG_CLASS_NAME + ": adding new marker");
            Marker marker = mMap.addMarker(getMarker(paoObject, iconId));
            mPaoMarkers.put(objectId, marker);
            if (TrailBookState.getMode() == TrailBookState.MODE_EDIT) {
                marker.setDraggable(true);
            }
        } else {
            Log.d(Constants.TRAILBOOK_TAG, LOG_CLASS_NAME + "updating existing marker");
            currentMarker.setIcon(BitmapDescriptorFactory.fromBitmap(scale(iconId)));
        }
    }

    public LatLng getSelectedLocation() {
        return mSelectedLocation;
    }

    private MarkerOptions getMarker(PointAttachedObject paObject, int iconId) {
        String content = paObject.getAttachment().toString();
        MarkerOptions options =  new MarkerOptions()
                .position(paObject.getLocation())
                .title(content)
                .anchor(.5f, 1f)
                .icon(BitmapDescriptorFactory.fromBitmap(scale(iconId)));

        return options;
    }

    private Bitmap scale (int resourceId) {
        BitmapDrawable bd=(BitmapDrawable) getResources().getDrawable(resourceId);
        Bitmap b=bd.getBitmap();
        Bitmap newBitmap=Bitmap.createScaledBitmap(b, b.getWidth()/1,b.getHeight()/1, false);
        return newBitmap;
    }

    private void addPoints(PathSegment s, PolylineOptions o) {
        String id = s.getId();
        ArrayList<LatLng> points = s.getPoints();
        putPolylineOnMap(o, points, id);
    }

    private void addPointsToPolylineOptions(PolylineOptions o, ArrayList<LatLng> points) {
        for (LatLng point : points)
            o.add(point);
    }

    private void putPolylineOnMap(PolylineOptions o, ArrayList<LatLng> points, String segmentId) {
        if (points == null || points.size() < 2)
            return;

        Polyline oldLine = mPathPolylines.get(segmentId);
        if (oldLine != null) {
            oldLine.setPoints(points);
            oldLine.setColor(getResources().getColor(R.color.DarkOrange));
        }
        else {
            putNewPolylineOnMap(o, points, segmentId);
        }
    }

    private void putNewPolylineOnMap(PolylineOptions o, ArrayList<LatLng> points, String segmentId) {
        if (mMap == null) {
            Log.d(Constants.TRAILBOOK_TAG, "Map is null");
            return;
        }

        addPointsToPolylineOptions(o, points);
        Polyline polyline = mMap.addPolyline(o);
        polyline.setColor(getResources().getColor(R.color.DarkOrange));
        mPathPolylines.put(segmentId, polyline);
    }

    private void addPathSummaryToMap(PathSummary summary) {
        if (shouldDisplayPath(summary)) {
            Log.v(Constants.TRAILBOOK_TAG, LOG_CLASS_NAME + "adding summary to map " + summary.getName());
            addEndMarkerToMap(summary);
        }
    }

    private void addEndMarkerToMap(PathSummary summary) {
        if (shouldDisplayPath(summary)) {
            if (mEndMarkers.get(summary.getId()) == null && summary != null && summary.getEnd() != null) {
                Marker endMarker = mMap.addMarker(getNewEndMarker(summary));
                mEndMarkers.put(summary.getId(), endMarker);
            }
        }
    }

    private MarkerOptions getNewEndMarker(PathSummary summary) {
        BitmapDescriptor icon = null;
        if (mPathManager.isStoredLocally(summary.getId())) {
            icon = BitmapDescriptorFactory.fromResource(R.drawable.path_marker);
        } else {
            icon = BitmapDescriptorFactory.fromResource(R.drawable.path_marker_cloud);
        }

        MarkerOptions options = new MarkerOptions()
                .position(summary.getEnd())
                .title(summary.getName())
                .icon(icon);
        return options;
    }

    private void addStartMarkerToMap(PathSummary summary) {
        if (mStartMarkers.get(summary.getId()) == null && summary != null && summary.getStart() != null) {
            Marker startMarker = mMap.addMarker(getNewStartMarker(summary));
            mStartMarkers.put(summary.getId(), startMarker);
        }
    }

    private MarkerOptions getNewStartMarker(PathSummary summary) {
        BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.parking);

        MarkerOptions options = new MarkerOptions()
                .position(summary.getStart())
                .title(getString(R.string.parking))
                .snippet(summary.getName())
                .icon(icon)
                .anchor(.5f,1f);
        return options;
    }

    public void zoomToPath(String pathId) {
        ArrayList<Polyline> segmentLines = getPolylinesForPath(pathId);
        if (segmentLines != null && segmentLines.size()>0) {
            LatLngBounds bounds = MapUtilities.getBoundsForPolylineArray(segmentLines);
            try {
                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 200));
            } catch(IllegalStateException e) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 10));
            }
        } else {
            PathSummary summary = mPathManager.getPathSummary(pathId);
            if (summary != null && summary.getEnd() != null)
                zoomToLocation(summary.getEnd());
        }
    }

    private ArrayList<Polyline> getPolylinesForPath(String pathId) {
        ArrayList<Polyline> segmentLines = new ArrayList<Polyline>();
        ArrayList<PathSegment> segments = mPathManager.getSegmentsForPath(pathId);
        if (segments == null)
            return null;

        for (PathSegment s:segments) {
            Polyline line = mPathPolylines.get(s.getId());
            if (line != null)
                segmentLines.add(line);
        }

        return segmentLines;
    }

    public void zoomToCurrentLocation() {
        Location currentLocation = TrailBookState.getCurrentLocation();

        zoomToLocation(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
    }

    private void zoomToLocation(LatLng loc) {
        if (loc != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, DEFAULT_ZOOM_LEVEL));
        }
    }

    @Subscribe
    public void onFilterChangedEvent(FilterChangedEvent event) {
        hideAllPaths();
    }

    @Subscribe
    public void onZoomRequestEvent(ZoomRequestEvent event) {
        if (mMap == null || !isMapLoaded()) {
            queueEventIfMapNotAvailable(event);
            return;
        }

        zoomToPath(event.getPathId());
    }

    @Subscribe
    public void onLocationChangedEvent(LocationChangedEvent event) {
        if (mMap == null || !isMapLoaded()) {
            return; //don't want to queue location changed events because they are frequent and transient.
        }

        int mode = TrailBookState.getMode();
        Location l = event.getLocation();
        if (mPathManager != null && mode == TrailBookState.MODE_FOLLOW) {
            try {
                String currentPath = TrailBookState.getActivePathId();
                Log.d(Constants.TRAILBOOK_TAG, "current path id: " + currentPath);
                ArrayList<PointAttachedObject> paObjects = mPathManager.getPointObjectsForPath(currentPath);
                if (paObjects != null) {
                    for (PointAttachedObject paObject : paObjects) {
                        double distanceToNote = TrailbookPathUtilities.getDistanceToNote(paObject, l);
                        if (distanceToNote < PreferenceUtilities.getNoteAlertDistanceInMeters(getActivity())) {
                            Log.d(Constants.TRAILBOOK_TAG, "adding selected note " + paObject.getAttachment().toString());
                            addPointOjbect(paObject, NoteFactory.getSelectedIconId(paObject.getAttachment().getType()));
                        } else {
                            Log.d(Constants.TRAILBOOK_TAG, "adding unselected note" + paObject.getAttachment().toString());
                            addPointOjbect(paObject, NoteFactory.getUnelectedIconId(paObject.getAttachment().getType()));
                        }
                    }
                }
                mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(l.getLatitude(), l.getLongitude() )));
            } catch (Exception e) {
                Log.e(Constants.TRAILBOOK_TAG, "Exception updating note markers.  map may not have been initialized", e);
            }
        } else if (mode == TrailBookState.MODE_LEAD) {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(l.getLatitude(), l.getLongitude() )));
        }
    }


    @Subscribe
    public void onSegmentDeletedEvent(SegmentDeletedEvent event) {
        if (mMap == null || !isMapLoaded()) {
            queueEventIfMapNotAvailable(event);
            return;
        }

        try {
            PathSegment seg = event.getSegment();
            removeSegmentFromMap(seg);
        } catch (Exception e) {
            Log.e(Constants.TRAILBOOK_TAG, "Exception onSegmentDeletedEvent.  map may not have been initialized", e);
        }

    }

    @Subscribe
    public void onPointAttachedObjectDeletedEvent(PointAttachedObjectDeletedEvent event) {
        if (mMap == null || !isMapLoaded()) {
            queueEventIfMapNotAvailable(event);
            return;
        }

        try {
            String paoId = event.getPaoId();
            removePaoFromMap(paoId);
        } catch (Exception e) {
            Log.e(Constants.TRAILBOOK_TAG, "Exception onPointAttachedObjectDeletedEvent.  map may not have been initialized", e);
        }

    }

    @Subscribe
    public void onMapObjectAddedEvent(MapObjectAddedEvent event){
        if (mMap == null || !isMapLoaded()) {
            queueEventIfMapNotAvailable(event);
            return;
        }

        try {
            PointAttachedObject paObject = event.getPao();
            if ( (TrailBookState.getMode() == TrailBookState.MODE_FOLLOW
                    || TrailBookState.getMode() == TrailBookState.MODE_LEAD
                    || TrailBookState.getMode() == TrailBookState.MODE_EDIT) &&
                    mPathManager.objectBelongsToPath(paObject.getId(), TrailBookState.getActivePathId())) {
                addPointOjbect(paObject, NoteFactory.getUnelectedIconId(paObject.getAttachment().getType()));
            }

        } catch (Exception e) {
            Log.e(Constants.TRAILBOOK_TAG, "Exception onNoteAddedEvent.  map may not have been initialized", e);
        }
    }

    @Subscribe
    public void onPathSummaryAddedEvent(PathSummaryAddedEvent event){
        if (mMap == null || !isMapLoaded()) {
            queueEventIfMapNotAvailable(event);
            return;
        }

        try {
            PathSummary summary = event.getPathSummary();
            addPathSummaryToMap(summary);
        } catch (Exception e) {
            Log.e(Constants.TRAILBOOK_TAG, "Exception onPathSummaryAddedEvent.  map may not have been initialized", e);
        }
    }

    @Subscribe
    public void onSegmentUpdatedEvent(SegmentUpdatedEvent event){
        if (mMap == null || !isMapLoaded()) {
            queueEventIfMapNotAvailable(event);
            return;
        }

        try {
            PathSegment seg = event.getSegment();
            addSegmentToMap(seg);
        } catch (Exception e) {
            Log.e(Constants.TRAILBOOK_TAG, "Exception onSegmentUpdatedEvent.  map may not have been initialized", e);
        }
    }

    @Subscribe
    public void onPathDeletedEvent(PathDeletedEvent event) {
        if (mMap == null || !isMapLoaded()) {
            queueEventIfMapNotAvailable(event);
            return;
        }

        try {
            String id = event.getPath().getId();
            if (!mPathManager.isPathInCloudCache(id)) {
                removeEndMarker(id);
            }
            removeStartMarker(id);
        } catch (Exception e) {
            Log.e(Constants.TRAILBOOK_TAG, "Exception onPathDeletedEvent.  map may not have been initialized", e);
        }
    }

    @Subscribe
    public void onPathReceivedEvent(PathReceivedEvent event) {
        if (mMap == null || !isMapLoaded()) {
            queueEventIfMapNotAvailable(event);
            return;
        }

        try {
            PathSummary summary = event.getPath().summary;
            String id = summary.getId();
            updateEndMarkerToLocal(id);
            Toast.makeText(getActivity(), "Download Completed", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(Constants.TRAILBOOK_TAG, "Exception onPathDeletedEvent.  map may not have been initialized", e);
        }
    }

    private LatLng getLatLngFromPoint(Point point) {
        return mMap.getProjection().fromScreenLocation(point);
    }

    @Subscribe
    public void onModeChangedEvent(ModeChangedEvent event) {
        if (mMap == null || !isMapLoaded()) {
            queueEventIfMapNotAvailable(event);
            return;
        }

        if (event.getNewMode() == TrailBookState.MODE_SEARCH) {
            prepareMapForSearchMode();
        } else if (event.getNewMode() == TrailBookState.MODE_LEAD) {
            prepareMapForLeadMode();
        } else if (event.getNewMode() == TrailBookState.MODE_EDIT) {
            prepareMapForEditMode();
        }
        displayMessage();
    }

    @Subscribe
    public void onRefreshMessageEvent(RefreshMessageEvent event) {
        if (mMap == null || !isMapLoaded()) {
            queueEventIfMapNotAvailable(event);
            return;
        }

        this.displayMessage();
    }

    @Subscribe
    public void onPathSummariesReceivedFromCloudEvent(PathSummariesReceivedFromCloudEvent event) {
        Log.d(Constants.TRAILBOOK_TAG, LOG_CLASS_NAME + ":onPathSummariesReceivedFromCloudEvent");
        if (mMap == null || !isMapLoaded()) {
            Log.d(Constants.TRAILBOOK_TAG, LOG_CLASS_NAME + ":queueing onPathSummariesReceivedFromCloudEvent");
            queueEventIfMapNotAvailable(event);
            return;
        }

        ArrayList<PathSummary> summaries = event.getPathSummaries();
        for (PathSummary summary : summaries) {
            addPathSummaryToMap(summary);
        }
    }

    @Subscribe
    public void onPathDetailRequestEvent(PathDetailRequestEvent event) {
        Log.d(Constants.TRAILBOOK_TAG, LOG_CLASS_NAME + ": onPathDetailRequestEvent");
        Log.d(Constants.TRAILBOOK_TAG, LOG_CLASS_NAME + ": mSlidingPanel = " + mSlidingPanel);
        showPathSummary(event.getPathId());
    }

    private void prepareMapForEditMode() {
        showOnlyPath(TrailBookState.getActivePathId());
        removeSelectedMarker();
        setAllPAOMarkersToMovable();
        zoomToPath(TrailBookState.getActivePathId());
    }

    private void setAllPAOMarkersToMovable() {
        Set<Marker> markers = mPaoMarkers.values();
        for (Marker m:markers) {
            m.setDraggable(true);
        }
    }

    private void setAllPAOMarkersToNotMovable() {
        Set<Marker> markers = mPaoMarkers.values();
        for (Marker m:markers) {
            if (m.isVisible())
                m.setDraggable(false);
        }
    }

    private void prepareMapForLeadMode() {
        showOnlyPath(TrailBookState.getActivePathId());
        setVisibilityForAllEndMarkers(false);
        removeSelectedMarker();
        setAllPAOMarkersToNotMovable();
        zoomToCurrentLocation();
    }

    private void prepareMapForSearchMode() {
        showAllPaths();
        setVisibilityForAllNoteMarkers(false);
        setVisibilityForAllStartMarkers(false);
        removeSelectedMarker();
        setAllPAOMarkersToNotMovable();
    }

    private void queueEventIfMapNotAvailable(Object event) {
        if (mMap == null || !isMapLoaded()) {
            Log.d(Constants.TRAILBOOK_TAG, LOG_CLASS_NAME + ":queueing event " + event);
            mEventQueue.add(event);
        }
    }

    private void queueEventWaitingForView(Object event) {
        mWaitingForViewEventQueue.add(event);
    }

    private void processWaitingForViewEvents() {
        Log.d(Constants.TRAILBOOK_TAG, "processing missed waiting for view events: " + mWaitingForViewEventQueue.size());
        while (!mWaitingForViewEventQueue.isEmpty()) {
            Object event = mWaitingForViewEventQueue.remove();
            Log.d(Constants.TRAILBOOK_TAG, "processing missed event: " + event);
            if (event instanceof PathDetailRequestEvent)
                onPathDetailRequestEvent((PathDetailRequestEvent) event);
        }
    }

    private void processQueuedEvents() {
        Log.d(Constants.TRAILBOOK_TAG, "processing missed events: " + mEventQueue.size());
        while (!mEventQueue.isEmpty()){
            Object event = mEventQueue.remove();
            Log.d(Constants.TRAILBOOK_TAG, "processing missed event: " + event);
            if (event instanceof SegmentDeletedEvent)
                onSegmentDeletedEvent((SegmentDeletedEvent) event);
            else if (event instanceof MapObjectAddedEvent)
                onMapObjectAddedEvent((MapObjectAddedEvent) event);
            else if (event instanceof PathSummaryAddedEvent)
                onPathSummaryAddedEvent((PathSummaryAddedEvent) event);
            else if (event instanceof SegmentUpdatedEvent)
                onSegmentUpdatedEvent((SegmentUpdatedEvent) event);
            else if (event instanceof PathDeletedEvent)
                onPathDeletedEvent((PathDeletedEvent) event);
            else if (event instanceof ModeChangedEvent)
                onModeChangedEvent((ModeChangedEvent) event);
            else if (event instanceof PointAttachedObjectDeletedEvent)
                onPointAttachedObjectDeletedEvent((PointAttachedObjectDeletedEvent) event);
            else if (event instanceof RefreshMessageEvent)
                onRefreshMessageEvent((RefreshMessageEvent) event);
            else if (event instanceof PathSummariesReceivedFromCloudEvent)
                onPathSummariesReceivedFromCloudEvent((PathSummariesReceivedFromCloudEvent) event);
            else if (event instanceof PathReceivedEvent)
                onPathReceivedEvent((PathReceivedEvent) event);
            else if (event instanceof ZoomRequestEvent)
                onZoomRequestEvent((ZoomRequestEvent) event);
        }
    }
}
