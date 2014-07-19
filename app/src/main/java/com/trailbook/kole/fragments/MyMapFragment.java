package com.trailbook.kole.fragments;

import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.trailbook.kole.activities.MapsActivity;
import com.trailbook.kole.activities.R;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.Note;
import com.trailbook.kole.data.Path;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.events.AllNotesAddedEvent;
import com.trailbook.kole.events.NoteAddedEvent;
import com.trailbook.kole.events.PathUpdatedEvent;
import com.trailbook.kole.tools.BusProvider;
import com.trailbook.kole.tools.PathManager;
import com.trailbook.kole.worker_fragments.LocationServicesFragment;
import com.trailbook.kole.worker_fragments.WorkerFragment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * Created by Fistik on 6/30/2014.
 */
public class MyMapFragment extends MapFragment implements GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener {
    private static final String LOG_TAG = "MyMapFragment";
    private static final float THICK = 10;
    private static final float MEDIUM = 6;

    private Bus bus;
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    private int mMode = Constants.SEARCH_MODE;
    private PathManager mPathManager;
    private HashMap<Marker, String> mMarkers;
    private HashMap<Polyline, String> mPathPolylines;
    private SlidingUpPanelLayout mMainPanel;
    private PathDetailsView mDetailsView;
    private HashMap<Marker,String> mNoteMarkers;

    /**
     * Listener interface to tell when the map is ready
     */
    public static interface OnMapReadyListener {
        void onMapReady();
    }

    public MyMapFragment() {
        super();
    }

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bus = BusProvider.getInstance();
        bus.register(this);
        mPathManager = PathManager.getInstance();

        mMarkers = new HashMap<Marker, String>();
        mNoteMarkers = new HashMap<Marker, String>();
        mPathPolylines = new HashMap<Polyline, String>();
        mMainPanel = (SlidingUpPanelLayout) getActivity().findViewById(R.id.main_panel);
    }

    @Override
    public void onDestroy () {
        bus.unregister(this);
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater arg0, ViewGroup arg1, Bundle arg2) {
        View v = super.onCreateView(arg0, arg1, arg2);

        mMap = this.getMap();
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapClickListener(this);
        setUpMapIfNeeded();
        if (mMode==Constants.SEARCH_MODE) {
            //get paths within the bounds of the current map.
            //testing: always search for paths for now

            getPathsFromDevice();
            startPathSummarySearch();
        }
        return v;
    }

    private void getPathsFromDevice() {
        mPathManager.loadPathsFromDevice(getActivity());
    }

    @Override
    public void onMapClick(LatLng latLng) {
        if (mMainPanel != null && mMainPanel.isPanelExpanded())
            mMainPanel.collapsePanel();

        //TODO: try to find the clicked path and treat as if clicked on the marker
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        String pathId = mMarkers.get(marker);
        Path path = mPathManager.getPath(pathId);
        PathSummary summary = null;
        if (path != null) {
            summary = path.getSummary();
            mDetailsView = (PathDetailsView)getActivity().findViewById(R.id.details_panel);
            //mDetailsView = new PathDetailsView(getActivity());
            mDetailsView.setPathId(pathId);

            WorkerFragment workFragment = ((MapsActivity)getActivity()).getWorkerFragment();
            LocationServicesFragment locationServicesFragment = ((MapsActivity)getActivity()).getLocationServicesFragment();
            mDetailsView.setDownloaderFragment(workFragment);
            mDetailsView.setLocationServiceFragment(locationServicesFragment);

            if (mMainPanel != null && !mMainPanel.isPanelExpanded())
                mMainPanel.expandPanel();
        }
        return false;
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
        //center the map on the current location
        mMap.setMyLocationEnabled(true);
        mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);


        Location currentLocation = mMap.getMyLocation();
        LatLng l;
        if (currentLocation ==  null)
            l = new LatLng(37.88478567867463,-119.34488281981714);
        else
            l = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(l, 6));
    }

    public static MyMapFragment newInstance() {
        MyMapFragment fragment = new MyMapFragment();
        return fragment;
    }

    public void startPathSummarySearch() {
        // If not retained (or first time running), we need to create it.
        WorkerFragment workFragment = ((MapsActivity)getActivity()).getWorkerFragment();
        workFragment.startGetPathSummaries(null, 0);
    }

    @Subscribe
    public void onNoteAddedEvent(NoteAddedEvent event){
        PointAttachedObject<Note> paoNote = event.getPaoNote();
        addPointNote(paoNote);
    }

    @Subscribe
    public void onAllNotesAddedEvent(AllNotesAddedEvent event){
        mPathManager.savePath(event.getPathId(), getActivity());
    }

    @Subscribe
    public void onPathUpdatedEvent(PathUpdatedEvent event){
        Path path = event.getPath();
        addPathToMap(path);
    }

    private void addPathToMap(Path path) {
        PolylineOptions lineOptions = getPolylineOptions(path);
        PathSummary summary = path.getSummary();
        addPathSummary(summary);
        if (path.getPointNotes() != null)
            addPointNotes(path.getPointNotes());

        addPoints(path, lineOptions);
    }

    private void addPointNotes(HashMap<String, PointAttachedObject<Note>> pointNotes) {
        Collection<PointAttachedObject<Note>> notes = pointNotes.values();
        for (PointAttachedObject<Note> paoNote:notes) {
            addPointNote(paoNote);
        }
    }

    private PolylineOptions getPolylineOptions(Path path) {
        PolylineOptions o = new PolylineOptions();
        if (path.isDownloaded()) {
            o.color(R.color.DodgerBlue);
            o.width(THICK);
        } else {
            o.color(R.color.PowderBlue);
            o.width(MEDIUM);
        }
        
        return o;
    }

    private void addPointNote(PointAttachedObject<Note> paoNote) {
        Note note = paoNote.getAttachment();
        String noteId = note.getNoteID();
        if (!mNoteMarkers.containsValue(noteId)){
            String content = "note";
            //TODO: image
            if (note.getNoteContent() != null && note.getNoteContent().length()>0)
                content = note.getNoteContent();
            MarkerOptions options =new MarkerOptions();
            //TODO: put the real note icon in
            Marker noteMarker = mMap.addMarker(options.position(paoNote.getLocation()).title(content));

            mMarkers.put(noteMarker, noteId);
        }
    }

    private void addPoints(Path p, PolylineOptions o) {
        String id = p.getId();
        ArrayList<LatLng> points = p.getPoints();
        if (points == null || points.size() < 2)
            return;

        //TODO: check if the points have changed.
        if (!mPathPolylines.containsValue(id)) {
            addPointsToPolylineOptions(o, points);
            putPolylineOnMap(o, id);
        }
    }

    private void addPointsToPolylineOptions(PolylineOptions o, ArrayList<LatLng> points) {
        for (LatLng point : points)
            o.add(point);
    }

    private void putPolylineOnMap(PolylineOptions o, String id) {
        Polyline polyline = mMap.addPolyline(o);
        mPathPolylines.put(polyline, id);
    }

    private void addPathSummary(PathSummary summary) {
        //TODO: check if start or end has changed.
        if (!mMarkers.containsValue((summary.getId()))) {
            Marker startMarker = mMap.addMarker(new MarkerOptions().position(summary.getStart()).title(summary.getName()));
            mMarkers.put(startMarker, summary.getId());
            Marker endMarker = mMap.addMarker(new MarkerOptions().position(summary.getEnd()).title("end " + summary.getName()));
            mMarkers.put(endMarker, summary.getId());
        }
    }
}
