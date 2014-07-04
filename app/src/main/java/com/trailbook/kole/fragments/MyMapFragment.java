package com.trailbook.kole.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.Path;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.events.PathSummariesReceivedEvent;
import com.trailbook.kole.events.PathSummaryAddedEvent;
import com.trailbook.kole.tools.BusProvider;
import com.trailbook.kole.tools.PathManager;
import com.trailbook.kole.worker_fragments.WorkerFragment;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Fistik on 6/30/2014.
 */
public class MyMapFragment extends MapFragment implements GoogleMap.OnMarkerClickListener {
    private static final String LOG_TAG = "MyMapFragment";

    private Bus bus;
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    private WorkerFragment mWorkFragment;
    private int mMode = Constants.SEARCH_MODE;
    private PathManager mPathManager;
    private HashMap<Marker, String> mMarkers;

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
        setUpMapIfNeeded();
        if (mMode==Constants.SEARCH_MODE) {
            //get paths within the bounds of the current map.
            //testing: always search for paths for now
            startPathSummarySearch();
        }
        return v;
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
            l = new LatLng(0,0);
        else
            l = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(l, 3));
    }

    public static MyMapFragment newInstance() {
        MyMapFragment fragment = new MyMapFragment();
        return fragment;
    }

    public void startPathSummarySearch() {
        FragmentManager fm = getFragmentManager();
        // Check to see if we have retained the worker fragment.
        mWorkFragment = (WorkerFragment)fm.findFragmentByTag("work");

        // If not retained (or first time running), we need to create it.
        if (mWorkFragment == null) {
            mWorkFragment = new WorkerFragment();
            // Tell it who it is working with.
            mWorkFragment.startGetPathSummaries(null, 0);
            fm.beginTransaction().add(mWorkFragment, "work").commit();
        }
    }

    @Subscribe
    public void onPathSummaryAddedEvent(PathSummaryAddedEvent event){
        PathSummary summary = event.getPathSummary();
        Marker startMarker = mMap.addMarker(new MarkerOptions().position(summary.getStart()).title(summary.getName()));
        Marker endMarker = mMap.addMarker(new MarkerOptions().position(summary.getEnd()).title("end " + summary.getName()));
        mMarkers.put(startMarker, summary.getId());
        mMarkers.put(endMarker, summary.getId());
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        String pathId = mMarkers.get(marker);
        Toast.makeText(getActivity(), pathId, Toast.LENGTH_SHORT).show();
        return false;
    }
}
