package com.trailbook.kole.activities;

import android.app.ActionBar;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.Note;
import com.trailbook.kole.data.Path;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.events.LocationChangedEvent;
import com.trailbook.kole.events.ModeChangedEvent;
import com.trailbook.kole.fragments.AlertDialogFragment;
import com.trailbook.kole.fragments.CreateNoteFragment;
import com.trailbook.kole.fragments.CreatePathDialogFragment;
import com.trailbook.kole.fragments.FollowPathSelectorFragment;
import com.trailbook.kole.fragments.FullNoteViewFragment;
import com.trailbook.kole.fragments.NavigationDrawerFragment;
import com.trailbook.kole.fragments.PathDetailsActionListener;
import com.trailbook.kole.fragments.PathSelectorFragment;
import com.trailbook.kole.fragments.TBPreferenceFragment;
import com.trailbook.kole.fragments.TrailBookMapFragment;
import com.trailbook.kole.helpers.ApplicationUtils;
import com.trailbook.kole.helpers.MapUtilities;
import com.trailbook.kole.helpers.TrailbookFileUtilities;
import com.trailbook.kole.helpers.TrailbookPathUtilities;
import com.trailbook.kole.location_processors.PathFollowerLocationProcessor;
import com.trailbook.kole.location_processors.TrailBookLocationReceiver;
import com.trailbook.kole.state_objects.BusProvider;
import com.trailbook.kole.state_objects.PathManager;
import com.trailbook.kole.state_objects.TrailBookState;
import com.trailbook.kole.worker_fragments.WorkerFragment;

import java.util.ArrayList;

public class TrailBookActivity extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks,
        PathDetailsActionListener,
        AlertDialogFragment.AlertDialogListener,
        CreatePathDialogFragment.CreatePathDialogListener,
        CreateNoteFragment.CreateNoteFragmentListener,
        PathSelectorFragment.OnPathSelectorFragmentInteractionListener {

    private static final String className = "TrailBookActivity";

    private static final int CONFIRM_CREATE_DIALOG_ID = 1;
    private static final int FILE_CHOOSER_DIALOG_ID = 2;

    private static final String MAPS_ACTIVITY_TAG = "MAPS_ACTIVITY";
    private static final String ADD_NOTE_FRAG_TAG = "ADD_NOTE_FRAG";
    private static final String PREF_FRAG_TAG = "PREF_FRAG";
    private static final String PATH_SELECT_UPLOAD_TAG = "PATH_SELECT_UPLOAD";
    private static final String SAVED_CURRENT_PATH_ID = "CURRENT_PATH_ID" ;
    private static final String SAVED_CURRENT_SEGMENT_ID = "CURRENT_SEGMENT_ID";

    private CharSequence mTitle; // Used to store the last screen title. For use in {@link #restoreActionBar()}.
    private TrailBookMapFragment mMapFragment;
    private WorkerFragment mWorkFragment;
    private PathManager mPathManager;
    private NavigationDrawerFragment mNavigationDrawerFragment;
    //private LocationServicesFragment mLocationServicesFragment;
    private TBPreferenceFragment mPreferencesFragment;
    private Bus bus;
    private Resources mResources;
    private SlidingUpPanelLayout mSlidingUpPanel;
    private Fragment mPathSelectorFragment;
    private Fragment mContent;
    private ProgressDialog mWaitingForLocationDialog;
    private TrailBookLocationReceiver mLocationReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(Constants.TRAILBOOK_TAG, className + " OnCreate");

        mLocationReceiver = new TrailBookLocationReceiver();
        mResources = this.getResources();

        bus = BusProvider.getInstance(); //create the event bus that will be used by fragments interested in sharing events.
        bus.register(this);

        if (savedInstanceState != null) {
            Log.d(Constants.TRAILBOOK_TAG, className + " restoring map fragment");
            mMapFragment = (TrailBookMapFragment) getFragmentManager().findFragmentByTag("map_fragment");
        } else {
            Log.d(Constants.TRAILBOOK_TAG, className + " creating map fragment");
            mMapFragment = TrailBookMapFragment.newInstance();
            getFragmentManager().beginTransaction().add(R.id.map_container, mMapFragment, "map_fragment").commit();
        }

        mPathManager = PathManager.getInstance();

        setContentView(R.layout.activity_maps);
        setUpNavDrawerFragment();
        setUpPreferencesFragment();
        setUpWorkFragmentIfNeeded();

        TrailBookState.restoreState();
        if (TrailBookState.getMode() == TrailBookState.MODE_LEAD) {
            TrailBookState.getInstance().resumeLeadingActivePath(true);
        } else if (TrailBookState.getMode()== TrailBookState.MODE_FOLLOW) {
            TrailBookState.getInstance().restoreActivePath();
            startFollowing(TrailBookState.getActivePathId());
        } else if (TrailBookState.getMode() == TrailBookState.MODE_SEARCH) {
            refreshPaths();
        }
    }

    @Override
    public void onNoteCreated(String noteId, Note newNote) {
        getFragmentManager().popBackStackImmediate();
        invalidateOptionsMenu();
        PointAttachedObject<Note> paoNote = mPathManager.getNote(noteId);
        if (paoNote == null)
            paoNote = attachNoteToCurrentLocation(newNote);
        else {
            paoNote.setAttachment(newNote);
        }
        addNoteToActivePath(paoNote);
        Log.d(Constants.TRAILBOOK_TAG, className +  " added note: " + paoNote.getId() + " contnt: " + newNote.getNoteContent() + " at " + paoNote.getLocation() + " to path " + TrailBookState.getActivePathId());
    }

    private void addNoteToActivePath(PointAttachedObject<Note> note) {
        String pathId = TrailBookState.getActivePathId();
        if (pathId != null) {
            mPathManager.addNoteToPath(pathId, note);
            mPathManager.savePath(pathId, this);
        }
    }

    private PointAttachedObject<Note> attachNoteToCurrentLocation(Note newNote) {
        Location l = TrailBookState.getCurrentLocation();
        if (l != null) {
            LatLng point = TrailbookPathUtilities.locationToLatLon(l);
            return new PointAttachedObject<Note>(TrailbookPathUtilities.getNewNoteId(), point, newNote);
        } else {
            return null;
        }
    }

    @Override
    public void onNoteCreateCanceled() {
        getFragmentManager().popBackStackImmediate();
        invalidateOptionsMenu();
    }

    @Override
    public void onPathSelectorFragmentResult(String action, String pathId) {
        if (PathSelectorFragment.DISMISS.equals(action) ) {
            getFragmentManager().popBackStackImmediate();
            return;
        }

        PathSummary summary = mPathManager.getPathSummary(pathId);
        if (summary == null) {
            Log.e(Constants.TRAILBOOK_TAG, className + " onPathSelectorFragmentResult can't find path:" + pathId);
            return;
        }

        if (PathSelectorFragment.UPLOAD.equals(action)) {
            if (isNetworkConnected()) {
                Log.d(Constants.TRAILBOOK_TAG, className + " Uploading path:" + pathId);
                //TODO: update path details and confirm
                //TODO: show progress dialog
                Toast.makeText(this, "Uploading " + summary.getName(), Toast.LENGTH_LONG).show();
                mWorkFragment.startPathUploadMongo(mPathManager.getPathSummary(pathId));
            } else {
                Log.d(Constants.TRAILBOOK_TAG, className + " no network connectivity");
                showNoNetworkStatusDialog();
            }
        } else if (PathSelectorFragment.DELETE.equals(action) ) {
            Log.d(Constants.TRAILBOOK_TAG, className + " Deleting path:" + pathId);
            mPathManager.deletePath(pathId, this);
        } else if (PathSelectorFragment.FOLLOW.equals(action) ) {
            getFragmentManager().popBackStackImmediate();
            onFollowRequested(pathId);
        } else if (PathSelectorFragment.TO_START.equals(action) ) {
            getFragmentManager().popBackStackImmediate();
            onNavigateToStart(pathId);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private void setUpPreferencesFragment() {
        mPreferencesFragment = new TBPreferenceFragment();
    }

    private void setUpNavDrawerFragment() {
        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    @Override
    protected void onResume() {
        super.onResume();
        invalidateOptionsMenu();
        setUpWorkFragmentIfNeeded();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.search_menu, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        MenuInflater inflater = getMenuInflater();
        if (TrailBookState.getMode() == TrailBookState.MODE_LEAD) {
            if (!isCreateNoteDialogShowing())
                inflater.inflate(R.menu.leader_menu, menu);
            else
                inflater.inflate(R.menu.create_note_menu, menu);
        } else if (TrailBookState.getMode() == TrailBookState.MODE_FOLLOW) {
            inflater.inflate(R.menu.follower_menu, menu);
        } else {
            getMenuInflater().inflate(R.menu.search_menu, menu);
        }
        return true;
    }

    private boolean isCreateNoteDialogShowing() {
        FragmentManager fm = getFragmentManager();
        if (fm.findFragmentByTag(ADD_NOTE_FRAG_TAG) != null){
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_bar_create_note) {
            collapseSlidingPanel();
            switchFragmentAndAddToBackstack(getCreateNoteFragment(TrailbookPathUtilities.getNewNoteId()),
                            ADD_NOTE_FRAG_TAG);
            invalidateOptionsMenu();
        } else if (id == R.id.quick_note_left || id == R.id.quick_note_right || id == R.id.quick_note_straight) {
            collapseSlidingPanel();
            Log.d(Constants.TRAILBOOK_TAG, className + " creating quicknote contextmenu");
            createQuickNote(id);
            invalidateOptionsMenu();
        } else if (id == R.id.action_bar_stop_following) {
//            stopLocationUpdates();
            switchToSearchMode();
        } else if (id == R.id.action_bar_stop_leading) {
//            stopLocationUpdates();
            switchToSearchMode();
            //todo: prompt for path details and ask to upload if network is available.
        } else if (id == R.id.action_filter) {
            Log.d(Constants.TRAILBOOK_TAG, className + " show filter dialog");
        } else if (id == R.id.action_list) {
            Log.d(Constants.TRAILBOOK_TAG, className + " listing paths within the view");
        } else if (id == R.id.action_refresh_map) {
            Log.d(Constants.TRAILBOOK_TAG, className + " refreshing paths the view");
            refreshPaths();
        }
        return super.onOptionsItemSelected(item);
    }

    private void switchFragmentAndAddToBackstack(Fragment frag, String tag) {
        FragmentManager fm = getFragmentManager();
        fm.beginTransaction()
                .replace(R.id.map_container, frag, tag)
                .addToBackStack(MAPS_ACTIVITY_TAG)
                .commit();
    }

    private void setUpWorkFragmentIfNeeded() {
        FragmentManager fm = getFragmentManager();
        mWorkFragment = (WorkerFragment)fm.findFragmentByTag("work");
        if (mWorkFragment == null) {
            createWorkerFragment();
        }
    }

    private void createWorkerFragment() {
        mWorkFragment = new WorkerFragment();
        getFragmentManager().beginTransaction().add(mWorkFragment, "work").commit();
    }

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        Fragment newFrag;
        if (position == 0) { // find paths
//            stopLocationUpdates();
            switchToSearchMode();
        } else if (position == 1) { // follow path
            if (mPathManager.hasDownloadedPaths())
                switchFragmentAndAddToBackstack(getFollowPathSelectorFragment(), PATH_SELECT_UPLOAD_TAG);
            else
                showNoPathsAlert();
        } else if (position == 2) { // new path
            //TODO:  warnIfStoppingUnfinishedPath();
            openNewPathDialog();
        } else if (position == 3) { // manage paths
            if (mPathManager.hasDownloadedPaths())
                switchFragmentAndAddToBackstack(getPathSelectorFragmentForDownloadedPaths(), PATH_SELECT_UPLOAD_TAG);
            else
                showNoPathsAlert();
        } else if (position == 4) { // import
            launchFileChooser();
        } else if (position == 5) { // prefs
            switchFragmentAndAddToBackstack(mPreferencesFragment, PREF_FRAG_TAG);
        } else { // unknown selection, go to search mode
            switchToSearchMode();
        }
    }

    private void launchFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);

        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");

        startActivityForResult(intent, FILE_CHOOSER_DIALOG_ID);
    }

    private void switchToSearchMode() {
        TrailBookState.getInstance().switchToSearchMode();
    }

    @Subscribe
    public void onModeChangedEvent(ModeChangedEvent event) {
        if (event.getNewMode() == TrailBookState.MODE_SEARCH) {
            getActionBar().setTitle("Find Paths");
            invalidateOptionsMenu();
            /*FragmentManager fragmentManager = getFragmentManager();
            Fragment newFrag;
            newFrag = mMapFragment;
            fragmentManager.beginTransaction()
                    .replace(R.id.map_container, newFrag)
                    .commit();*/
        }
    }

    private void openNewPathDialog() {
        collapseSlidingPanel();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("new_path_dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        CreatePathDialogFragment newFragment = CreatePathDialogFragment.newInstance(R.string.cpd_title);
        newFragment.setListener(this);
        newFragment.show(ft, "new_path_dialog");
    }

    @Override
    public void processNewPathClick(String pathName) {
        String newSegmentId = mPathManager.makeNewSegment();
        TrailBookState.setActiveSegmentId(newSegmentId);
        String newPathId = mPathManager.makeNewPath(pathName, newSegmentId, ApplicationUtils.getDeviceId(this));
        TrailBookState.setActivePathId(newPathId);
        Fragment f = getFragmentManager().findFragmentByTag("new_path_dialog");
        if (f != null) {
            Log.d(Constants.TRAILBOOK_TAG, className + " removing dialog");
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.remove(f);
            ft.commit();
        }
        startLeading(newPathId, newSegmentId);
    }

    private void warnIfStoppingUnfinishedPath() {
        if (TrailBookState.getMode() == TrailBookState.MODE_LEAD) {
            DialogFragment f = AlertDialogFragment.newInstance(mResources.getString(R.string.confirm_new_path_title),
                    mResources.getString(R.string.warn_path_unfinished),
                    mResources.getString(R.string.cancel_new_path),
                    mResources.getString(R.string.ok_new_path),
                    CONFIRM_CREATE_DIALOG_ID);
            f.show(getFragmentManager(), "alert_dialog");
        }
    }

    @Override
    public void onCancelClicked(int id) {
        if (id == CONFIRM_CREATE_DIALOG_ID) {
        }
    }

    @Override
    public void onOkClicked(int id) {
        if (id == CONFIRM_CREATE_DIALOG_ID) {
            openNewPathDialog();
        }
    }

    public TrailBookActivity() {}

    @Override
    public void onDownloadRequested(String pathId) {
        mWorkFragment.startDownloadPath(pathId);
        collapseSlidingPanel();
    }

    @Override
    public void onFollowRequested(String pathId) {
        collapseSlidingPanel();
        TrailBookState.setMode(TrailBookState.MODE_FOLLOW);
        mMapFragment.showNotesOnlyForPath(pathId);
        mMapFragment.zoom(pathId);
        getActionBar().setTitle("Following " + mPathManager.getPathSummary(pathId).getName());
        invalidateOptionsMenu();
        startFollowing(pathId);
    }

    private void startFollowing(String pathId) {
        TrailBookState.setActivePathId( pathId);
        TrailBookState.setActiveSegmentId( null);
        TrailBookState.setLocationProcessor(new PathFollowerLocationProcessor(pathId, this));
        TrailBookState.getInstance().startLocationUpdates();
    }

    @Override
    public void onZoomRequested(String pathId) {
        collapseSlidingPanel();
        mMapFragment.zoom(pathId);
    }

    @Override
    public void onNavigateToStart(String pathId) {
        collapseSlidingPanel();
/*        if (TrailBookState.getMode() == TrailBookState.MODE_FOLLOW ||
                    TrailBookState.getMode() == TrailBookState.MODE_LEAD) {
            stopLocationUpdates();
        }*/
        switchToSearchMode();

        LatLng startCoords = mPathManager.getStartCoordsForPath(pathId);
        if (startCoords != null)
            MapUtilities.navigateTo(this, String.valueOf(startCoords.latitude) + "," + String.valueOf(startCoords.longitude));
    }

    @Override
    public void onResumeLeadingRequested(String pathId) {
/*        collapseSlidingPanel();
        TrailBookState.setActivePathId(pathId);

        //resume the last segment
        TrailBookState.setActiveSegmentId(mPathManager.getPath(pathId).getLastSegment());
        mMapFragment.zoomToCurrentLocation();*/

        startLeading(pathId, mPathManager.getPathSummary(pathId).getLastSegment());
    }

    private void startLeading(String pathId, String segmentId) {
        collapseSlidingPanel();
        invalidateOptionsMenu();
        TrailBookState.getInstance().switchToLeadMode(pathId, segmentId);
        waitForLocation();

/*
        TrailBookState.setMode(TrailBookState.MODE_LEAD);
        invalidateOptionsMenu();
        TrailBookState.setLocationProcessor(new PathLeaderLocationProcessor(
                this,
                TrailBookState.getActiveSegmentId(),
                TrailBookState.getActivePathId()));
        TrailBookState.getInstance().startLocationUpdates();*/
    }

    private void collapseSlidingPanel() {
        SlidingUpPanelLayout slidingUpPanel = (SlidingUpPanelLayout) findViewById(R.id.main_panel);
        if (slidingUpPanel != null)
            slidingUpPanel.collapsePanel();
    }

    private boolean isSlidingPanelExpanded() {
        SlidingUpPanelLayout slidingUpPanel = (SlidingUpPanelLayout) findViewById(R.id.main_panel);
        if (slidingUpPanel != null && slidingUpPanel.isPanelExpanded())
            return true;
        else
            return false;
    }

    @Override
    public void onBackPressed() {
        if (isSlidingPanelExpanded()) {
            collapseSlidingPanel();
        } else {
            int backStackEntryCount = getFragmentManager().getBackStackEntryCount();
            Log.d(Constants.TRAILBOOK_TAG, className + ": backstack count:" + backStackEntryCount);
            if(backStackEntryCount == 0) {
                if (TrailBookState.getMode() == TrailBookState.MODE_LEAD)
                    showExitAlert(getString(R.string.exit_confirm_lead_mesage));
                else if (TrailBookState.getMode() == TrailBookState.MODE_FOLLOW)
                    showExitAlert(getString(R.string.exit_confirm_follow_mesage));
                else
                    super.onBackPressed();
            } else {
                getFragmentManager().popBackStack();
            }
        }
    }

    private void createQuickNote(int id) {
        String content = TrailbookPathUtilities.getQuickNoteContent(getResources(), id);
        if (content != null) {
            Note qn = new Note();
            qn.setNoteContent(content);
            PointAttachedObject<Note> paoNote = attachNoteToCurrentLocation(qn);
            addNoteToActivePath(paoNote);
            Log.d(Constants.TRAILBOOK_TAG, className + ": added quick note:" + content + " at " + paoNote.getLocation() + " to segment " + TrailBookState.getActiveSegmentId());
        }
    }

    public void showFullNote(String noteId) {
        if (TrailBookState.getMode() == TrailBookState.MODE_LEAD) {
            switchFragmentAndAddToBackstack(getCreateNoteFragment(noteId), ADD_NOTE_FRAG_TAG);
            invalidateOptionsMenu();
        } else {
            switchFragmentAndAddToBackstack(getFullNoteViewFragment(noteId), PATH_SELECT_UPLOAD_TAG);
        }
    }

    private Fragment getCreateNoteFragment(String noteId) {
        CreateNoteFragment f = CreateNoteFragment.newInstance(noteId);
        return f;
    }

    private Fragment getFullNoteViewFragment(String noteId) {
        return FullNoteViewFragment.newInstance(noteId);
    }

    private PathSelectorFragment getPathSelectorFragmentForDownloadedPaths() {
        //return PathsOnDeviceSelectorFragment.newInstance();
        ArrayList<String> downloadedPathIds = mPathManager.getDownloadedPathIds();
        return PathSelectorFragment.newInstance(downloadedPathIds);
    }

    private FollowPathSelectorFragment getFollowPathSelectorFragment() {
        ArrayList<String> downloadedPathIds = mPathManager.getDownloadedPathIds();
        return FollowPathSelectorFragment.newInstance(downloadedPathIds);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        if (requestCode == FILE_CHOOSER_DIALOG_ID && resultCode == Activity.RESULT_OK) {
            try {
                Uri uri = null;
                if (resultData != null) {
                    uri = resultData.getData();
                    Log.d(Constants.TRAILBOOK_TAG, className + ": Chosen file Uri: " + uri.toString());
                    String pathKml = TrailbookFileUtilities.readTextFromUri(this, uri);
                    Path pathContainer = TrailbookPathUtilities.parseXML(pathKml);
                    Log.d(Constants.TRAILBOOK_TAG, className + ": parsed path:" + pathContainer.summary.getName());
                    mPathManager.savePath(pathContainer, this);
                }
            } catch (Exception e) {
                Log.d(Constants.TRAILBOOK_TAG, className + ": Exception getting kml file", e);
            }
        }
    }

    @Subscribe
    public void onLocationChangedEvent(LocationChangedEvent event){
        cancelWaitForLocationDialog();
    }

/*    @Subscribe
    public void onAllNotesAddedEvent(AllNotesAddedEvent event){
        try {
            mPathManager.savePath(event.getPathId(), this);
        } catch (Exception e) {
            Log.e(Constants.TRAILBOOK_TAG, "Exception onAllNotesAddedEvent.", e);
        }
    }*/

    public void refreshPaths() {
        mPathManager.loadPathsFromDevice(this);
        if (isTimeToRereshFromCloud()) {
            refreshFromCloudIfConnectedToNetwork();
        }
    }

    private void refreshFromCloudIfConnectedToNetwork() {
        if (isNetworkConnected()) {
            mWorkFragment.startGetPathSummaries(null, 0);
        } else {
            toastNoNetwork();
        }
    }

    private boolean isTimeToRereshFromCloud() {
        long msSinceLastRefresh = ApplicationUtils.getCurrentTimeStamp() -TrailBookState.getLastRefreshedFromCloudTimeStamp();
        Log.d(Constants.TRAILBOOK_TAG, "Time since last refresh" + msSinceLastRefresh/1000/60 + " min");
        if (msSinceLastRefresh > Constants.CLOUD_REFRESH_DEFAULT_TIME_DELTA) {
            return true;
        } else {
            return false;
        }
    }

    private void waitForLocation() {
        mWaitingForLocationDialog = new ProgressDialog(this);
        mWaitingForLocationDialog.setMessage(getString(R.string.waiting_for_location));
        mWaitingForLocationDialog.setCancelable(false);
        mWaitingForLocationDialog.show();
    }

    private void cancelWaitForLocationDialog() {
        if (mWaitingForLocationDialog != null) {
            mWaitingForLocationDialog.dismiss();
            mWaitingForLocationDialog = null;
        }
    }

    private void showNoPathsAlert() {
        DialogInterface.OnClickListener clickListenerOK = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int id) {
                Log.d(Constants.TRAILBOOK_TAG, className + ": dismissing no paths dialog");
                //todo: launch searcher here
            }
        };
        Log.d(Constants.TRAILBOOK_TAG, className + ": showing no paths alert");
        ApplicationUtils.showAlert(this, clickListenerOK, getString(R.string.info_no_paths_downloaded_title), getString(R.string.info_no_paths_downloaded_message), getString(R.string.OK), null);
    }

    private void showExitAlert(String reason) {
        DialogInterface.OnClickListener clickListenerOK = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int id) {
                Log.d(Constants.TRAILBOOK_TAG, className + ": exiting application");
                TrailBookState.getInstance().stopLocationUpdates();
                finish();
            }
        };
        Log.d(Constants.TRAILBOOK_TAG, className + ": showing exit confirm alert");
        ApplicationUtils.showAlert(this, clickListenerOK, getString(R.string.confirm_exit_title), reason, getString(R.string.exit_confirm), getString(R.string.cn_cancel));
    }

    private boolean isNetworkConnected() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        } else {
            return false;
        }
    }

    public void showNoNetworkStatusDialog() {
        DialogInterface.OnClickListener clickListenerOK = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int id) {
                Log.d(Constants.TRAILBOOK_TAG, className + ": dismissing no paths dialog");
            }
        };
        ApplicationUtils.showAlert(this, clickListenerOK, getString(R.string.no_network_title), getString(R.string.no_network_message), getString(R.string.OK), null);
    }

    private void toastNoNetwork() {
        Toast.makeText(this, getString(R.string.no_network_toast), Toast.LENGTH_LONG).show();
    }

}
