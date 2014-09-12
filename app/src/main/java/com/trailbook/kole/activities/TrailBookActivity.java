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
import com.trailbook.kole.data.PathContainer;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.events.LocationChangedEvent;
import com.trailbook.kole.fragments.AlertDialogFragment;
import com.trailbook.kole.fragments.CreateNoteFragment;
import com.trailbook.kole.fragments.CreatePathDialogFragment;
import com.trailbook.kole.fragments.FollowPathSelectorFragment;
import com.trailbook.kole.fragments.FullNoteViewFragment;
import com.trailbook.kole.fragments.TrailBookMapFragment;
import com.trailbook.kole.fragments.NavigationDrawerFragment;
import com.trailbook.kole.fragments.PathDetailsActionListener;
import com.trailbook.kole.fragments.PathSelectorFragment;
import com.trailbook.kole.fragments.PathsOnDeviceSelectorFragment;
import com.trailbook.kole.fragments.TBPreferenceFragment;
import com.trailbook.kole.helpers.ApplicationUtils;
import com.trailbook.kole.helpers.TrailbookFileUtilities;
import com.trailbook.kole.state_objects.TrailBookState;
import com.trailbook.kole.state_objects.BusProvider;
import com.trailbook.kole.helpers.MapUtilities;
import com.trailbook.kole.location_processors.PathFollowerLocationProcessor;
import com.trailbook.kole.location_processors.PathLeaderLocationProcessor;
import com.trailbook.kole.state_objects.PathManager;
import com.trailbook.kole.helpers.TrailbookPathUtilities;
import com.trailbook.kole.worker_fragments.LocationServicesFragment;
import com.trailbook.kole.worker_fragments.WorkerFragment;

import java.util.ArrayList;

public class TrailBookActivity extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks,
        PathDetailsActionListener,
        AlertDialogFragment.AlertDialogListener,
        CreatePathDialogFragment.CreatePathDialogListener,
        CreateNoteFragment.CreateNoteFragmentListener,
        PathSelectorFragment.OnPathSelectorFragmentInteractionListener {

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
    private LocationServicesFragment mLocationServicesFragment;
    private TBPreferenceFragment mPreferencesFragment;
    private Bus bus;
    private Resources mResources;
    private SlidingUpPanelLayout mSlidingUpPanel;
    private Fragment mPathSelectorFragment;
    private Fragment mContent;
    private ProgressDialog mWaitingForLocationDialog;
    private TrailBookState mApplicationState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Restore the fragment's instance
        if (savedInstanceState != null) {
            Log.d(Constants.TRAILBOOK_TAG, "restoring activity instance state");
/* this is stored in the application state now
            mCurrentPathId = savedInstanceState.getString(SAVED_CURRENT_PATH_ID);
            mCurrentSegmentId = savedInstanceState.getString(SAVED_CURRENT_SEGMENT_ID);
            mMode = savedInstanceState.getString(SAVED_MODE);
*/
        }

        mApplicationState = (TrailBookState) getApplicationContext();
        mResources = this.getResources();

        bus = BusProvider.getInstance(); //create the event bus that will be used by fragments interested in sharing events.
        bus.register(this);
        mMapFragment= TrailBookMapFragment.newInstance();
        mMapFragment.setParentActivity(this);
        Log.d(Constants.TRAILBOOK_TAG, "restoring map fragment");
        mPathManager = PathManager.getInstance();

        setContentView(R.layout.activity_maps);
        setUpNavDrawerFragment();
        setUpPreferencesFragment();
        setUpLocationServicesFragmentIfNeeded();

        TrailBookState.restoreState();
        if (TrailBookState.getMode() == TrailBookState.MODE_LEAD)
            startLeading();
        else if (TrailBookState.getMode()== TrailBookState.MODE_FOLLOW)
            startFollowing(TrailBookState.getActivePathId());
    }

    @Override
    public void onNoteCreated(Note newNote) {
        getFragmentManager().popBackStackImmediate();
        invalidateOptionsMenu();
        PointAttachedObject<Note> paoNote = attachNoteToCurrentLocation(newNote);
        addNoteToActiveSegment(paoNote);
        Log.d(Constants.TRAILBOOK_TAG, "added note:" + newNote.getNoteContent() + " at " + paoNote.getLocation() + " to segment " + TrailBookState.getActiveSegmentId());
    }

    private void addNoteToActiveSegment(PointAttachedObject<Note> note) {
        String segmentId = TrailBookState.getActiveSegmentId();
        String pathId = TrailBookState.getActivePathId();
        if (segmentId != null && pathId != null) {
            mPathManager.addNoteToSegment(mPathManager.getSegment(segmentId), note);
            mPathManager.savePath(pathId, this);
        }
    }

    private PointAttachedObject<Note> attachNoteToCurrentLocation(Note newNote) {
        Location l = TrailBookState.getCurrentLocation();
        if (l != null) {
            LatLng point = TrailbookPathUtilities.locationToLatLon(l);
            return new PointAttachedObject<Note>(point, newNote);
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

        Path p = mPathManager.getPath(pathId);
        if (p == null) {
            Log.e(Constants.TRAILBOOK_TAG, "onPathSelectorFragmentResult can't find path:" + pathId);
            return;
        }

        if (PathSelectorFragment.UPLOAD.equals(action)) {
            Log.d(Constants.TRAILBOOK_TAG, "Uploading path:" + pathId);
            //TODO: update path details and confirm
            //TODO: show progress dialog
            Toast.makeText(this, "Uploading " + p.getSummary().getName(), Toast.LENGTH_SHORT).show();
            mWorkFragment.startPathUpload(mPathManager.getPath(pathId));
        } else if (PathSelectorFragment.DELETE.equals(action) ) {
            Log.d(Constants.TRAILBOOK_TAG, "Deleting path:" + pathId);
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
/* this is in application state now
        outState.putString(SAVED_CURRENT_PATH_ID, mCurrentPathId);
        outState.putString(SAVED_CURRENT_SEGMENT_ID, mCurrentSegmentId);
        outState.putString(SAVED_MODE, mMode);
*/
        Log.d(Constants.TRAILBOOK_TAG, "putting fragment state");
//        getFragmentManager().putFragment(outState, "mContent", mContent);
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
            switchFragmentAndAddToBackstack(CreateNoteFragment.newInstance(TrailbookPathUtilities.getNewNoteId(),
                            TrailBookState.getActiveSegmentId()),
                            ADD_NOTE_FRAG_TAG);
            invalidateOptionsMenu();
        } else if (id == R.id.quick_note_left || id == R.id.quick_note_right || id == R.id.quick_note_straight) {
            collapseSlidingPanel();
            Log.d(Constants.TRAILBOOK_TAG, "creating quicknote contextmenu");
            createQuickNote(id);
            invalidateOptionsMenu();
        } else if (id == R.id.action_bar_stop_following) {
            stopLocationUpdates();
        } else if (id == R.id.action_filter) {
            Log.d(Constants.TRAILBOOK_TAG, "show filter dialog");
        } else if (id == R.id.action_list) {
            Log.d(Constants.TRAILBOOK_TAG, "listing paths within the view");
        } else if (id == R.id.action_refresh_map) {
            Log.d(Constants.TRAILBOOK_TAG, "refreshing paths the view");
            if (mMapFragment!=null) {
                mMapFragment.refreshPaths();
            }
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

    public WorkerFragment getWorkerFragment() {
        setUpWorkFragmentIfNeeded();
        return mWorkFragment;
    }

    private void setUpLocationServicesFragmentIfNeeded() {
        Log.d(Constants.TRAILBOOK_TAG, "Setting up location services fragment");
        FragmentManager fm = getFragmentManager();
        mLocationServicesFragment = (LocationServicesFragment)fm.findFragmentByTag("locationServices");
        if (mLocationServicesFragment == null) {
            createLocationServicesFragment();
        }
    }

    private void createLocationServicesFragment() {
        Log.d(Constants.TRAILBOOK_TAG, "creating new location services fragment");
        mLocationServicesFragment = new LocationServicesFragment();
        getFragmentManager().beginTransaction().add(mLocationServicesFragment, "locationServices").commit();
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
            switchToSearchMode();
        } else if (position == 1) { // follow path
            switchFragmentAndAddToBackstack(getFollowPathSelectorFragment(), PATH_SELECT_UPLOAD_TAG);
        } else if (position == 2) { // new path
            //TODO:  warnIfStoppingUnfinishedPath();
            openNewPathDialog();
        } else if (position == 3) { // manage paths
            switchFragmentAndAddToBackstack(getPathSelectorFragmentForDownloadedPaths(), PATH_SELECT_UPLOAD_TAG);
        } else if (position == 4) { // import
            launchFileChooser();
        } else if (position == 5) { // prefs
            switchFragmentAndAddToBackstack(mPreferencesFragment, PREF_FRAG_TAG);
        } else { // unknown selection, go to search mode
            switchToSearchMode();
        }
    }

    private void launchFileChooser() {
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Filter to show only images, using the image MIME data type.
        // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
        // To search for all documents available via installed storage providers,
        // it would be "*/*".
        intent.setType("*/*");

        startActivityForResult(intent, FILE_CHOOSER_DIALOG_ID);
    }

    private void switchToSearchMode() {
        getActionBar().setTitle("Find Paths");
        FragmentManager fragmentManager = getFragmentManager();
        Fragment newFrag;
        TrailBookState.setMode(TrailBookState.MODE_SEARCH);
        invalidateOptionsMenu();
        newFrag = mMapFragment;
        fragmentManager.beginTransaction()
                .replace(R.id.map_container, newFrag)
                .commit();
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
            Log.d(Constants.TRAILBOOK_TAG, "removing dialog");
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.remove(f);
            ft.commit();
        }
        startLeading();
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

    public LocationServicesFragment getLocationServicesFragment() {
        return mLocationServicesFragment;
    }

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
        getActionBar().setTitle("Following " + mPathManager.getPath(pathId).getSummary().getName());
        invalidateOptionsMenu();
        startFollowing(pathId);
    }

    private void startFollowing(String pathId) {
        TrailBookState.setActivePathId( pathId);
        TrailBookState.setActiveSegmentId( null);
        mLocationServicesFragment.startUpdates(new PathFollowerLocationProcessor(pathId, this));
    }

    private void stopLocationUpdates() {
        mLocationServicesFragment.stopUpdates();
        mLocationServicesFragment.removeAllNotifications();
        TrailBookState.setMode(TrailBookState.MODE_SEARCH);
        TrailBookState.setActivePathId(null);
        TrailBookState.setActivePathId(null);
        invalidateOptionsMenu();
        mMapFragment.showAllPaths();
        mMapFragment.setVisibilityForAllNoteMarkers(false);
        mMapFragment.setVisibilityForAllStartMarkers(false);
    }

    @Override
    public void onZoomRequested(String pathId) {
        collapseSlidingPanel();
        mMapFragment.zoom(pathId);
    }

    @Override
    public void onNavigateToStart(String pathId) {
        collapseSlidingPanel();
        if (TrailBookState.getMode() == TrailBookState.MODE_FOLLOW ||
                    TrailBookState.getMode() == TrailBookState.MODE_LEAD) {
            stopLocationUpdates();
        }

        LatLng startCoords = mPathManager.getStartCoordsForPath(pathId);
        if (startCoords != null)
            MapUtilities.navigateTo(this, String.valueOf(startCoords.latitude) + "," + String.valueOf(startCoords.longitude));
    }

    @Override
    public void onResumeLeadingRequested(String pathId) {
        collapseSlidingPanel();
        TrailBookState.setActivePathId(pathId);

        //resume the last segment
        TrailBookState.setActiveSegmentId(mPathManager.getPath(pathId).getLastSegment());
        mMapFragment.zoomToCurrentLocation();
        startLeading();
    }

    private void startLeading() {
        TrailBookState.setMode(TrailBookState.MODE_LEAD);
        invalidateOptionsMenu();
        mLocationServicesFragment.startUpdates(
                new PathLeaderLocationProcessor(
                        this,
                        TrailBookState.getActiveSegmentId(),
                        TrailBookState.getActivePathId())
        );
        waitForLocation();
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
            Log.d(Constants.TRAILBOOK_TAG, "MapsActivity: backstack count:" + backStackEntryCount);
            if(backStackEntryCount == 0) {
                //we're exiting the main activity
                mLocationServicesFragment.stopUpdates();
                mLocationServicesFragment.removeAllNotifications();
            }

            super.onBackPressed();
        }
    }

/*    private void startSavedMode() {
        if (mMode.equals(MODE_LEAD)) {
            startLeading();
        } else if (mMode.equals(MODE_FOLLOW)) {
            startFollowing(mCurrentPathId);
        } else if (mMode.equals(MODE_SEARCH)) {
            switchToSearchMode();
        }
    }*/

    private void setUpFragments() {
        setUpNavDrawerFragment();
        setUpPreferencesFragment();
        setUpLocationServicesFragmentIfNeeded();
    }

    private void initializeGlobals() {
        mResources = this.getResources();
        bus = BusProvider.getInstance(); //create the event bus that will be used by fragments interested in sharing events.
        bus.register(this);
        mPathManager = PathManager.getInstance();
    }

    private void createQuickNote(int id) {
        String content = TrailbookPathUtilities.getQuickNoteContent(getResources(), id);
        if (content != null) {
            Note qn = new Note(TrailbookPathUtilities.getNewNoteId(), TrailBookState.getActiveSegmentId());
            qn.setNoteContent(content);
            PointAttachedObject<Note> paoNote = attachNoteToCurrentLocation(qn);
            addNoteToActiveSegment(paoNote);
            Log.d(Constants.TRAILBOOK_TAG, "added quick note:" + content + " at " + paoNote.getLocation() + " to segment " + TrailBookState.getActiveSegmentId());
        }
    }

    public void showFullNote(String noteId) {
        switchFragmentAndAddToBackstack(getFullNoteViewFragment(noteId), PATH_SELECT_UPLOAD_TAG);
    }

    private Fragment getFullNoteViewFragment(String noteId) {
        return FullNoteViewFragment.newInstance(noteId);
    }

    private PathsOnDeviceSelectorFragment getPathSelectorFragmentForDownloadedPaths() {
        return PathsOnDeviceSelectorFragment.newInstance();
    }

    private FollowPathSelectorFragment getFollowPathSelectorFragment() {
        return FollowPathSelectorFragment.newInstance();
    }

    private void setUpMapFragment() {
        Log.d(Constants.TRAILBOOK_TAG, "restoring map fragment");
        mMapFragment= TrailBookMapFragment.newInstance();
        mMapFragment.setParentActivity(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        if (requestCode == FILE_CHOOSER_DIALOG_ID && resultCode == Activity.RESULT_OK) {
            try {
                Uri uri = null;
                if (resultData != null) {
                    uri = resultData.getData();
                    Log.d(Constants.TRAILBOOK_TAG, "Chosen file Uri: " + uri.toString());
                    String pathKml = TrailbookFileUtilities.readTextFromUri(this, uri);
                    PathContainer pathContainer = TrailbookPathUtilities.parseXML(pathKml);
                    Log.d(Constants.TRAILBOOK_TAG, "parsed path:" + pathContainer.path.getSummary().getName());
                    mPathManager.savePath(pathContainer, this);
                }
            } catch (Exception e) {
                Log.d(Constants.TRAILBOOK_TAG, "Exception getting kml file", e);
            }
        }
    }

    @Subscribe
    public void onLocationChangedEvent(LocationChangedEvent event){
        cancelWaitForLocationDialog();
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
}
