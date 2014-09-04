package com.trailbook.kole.activities;

import android.app.ActionBar;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.res.Resources;
import android.location.Location;
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
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.events.LocationChangedEvent;
import com.trailbook.kole.fragments.AlertDialogFragment;
import com.trailbook.kole.fragments.CreateNoteFragment;
import com.trailbook.kole.fragments.CreatePathDialogFragment;
import com.trailbook.kole.fragments.MyMapFragment;
import com.trailbook.kole.fragments.NavigationDrawerFragment;
import com.trailbook.kole.fragments.PathDetailsActionListener;
import com.trailbook.kole.fragments.PathSelectorFragment;
import com.trailbook.kole.fragments.PathsToUploadSelectorFragment;
import com.trailbook.kole.fragments.TBPreferenceFragment;
import com.trailbook.kole.tools.BusProvider;
import com.trailbook.kole.tools.MapUtilities;
import com.trailbook.kole.tools.PathFollowerLocationProcessor;
import com.trailbook.kole.tools.PathLeaderLocationProcessor;
import com.trailbook.kole.tools.PathManager;
import com.trailbook.kole.tools.TrailbookPathUtilities;
import com.trailbook.kole.worker_fragments.LocationServicesFragment;
import com.trailbook.kole.worker_fragments.WorkerFragment;

import java.util.ArrayList;

public class MapsActivity extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks,
        PathDetailsActionListener,
        AlertDialogFragment.AlertDialogListener,
        CreatePathDialogFragment.CreatePathDialogListener,
        CreateNoteFragment.CreateNoteFragmentListener,
        PathSelectorFragment.OnFragmentInteractionListener {

    private static final int CONFIRM_CREATE_DIALOG_ID = 1;
    private static final String MAPS_ACTIVITY_TAG = "MAPS_ACTIVITY";
    private static final String ADD_NOTE_FRAG_TAG = "ADD_NOTE_FRAG";
    private static final String PREF_FRAG_TAG = "PREF_FRAG";
    private static final String PATH_SELECT_UPLOAD_TAG = "PATH_SELECT_UPLOAD";
    private static final String SAVED_CURRENT_PATH_ID = "CURRENT_PATH_ID" ;
    private static final String SAVED_CURRENT_SEGMENT_ID = "CURRENT_SEGMENT_ID";
    public static final String MODE_SEARCH = "SEARCH";
    public static final String MODE_LEAD = "LEAD";
    public static final String MODE_FOLLOW = "FOLLOW";
    public static final String SAVED_MODE = "MODE";

    private CharSequence mTitle; // Used to store the last screen title. For use in {@link #restoreActionBar()}.
    private MyMapFragment mMapFragment;
    private WorkerFragment mWorkFragment;
    private PathManager mPathManager;
    private NavigationDrawerFragment mNavigationDrawerFragment;
    private LocationServicesFragment mLocationServicesFragment;
    private TBPreferenceFragment mPreferencesFragment;
    private Bus bus;
    private String mMode;
    private Resources mResources;
    private SlidingUpPanelLayout mSlidingUpPanel;
    private String mCurrentPathId;
    private String mCurrentSegmentId;
    private Fragment mPathSelectorFragment;
    private Fragment mContent;

    @Override
    public void onNoteCreated(Note newNote) {
        getFragmentManager().popBackStackImmediate();
        invalidateOptionsMenu();
        LatLng point = TrailbookPathUtilities.locationToLatLon(mPathManager.getCurrentLocation());
        PointAttachedObject<Note> note = new PointAttachedObject<Note>(point,newNote);
        if (mCurrentSegmentId != null && mCurrentPathId != null) {
            mPathManager.addNoteToSegment(mPathManager.getSegment(mCurrentSegmentId), note);
            mPathManager.savePath(mCurrentPathId, this);
        }
    }

    @Override
    public void onNoteCreateCanceled() {
        getFragmentManager().popBackStackImmediate();
        invalidateOptionsMenu();
    }

    @Override
    public void onFragmentInteraction(String action, String pathId) {
        if (PathSelectorFragment.UPLOAD == action) {
            Path p = mPathManager.getPath(pathId);
            if (p != null) {
                //TODO: update path details and confirm
                Toast.makeText(this, "Uploading " + p.getSummary().getName(), Toast.LENGTH_SHORT).show();
                mWorkFragment.startPathUpload(mPathManager.getPath(pathId));
            } else
                Log.e(Constants.TRAILBOOK_TAG, "null path ID in path upload selected");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMode = MODE_SEARCH;

        //Restore the fragment's instance
        if (savedInstanceState != null) {
            Log.d(Constants.TRAILBOOK_TAG, "getting fragment state");
            mCurrentPathId = savedInstanceState.getString(SAVED_CURRENT_PATH_ID);
            mCurrentSegmentId = savedInstanceState.getString(SAVED_CURRENT_SEGMENT_ID);
            mMode = savedInstanceState.getString(SAVED_MODE);
        }

        mResources = this.getResources();

        bus = BusProvider.getInstance(); //create the event bus that will be used by fragments interested in sharing events.
        bus.register(this);
        mMapFragment=MyMapFragment.newInstance();
        Log.d(Constants.TRAILBOOK_TAG, "restoring map fragment");
        mPathManager = PathManager.getInstance();

        setContentView(R.layout.activity_maps);
        setUpNavDrawerFragment();
        setUpPreferencesFragment();
        setUpLocationServicesFragmentIfNeeded();
        if (mMode.equals(MODE_LEAD))
            startLeading();
        else if (mMode.equals(MODE_FOLLOW))
            startFollowing(mCurrentPathId);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SAVED_CURRENT_PATH_ID, mCurrentPathId);
        outState.putString(SAVED_CURRENT_SEGMENT_ID, mCurrentSegmentId);
        outState.putString(SAVED_MODE, mMode);
        Log.d(Constants.TRAILBOOK_TAG, "putting fragment state");
//        getFragmentManager().putFragment(outState, "mContent", mContent);
    }

    private PathsToUploadSelectorFragment getPathSelectorFragmentForDownloadedPaths() {
        return PathsToUploadSelectorFragment.newInstance();
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
            getMenuInflater().inflate(R.menu.my, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        MenuInflater inflater = getMenuInflater();
        if (mMode == MODE_LEAD) {
            if (!isCreateNoteDialogShowing())
                inflater.inflate(R.menu.leader_menu, menu);
            else
                inflater.inflate(R.menu.create_note_menu, menu);
        } else {
            getMenuInflater().inflate(R.menu.my, menu);
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
            switchFragmentAndAddToBackstack(CreateNoteFragment.newInstance(TrailbookPathUtilities.getNewNoteId(), mCurrentSegmentId), ADD_NOTE_FRAG_TAG);
            invalidateOptionsMenu();
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
        FragmentManager fm = getFragmentManager();
        mLocationServicesFragment = (LocationServicesFragment)fm.findFragmentByTag("locationServices");
        if (mLocationServicesFragment == null)
            createLocationServicesFragment();
    }

    private void createLocationServicesFragment() {
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
        FragmentManager fragmentManager = getFragmentManager();
        Fragment newFrag;
        if (position == 1) { // find paths
            mMode = MODE_SEARCH;
            invalidateOptionsMenu();
        } else if (position == 2) { // new path
            //TODO:  warnIfStoppingUnfinishedPath();
            openNewPathDialog();
        } else if (position == 3) {
            switchFragmentAndAddToBackstack(getPathSelectorFragmentForDownloadedPaths(), PATH_SELECT_UPLOAD_TAG);
        } else if (position == 4) { // prefs
            switchFragmentAndAddToBackstack(mPreferencesFragment, PREF_FRAG_TAG);
        } else {
            newFrag = mMapFragment;
            fragmentManager.beginTransaction()
                    .replace(R.id.map_container, newFrag)
                    .commit();
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
        mCurrentSegmentId = mPathManager.makeNewSegment();
        mCurrentPathId = mPathManager.makeNewPath(pathName, mCurrentSegmentId);
        Fragment f = getFragmentManager().findFragmentByTag("new_path_dialog");
        if (f != null) {
            Log.d(Constants.TRAILBOOK_TAG, "removing dialog");
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.remove(f);
            ft.commit();
        }
        startLeading();
    }

    public String getMode() {
        return mMode;
    }

    private void warnIfStoppingUnfinishedPath() {
        if (mMode == MODE_LEAD) {
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

    public MapsActivity() {}

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
        mMode = MODE_FOLLOW;
        mMapFragment.showNotesOnlyForPath(pathId);
        invalidateOptionsMenu();
        startFollowing(pathId);
    }

    private void startFollowing(String pathId) {
        mLocationServicesFragment.startUpdates(new PathFollowerLocationProcessor(pathId, this));
    }

    @Override
    public void onZoomRequested(String pathId) {
        collapseSlidingPanel();
        mMapFragment.zoom(pathId);
    }

    @Override
    public void onNavigateToStart(String pathId) {
        collapseSlidingPanel();
        LatLng startCoords = mPathManager.getStartCoordsForPath(pathId);
        if (startCoords != null)
            MapUtilities.navigateTo(this, String.valueOf(startCoords.latitude) + "," + String.valueOf(startCoords.longitude));
    }

    @Override
    public void onResumeLeadingRequested(String pathId) {
        collapseSlidingPanel();
        mCurrentPathId = pathId;
        //resume the last segment
        mCurrentSegmentId = mPathManager.getPath(pathId).getLastSegment();
        startLeading();
    }

    private void startLeading() {
        mMode = MODE_LEAD;
        invalidateOptionsMenu();
        mLocationServicesFragment.startUpdates(new PathLeaderLocationProcessor(this, mCurrentSegmentId, mCurrentPathId));
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
            mLocationServicesFragment.stopUpdates();
            super.onBackPressed();
        }
    }
}
