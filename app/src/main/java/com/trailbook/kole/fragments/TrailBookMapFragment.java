package com.trailbook.kole.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
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

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
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
import com.trailbook.kole.data.Note;
import com.trailbook.kole.data.PathSegment;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.events.LocationChangedEvent;
import com.trailbook.kole.events.ModeChangedEvent;
import com.trailbook.kole.events.NoteAddedEvent;
import com.trailbook.kole.events.PathDeletedEvent;
import com.trailbook.kole.events.PathSummaryAddedEvent;
import com.trailbook.kole.events.SegmentDeletedEvent;
import com.trailbook.kole.events.SegmentUpdatedEvent;
import com.trailbook.kole.helpers.MapUtilities;
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
    private static final String SAVED_LAT_LNG_BOUNDS_GSON = "BOUNDS";
    private static final float DEFAULT_ZOOM_LEVEL = 16;

    private String mActivePathId;
    //private TrailBookActivity parentActivity;
    private SharedPreferences savedStatePrefs;

    public enum MarkerType {START,END,NOTE,UNKNOWN}

    private Bus bus;
    private LinkedList mEventQueue;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private LatLngBounds mBounds;
    private static final LatLngBounds startBounds = new LatLngBounds(
            new LatLng( 31.922209,-123.235040), new LatLng( 42.054010,-109.056520)
    );

    private PathManager mPathManager;
    private BidiMap<String, Marker> mStartMarkers;
    private BidiMap<String, Marker> mEndMarkers;
    private BidiMap<String, Polyline> mPathPolylines;
    private SlidingUpPanelLayout slidingPanel;
    private BidiMap<String,Marker> mNoteMarkers;
    private boolean mIsMapLoaded = false;

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
        setMapTypeToUserPreference();
    }

    private void setMapTypeToUserPreference() {
        int mapPreference = PreferenceUtilities.getMapType(getActivity());
        Log.d(Constants.TRAILBOOK_TAG, "map preference:" + mapPreference);
        if (mMap != null) {
            mMap.setMapType(mapPreference);
        }
    }

    private void restoreState(Bundle savedInstanceState) {
        Log.d(Constants.TRAILBOOK_TAG, "restoring map");
        savedStatePrefs = getActivity().getPreferences(Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String startBoundsJson = gson.toJson(startBounds, LatLngBounds.class);
        String jsonLastBounds = savedStatePrefs.getString(SAVED_LAT_LNG_BOUNDS_GSON, startBoundsJson);
        mBounds = gson.fromJson(jsonLastBounds, LatLngBounds.class);
        Log.d(Constants.TRAILBOOK_TAG, "TrailBookMapFragment: restored bounds:" + mBounds);
    }

    @Override
    public void onMapLoaded() {
        Location l = TrailBookState.getCurrentLocation();
        Log.d(Constants.TRAILBOOK_TAG, "map loaded.  bounds:" + mBounds);
        Log.d(Constants.TRAILBOOK_TAG, "current location: " + l);
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(mBounds, 30));
        setMapTypeToUserPreference();
        mIsMapLoaded = true;
    }

    private void createMapObjects() {
        mStartMarkers = new DualHashBidiMap<String, Marker>();
        mEndMarkers = new DualHashBidiMap<String, Marker>();
        mNoteMarkers = new DualHashBidiMap<String, Marker>();
        mPathPolylines = new DualHashBidiMap<String, Polyline>();
        mIsMapLoaded = false;
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

        mMap = this.getMap();
        setUpMapIfNeeded();
        hideMapMessage();
        Log.d(Constants.TRAILBOOK_TAG, "TrailBookMapFragment Mode:" + TrailBookState.getMode());

        drawLoadedPaths();
        processQueuedEvents();

        Bundle savedState = getArguments();
        if (savedState != null)
            restoreState(savedState);

        return v;
    }

    private void drawLoadedPaths() {
        ArrayList<PathSummary> summaries = mPathManager.getDownloadedPathSummaries();
        for (PathSummary summary:summaries) {
            addPathSummaryToMap(summary);
            ArrayList<PathSegment> segments = mPathManager.getSegmentsForPath(summary.getId());
            for (PathSegment segment:segments) {
                addSegmentToMap(segment);
            }
        }
    }

    private void hideMapMessage() {
        TextView tvMapMessage = (TextView)(getActivity().findViewById(R.id.map_tv_message));
        if (tvMapMessage != null) {
            tvMapMessage.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.d(Constants.TRAILBOOK_TAG, "saving map fragment instance state");
        String jsonBounds = getJsonBounds();
        if (isBoundsValid(jsonBounds))
            saveLastBounds(jsonBounds);
        //getArguments().putString(SAVED_LAT_LNG_BOUNDS_GSON, jsonBounds);
    }

    private boolean isBoundsValid(String jsonBounds) {
        return !jsonBounds.contains("\"latitude\":0.0,\"longitude\":0.0");
    }

    private void saveLastBounds(String jsonBounds) {
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(SAVED_LAT_LNG_BOUNDS_GSON, jsonBounds);
        editor.commit();
        Log.d(Constants.TRAILBOOK_TAG, "TrailBookMapFragment: Saved bounds " + jsonBounds);
    }

    private String getJsonBounds() {
        mBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
        Gson gson = new Gson();
        String jsonBounds = gson.toJson(mBounds);
        Log.d(Constants.TRAILBOOK_TAG, "TrailBookMapFragment: bounds," + jsonBounds);
        return jsonBounds;
    }

    public void showNotesOnlyForPath(String pathId) {
        hideAllPaths();
        setVisibilityForAllNoteMarkers(false);
        ArrayList<PointAttachedObject<Note>> pointNotes = mPathManager.getPointNotesForPath(pathId);
        showPointNotes(pointNotes);
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
        if (mNoteMarkers == null)
            return;

        Set<Marker> noteMarkers = mNoteMarkers.values();
        ArrayList<String> removedNoteIds = new ArrayList<String>();
        for (Marker m:noteMarkers) {
            String noteId = mNoteMarkers.getKey(m);
            removedNoteIds.add(noteId);
            m.setVisible(show);
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {
        Log.d(Constants.TRAILBOOK_TAG, "Clicked map, " + latLng.latitude + "," + latLng.longitude);
        collapseSlidingPanelIfExpanded();

        //TODO: try to find the clicked path and treat as if clicked on the marker
    }

    private void collapseSlidingPanelIfExpanded() {
        if (slidingPanel != null && slidingPanel.isPanelExpanded())
            slidingPanel.collapsePanel();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        MarkerType markerType = getMarkerType(marker);
        if (markerType == MarkerType.START) {
//            collapseSlidingPanelIfExpanded();
            String pathId = mStartMarkers.getKey(marker);
            return showPathSummary(pathId);
        } else if (markerType == MarkerType.END) {
//            collapseSlidingPanelIfExpanded();
            String pathId = mEndMarkers.getKey(marker);
            return showPathSummary(pathId);
        }  else if (markerType == MarkerType.NOTE) {
//            collapseSlidingPanelIfExpanded();
            String noteId = mNoteMarkers.getKey(marker);
            return showNote(noteId);
        }
        return false;
    }

    @Override
    public void onClick(View view) {
        Log.d(Constants.TRAILBOOK_TAG, "View clicked:" + view.getId() + ", " + view.getTag());
        if (view.getId() == R.id.nv_small_note_layout || view.getId() == R.id.vn_button_expand) {
            Log.d(Constants.TRAILBOOK_TAG, "Note clicked");
            collapseSlidingPanelIfExpanded();

            ((TrailBookActivity)getActivity()).showFullNote(((NoteView)view).getNoteId());
        }
    }

    private boolean showNote(String noteId) {
        if (noteId == null) {
            Log.d(Constants.TRAILBOOK_TAG, "Null Path ID");
            return true;
        }
        noteId = noteId.substring(0,noteId.length());

        if (slidingPanel != null) {
/*
            int height = getFullWindowHeight();
            slidingPanel.setPanelHeight(height/2);
            slidingPanel.requestLayout();
*/
            expandSlidingPanelIfCollapsed();
            NoteView nv = getNoteView(noteId);
            nv.setOnClickListener(this);
            addViewToSlidingUpPanel(nv);
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

    private NoteView getNoteView(String noteId) {
        NoteView nv = new SmallNoteView(getActivity());
        nv.setNoteId(noteId);
        return nv;
    }

    private boolean showPathSummary(String pathId) {
        if (pathId == null) {
            Log.d(Constants.TRAILBOOK_TAG, "Null Path ID");
            return true;
        }

        if (slidingPanel != null) {
/*            int height = getFullWindowHeight();
            slidingPanel.setPanelHeight(height/4);
            slidingPanel.requestLayout();*/

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
        id = mNoteMarkers.getKey(marker);
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
        mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);


/*        Location currentLocation = mMap.getMyLocation();
        LatLng l;
        if (currentLocation ==  null)
            l = new LatLng(37.88478567867463,-119.34488281981714);
        else
            l = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(l, 6));*/
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

    private void removeSegmentFromMap(PathSegment segment) {
//deleteme        removePointNotes(segment.getPointNotes());
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

    private boolean isDisplayNotes(String pathId) {
        if (!pathId.equals(mActivePathId))
            return false;
        //if (((MapsActivity)getActivity()).getMode() == MapsActivity.MODE_SEARCH)
        if (TrailBookState.getMode() == TrailBookState.MODE_SEARCH)
            return false;

        return true;
    }

    private void addPointNotes(ArrayList<PointAttachedObject<Note>> pointNotes) {
        for (PointAttachedObject<Note> paoNote:pointNotes) {
            addPointNote(paoNote, R.drawable.ic_map_note_unselected);
        }
    }

    private void showPointNotes(ArrayList<PointAttachedObject<Note>> pointNotes) {
        for (PointAttachedObject<Note> paoNote:pointNotes) {
            if (paoNote != null)
                showPointNote(paoNote);
        }
    }

    private void showPointNote(PointAttachedObject<Note> paoNote) {
        String noteId = paoNote.getId();
        Marker m = mNoteMarkers.get(noteId);
        if (m != null)
            m.setVisible(true);
        else
            addPointNote(paoNote, R.drawable.ic_map_note_unselected);
    }

    private PolylineOptions getPolylineOptions(PathSegment segment) {
        PolylineOptions o = new PolylineOptions();
//        o.color(R.color.Cornsilk);
        o.width(THICK);

        return o;
    }

    private void addPointNote(PointAttachedObject<Note> paoNote, int iconId) {
        String noteId = paoNote.getId();
        Marker currentMarker = mNoteMarkers.get(noteId);
        if (currentMarker == null){
            Log.d(Constants.TRAILBOOK_TAG, "adding new marker");
        } else {
            Log.d(Constants.TRAILBOOK_TAG, "updating existing marker");
            currentMarker.remove();
            mNoteMarkers.remove(noteId);
        }
        Marker noteMarker = mMap.addMarker(getNoteMarker(paoNote, iconId));
        mNoteMarkers.put(noteId, noteMarker);
    }

    private MarkerOptions getNoteMarker(PointAttachedObject<Note> paoNote, int iconId) {
        String content = paoNote.getAttachment().getNoteContent();
        MarkerOptions options =  new MarkerOptions()
                .position(paoNote.getLocation())
                .title(content)
                .anchor(.5f,.5f)
                .icon(BitmapDescriptorFactory.fromBitmap(scale(iconId)));
//here select the icon based on distance to note
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
        //TODO: check if start or end has changed.
        addEndMarkerToMap(summary);
    }

    private void addEndMarkerToMap(PathSummary summary) {
        if (mEndMarkers.get(summary.getId()) == null && summary != null && summary.getStart() != null && summary.getEnd() != null) {
            Marker endMarker = mMap.addMarker(getNewEndMarker(summary));
            mEndMarkers.put(summary.getId(), endMarker);
        }
    }

    private MarkerOptions getNewEndMarker(PathSummary summary) {
        MarkerOptions options = new MarkerOptions()
                .position(summary.getEnd())
                .title(summary.getName())
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_placemark));
        return options;
    }

    private void addStartMarkerToMap(PathSummary summary) {
        if (mStartMarkers.get(summary.getId()) == null && summary != null && summary.getStart() != null && summary.getEnd() != null) {
            Marker startMarker = mMap.addMarker(new MarkerOptions().position(summary.getStart()).title(summary.getName()));
            mStartMarkers.put(summary.getId(), startMarker);
        }
    }

    public void zoom(String pathId) {
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
        if (mMap == null)
            return; //don't want to queue location changed events because they are frequent and transient.

        try {
            if (mPathManager == null || TrailBookState.getMode() != TrailBookState.MODE_FOLLOW)
                return;

            Location l = event.getLocation();
            String currentPath = TrailBookState.getActivePathId();
            Log.d(Constants.TRAILBOOK_TAG, "current path id: " + currentPath);
            ArrayList<PointAttachedObject<Note>> paoNotes = mPathManager.getPointNotesForPath(currentPath);
            if (paoNotes != null) {
                for (PointAttachedObject<Note> paoNote : paoNotes) {
                    double distanceToNote = TrailbookPathUtilities.getDistanceToNote(paoNote, l);
                    if (distanceToNote < PreferenceUtilities.getNoteAlertDistanceInMeters(getActivity())) {
                        Log.d(Constants.TRAILBOOK_TAG, "adding selected note " + paoNote.getAttachment().getNoteContent());
                        addPointNote(paoNote, R.drawable.ic_map_note_selected);
                    } else {
                        Log.d(Constants.TRAILBOOK_TAG, "adding unselected note" + paoNote.getAttachment().getNoteContent());
                        addPointNote(paoNote, R.drawable.ic_map_note_unselected);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(Constants.TRAILBOOK_TAG, "Exception updating note markers.  map may not have been initialized", e);
        }
    }


    @Subscribe
    public void onSegmentDeletedEvent(SegmentDeletedEvent event) {
        if (mMap == null) {
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
    public void onNoteAddedEvent(NoteAddedEvent event){
        if (mMap == null) {
            queueEventIfMapNotAvailable(event);
            return;
        }

        try {
            PointAttachedObject<Note> paoNote = event.getPaoNote();
            if ( (TrailBookState.getMode() == TrailBookState.MODE_FOLLOW ||
                    TrailBookState.getMode() == TrailBookState.MODE_LEAD) &&
                    mPathManager.noteBelongsToPath(paoNote.getId(), TrailBookState.getActivePathId())) {
                addPointNote(paoNote, R.drawable.ic_map_note_unselected);
            }

        } catch (Exception e) {
            Log.e(Constants.TRAILBOOK_TAG, "Exception onNoteAddedEvent.  map may not have been initialized", e);
        }
    }

    @Subscribe
    public void onPathSummaryAddedEvent(PathSummaryAddedEvent event){
        if (mMap == null) {
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
        if (mMap == null) {
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
        if (mMap == null) {
            queueEventIfMapNotAvailable(event);
            return;
        }

        try {
            String id = event.getPath().getId();
            removeEndMarker(id);
            removeStartMarker(id);
        } catch (Exception e) {
            Log.e(Constants.TRAILBOOK_TAG, "Exception onPathDeletedEvent.  map may not have been initialized", e);
        }
    }

    @Subscribe
    public void onModeChangedEvent(ModeChangedEvent event) {
        if (mMap == null) {
            queueEventIfMapNotAvailable(event);
            return;
        }

        if (event.getNewMode() == TrailBookState.MODE_SEARCH) {
            showAllPaths();
            setVisibilityForAllNoteMarkers(false);
            setVisibilityForAllStartMarkers(false);
        } else if (event.getNewMode() == TrailBookState.MODE_LEAD) {
            showNotesOnlyForPath(TrailBookState.getActivePathId());
            setVisibilityForAllEndMarkers(false);
            zoomToCurrentLocation();
        }
    }

    private void queueEventIfMapNotAvailable(Object event) {
        if (mMap == null)
            mEventQueue.add(event);
    }

    private void processQueuedEvents() {
        while (!mEventQueue.isEmpty()){
            Object event = mEventQueue.remove();
            Log.d(Constants.TRAILBOOK_TAG, "processing missed event: " + event);
            if (event instanceof SegmentDeletedEvent)
                onSegmentDeletedEvent((SegmentDeletedEvent) event);
            else if (event instanceof NoteAddedEvent)
                onNoteAddedEvent((NoteAddedEvent) event);
            else if (event instanceof PathSummaryAddedEvent)
                onPathSummaryAddedEvent((PathSummaryAddedEvent) event);
            else if (event instanceof SegmentUpdatedEvent)
                onSegmentUpdatedEvent((SegmentUpdatedEvent) event);
            else if (event instanceof PathDeletedEvent)
                onPathDeletedEvent((PathDeletedEvent) event);
            else if (event instanceof ModeChangedEvent)
                onModeChangedEvent((ModeChangedEvent) event);
        }
    }



}
