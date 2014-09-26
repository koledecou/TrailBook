package com.trailbook.kole.worker_fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.Note;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.data.Path;
import com.trailbook.kole.data.PathSegment;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.events.PathReceivedEvent;
import com.trailbook.kole.helpers.DownloadImageTask;
import com.trailbook.kole.helpers.TrailbookFileUtilities;
import com.trailbook.kole.services.async_tasks.AsyncGetPathFromRemoteDB;
import com.trailbook.kole.services.async_tasks.AsyncGetPathSummaries;
import com.trailbook.kole.services.async_tasks.AsyncUploadMultipartEntities;
import com.trailbook.kole.services.async_tasks.AsyncUploadPath;
import com.trailbook.kole.services.web.TrailbookPathServices;
import com.trailbook.kole.state_objects.BusProvider;
import com.trailbook.kole.state_objects.PathManager;
import com.trailbook.kole.state_objects.TrailBookState;

import org.apache.http.entity.mime.MultipartEntity;

import java.io.File;
import java.util.ArrayList;

import retrofit.RestAdapter;

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
        AsyncGetPathSummaries asyncGetPathSummaries = new AsyncGetPathSummaries();
        asyncGetPathSummaries.execute();
        TrailBookState.resetLastRefreshedFromCloudTimeStamp();
    }

    private void startGetImage(Note note) {
        String imageFileName = note.getImageFileName();
        File deviceImageFile = TrailbookFileUtilities.getInternalImageFile(imageFileName);

        Log.d(Constants.TRAILBOOK_TAG, "WorkerFragment: downloading to: " + deviceImageFile);
        String webServerImageDir=TrailbookFileUtilities.getWebServerImageDir();
        String webServerImageFileName = webServerImageDir + "/" + imageFileName;

        Log.d(Constants.TRAILBOOK_TAG, "webserver image file name: " + webServerImageFileName);
        new DownloadImageTask(deviceImageFile).execute(webServerImageFileName);

        //TODO: use picasso to get the image?
//        Picasso.with(getActivity()).load(webServerImageFileName).into(new BitmapFileTarget(imageFile));
    }

    public void startPathUploadMongo(PathSummary summary) {
        ArrayList<PathSegment> segments2 = PathManager.getInstance().getSegmentsForPath(summary.getId());
        ArrayList<PointAttachedObject<Note>> notes = PathManager.getInstance().getPointNotesForPath(summary.getId());
        Path pathContainer = new Path(summary, segments2, notes);

        Log.d(Constants.TRAILBOOK_TAG, "WorkerFragment: uploading path " + summary.getName());
        AsyncUploadPath asyncUploadPath = new AsyncUploadPath();
        asyncUploadPath.execute(pathContainer);

        PostImages(summary);
    }

    private void PostImages(PathSummary summary) {
        ArrayList<PointAttachedObject<Note>> paoNotes = pathManager.getPointNotesForPath(summary.getId());
        for (PointAttachedObject<Note> paoNote:paoNotes) {
            PostImage(paoNote.getAttachment());
        }
    }

    private void PostImage(Note n) {
        ArrayList<MultipartEntity> entities = new ArrayList<MultipartEntity>();
        if (n.getImageFileName() != null && n.getImageFileName().length()>0) {
            MultipartEntity entity = TrailbookFileUtilities.getMultipartEntityForNoteImage(n);
            if (entity != null)
                entities.add(entity);
        }
        startImageUpload(entities);
    }

    private void startImageUpload(ArrayList<MultipartEntity> entities) {
        AsyncUploadMultipartEntities uploadImageTask = new AsyncUploadMultipartEntities(TrailbookFileUtilities.getImageUploadUrl());
        MultipartEntity[] entitiesArray = new MultipartEntity[entities.size()];
        entities.toArray(entitiesArray);
        uploadImageTask.execute(entitiesArray);
    }

    public void startDownloadPath(String pathId) {
        AsyncGetPathFromRemoteDB asyncGetPathFromRemoteDB = new AsyncGetPathFromRemoteDB();
        asyncGetPathFromRemoteDB.execute(pathId);
    }

    @Subscribe
    public void onPathReceivedEvent(PathReceivedEvent event) {
        Path path = event.getPath();
        ArrayList<PointAttachedObject<Note>> notes = path.notes;
        for (PointAttachedObject<Note> paoNote:notes) {
            String imageFileName = paoNote.getAttachment().getImageFileName();
            if (imageFileName != null && imageFileName.length()>0)
                startGetImage(paoNote.getAttachment());
        }
    }
}