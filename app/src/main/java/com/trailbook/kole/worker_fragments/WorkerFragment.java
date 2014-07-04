package com.trailbook.kole.worker_fragments;

import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.squareup.otto.Bus;
import com.squareup.otto.Produce;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.events.PathSummariesReceivedEvent;
import com.trailbook.kole.services.PathService;
import com.trailbook.kole.tools.BusProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * This background fragment has no UI. It gets path summaries from a web service call
 * and puts the summaries on the bus.
 */
public class WorkerFragment extends Fragment {
    private Bus bus;

    /**
     * Fragment initialization.  We way we want to be retained and
     * start the async call.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        bus=BusProvider.getInstance();
        bus.register(this);
    }
    /**
     * This is called when the Fragment's Activity is ready to go, after
     * its content view has been installed; it is called both after
     * the initial fragment creation and after the fragment is re-attached
     * to a new activity.
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //do stuff here for when the activity is destroyed an re-created (orientation change)
    }
    /**
     * This is called when the fragment is going away.  It is NOT called
     * when the fragment is being propagated between activity instances.
     */
    @Override
    public void onDestroy() {
        bus.unregister(this);
        // Kill any running service

        super.onDestroy();
    }
    /**
     * This is called right before the fragment is detached from its
     * current activity instance.
     */
    @Override
    public void onDetach() {
        // This fragment is being detached from its activity.  We need
        // to make sure its thread is not going to touch any activity
        // state after returning from this function.

        super.onDetach();
    }
    /**
     * Kick off the path load.
     */
    public void startGetPathSummaries(LatLng center, long radius) {
        Map<String, String> options = new HashMap<String, String>();
        options.put("lat", center==null?"0":String.valueOf(center.latitude));
        options.put("lon", center==null?"0":String.valueOf(center.longitude));
        options.put("radius", String.valueOf(radius));

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(Constants.BASE_URL)
                .build();
        Callback<ArrayList<PathSummary>> callback = new Callback<ArrayList<PathSummary>>(){
            @Override
            public void failure(RetrofitError error) {
                Log.e("Trailbook", "Failed to get path summaries", error);
            }

            @Override
            public void success(ArrayList<PathSummary> pathSummaries, Response response) {
                bus.post(new PathSummariesReceivedEvent(pathSummaries));
            }
        };

        PathService service = restAdapter.create(PathService.class);
        service.getPaths(options, callback);
    }
}