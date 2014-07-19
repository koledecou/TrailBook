package com.trailbook.kole.worker_fragments;

import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.Note;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.events.NoteAddedEvent;
import com.trailbook.kole.events.NotesReceivedEvent;
import com.trailbook.kole.events.PathPointsReceivedEvent;
import com.trailbook.kole.events.PathSummariesReceivedEvent;
import com.trailbook.kole.events.PathSummaryAddedEvent;
import com.trailbook.kole.services.TrailbookPathServices;
import com.trailbook.kole.tools.BusProvider;
import com.trailbook.kole.tools.BitmapFileTarget;
import com.trailbook.kole.tools.TrailbookFileUtilities;

import java.io.File;
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
    private RestAdapter mRestAdapter;
    private TrailbookPathServices mService;

    public WorkerFragment () {
        super();

        bus=BusProvider.getInstance();
        bus.register(this);
        initializeRestAdaptor();
    }

    /**
     * Fragment initialization. We want to be retained.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    private void initializeRestAdaptor() {
        mRestAdapter = new RestAdapter.Builder()
                .setEndpoint(Constants.BASE_URL)
                .build();
        mService = mRestAdapter.create(TrailbookPathServices.class);
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
        //TODO: Kill any running service

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

        mService.getPathSummaries(options, callback);
    }

    public void startGetPathPoints(String pathId, Integer maxPoints) {
        if (maxPoints == null)
            maxPoints = -1; // get all points

        Map<String, String> options = new HashMap<String, String>();
        options.put("pathid",pathId);
        options.put("maxPoints", String.valueOf(maxPoints));

        Callback<PathPointsReceivedEvent.PathIDWithPoints> callback = new Callback<PathPointsReceivedEvent.PathIDWithPoints>(){
            @Override
            public void failure(RetrofitError error) {
                Log.e("Trailbook", "Failed to get path points", error);
            }

            @Override
            public void success(PathPointsReceivedEvent.PathIDWithPoints pathIDWithPoints, Response response) {
                bus.post(new PathPointsReceivedEvent(pathIDWithPoints));
            }
        };

        mService.getPathPoints(options, callback);
    }

    @Subscribe
    public void onPathSummaryAddedEvent(PathSummaryAddedEvent event){
        PathSummary summary = event.getPathSummary();

        startGetPathPoints(summary.getId(), new Integer(Constants.MEDIUM_DETAIL));
    }

    @Subscribe
    public void onNoteAddedEvent(NoteAddedEvent event) {
        PointAttachedObject<Note> paoNote = event.getPaoNote();
        String imageFileName = paoNote.getAttachment().imageFileName;
        if (imageFileName != null && imageFileName.length()>0)
            startGetImage(paoNote.getAttachment());
    }

    private void startGetImage(Note note) {
        String imageFileName = note.getImageFileName();
        String pathId = note.getParentPathId();
        File imageFile = TrailbookFileUtilities.getInternalImageFile(getActivity(), pathId, imageFileName);
        Picasso.with(getActivity()).load(Constants.webServerImageDir + "/" + imageFileName).into(new BitmapFileTarget(imageFile));
    }

    public void startGetNotes(String pathId) {
        Map<String, String> options = new HashMap<String, String>();
        options.put("pathid",pathId);

        Callback<NotesReceivedEvent.PathIDWithNotes> callback = new Callback<NotesReceivedEvent.PathIDWithNotes>(){
            @Override
            public void failure(RetrofitError error) {
                Log.e("Trailbook", "Failed to get notes", error);
            }

            @Override
            public void success(NotesReceivedEvent.PathIDWithNotes pathIDWithNotes, Response response) {
                bus.post(new NotesReceivedEvent(pathIDWithNotes));
            }
        };

        mService.getNotes(options, callback);
    }
}