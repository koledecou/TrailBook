package com.trailbook.kole.worker_fragments;

import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.Note;
import com.trailbook.kole.data.Path;
import com.trailbook.kole.data.PathSegment;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.events.NoteAddedEvent;
import com.trailbook.kole.events.NotesReceivedEvent;
import com.trailbook.kole.events.PathSegmentMapRecievedEvent;
import com.trailbook.kole.events.PathSummariesReceivedEvent;
import com.trailbook.kole.events.SegmentPointsReceivedEvent;
import com.trailbook.kole.services.TrailbookPathServices;
import com.trailbook.kole.tools.BusProvider;
import com.trailbook.kole.tools.DownloadImageTask;
import com.trailbook.kole.tools.PathManager;
import com.trailbook.kole.tools.TrailbookFileUtilities;
import com.trailbook.kole.tools.TrailbookPathUtilities;

import org.apache.http.entity.mime.MultipartEntity;

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
    private PathManager pathManager = PathManager.getInstance();

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
                .setEndpoint(Constants.BASE_CGIBIN_URL)
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
                Log.d(Constants.TRAILBOOK_TAG, "Loaded path summary count, " + pathSummaries.size());
                bus.post(new PathSummariesReceivedEvent(pathSummaries));
            }
        };

        mService.getPathSummaries(options, callback);
    }

    public void startGetPathPoints(String pathId, Integer maxPoints) {
        Map<String, String> options = new HashMap<String, String>();
        options.put("segmentid",pathId);

        Callback<SegmentPointsReceivedEvent.SegmentIDWithPoints> callback = new Callback<SegmentPointsReceivedEvent.SegmentIDWithPoints>(){
            @Override
            public void failure(RetrofitError error) {
                Log.e("Trailbook", "Failed to get path points", error);
            }

            @Override
            public void success(SegmentPointsReceivedEvent.SegmentIDWithPoints segmentIDWithPoints, Response response) {
                bus.post(new SegmentPointsReceivedEvent(segmentIDWithPoints));
            }
        };

        mService.getPoints(options, callback);
    }
/*
    @Subscribe
    public void onPathSummaryAddedEvent(PathSummaryAddedEvent event){
        PathSummary summary = event.getPathSummary();
        startGetPathPoints(summary.getId(), new Integer(Constants.MEDIUM_DETAIL));
    }
*/
    private void startGetImage(Note note) {
        String imageFileName = note.getImageFileName();
        String segmentId = note.getParentSegmentId();
        File imageFile = TrailbookFileUtilities.getInternalImageFile(getActivity(), segmentId, imageFileName);

        Log.d(Constants.TRAILBOOK_TAG, "image file name: " + imageFile);
        String webServerImageDir=TrailbookFileUtilities.getWebServerImageDir(segmentId);
        String webServerImageFileName = webServerImageDir + "/" + imageFileName;

        Log.d(Constants.TRAILBOOK_TAG, "webserver image file name: " + webServerImageFileName);
        new DownloadImageTask(imageFile).execute(webServerImageFileName);

        //TODO: use picasso to get the image?
//        Picasso.with(getActivity()).load(webServerImageFileName).into(new BitmapFileTarget(imageFile));
    }

    public void startPathUpload(Path p) {
        Callback<String> pathSummaryUploadedCallback = new Callback<String>(){
            @Override
            public void failure(RetrofitError error) {
                Log.e(Constants.TRAILBOOK_TAG, "Failed to upload path summary", error);
                Log.e(Constants.TRAILBOOK_TAG,"status="+error.getResponse());
                Log.e(Constants.TRAILBOOK_TAG,"status="+error.getResponse().getStatus());
            }

            @Override
            public void success(String sResponse, Response response) {
                Log.d(Constants.TRAILBOOK_TAG, "Response from path upload: " + sResponse);
            }
        };
        Callback<String> pathSegmentMapUploadedCallback = new Callback<String>(){
            @Override
            public void failure(RetrofitError error) {
                Log.e(Constants.TRAILBOOK_TAG, "Failed to upload path segment map", error);
                Log.e(Constants.TRAILBOOK_TAG,"status="+error.getResponse());
                Log.e(Constants.TRAILBOOK_TAG,"status="+error.getResponse().getStatus());
            }

            @Override
            public void success(String sResponse, Response response) {
                Log.d(Constants.TRAILBOOK_TAG, "Response from path segment map upload: " + sResponse);
            }
        };
        Callback<String> pathPointsUploadedCallback = new Callback<String>(){
            @Override
            public void failure(RetrofitError error) {
                Log.e(Constants.TRAILBOOK_TAG, "Failed to upload points", error);
                Log.e(Constants.TRAILBOOK_TAG,"status="+error.getResponse());
                Log.e(Constants.TRAILBOOK_TAG,"status="+error.getResponse().getStatus());
            }

            @Override
            public void success(String sResponse, Response response) {
                Log.d(Constants.TRAILBOOK_TAG, "Response from path upload: " + sResponse);
            }
        };
        Callback<String> pathPointNotesUploadedCallback = new Callback<String>(){
            @Override
            public void failure(RetrofitError error) {
                Log.e(Constants.TRAILBOOK_TAG, "Failed to upload point notes", error);
                Log.e(Constants.TRAILBOOK_TAG,"status="+error.getResponse());
                Log.e(Constants.TRAILBOOK_TAG,"status="+error.getResponse().getStatus());
            }

            @Override
            public void success(String sResponse, Response response) {
                Log.d(Constants.TRAILBOOK_TAG, "Response from path upload: " + sResponse);
            }
        };

        Callback<String> imageUploadedCallback = new Callback<String>(){
            @Override
            public void failure(RetrofitError error) {
                Log.e(Constants.TRAILBOOK_TAG, "Failed to upload images", error);
                Log.e(Constants.TRAILBOOK_TAG,"status="+error.getResponse());
                Log.e(Constants.TRAILBOOK_TAG,"status="+error.getResponse().getStatus());
            }

            @Override
            public void success(String sResponse, Response response) {
                Log.d(Constants.TRAILBOOK_TAG, "Response from path upload: " + sResponse);
            }
        };

        PostPathSummary(p, pathSummaryUploadedCallback);
        PostPathSegmentMap(p, pathSegmentMapUploadedCallback);
        PostPathPoints(p, pathPointsUploadedCallback);
        PostPathPointNotes(p, pathPointNotesUploadedCallback);
        PostImages(p);
    }

    private void PostImages(Path p) {
        ArrayList<PathSegment> segments = pathManager.getSegmentsForPath(p);
        for (PathSegment s:segments) {
            PostImages(s);
        }
    }

    private void PostImages(PathSegment s) {
        ArrayList<Note> notes = s.getNotes();
        ArrayList<MultipartEntity> entities = new ArrayList<MultipartEntity>();
        for (Note n:notes) {
            if (n.getImageFileName() != null && n.getImageFileName().length()>0) {
                MultipartEntity entity = TrailbookFileUtilities.getMultipartEntityForNoteImage(getActivity(), n);
                if (entity != null)
                    entities.add(entity);
            }
        }
        startImageUpload(entities);
    }

    private void PostPathSummary(Path p, Callback<String> cb) {
        String pathSummaryFileContents = TrailbookPathUtilities.getPathSummaryJSONString(p);
        Log.d(Constants.TRAILBOOK_TAG, pathSummaryFileContents);
        String fileName = p.getId() + "_summary.tb";
        String dir = Constants.pathsDir + "/" + p.getId();
        mService.postStringFileContents(pathSummaryFileContents, dir, fileName, cb);
    }

    private void PostPathSegmentMap(Path p, Callback<String> cb) {
        String pathSementMapFileContents = TrailbookPathUtilities.getPathSegmentMapJSONString(p);
        Log.d(Constants.TRAILBOOK_TAG, pathSementMapFileContents);
        String fileName = p.getId() + "_segments.tb";
        String dir = Constants.pathsDir + "/" + p.getId();
        mService.postStringFileContents(pathSementMapFileContents, dir, fileName, cb);
    }

    private void PostPathPoints(Path p, Callback<String> cb) {
        ArrayList<PathSegment> segments = pathManager.getSegmentsForPath(p);
        for (PathSegment s:segments) {
            PostSegmentPoints(s, cb);
        }
    }

    private void PostSegmentPoints(PathSegment s, Callback<String> cb) {
        String pointsFileContents = TrailbookPathUtilities.getSegmentPointsJSONString(s);
        Log.d(Constants.TRAILBOOK_TAG, pointsFileContents);
        String fileName = s.getId() + "_points.tb";
        String dir = Constants.segmentsDir + "/" + s.getId();
        mService.postStringFileContents(pointsFileContents, dir, fileName, cb);
    }

    private void PostPathPointNotes(Path p, Callback<String> cb) {
        ArrayList<PathSegment> segments = pathManager.getSegmentsForPath(p);
        for (PathSegment s:segments) {
            PostSegmentPointNotes(s, cb);
        }
    }

    private void PostSegmentPointNotes(PathSegment s, Callback<String> cb) {
        String notesContents = TrailbookPathUtilities.getSegmentNotesJSONString(s);
        Log.d(Constants.TRAILBOOK_TAG, notesContents);
        String fileName = s.getId() + "_notes.tb";
        String dir = Constants.segmentsDir + "/" + s.getId();
        mService.postStringFileContents(notesContents, dir, fileName, cb);
    }

    private void startImageUpload(ArrayList<MultipartEntity> entities) {
        AsyncUploadMultipartEntities uploadImageTask = new AsyncUploadMultipartEntities(TrailbookFileUtilities.getImageUploadUrl());
        MultipartEntity[] entitiesArray = new MultipartEntity[entities.size()];
        entities.toArray(entitiesArray);
        uploadImageTask.execute(entitiesArray);
    }

    public void startDownloadPath(String pathId) {
        pathManager.savePathSummary(pathId, getActivity());
        Map<String, String> options = new HashMap<String, String>();
        options.put("pathid",pathId);
        Log.d(Constants.TRAILBOOK_TAG, "downloading path id " + pathId);
        Callback<PathSegmentMapRecievedEvent.SegmentListWithPathID> callback = new Callback<PathSegmentMapRecievedEvent.SegmentListWithPathID>(){
            @Override
            public void failure(RetrofitError error) {
                Log.e("Trailbook", "Failed to get segment list", error);
            }

            @Override
            public void success(PathSegmentMapRecievedEvent.SegmentListWithPathID segmentListWithPathID, Response response) {
                String pathId = segmentListWithPathID.getPathId();
                ArrayList<String> segmentIds = segmentListWithPathID.getSegmentIds();

                pathManager.setSegmentIdsForPath(pathId, segmentIds);
                for (String segmentId:segmentIds) {
                    startDownloadSegment(segmentId);
                }
                pathManager.savePathSegmentMap(pathId, getActivity());
            }
        };

        mService.getPathSegmentMap(options,callback);
    }

    private void startDownloadSegment(String segmentId) {
        startDownloadPoints(segmentId);
        startDownloadNotes(segmentId);
    }

    private void startDownloadPoints(String segmentId) {
        Map<String, String> options = new HashMap<String, String>();
        options.put("segmentid",segmentId);
        Log.d(Constants.TRAILBOOK_TAG, "downloading points for segment id " + segmentId);
        Callback<SegmentPointsReceivedEvent.SegmentIDWithPoints> callback = new Callback<SegmentPointsReceivedEvent.SegmentIDWithPoints>(){
            @Override
            public void failure(RetrofitError error) {
                Log.e("Trailbook", "Failed to get segment points", error);
            }

            @Override
            public void success(SegmentPointsReceivedEvent.SegmentIDWithPoints segmentIDWithPoints, Response response) {
                String segmentId = segmentIDWithPoints.getSegmentId();
                ArrayList<LatLng> points = segmentIDWithPoints.getPoints();

                pathManager.setSegmentPoints(segmentId, points);
                pathManager.saveSegmentPoints(segmentId, getActivity());
            }
        };

        mService.getPoints(options, callback);
    }

    private void startDownloadNotes(String segmentId) {
        Map<String, String> options = new HashMap<String, String>();
        options.put("segmentid",segmentId);
        Log.d(Constants.TRAILBOOK_TAG, "downloading notes for segment id " + segmentId);
        Callback<NotesReceivedEvent.SegmentIDWithNotes> callback = new Callback<NotesReceivedEvent.SegmentIDWithNotes>(){
            @Override
            public void failure(RetrofitError error) {
                Log.e("Trailbook", "Failed to get segment notes", error);
            }

            @Override
            public void success(NotesReceivedEvent.SegmentIDWithNotes segmentIDWithNotes, Response response) {
                String segmentId = segmentIDWithNotes.getSegmentId();
                HashMap<String,PointAttachedObject<Note>> notes = segmentIDWithNotes.getPointNotes();
                pathManager.setSegmentNotes(segmentId, notes);
                for (PointAttachedObject<Note> paoNote:notes.values()) {
                    String imageFileName = paoNote.getAttachment().getImageFileName();
                    if (imageFileName != null && imageFileName.length()>0)
                        startGetImage(paoNote.getAttachment());
                }
                pathManager.saveSegmentNotes(segmentId, getActivity());
            }
        };

        mService.getNotes(options, callback);
    }
}