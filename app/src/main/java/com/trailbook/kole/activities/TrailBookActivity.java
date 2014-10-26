package com.trailbook.kole.activities;

import android.accounts.AccountManager;
import android.app.ActionBar;
import android.app.Activity;
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
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.trailbook.kole.activities.utils.Action;
import com.trailbook.kole.activities.utils.PathUploaderAction;
import com.trailbook.kole.data.Attachment;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.Note;
import com.trailbook.kole.data.Path;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.events.LocationChangedEvent;
import com.trailbook.kole.events.ModeChangedEvent;
import com.trailbook.kole.fragments.NavigationDrawerFragment;
import com.trailbook.kole.fragments.PathDetailsActionListener;
import com.trailbook.kole.fragments.TBPreferenceFragment;
import com.trailbook.kole.fragments.TrailBookMapFragment;
import com.trailbook.kole.fragments.dialogs.AlertDialogFragment;
import com.trailbook.kole.fragments.dialogs.CreatePathDialogFragment;
import com.trailbook.kole.fragments.path_selector.FollowPathSelectorFragment;
import com.trailbook.kole.fragments.path_selector.PathSelectorFragment;
import com.trailbook.kole.fragments.point_attached_object_create.CreatePointObjectListener;
import com.trailbook.kole.fragments.point_attched_object_view.FullObjectViewFragment;
import com.trailbook.kole.helpers.ApplicationUtils;
import com.trailbook.kole.helpers.MapUtilities;
import com.trailbook.kole.helpers.NoteFactory;
import com.trailbook.kole.helpers.TrailbookFileUtilities;
import com.trailbook.kole.helpers.TrailbookPathUtilities;
import com.trailbook.kole.location_processors.PathFollowerLocationProcessor;
import com.trailbook.kole.location_processors.TrailBookLocationReceiver;
import com.trailbook.kole.state_objects.Authenticator;
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
        CreatePointObjectListener,
        PathSelectorFragment.OnPathSelectorFragmentInteractionListener,
        PopupMenu.OnMenuItemClickListener
{

    private static final String className = "TrailBookActivity";

    private static final int CONFIRM_CREATE_DIALOG_ID = 1;
    private static final int FILE_CHOOSER_DIALOG_ID = 2;

    private static final String MAPS_ACTIVITY_TAG = "MAPS_ACTIVITY";
    private static final String ADD_NOTE_FRAG_TAG = "ADD_NOTE_FRAG";
    private static final String PREF_FRAG_TAG = "PREF_FRAG";
    private static final String PATH_SELECT_UPLOAD_TAG = "PATH_SELECT_UPLOAD";
    private static final String PATH_SELECT_TAG = "PATH_SELECT_UPLOAD";
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
    private  String mActionPath = null;

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
        Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ": activity onCreate() mode:" + TrailBookState.getMode());
        if (TrailBookState.getMode() == TrailBookState.MODE_LEAD) {
            TrailBookState.getInstance().resumeLeadingActivePath(true);
        } else if (TrailBookState.getMode()== TrailBookState.MODE_FOLLOW) {
            TrailBookState.getInstance().restoreActivePath();
            startFollowing(TrailBookState.getActivePathId());
        } else if (TrailBookState.getMode() == TrailBookState.MODE_SEARCH) {
            refreshPaths(false);
        } else if (TrailBookState.getMode() == TrailBookState.MODE_EDIT) {
            TrailBookState.getInstance().restoreActivePath();
        } else {
            refreshPaths(true);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Authenticator.getInstance().disconnect();
    }

    @Override
    public void onPointObjectCreated(String noteId, Attachment newAttachment) {
        getFragmentManager().popBackStackImmediate();
        invalidateOptionsMenu();
        PointAttachedObject paoNote = mPathManager.getPointAttachedObject(noteId);
        if (paoNote == null) {
            if (TrailBookState.getMode() == TrailBookState.MODE_LEAD)
                paoNote = attachNoteToCurrentLocation(newAttachment);
            else if (TrailBookState.getMode() == TrailBookState.MODE_EDIT) {
                LatLng location = mMapFragment.getSelectedLocation();
                Log.d(Constants.TRAILBOOK_TAG, className + " creating pao note for location " + location);
                paoNote = attachNoteToLocation(location, newAttachment);
                mMapFragment.removeSelectedMarker();
            }
        } else {
            paoNote.setAttachment(newAttachment);
        }
        addNoteToActivePath(paoNote);
        Log.d(Constants.TRAILBOOK_TAG, className +  " added note: " + paoNote.getId() + " contnt: " + newAttachment.getShortContent() + " at " + paoNote.getLocation() + " to path " + TrailBookState.getActivePathId());
    }

    @Override
    public void onPaoDeleted(String paoId) {
        getFragmentManager().popBackStackImmediate();
        invalidateOptionsMenu();
        mPathManager.deletePaoFromPath(TrailBookState.getActivePathId(), paoId);
        mPathManager.savePath(TrailBookState.getActivePathId());
    }

    private void addNoteToActivePath(PointAttachedObject note) {
        String pathId = TrailBookState.getActivePathId();
        if (pathId != null) {
            mPathManager.addPointAttachedObjectToPath(pathId, note);
            mPathManager.savePath(pathId);
        }
    }

    private PointAttachedObject attachNoteToCurrentLocation(Attachment attachment) {
        Location l = TrailBookState.getCurrentLocation();
        if (l != null) {
            LatLng point = TrailbookPathUtilities.locationToLatLon(l);
            return attachNoteToLocation(point, attachment);
        } else {
            return null;
        }
    }

    private PointAttachedObject attachNoteToLocation(LatLng point, Attachment attachment) {
        return new PointAttachedObject(TrailbookPathUtilities.getNewNoteId(), point, attachment);
    }

    @Override
    public void onPointObjectCreateCanceled() {
        getFragmentManager().popBackStackImmediate();
        invalidateOptionsMenu();
    }

    @Override
    public void processMenuAction(String pathId, MenuItem item) {
        mActionPath = pathId;
        onMenuItemClick(item);
    }

    @Override
    public void executeAction(int action, String pathId) {
        collapseSlidingPanel();
        if (action != ApplicationUtils.MENU_CONTEXT_DELETE_ID)
            getFragmentManager().popBackStackImmediate();

        if (action == ApplicationUtils.MENU_CONTEXT_DISMISS_ID) {
            return;
        }

        PathSummary summary = mPathManager.getPathSummary(pathId);
        if (summary == null) {
            Log.e(Constants.TRAILBOOK_TAG, className + " onPathSelectorFragmentResult can't find path:" + pathId);
            return;
        }

        Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ": pathId=" + pathId + " action= " + action);
        if (action == ApplicationUtils.MENU_CONTEXT_UPLOAD_ID) {
            if (isNetworkConnected()) {
                Log.d(Constants.TRAILBOOK_TAG, className + " Uploading path:" + pathId);
                //TODO: update path details and confirm
                //TODO: show progress dialog
                Action uploadAction = new PathUploaderAction(mWorkFragment, mPathManager.getPathSummary(pathId));
                String userId = TrailBookState.getCurrentUserId();
                if (userId == null || userId == "-1") {
                    authenticateForAction(uploadAction);
                    //we don't actually need to connect.
                    //Authenticator.getInstance().connect();
                } else {
                    Log.d(Constants.TRAILBOOK_TAG, className + ": already has user id " + userId);
                    summary.setOwnerID(userId);
                    uploadAction.execute();
                }
            } else {
                Log.d(Constants.TRAILBOOK_TAG, className + " no network connectivity");
                showNoNetworkStatusDialog();
            }
        } else if (action == ApplicationUtils.MENU_CONTEXT_DELETE_ID) {
            Log.d(Constants.TRAILBOOK_TAG, className + " Deleting path:" + pathId);
            mPathManager.deletePath(pathId, this);
        } else if (action == ApplicationUtils.MENU_CONTEXT_FOLLOW_ID ) {
            getFragmentManager().popBackStackImmediate();
            onFollowRequested(pathId);
        } else if (action == ApplicationUtils.MENU_CONTEXT_TO_START_ID) {
            getFragmentManager().popBackStackImmediate();
            onNavigateToStart(pathId);
        } else if (action == ApplicationUtils.MENU_CONTEXT_EDIT_ID) {
            getFragmentManager().popBackStackImmediate();
            TrailBookState.getInstance().switchToEditMode(pathId);
        } else if (action == ApplicationUtils.MENU_CONTEXT_ZOOM_ID) {
            onZoomRequested(pathId);
        } else if (action == ApplicationUtils.MENU_CONTEXT_DOWNLOAD_ID) {
            onDownloadRequested(pathId);
        } else if (action == ApplicationUtils.MENU_CONTEXT_DELETE_FROM_CLOUD_ID) {
            if (isNetworkConnected()) {
                Log.d(Constants.TRAILBOOK_TAG, className + " Confirming delete path:" + pathId);
                deleteFromCloud(pathId);
            } else {
                Log.d(Constants.TRAILBOOK_TAG, className + " no network connectivity");
                showNoNetworkStatusDialog();
            }
        } else if (action == ApplicationUtils.MENU_CONTEXT_RESUME_ID) {
            Log.d(Constants.TRAILBOOK_TAG, className + ": resuming path " + pathId);
            String newSegmentId = mPathManager.addNewSegmentToPath(pathId);
            TrailBookState.setActiveSegmentId(newSegmentId);
            startLeading(pathId, newSegmentId);
        }
    }

    private void authenticateForAction(Action uploadAction) {
        Log.d(Constants.TRAILBOOK_TAG, className + ": getting account");
        Authenticator.getInstance().initializeAuthentication(this);
        Authenticator.getInstance().pickUserAccount();
        Authenticator.getInstance().setActionOnAccountReceived(uploadAction);
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
    protected  void onStart() {
        super.onStart();
        /*Authenticator.getInstance().pickUserAccount();
        Authenticator.getInstance().connect();*/
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
        if (TrailBookState.getMode() == TrailBookState.MODE_LEAD ) {
            if (!isCreateNoteDialogShowing())
                inflater.inflate(R.menu.leader_menu, menu);
            else
                inflater.inflate(R.menu.create_note_menu, menu);
        } else if (TrailBookState.getMode() == TrailBookState.MODE_EDIT) {
            if (!isCreateNoteDialogShowing())
                inflater.inflate(R.menu.edit_menu, menu);
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
            if (!canCreateNote()) {
                Toast.makeText(this, getString(R.string.select_location_first), Toast.LENGTH_LONG).show();
            } else {
                showCreatePointObjectFragment(NoteFactory.NOTE);
            }
        } else if (id == R.id.action_bar_create_climb) {
            collapseSlidingPanel();
            if (!canCreateClimb()) {
                Toast.makeText(this, getString(R.string.select_location_first), Toast.LENGTH_LONG).show();
            } else {
                showCreatePointObjectFragment(NoteFactory.CLIMB);
            }
        } else if (id == R.id.quick_note_left || id == R.id.quick_note_right || id == R.id.quick_note_straight) {
            collapseSlidingPanel();
            Log.d(Constants.TRAILBOOK_TAG, className + " creating quicknote contextmenu");
            createQuickNote(id);
            invalidateOptionsMenu();
        } else if (id == R.id.action_bar_stop_following) {
//            stopLocationUpdates();
            switchToSearchMode();
        } else if (id == R.id.action_bar_done_editing) {
            switchToSearchMode();
        } else if (id == R.id.action_bar_stop_leading) {
//            stopLocationUpdates();
            switchToSearchMode();
            //todo: prompt for path details and ask to upload if network is available.
        } else if (id == R.id.action_filter) {
            Log.d(Constants.TRAILBOOK_TAG, className + " show filter dialog");
        } else if (id == R.id.action_list) {
            Log.d(Constants.TRAILBOOK_TAG, className + " listing paths within the view");
            displayPathSelectorForPathsInMapView();
        } else if (id == R.id.action_refresh_map) {
            Log.d(Constants.TRAILBOOK_TAG, className + " refreshing paths the view");
            refreshPaths(true);
        }
        return super.onOptionsItemSelected(item);
    }

    private void displayPathSelectorForPathsInMapView() {
        ArrayList<String> pathIds = PathManager.getInstance().getPathsWithinBounds(mMapFragment.getBounds());
        if (pathIds.size() > 0)
            switchFragmentAndAddToBackstack(getPathSelectorFragmentForPaths(pathIds), PATH_SELECT_TAG);
        else
            showNoPathsWithinViewAlert();
    }

    private void showListViewForPaths(ArrayList<PathSummary> pathsWithinBounds) {

    }

    private boolean canCreateNote() {
        if (TrailBookState.getMode() == TrailBookState.MODE_EDIT && mMapFragment.getSelectedLocation() == null)
            return false;
        else
            return true;
    }

    private boolean canCreateClimb() {
        if (TrailBookState.getMode() == TrailBookState.MODE_EDIT && mMapFragment.getSelectedLocation() == null)
            return false;
        else
            return true;
    }

    private void showCreatePointObjectFragment(String type) {
        switchFragmentAndAddToBackstack(NoteFactory.getCreatePointObjectFragment(TrailbookPathUtilities.getNewNoteId(), type),
                ADD_NOTE_FRAG_TAG);
        invalidateOptionsMenu();
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
            launchFileChooserForKmlImport();
        } else if (position == 5) { // prefs
            switchFragmentAndAddToBackstack(mPreferencesFragment, PREF_FRAG_TAG);
        } else { // unknown selection, go to search mode
            switchToSearchMode();
        }
    }

    private void launchFileChooserForKmlImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        // Need this for google drive
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {
                "*/*"
        });

        startActivityForResult(intent, FILE_CHOOSER_DIALOG_ID);
    }

    private void switchToSearchMode() {
        TrailBookState.getInstance().switchToSearchMode();
    }

    @Subscribe
    public void onModeChangedEvent(ModeChangedEvent event) {
        if (event.getNewMode() == TrailBookState.MODE_SEARCH
                || event.getNewMode() == TrailBookState.MODE_EDIT) {
            invalidateOptionsMenu();
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
    public void onNewPathClick(String pathName) {
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
        mMapFragment.showOnlyPath(pathId);
        mMapFragment.zoomToPath(pathId);
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
        mMapFragment.zoomToPath(pathId);
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
    public void onMoreActionsSelected(String pathId, View v) {
//        startLeading(pathId, mPathManager.getPathSummary(pathId).getLastSegment());
        mActionPath = pathId;
        ApplicationUtils.showActionsPopupForPath(this, v, this, pathId);
    }

    private void startLeading(String pathId, String segmentId) {
        collapseSlidingPanel();
        invalidateOptionsMenu();
        TrailBookState.getInstance().switchToLeadMode(pathId, segmentId);
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
            PointAttachedObject paoNote = null;
            if (TrailBookState.getMode() == TrailBookState.MODE_LEAD) {
                paoNote = attachNoteToCurrentLocation(qn);
            }else if (TrailBookState.getMode() == TrailBookState.MODE_EDIT) {
                LatLng selectedLocation = mMapFragment.getSelectedLocation();
                if (selectedLocation != null) {
                    paoNote = attachNoteToLocation(selectedLocation, qn);
                    mMapFragment.removeSelectedMarker();
                } else {
                    Toast.makeText(this, getString(R.string.select_location_first), Toast.LENGTH_LONG).show();
                    return;
                }
            }
            addNoteToActivePath(paoNote);
            Log.d(Constants.TRAILBOOK_TAG, className + ": added quick note:" + content + " at " + paoNote.getLocation() + " to segment " + TrailBookState.getActiveSegmentId());
        }
    }

    public void showFullObject(String paoId) {
        if (TrailBookState.getMode() == TrailBookState.MODE_LEAD
                || TrailBookState.getMode() == TrailBookState.MODE_EDIT) {
            PointAttachedObject pao = mPathManager.getPointAttachedObject(paoId);
            switchFragmentAndAddToBackstack(NoteFactory.getCreatePointObjectFragment(paoId, pao.getAttachment().getType()), ADD_NOTE_FRAG_TAG);
            invalidateOptionsMenu();
        } else {
            switchFragmentAndAddToBackstack(getFullNoteViewFragment(paoId), PATH_SELECT_UPLOAD_TAG);
        }
    }

    private Fragment getFullNoteViewFragment(String noteId) {
        return FullObjectViewFragment.newInstance(noteId);
    }

    private PathSelectorFragment getPathSelectorFragmentForDownloadedPaths() {
        //return PathsOnDeviceSelectorFragment.newInstance();
        ArrayList<String> downloadedPathIds = mPathManager.getDownloadedPathIds();
        return PathSelectorFragment.newInstance(downloadedPathIds);
    }

    private PathSelectorFragment getPathSelectorFragmentForPaths(ArrayList<String> pathIds) {
        return PathSelectorFragment.newInstance(pathIds);
    }

    private FollowPathSelectorFragment getFollowPathSelectorFragment() {
        ArrayList<String> downloadedPathIds = mPathManager.getDownloadedPathIds();
        return FollowPathSelectorFragment.newInstance(downloadedPathIds);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        if (requestCode == FILE_CHOOSER_DIALOG_ID && resultCode == Activity.RESULT_OK) {
            collapseSlidingPanel();
            try {
                Uri uri = null;
                if (resultData != null) {
                    uri = resultData.getData();
                    Log.d(Constants.TRAILBOOK_TAG, className + ": Chosen file Uri: " + uri.toString());
                    String pathKml = TrailbookFileUtilities.readTextFromUri(this, uri);
                    Path pathContainer = TrailbookPathUtilities.parseXML(pathKml);
                    String pathName = pathContainer.summary.getName();

                    Log.d(Constants.TRAILBOOK_TAG, className + ": parsed path:" + pathName);
                    mPathManager.savePath(pathContainer);
                    mPathManager.addPath(pathContainer);
/*                    if (!PathManager.getInstance().doesSummaryWithNameAlreadyExist(pathName)) {
                        mPathManager.savePath(pathContainer, this);
                        mPathManager.addPath(pathContainer);
                    } else
                        showPathExistsAlert(pathName);*/
                }
            } catch (Exception e) {
                Log.d(Constants.TRAILBOOK_TAG, className + ": Exception getting kml file", e);
                showErrorParsingKMLAlert();
            }
        } else if (requestCode == Authenticator.REQUEST_CODE_SIGN_IN ) {
            if (resultCode == RESULT_OK) {
                Log.d(Constants.TRAILBOOK_TAG, className + " back from authentication intent.  result is " + resultCode + " resultData = " + resultData);
                Authenticator.getInstance().onReturnFromIntent();
            } else {
                //todo: failed
            }
        } else if (requestCode == Authenticator.REQUEST_CODE_PICK_ACCOUNT) {
            if (resultCode == RESULT_OK && resultData != null && resultData.getStringExtra(AccountManager.KEY_ACCOUNT_NAME) != null) {
                Log.d(Constants.TRAILBOOK_TAG, className + " back from authentication pick account intent.  result is " + resultCode + " resultData = " + resultData);
                Authenticator.getInstance().onGotAccount(resultData.getStringExtra(AccountManager.KEY_ACCOUNT_NAME));
            } else {
                DialogInterface.OnClickListener clickListenerOK = new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        Log.d(Constants.TRAILBOOK_TAG, className + ": dismissing can't get account dialog");
                    }
                };
                ApplicationUtils.showAlert(this,clickListenerOK ,"Unable to obtain Google Account", "A Google Services account is required to add trails to the Trailbook cloud.", getString(R.string.OK), null);
            }
        }
    }

    @Subscribe
    public void onLocationChangedEvent(LocationChangedEvent event){
        if (mWaitingForLocationDialog == null) //the dialog has already been dismissed.
            return;

        if (TrailbookPathUtilities.isAccurateEnoughToStartLeading(event.getLocation()))
            cancelWaitForLocationDialog();
    }

    public void refreshPaths(boolean forceCloudRefresh) {
        mWorkFragment.startGetPathSummariesLocal();
        if (forceCloudRefresh || isTimeToRereshFromCloud()) {
            refreshFromCloudIfConnectedToNetwork();
        }
    }

    private void refreshFromCloudIfConnectedToNetwork() {
        if (isNetworkConnected()) {
            Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ": refreshing from cloud");
            mWorkFragment.startGetPathSummariesRemote(null, 0);
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

    private void showNoPathsWithinViewAlert() {
        DialogInterface.OnClickListener clickListenerOK = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int id) {
                Log.d(Constants.TRAILBOOK_TAG, className + ": dismissing no paths dialog");
                //todo: launch searcher here
            }
        };
        Log.d(Constants.TRAILBOOK_TAG, className + ": showing no paths alert");
        ApplicationUtils.showAlert(this, clickListenerOK, getString(R.string.info_no_paths_within_view_title), getString(R.string.no_paths_within_view_message), getString(R.string.OK), null);
    }

    private void showExitAlert(String reason) {
        DialogInterface.OnClickListener clickListenerOK = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int id) {
                Log.d(Constants.TRAILBOOK_TAG, className + ": exiting application");
                TrailBookState.getInstance().stopLocationUpdates();
                TrailBookState.getInstance().removeAllNotificaions();
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


    private void showErrorParsingKMLAlert() {
        DialogInterface.OnClickListener clickListenerOK = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int id) {
                Log.d(Constants.TRAILBOOK_TAG, className + ": dismissing kml parse error dialog");
            }
        };
        ApplicationUtils.showAlert(this, clickListenerOK, getString(R.string.parse_error), getString(R.string.parse_error_message), getString(R.string.OK), null);
    }

    private void showPathExistsAlert(String pathName) {
        DialogInterface.OnClickListener clickListenerOK = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int id) {
                Log.d(Constants.TRAILBOOK_TAG, className + ": dismissing kml parse error dialog");
            }
        };
        String message = String.format(getString(R.string.path_exists_message), pathName);
        String title = String.format(getString(R.string.path_exists), pathName);
        ApplicationUtils.showAlert(this, clickListenerOK, title, message, getString(R.string.OK), null);
    }

    private void toastNoNetwork() {
        Toast.makeText(this, getString(R.string.no_network_toast), Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (mActionPath == null)
            return false;

        executeAction(item.getItemId(), mActionPath);
        mActionPath = null;
        return true;
    }

    public void deleteFromCloud(final String pathId) {
        DialogInterface.OnClickListener affirmCloudDeleteListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int id) {
            Log.d(Constants.TRAILBOOK_TAG, className + ": deleting path " + pathId);
            mWorkFragment.startPathDeleteMongo(pathId);
            }
        };
        ApplicationUtils.showAlert(this, affirmCloudDeleteListener, "Delete From Cloud?", "This action is not reversible.", "Delete", "Cancel");
    }
}
