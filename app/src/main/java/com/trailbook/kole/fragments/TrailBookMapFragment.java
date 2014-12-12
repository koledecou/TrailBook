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
import android.view.LayoutInflater;
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
import com.trailbook.kole.events.LocationChangedEvent;
import com.trailbook.kole.events.MapObjectAddedEvent;
import com.trailbook.kole.events.ModeChangedEvent;
import com.trailbook.kole.events.PathDeletedEvent;
import com.trailbook.kole.events.PathReceivedEvent;
import com.trailbook.kole.events.PathSummariesReceivedFromCloudEvent;
import com.trailbook.kole.events.PathSummaryAddedEvent;
import com.trailbook.kole.events.PointAttachedObjectDeletedEvent;
import com.trailbook.kole.events.RefreshMessageEvent;
import com.trailbook.kole.events.SegmentDeletedEvent;
import com.trailbook.kole.events.SegmentUpdatedEvent;
import com.trailbook.kole.fragments.point_attched_object_view.PointAttachedObjectView;
import com.trailbook.kole.helpers.MapUtilities;
import com.trailbook.kole.helpers.NoteFactory;
import com.trailbook.kole.helpers.PreferenceUtilities;
import com.trailbook.kole.helpers.TrailbookPathUtilities;
import com.trailbook.kole.state_objects.BusProvider;
import com.trailbook.kole.state_objects.PathManager;
import com.trailbook.kole.state_objects.TrailBookState;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Set;

/**
 * Created by Fistik on 6/30/2014.
 */
public class TrailBookMapFragment extends MapFragment implements GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener, View.OnClickListener, GoogleMap.OnMapLoadedCallback {
    private static final String LOG_TAG = "MyMapFragment";
    private static final float THICK = 10;
    private static final float MEDIUM = 6;
    private static final String SAVED_ZOOM_LEVEL = "ZOOM";
    private static final String SAVED_SELECTED_LOCATION = "SELECTED_LOC";
    private static final String SAVED_MAP_CENTER = "MAP_CENTER";

    private static final float DEFAULT_ZOOM_LEVEL = 16;
    private static final LatLng DEFAULT_MAP_CENTER = new LatLng( 34.8326509,-111.7693473);


    private SharedPreferences savedStatePrefs;
    private LatLng mSelectedLocation = null;
    private boolean mMapLoaded = false;

    public boolean isMapLoaded() {
        return mMapLoaded;
    }

    public LatLngBounds getBounds() {
        if (mMap != null)
            return mMap.getProjection().getVisibleRegion().latLngBounds;
        else
            return null;
    }

    public enum MarkerType {START,END,NOTE,UNKNOWN}

    private Bus bus;
    private LinkedList mEventQueue;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private float mZoomLevel = DEFAULT_ZOOM_LEVEL;
    private LatLng mCenterPoint = DEFAULT_MAP_CENTER;


    private PathManager mPathManager;
    private BidiMap<String, Marker> mStartMarkers;
    private BidiMap<String, Marker> mEndMarkers;
    private BidiMap<String, Polyline> mPathPolylines;
    private SlidingUpPanelLayout slidingPanel;
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
//        Toast.makeText(TrailBookState.getInstance(), "TrailBookMapFragment.onCreate()", Toast.LENGTH_SHORT).show();
//        slidingPanel.setSlidingEnabled(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(Constants.TRAILBOOK_TAG, "TrailBookMapFragment: onActivityCreated");

        slidingPanel = (SlidingUpPanelLayout) getActivity().findViewById(R.id.main_panel);
    }

    @Override
    public void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        setMapTypeToUserPreference();
    }

    private void setMapTypeToUserPreference() {
        int mapPreference = PreferenceUtilities.getMapType(getActivity());
        Log.d(Constants.TRAILBOOK_TAG, getClass().getName() + ": map preference:" + mapPreference);
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
        Log.d(Constants.TRAILBOOK_TAG, getClass().getName() + ": restored zoom level "+mZoomLevel);

        String jsonSelectedLocation = savedStatePrefs.getString(SAVED_SELECTED_LOCATION, null);
        if (jsonSelectedLocation != null && TrailBookState.getMode()==TrailBookState.MODE_EDIT) {
            mSelectedLocation = gson.fromJson(jsonSelectedLocation, LatLng.class);
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
        TextView tvMapMessage = (TextView)(getActivity().findViewById(R.id.map_tv_message));
        if (tvMapMessage == null) {
            return;
        }

        if (TrailBookState.getMode() == TrailBookState.MODE_EDIT) {
            tvMapMessage.setVisibility(View.VISIBLE);
            PathSummary summary = mPathManager.getPathSummary(TrailBookState.getActivePathId());
            String message = String.format(getString(R.string.editing_message), summary.getName());
            tvMapMessage.setText(message);
        } else if (TrailBookState.getMode() == TrailBookState.MODE_LEAD) {
            String message;
            if (TrailBookState.isListeningToLocationUpdates()) {
                PathSummary summary = mPathManager.getPathSummary(TrailBookState.getActivePathId());
                message = String.format(getString(R.string.leading_message), summary.getName());
            } else {
                message = getString(R.string.location_updates_paused);
            }
            tvMapMessage.setVisibility(View.VISIBLE);
            tvMapMessage.setText(message);
        } else if (TrailBookState.getMode() == TrailBookState.MODE_FOLLOW) {
            tvMapMessage.setVisibility(View.VISIBLE);
            PathSummary summary = mPathManager.getPathSummary(TrailBookState.getActivePathId());
            String message = String.format(getString(R.string.following_message), summary.getName());
            tvMapMessage.setText(message);
        } else {
            tvMapMessage.setVisibility(View.INVISIBLE);
        }
    }

    private void createMapObjects() {
        mStartMarkers = new DualHashBidiMap<String, Marker>();
        mEndMarkers = new DualHashBidiMap<String, Marker>();
        mPaoMarkers = new DualHashBidiMap<String, Marker>();
        mPathPolylines = new DualHashBidiMap<String, Polyline>();
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
        mEventQueue = new LinkedList();
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
        Log.d(Constants.TRAILBOOK_TAG, "TrailBookMapFragment Mode:" + TrailBookState.getMode());

        return v;
    }

    private void drawLoadedPaths() {
        Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ": drawLoadedPaths()");
        ArrayList<PathSummary> summaries = mPathManager.getDownloadedPathSummaries();
        for (PathSummary summary:summaries) {
            addPathSummaryToMap(summary);
            ArrayList<PathSegment> segments = mPathManager.getSegmentsForPath(summary.getId());
            for (PathSegment segment:segments) {
                addSegmentToMap(segment);
            }
        }
        hideNonActivePathsIfNeeded();
    }

    private void hideMapMessage() {
        TextView tvMapMessage = (TextView)(getActivity().findViewById(R.id.map_tv_message));
        if (tvMapMessage != null) {
            tvMapMessage.setVisibility(View.INVISIBLE);
        }
    }

    public void removeSelectedMarker() {
        Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ": removing selected marker");
        if (mSelectedPointMarker != null) {
            mSelectedPointMarker.remove();
        }
        mSelectedLocation = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        collapseSlidingPanelIfExpanded();

        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        Log.d(Constants.TRAILBOOK_TAG, "saving map fragment instance state");

        saveCenterPoint(editor);
        saveZoomLevel(editor);
        saveSelectedLocation(editor);

        editor.commit();
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
        setVisibilityForAllNoteMarkers(false);
        ArrayList<PointAttachedObject> paObjects = mPathManager.getPointObjectsForPath(pathId);
        showPointObjects(paObjects);
        showLinesForPath(pathId);
        showStartPointForPath(pathId);
        showEndPointForPath(pathId);
    }

    private void showStartPointForPath(String pathId) {
        Marker m = mStartMarkers.get(pathId);
        if (m != null)
            m.setVisible(true);
        else
            addStartMarkerToMap(mPathManager.getPathSummary(pathId));
    }

    private void showEndPointForPath(String pathId) {
        Marker m = mEndMarkers.get(pathId);
        if (m != null)
            m.setVisible(true);
        else
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

        if (TrailBookState.getMode() == TrailBookState.MODE_EDIT) {
            LatLng closestPointOnPath = TrailbookPathUtilities.getNearestPointOnPath(latLng, TrailBookState.getActivePathId());
            mSelectedLocation = closestPointOnPath;
            putSelectedMarkerOnMap(closestPointOnPath);
        }
    }

    private void putSelectedMarkerOnMap(LatLng point) {
        if (mSelectedPointMarker != null)
            mSelectedPointMarker.remove();

        if (point != null) {
            Log.d(Constants.TRAILBOOK_TAG, getClass().getName() + ": putting selected marker on map:" + point);
            mSelectedPointMarker = mMap.addMarker(new MarkerOptions()
                    .position(point)
                    .icon(BitmapDescriptorFactory.fromBitmap(scale(R.drawable.ic_marker_add_new_note))));
        }
    }

    private void collapseSlidingPanelIfExpanded() {
        if (slidingPanel != null && slidingPanel.isPanelExpanded())
            slidingPanel.collapsePanel();
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
    public void onClick(View view) {
        Log.d(Constants.TRAILBOOK_TAG, "View clicked:" + view.getId() + ", " + view.getTag());
        if (view.getId() == R.id.nv_small_note_layout || view.getId() == R.id.vc_small_climb_layout || view.getId() == R.id.vn_button_expand) {
            Log.d(Constants.TRAILBOOK_TAG, "Note clicked");
            collapseSlidingPanelIfExpanded();

            ((TrailBookActivity)getActivity()).showFullObject(((PointAttachedObjectView) view).getPaoId());
        }
    }

    private boolean showPAObject(String paoId) {
        if (paoId == null) {
            Log.d(Constants.TRAILBOOK_TAG, "Null Path ID");
            return true;
        }
        paoId = paoId.substring(0,paoId.length());

        if (slidingPanel != null) {
/*
            int height = getFullWindowHeight();
            slidingPanel.setPanelHeight(height/2);
            slidingPanel.requestLayout();
*/
            expandSlidingPanelIfCollapsed();
            PointAttachedObjectView v = NoteFactory.getPaoSmallView(PathManager.getInstance().getPointAttachedObject(paoId));
            v.setOnClickListener(this);
            addViewToSlidingUpPanel(v);
        }
        return false;
    }

    private void expandSlidingPanelIfCollapsed() {
        if (!slidingPanel.isPanelExpanded()) {
            slidingPanel.expandPanel();
        }
    }

    private int getFullWindowHeight() {
        DisplayMetrics dm = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
        return dm.heightPixels;
    }

    private boolean showPathSummary(String pathId) {
        if (pathId == null) {
            Log.d(Constants.TRAILBOOK_TAG, "Null Path ID");
            return true;
        }

        if (slidingPanel != null) {
            PathDetailsView pdv = getPathDetailsView(pathId);
            addViewToSlidingUpPanel(pdv);
            expandSlidingPanelIfCollapsed();
        }

        return false;
    }

    private void addViewToSlidingUpPanel(View v) {
        LinearLayout panelContainer = (LinearLayout)getActivity().findViewById(R.id.details_panel_container);
        panelContainer.removeAllViews();
        panelContainer.addView(v);
    }

    private PathDetailsView getPathDetailsView(String pathId) {
//        PathDetailsView pv = (PathDetailsView) getActivity().getLayoutInflater().inflate(R.layout.path_details, null);
        PathDetailsView pv = new PathDetailsView(getActivity());
        pv.setPathId(pathId);
        pv.setActionListener((PathDetailsActionListener) getActivity());
        return pv;
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
        mMap.setOnMapClickListener(this);
        mMap.setMyLocationEnabled(true);
        setMapTypeToUserPreference();

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(mCenterPoint)
                .zoom(mZoomLevel)
                .build();                   // Creates a CameraPosition from the builder
        Log.d(Constants.TRAILBOOK_TAG, getClass().getName() + ": zooming to " + mCenterPoint + " at zoom level " + mZoomLevel);
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
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
            BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.ic_placemark);
            endMarker.setIcon(icon);
        }
    }

    private void removePaoFromMap(String paoId) {
        Marker paoMarker = mPaoMarkers.get(paoId);
        if (paoMarker != null) {
            Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ": removing pao " + paoId);
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
            Log.d(Constants.TRAILBOOK_TAG, getClass().getName() + ": adding new marker");
            Marker marker = mMap.addMarker(getMarker(paoObject, iconId));
            mPaoMarkers.put(objectId, marker);
        } else {
            Log.d(Constants.TRAILBOOK_TAG, getClass().getName() + "updating existing marker");
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
                .anchor(.5f, .5f)
                .icon(BitmapDescriptorFactory.fromBitmap(scale(iconId)));

        return options;
    }

    private Bitmap scale (int resourceId) {
        BitmapDrawable bd=(BitmapDrawable) getResources().getDrawable(resourceId);
        Bitmap b=bd.getBitmap();
        Bitmap newBitmap=Bitmap.createScaledBitmap(b, b.getWidth()/4,b.getHeight()/4, false);
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
        if (oldLine != null)
            oldLine.setPoints(points);
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
        mPathPolylines.put(segmentId, polyline);
    }

    private void addPathSummaryToMap(PathSummary summary) {
        Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + "adding summary to map " + summary.getName());
        addEndMarkerToMap(summary);
    }

    private void addEndMarkerToMap(PathSummary summary) {
        if (mEndMarkers.get(summary.getId()) == null && summary != null && summary.getStart() != null && summary.getEnd() != null) {
            Marker endMarker = mMap.addMarker(getNewEndMarker(summary));
            mEndMarkers.put(summary.getId(), endMarker);
        }
    }

    private MarkerOptions getNewEndMarker(PathSummary summary) {
        BitmapDescriptor icon = null;
        if (mPathManager.isStoredLocally(summary.getId())) {
            icon = BitmapDescriptorFactory.fromResource(R.drawable.ic_placemark);
        } else {
            icon = BitmapDescriptorFactory.fromResource(R.drawable.ic_placemark_cloud);
        }

        MarkerOptions options = new MarkerOptions()
                .position(summary.getEnd())
                .title(summary.getName())
                .icon(icon);
        return options;
    }

    private void addStartMarkerToMap(PathSummary summary) {
        if (mStartMarkers.get(summary.getId()) == null && summary != null && summary.getStart() != null && summary.getEnd() != null) {
            Marker startMarker = mMap.addMarker(new MarkerOptions().position(summary.getStart()).title(summary.getName()));
            mStartMarkers.put(summary.getId(), startMarker);
        }
    }

    public void zoomToPath(String pathId) {
        ArrayList<Polyline> segmentLines = getPolylinesForPath(pathId);
        if (segmentLines != null && segmentLines.size()>0) {
            LatLngBounds bounds = MapUtilities.getBoundsForPolylineArray(segmentLines);
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 30));
        } else {
            PathSummary summary = mPathManager.getPathSummary(pathId);
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
    public void onLocationChangedEvent(LocationChangedEvent event) {
        if (mMap == null || !isMapLoaded()) {
            return; //don't want to queue location changed events because they are frequent and transient.
        }

        try {
            if (mPathManager == null || TrailBookState.getMode() != TrailBookState.MODE_FOLLOW)
                return;

            Location l = event.getLocation();
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
        } catch (Exception e) {
            Log.e(Constants.TRAILBOOK_TAG, "Exception updating note markers.  map may not have been initialized", e);
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
        Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ":onPathSummariesReceivedFromCloudEvent");
        if (mMap == null || !isMapLoaded()) {
            Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ":queueing onPathSummariesReceivedFromCloudEvent");
            queueEventIfMapNotAvailable(event);
            return;
        }

        ArrayList<PathSummary> summaries = event.getPathSummaries();
        for (PathSummary summary : summaries) {
            addPathSummaryToMap(summary);
        }
    }

    private void prepareMapForEditMode() {
        showOnlyPath(TrailBookState.getActivePathId());
        setVisibilityForAllEndMarkers(false);
        removeSelectedMarker();
        zoomToPath(TrailBookState.getActivePathId());
    }

    private void prepareMapForLeadMode() {
        showOnlyPath(TrailBookState.getActivePathId());
        setVisibilityForAllEndMarkers(false);
        removeSelectedMarker();
        zoomToCurrentLocation();
    }

    private void prepareMapForSearchMode() {
        showAllPaths();
        setVisibilityForAllNoteMarkers(false);
        setVisibilityForAllStartMarkers(false);
        removeSelectedMarker();
    }

    private void queueEventIfMapNotAvailable(Object event) {
        if (mMap == null || !isMapLoaded()) {
            Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ":queueing event " + event);
            mEventQueue.add(event);
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
        }
    }
}
