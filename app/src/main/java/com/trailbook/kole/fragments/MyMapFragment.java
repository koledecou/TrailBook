package com.trailbook.kole.fragments;

import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tagmanager.Container;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.trailbook.kole.activities.MapsActivity;
import com.trailbook.kole.activities.R;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.Note;
import com.trailbook.kole.data.Path;
import com.trailbook.kole.data.PathSegment;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.events.AllNotesAddedEvent;
import com.trailbook.kole.events.NoteAddedEvent;
import com.trailbook.kole.events.PathSummaryAddedEvent;
import com.trailbook.kole.events.PathUpdatedEvent;
import com.trailbook.kole.events.SegmentUpdatedEvent;
import com.trailbook.kole.tools.BusProvider;
import com.trailbook.kole.tools.PathManager;
import com.trailbook.kole.tools.TrailbookPathUtilities;
import com.trailbook.kole.worker_fragments.WorkerFragment;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by Fistik on 6/30/2014.
 */
public class MyMapFragment extends MapFragment implements GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener {
    private static final String LOG_TAG = "MyMapFragment";
    private static final float THICK = 10;
    private static final float MEDIUM = 6;
    private String mActivePathId;

    public enum MarkerType {START,END,NOTE,UNKNOWN}

    private Bus bus;
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    private int mMode = Constants.SEARCH_MODE;
    private PathManager mPathManager;
    private BidiMap<String, Marker> mStartMarkers;
    private BidiMap<String, Marker> mEndMarkers;
    private BidiMap<String, Polyline> mPathPolylines;
    private SlidingUpPanelLayout mMainPanel;
    private BidiMap<String,Marker> mNoteMarkers;

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

        mStartMarkers = new DualHashBidiMap<String, Marker>();
        mEndMarkers = new DualHashBidiMap<String, Marker>();
        mNoteMarkers = new DualHashBidiMap<String, Marker>();
        mPathPolylines = new DualHashBidiMap<String, Polyline>();
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
            //todo: get paths within the bounds of the current map.
            //todo: testing: always search for paths for now

            mPathManager.loadPathsFromDevice(getActivity());
            startPathSummarySearch();
        }
        return v;
    }

    public void showNotesOnlyForPath(String pathId) {
        hideAllNoteMarkers();
        HashMap<String, PointAttachedObject<Note>> pointNotes = mPathManager.getPointNotesForPath(pathId);
        showPointNotes(pointNotes);
    }

    private void hideAllNoteMarkers() {
        Set<Marker> noteMarkers = mNoteMarkers.values();
        ArrayList<String> removedNoteIds = new ArrayList<String>();
        for (Marker m:noteMarkers) {
            String noteId = mNoteMarkers.getKey(m);
            removedNoteIds.add(noteId);
            m.setVisible(false);
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {
        if (mMainPanel != null && mMainPanel.isPanelExpanded())
            mMainPanel.collapsePanel();

        //TODO: try to find the clicked path and treat as if clicked on the marker
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
            String noteId = mNoteMarkers.getKey(marker);
            return showNote(noteId);
        }
        return false;
    }

    private boolean showNote(String noteId) {
        if (noteId == null) {
            Log.d(Constants.TRAILBOOK_TAG, "Null Path ID");
            return true;
        }
        noteId = noteId.substring(0,noteId.length());

        NoteView nv = getNoteView(noteId);
        addViewToSlidingUpPanel(nv);

        if (mMainPanel != null && !mMainPanel.isPanelExpanded())
            mMainPanel.expandPanel();

        return false;
    }

    private NoteView getNoteView(String noteId) {
//        NoteView nv = (NoteView) getActivity().getLayoutInflater().inflate(R.layout.view_note, null);
        NoteView nv = new NoteView(getActivity());
        nv.setNoteId(noteId);
        return nv;
    }

    private boolean showPathSummary(String pathId) {
        if (pathId == null) {
            Log.d(Constants.TRAILBOOK_TAG, "Null Path ID");
            return true;
        }
        PathDetailsView pdv = getPathDetailsView(pathId);
        addViewToSlidingUpPanel(pdv);

        if (mMainPanel != null && !mMainPanel.isPanelExpanded())
            mMainPanel.expandPanel();

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
    public void onPathSummaryAddedEvent(PathSummaryAddedEvent event){
        PathSummary summary = event.getPathSummary();
        addPathSummaryToMap(summary);
    }

    @Subscribe
    public void onSegmentUpdatedEvent(SegmentUpdatedEvent event){
        PathSegment seg = event.getSegment();
        addSegmentToMap(seg);
    }

    private void addSegmentToMap(PathSegment segment) {
        if (segment.getPointNotes() != null)
            addPointNotes(segment.getPointNotes());

        if (segment.getPoints() != null)
            addPoints(segment, getPolylineOptions(segment));
    }

    private boolean isDisplayNotes(String pathId) {
        if (!pathId.equals(mActivePathId))
            return false;
        if (((MapsActivity)getActivity()).getMode() == MapsActivity.Mode.SEARCH)
            return false;

        return true;
    }

    private void addPointNotes(HashMap<String, PointAttachedObject<Note>> pointNotes) {
        Collection<PointAttachedObject<Note>> notes = pointNotes.values();
        for (PointAttachedObject<Note> paoNote:notes) {
            addPointNote(paoNote);
        }
    }

    private void showPointNotes(HashMap<String, PointAttachedObject<Note>> pointNotes) {
        Collection<PointAttachedObject<Note>> notes = pointNotes.values();
        for (PointAttachedObject<Note> paoNote:notes) {
            showPointNote(paoNote);
        }
    }

    private void showPointNote(PointAttachedObject<Note> paoNote) {
        String noteId = paoNote.getAttachment().getNoteID();
        Marker m = mNoteMarkers.get(noteId);
        if (m != null)
            m.setVisible(true);
        else
            addPointNote(paoNote);
    }

    private PolylineOptions getPolylineOptions(PathSegment segment) {
        PolylineOptions o = new PolylineOptions();
        o.color(R.color.Cornsilk);
        o.width(THICK);

        return o;
    }

    private void addPointNote(PointAttachedObject<Note> paoNote) {
        Note note = paoNote.getAttachment();
        String noteId = note.getNoteID();
        if (mNoteMarkers.get(noteId) == null){
            String content = "note";
            //TODO: image
            if (note.getNoteContent() != null && note.getNoteContent().length()>0)
                content = note.getNoteContent();
            MarkerOptions options =new MarkerOptions();
            //TODO: put the real note icon in
            Marker noteMarker = mMap.addMarker(options.position(paoNote.getLocation()).title(content));

            mNoteMarkers.put(noteId, noteMarker);
        }
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

    private void putPolylineOnMap(PolylineOptions o, ArrayList<LatLng> points, String id) {
        if (points == null || points.size() < 2)
            return;

        Polyline oldLine = mPathPolylines.get(id);
        if (oldLine != null)
            oldLine.setPoints(points);
        else {
            putNewPolylineOnMap(o, points, id);
        }
    }

    private void putNewPolylineOnMap(PolylineOptions o, ArrayList<LatLng> points, String id) {
        addPointsToPolylineOptions(o, points);
        Polyline polyline = mMap.addPolyline(o);
        mPathPolylines.put(id, polyline);
    }

    private void addPathSummaryToMap(PathSummary summary) {
        //TODO: check if start has changed.
        if (mEndMarkers.get(summary.getId()) == null && summary != null && summary.getStart() != null && summary.getEnd() != null) {
//            Marker startMarker = mMap.addMarker(new MarkerOptions().position(summary.getStart()).title(summary.getName()));
//            mStartMarkers.put(summary.getId(), startMarker);
            Marker endMarker = mMap.addMarker(new MarkerOptions().position(summary.getEnd()).title("end " + summary.getName()));
            mEndMarkers.put(summary.getId(), endMarker);
        }
    }

    public void zoom(String pathId) {
        Path p = mPathManager.getPath(pathId);
        LatLng start = p.getSummary().getStart();
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(start, mMap.getMaxZoomLevel()));
    }
}
