package com.trailbook.kole.worker_fragments;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.squareup.otto.Bus;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.Path;
import com.trailbook.kole.data.PathSegment;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.data.TrailBookComment;
import com.trailbook.kole.helpers.DownloadImageTask;
import com.trailbook.kole.helpers.TrailbookFileUtilities;
import com.trailbook.kole.services.async_tasks.AsyncCloudDeletePath;
import com.trailbook.kole.services.async_tasks.AsyncGetPathSummariesFromLocalDevice;
import com.trailbook.kole.services.async_tasks.AsyncGetPathSummariesFromRemoteDB;
import com.trailbook.kole.services.async_tasks.AsyncUploadAttachedComment;
import com.trailbook.kole.services.async_tasks.AsyncUploadComment;
import com.trailbook.kole.services.download.DownloadPathService;
import com.trailbook.kole.services.upload.UploadPathService;
import com.trailbook.kole.services.web.TrailbookPathServices;
import com.trailbook.kole.state_objects.BusProvider;
import com.trailbook.kole.state_objects.PathManager;
import com.trailbook.kole.state_objects.TrailBookState;

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

    public void startGetPathSummariesRemote(LatLng center, long radius) {
        AsyncGetPathSummariesFromRemoteDB asyncGetPathSummaries = new AsyncGetPathSummariesFromRemoteDB();
        asyncGetPathSummaries.execute();
        TrailBookState.resetLastRefreshedFromCloudTimeStamp();
    }

    public void startGetPathSummariesLocal() {
        AsyncGetPathSummariesFromLocalDevice asyncGetPathSummaries = new AsyncGetPathSummariesFromLocalDevice();
        asyncGetPathSummaries.execute();
    }

    private void startGetImage(String imageFileName) {
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
        ArrayList<PointAttachedObject> paObjects = PathManager.getInstance().getPointObjectsForPath(summary.getId());
        Path pathContainer = new Path(summary, segments2, paObjects);

        Log.d(Constants.TRAILBOOK_TAG, "WorkerFragment: uploading path " + summary.getName());
        Intent intent = new Intent(getActivity(), UploadPathService.class);
        intent.putExtra(UploadPathService.PATH_ID_KEY, summary.getId());
        getActivity().startService(intent);

        //todo: do this on broadcast message recieved
        pathManager.savePathSummaryToCloudCache(summary);
    }

    public void startDownloadPath(String pathId) {
/*        DownloadPathService asyncGetPathFromRemoteDB = new DownloadPathService();
        asyncGetPathFromRemoteDB.execute(pathId);*/

        Log.d(Constants.TRAILBOOK_TAG, "WorkerFragment: downloading path " + pathId);
        Intent intent = new Intent(getActivity(), DownloadPathService.class);
        intent.putExtra(DownloadPathService.PATH_ID_KEY, pathId);
        getActivity().startService(intent);
        getActivity().setProgressBarIndeterminateVisibility(true);
    }

/*
    @Subscribe
    public void onPathReceivedEvent(PathReceivedEvent event) {
        Path path = event.getPath();
        ArrayList<PointAttachedObject> paObjects = path.paObjects;
        for (PointAttachedObject pao:paObjects) {
            ArrayList<String> imageFileNames = pao.getAttachment().getImageFileNames();
            if (imageFileNames != null && imageFileNames.size()>0) {
                for (String imageFileName:imageFileNames) {
                    startGetImage(imageFileName);
                }
            }
        }
    }
*/

    public void startPathDeleteMongo(String pathId) {
        AsyncCloudDeletePath asyncCloudDeletePath = new AsyncCloudDeletePath();
        asyncCloudDeletePath.execute(pathManager.getPathSummary(pathId));
    }

    public void startCommentUploadMongo(TrailBookComment comment) {
        AsyncUploadComment asyncUploadComment = new AsyncUploadComment();
        asyncUploadComment.execute(comment);
    }

    public void startAttachedCommentUploadMongo(PointAttachedObject paoComment) {
        AsyncUploadAttachedComment asyncUploadAttachedComment = new AsyncUploadAttachedComment();
        asyncUploadAttachedComment.execute(paoComment);
    }
}