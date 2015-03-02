package com.trailbook.kole.activities;

import android.accounts.AccountManager;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.PopupMenu;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.trailbook.kole.activities.utils.Action;
import com.trailbook.kole.activities.utils.AttachedCommentUploaderAction;
import com.trailbook.kole.activities.utils.CommentUploaderAction;
import com.trailbook.kole.activities.utils.LoginUtil;
import com.trailbook.kole.activities.utils.PathUploaderAction;
import com.trailbook.kole.data.Attachment;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.Note;
import com.trailbook.kole.data.Path;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.data.TrailBookComment;
import com.trailbook.kole.events.LocationChangedEvent;
import com.trailbook.kole.events.ModeChangedEvent;
import com.trailbook.kole.events.PathCommentAddedEvent;
import com.trailbook.kole.events.PathDetailRequestEvent;
import com.trailbook.kole.events.PathReceivedEvent;
import com.trailbook.kole.events.RefreshMessageEvent;
import com.trailbook.kole.events.ZoomRequestEvent;
import com.trailbook.kole.fragments.DisplayCommentsFragment;
import com.trailbook.kole.fragments.FilterGroupsFragment;
import com.trailbook.kole.fragments.NavigationDrawerFragment;
import com.trailbook.kole.fragments.PathDetailsActionListener;
import com.trailbook.kole.fragments.TBPreferenceFragment;
import com.trailbook.kole.fragments.TrailBookMapFragment;
import com.trailbook.kole.fragments.dialogs.AlertDialogFragment;
import com.trailbook.kole.fragments.dialogs.CreateCommentFragment;
import com.trailbook.kole.fragments.dialogs.CreatePathDialogFragment;
import com.trailbook.kole.fragments.path_selector.FollowPathSelectorFragment;
import com.trailbook.kole.fragments.path_selector.PathSelectorFragment;
import com.trailbook.kole.fragments.point_attached_object_create.CreatePointObjectListener;
import com.trailbook.kole.fragments.point_attched_object_view.FullObjectViewFragment;
import com.trailbook.kole.fragments.upload.PathUploadDetailsFragment;
import com.trailbook.kole.helpers.ApplicationUtils;
import com.trailbook.kole.helpers.MapUtilities;
import com.trailbook.kole.helpers.NoteFactory;
import com.trailbook.kole.helpers.TrailbookFileUtilities;
import com.trailbook.kole.helpers.TrailbookPathUtilities;
import com.trailbook.kole.location_processors.NotificationUtils;
import com.trailbook.kole.location_processors.PathFollowerLocationProcessor;
import com.trailbook.kole.location_processors.TrailBookLocationReceiver;
import com.trailbook.kole.services.async_tasks.AsyncGetPathFromLocalDevice;
import com.trailbook.kole.services.download.DownloadPathService;
import com.trailbook.kole.services.upload.UploadPathService;
import com.trailbook.kole.state_objects.Authenticator;
import com.trailbook.kole.state_objects.BusProvider;
import com.trailbook.kole.state_objects.PathManager;
import com.trailbook.kole.state_objects.TrailBookState;
import com.trailbook.kole.worker_fragments.WorkerFragment;

import java.io.File;
import java.util.ArrayList;

public class TrailBookActivity extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks,
        PathDetailsActionListener,
        AlertDialogFragment.AlertDialogListener,
        CreatePathDialogFragment.CreatePathDialogListener,
        CreatePointObjectListener,
        PathSelectorFragment.OnPathSelectorFragmentInteractionListener,
        PopupMenu.OnMenuItemClickListener, CreateCommentFragment.CreateCommentDialogListener, PathUploadDetailsFragment.UploadPathDialogListener {

    private static final String CLASS_NAME = "TrailBookActivity";

    private static final int CONFIRM_CREATE_DIALOG_ID = 1;
    private static final int FILE_CHOOSER_DIALOG_ID = 2;

    public static final String MAPS_ACTIVITY_TAG = "MAPS_ACTIVITY";
    private static final String ADD_NOTE_FRAG_TAG = "ADD_NOTE_FRAG";
    private static final String PREF_FRAG_TAG = "PREF_FRAG";
    private static final String PATH_SELECT_UPLOAD_TAG = "PATH_SELECT_UPLOAD";
    private static final String PATH_SELECT_TAG = "PATH_SELECT_UPLOAD";
    private static final String SAVED_CURRENT_PATH_ID = "CURRENT_PATH_ID" ;
    private static final String SAVED_CURRENT_SEGMENT_ID = "CURRENT_SEGMENT_ID";
    private static final String SCOPE =  "https://www.googleapis.com/auth/plus.login";
    private static final String SHOW_PATH_COMMENTS_TAG = "SHOW_PATH_COMMENTS";
    public static final String NEW_PATH_DIALOG_ID = "new_path_dialog";
    public static final String PATH_DETAILS_DIALOG_TAG = "path_details_dialog";
    private static final String FILTER_DIALOG_TAG = "FILTER_DIALOG";
    public static final String INITIAL_PATH_ID_KEY = "INITIAL_PATH_ID";

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
    private TrailBookLocationReceiver mLocationReceiver;
    private ProgressDialog mWaitingForLocationDialog;
    private  String mActionPath = null;
    private WebServiceStateReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        requestWindowFeature(Window.FEATURE_PROGRESS);

        Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " OnCreate");

        mLocationReceiver = new TrailBookLocationReceiver();
        mResources = this.getResources();

        bus = BusProvider.getInstance(); //create the event bus that will be used by fragments interested in sharing events.
        bus.register(this);

        if (savedInstanceState != null) {
            restoreFragments(savedInstanceState);
        } else {
            Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " creating map fragment");
            mMapFragment = TrailBookMapFragment.newInstance();
            getFragmentManager().beginTransaction().add(R.id.map_container, mMapFragment, "map_fragment").commit();
        }

        mPathManager = PathManager.getInstance();

        setContentView(R.layout.activity_maps);
        registerBroadcastReceivers();
        setUpNavDrawerFragment();
        setUpPreferencesFragment();
        setUpWorkFragmentIfNeeded();

        String launchedPathId = getLaunchPathId();
        if (launchedPathId == null || launchedPathId.equals(TrailBookState.NO_START_PATH)) {
            launchFromRestoredState();
        } else {
            Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " launching path " + launchedPathId);
            TrailBookState.setMode(TrailBookState.MODE_SEARCH);
            AsyncGetPathFromLocalDevice asyncGetPath = new AsyncGetPathFromLocalDevice();
            asyncGetPath.execute(launchedPathId);
            onZoomRequested(launchedPathId);
            refreshPaths(false);
            requestShowPathDetails(launchedPathId);
        }
    }

    private void restoreFragments(Bundle savedInstanceState) {
        Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " restoring map fragment");
        mMapFragment = (TrailBookMapFragment) getFragmentManager().findFragmentByTag("map_fragment");
    }

    private void requestShowPathDetails(String launchedPathId) {
        PathDetailRequestEvent event = new PathDetailRequestEvent(launchedPathId);
        bus.post(event);
    }

    private String getLaunchPathId() {
        Intent i = getIntent();
        if (i != null) {
           return i.getStringExtra(INITIAL_PATH_ID_KEY);
        }
        return null;
    }

    private void launchFromRestoredState() {
        TrailBookState.restoreState();
        Log.d(Constants.TRAILBOOK_TAG, getLocalClassName() + ": activity onCreate() mode:" + TrailBookState.getMode());
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

    private void registerBroadcastReceivers() {
        IntentFilter uploadFilter = new IntentFilter(UploadPathService.BROADCAST_ACTION);
        uploadFilter.addCategory(Intent.CATEGORY_DEFAULT);
        IntentFilter downloadFilter = new IntentFilter(DownloadPathService.BROADCAST_ACTION);
        downloadFilter.addCategory(Intent.CATEGORY_DEFAULT);
        receiver = new WebServiceStateReceiver();
        registerReceiver(receiver, uploadFilter);
        registerReceiver(receiver, downloadFilter);
    }

    private void unRegisterBroadcastReceivers() {
        if (receiver != null)
            try {
                unregisterReceiver(receiver);
            } catch (Exception e) {
                //do nothing.  seems to be android bug
            }
    }

    @Override
    public void onStop() {
        super.onStop();
        Authenticator.getInstance().disconnect();
        unRegisterBroadcastReceivers();
    }

    @Override
    public void onDestroy () {
        bus.unregister(this);
        super.onDestroy();
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
                Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " creating pao note for location " + location);
                paoNote = attachNoteToLocation(location, newAttachment);
                mMapFragment.removeSelectedMarker();
            }
        } else {
            paoNote.setAttachment(newAttachment);
        }
        addNoteToActivePath(paoNote);
        Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " added note: " + paoNote.getId() + " contnt: " + newAttachment.getShortContent() + " at " + paoNote.getLocation() + " to path " + TrailBookState.getActivePathId());
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
            Log.e(Constants.TRAILBOOK_TAG, CLASS_NAME + " onPathSelectorFragmentResult can't find path:" + pathId);
            return;
        }

        Log.d(Constants.TRAILBOOK_TAG, getLocalClassName() + ": pathId=" + pathId + " action= " + action);
        if (action == ApplicationUtils.MENU_CONTEXT_UPLOAD_ID) {
            openPathDetailsDialog(pathId);
        } else if (action == ApplicationUtils.MENU_CONTEXT_DELETE_ID) {
            Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " Deleting path:" + pathId);
            mPathManager.deletePath(pathId, this);
            refreshFromCloudIfConnectedToNetwork();
        } else if (action == ApplicationUtils.MENU_CONTEXT_FOLLOW_ID ) {
            getFragmentManager().popBackStackImmediate();
            onFollowRequested(pathId);
        } else if (action == ApplicationUtils.MENU_CONTEXT_TO_START_ID) {
            getFragmentManager().popBackStackImmediate();
            onNavigateToStart(pathId);
        } else if (action == ApplicationUtils.MENU_CONTEXT_EDIT_ID) {
            getFragmentManager().popBackStackImmediate();
            TrailBookState.getInstance().switchToEditMode(pathId);
            displayEditHelp();
        } else if (action == ApplicationUtils.MENU_CONTEXT_ZOOM_ID) {
            onZoomRequested(pathId);
        } else if (action == ApplicationUtils.MENU_CONTEXT_DOWNLOAD_ID) {
            onDownloadRequested(pathId);
        } else if (action == ApplicationUtils.MENU_CONTEXT_DELETE_FROM_CLOUD_ID) {
            if (isNetworkConnected()) {
                Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " Confirming delete path:" + pathId);
                deleteFromCloud(pathId);
            } else {
                Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " no network connectivity");
                showNoNetworkStatusDialog();
            }
        } else if (action == ApplicationUtils.MENU_CONTEXT_RESUME_ID) {
            Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + ": resuming path " + pathId);
            String newSegmentId = mPathManager.addNewSegmentToPath(pathId);
            TrailBookState.setActiveSegmentId(newSegmentId);
            startLeading(pathId, newSegmentId);
        } else if (action == ApplicationUtils.MENU_CONTEXT_SHARE_ID) {
            Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + ": sharing path " + pathId);
            Path path = mPathManager.getPath(pathId);
            File file = TrailbookFileUtilities.zipPathToTempFile(path);
            ApplicationUtils.sendFileViaEmail(this, file);
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
        Log.d(Constants.TRAILBOOK_TAG, "Setting up nav drawer fragment");
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
        if (mNavigationDrawerFragment != null && !mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.search_menu, menu);
            restoreActionBar();
            configureSearchManager(menu);

            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    private void configureSearchManager(Menu menu) {
        Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " configuring search manager");
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.menu_search).getActionView();
        ComponentName cn = new ComponentName(this, SearchResultsActivity.class);
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(cn));
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
            configureSearchManager(menu);
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
            collapseSlidingPanel();
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
            Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " creating quicknote contextmenu");
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
/*        } else if (id == R.id.action_filter) {
            Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " show filter dialog");
            showFilterDialog(); */
        } else if (id == R.id.action_list) {
            Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " listing paths within the view");
            displayPathSelectorForPathsInMapView();
        } else if (id == R.id.action_refresh_map) {
            Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " refreshing paths the view");
            refreshPaths(true);
        }/* else if (id == R.id.action_bar_add_comment) {
            showComments();
        }*/ else if (id == R.id.action_bar_delete_selection) {
            deleteSelection();
        } else if (id == R.id.action_bar_help) {
            displayEditHelp();
        } else if (id == R.id.action_bar_mark_start) {
            addSelectedPointToStartOfTrail();
        } else if (id == R.id.action_bar_mark_end) {
            addSelectedPointToEndOfTrail();
        }

        return super.onOptionsItemSelected(item);
    }

    private void deleteSelection() {
        LatLng selectedLocation = mMapFragment.getSelectedLocation();
        if (selectedLocation != null) {
            String pathId = TrailBookState.getActivePathId();
            TrailbookPathUtilities.deletePointFromPath(pathId, selectedLocation);
            mPathManager.savePath(pathId);
            mMapFragment.removeSelectedMarker();
            mMapFragment.refreshSegmentsForActivePath();

            //todo: add to undo stack
        } else {
            Toast.makeText(this, getString(R.string.select_location_first_delete), Toast.LENGTH_LONG).show();
            return;
        }
    }

    private void addSelectedPointToStartOfTrail() {
        LatLng selectedLocation = mMapFragment.getSelectedLocation();
        if (selectedLocation != null) {
            String pathId = TrailBookState.getActivePathId();
            PathSummary summary = mPathManager.getPathSummary(pathId);
            summary.setStart(selectedLocation);
            mPathManager.savePath(pathId);
            mMapFragment.removeSelectedMarker();
            mMapFragment.showStartPointForPath(pathId);

            //todo: add to undo stack
        } else {
            Toast.makeText(this, getString(R.string.select_location_first_mark_start), Toast.LENGTH_LONG).show();
            return;
        }
    }

    private void addSelectedPointToEndOfTrail() {
        LatLng selectedLocation = mMapFragment.getSelectedLocation();
        if (selectedLocation != null) {
            String pathId = TrailBookState.getActivePathId();
            PathSummary summary = mPathManager.getPathSummary(pathId);
            summary.setEnd(selectedLocation);
            mPathManager.savePath(pathId);
            mMapFragment.removeSelectedMarker();
            mMapFragment.showEndPointForPath(pathId);

            //todo: add to undo stack
        } else {
            Toast.makeText(this, getString(R.string.select_location_first_mark_start), Toast.LENGTH_LONG).show();
            return;
        }
    }

    private void displayPathSelectorForPathsInMapView() {
        ArrayList<String> pathIds = PathManager.getInstance().getPathsWithinBounds(mMapFragment.getBounds());
        if (pathIds.size() > 0)
            switchFragmentAndAddToBackstack(getPathSelectorFragmentForPaths(pathIds, getString(R.string.title_paths_in_view)), PATH_SELECT_TAG);
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
        if (!ApplicationUtils.isFragmentShowing(fm, tag)) {
            fm.beginTransaction()
                    .replace(R.id.map_container, frag, tag)
                    .addToBackStack(MAPS_ACTIVITY_TAG)
                    .commit();
        }
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
        //actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        Fragment newFrag;
        if (position == 0) { // find paths
            //todo: show info dialog on how to search first time.
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
        Fragment prev = getFragmentManager().findFragmentByTag(NEW_PATH_DIALOG_ID);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        CreatePathDialogFragment newFragment = CreatePathDialogFragment.newInstance(R.string.cpd_title);
        newFragment.setListener(this);
        newFragment.show(ft, NEW_PATH_DIALOG_ID);
    }

    private void openPathDetailsDialog(String pathId) {
        collapseSlidingPanel();
        switchFragmentAndAddToBackstack(getPathUploadDetailsFragment(pathId), PATH_DETAILS_DIALOG_TAG);
    }

    private void showComments() {
        collapseSlidingPanel();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag(SHOW_PATH_COMMENTS_TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        DisplayCommentsFragment newFragment = DisplayCommentsFragment.newInstance(TrailBookState.getActivePathId());
        switchFragmentAndAddToBackstack(newFragment, SHOW_PATH_COMMENTS_TAG);
        invalidateOptionsMenu();
    }

    private void showFilterDialog() {
        collapseSlidingPanel();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag(FILTER_DIALOG_TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        FilterGroupsFragment newFragment = FilterGroupsFragment.newInstance();
        switchFragmentAndAddToBackstack(newFragment, FILTER_DIALOG_TAG);
        invalidateOptionsMenu();
    }

    @Override
    public void onNewPathClick(String pathName) {
        String newSegmentId = mPathManager.makeNewSegment();
        TrailBookState.setActiveSegmentId(newSegmentId);
        String newPathId = mPathManager.makeNewPath(pathName, newSegmentId, ApplicationUtils.getDeviceId(this));
        TrailBookState.setActivePathId(newPathId);
        Fragment f = getFragmentManager().findFragmentByTag(NEW_PATH_DIALOG_ID);
        if (f != null) {
            Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " removing dialog");
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
        if (isNetworkConnected()) {
            mWorkFragment.startDownloadPath(pathId);
            mMapFragment.displayMessage(getString(R.string.download_in_progress));
            collapseSlidingPanel();
        }else {
            Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " no network connectivity");
            showNoNetworkStatusDialog();
        }
    }

    @Override
    public void onFollowRequested(String pathId) {
        collapseSlidingPanel();
        TrailBookState.setMode(TrailBookState.MODE_FOLLOW);
        mMapFragment.showOnlyPath(pathId);
        mMapFragment.zoomToPath(pathId);
        mMapFragment.hideEndMarker(pathId);
        getActionBar().setTitle("Following " + mPathManager.getPathSummary(pathId).getName());
        invalidateOptionsMenu();
        startFollowing(pathId);
    }

    private void startFollowing(String pathId) {
        TrailBookState.setActivePathId(pathId);
        TrailBookState.setActiveSegmentId(null);
        TrailBookState.setLocationProcessor(new PathFollowerLocationProcessor(pathId, this));
        TrailBookState.getInstance().startLocationUpdates();
        bus.post(new RefreshMessageEvent());
    }

    @Override
    public void onZoomRequested(String pathId) {
        collapseSlidingPanel();
        ZoomRequestEvent event = new ZoomRequestEvent(pathId);
        bus.post(event);
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
        //ApplicationUtils.showActionsPopupForPath(this, v, this, pathId);
        openContextMenu(v);
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
            slidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
    }

    private boolean isSlidingPanelExpanded() {
        SlidingUpPanelLayout slidingUpPanel = (SlidingUpPanelLayout) findViewById(R.id.main_panel);
        if (slidingUpPanel != null && (slidingUpPanel.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED))
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
            Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + ": backstack count:" + backStackEntryCount);
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
            Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + ": added quick note:" + content + " at " + paoNote.getLocation() + " to segment " + TrailBookState.getActiveSegmentId());
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

    private PathUploadDetailsFragment getPathUploadDetailsFragment(String pathId) {
        Log.d(Constants.TRAILBOOK_TAG, "TrailBookActivit: making path details fragment for path: " + pathId);
        PathUploadDetailsFragment frag = PathUploadDetailsFragment.newInstance(pathId);
        Log.d(Constants.TRAILBOOK_TAG, "TrailBookActivit: finished making path details fragment for path: " + pathId);
        return frag;
    }

    private PathSelectorFragment getPathSelectorFragmentForDownloadedPaths() {
        //return PathsOnDeviceSelectorFragment.newInstance();
        ArrayList<String> downloadedPathIds = mPathManager.getDownloadedPathIds();
        return PathSelectorFragment.newInstance(downloadedPathIds, getString(R.string.title_downloaded_paths));
    }

    private PathSelectorFragment getPathSelectorFragmentForPaths(ArrayList<String> pathIds, String title) {
        return PathSelectorFragment.newInstance(pathIds, title);
    }

    private FollowPathSelectorFragment getFollowPathSelectorFragment() {
        ArrayList<String> downloadedPathIds = mPathManager.getDownloadedPathIds();
        return FollowPathSelectorFragment.newInstance(downloadedPathIds, getString(R.string.title_downloaded_paths));
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
                    Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + ": Chosen file Uri: " + uri.toString());
                    String pathKml = TrailbookFileUtilities.readTextFromUri(this, uri);
                    Path pathContainer = TrailbookPathUtilities.parseXML(pathKml);
                    String pathName = pathContainer.summary.getName();

                    Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + ": parsed path:" + pathName);
                    mPathManager.savePath(pathContainer);
                    mPathManager.addPath(pathContainer);
                }
            } catch (Exception e) {
                Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + ": Exception getting kml file", e);
                showErrorParsingKMLAlert();
            }
        } else if (requestCode == Authenticator.REQUEST_CODE_SIGN_IN ) {
            if (resultCode == RESULT_OK) {
                Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " back from authentication intent.  result is " + resultCode + " resultData = " + resultData);
                Authenticator.getInstance().onReturnFromIntent();
            } else {
                //todo: failed
            }
        } else if (requestCode == Authenticator.REQUEST_CODE_PICK_ACCOUNT) {
            if (resultCode == RESULT_OK && resultData != null && resultData.getStringExtra(AccountManager.KEY_ACCOUNT_NAME) != null) {
                Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " back from authentication pick account intent.  result is " + resultCode + " resultData = " + resultData);
                String accountName = resultData.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                Authenticator.getInstance().onGotAccount(accountName);
            } else {
                DialogInterface.OnClickListener clickListenerOK = new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + ": dismissing can't get account dialog");
                    }
                };
                ApplicationUtils.showAlert(this,clickListenerOK ,"Unable to obtain Google Account", "A Google Services account is required to add trails to the Trailbook cloud or comment on existing trails.", getString(R.string.OK), null);
            }
        }
    }

    @Subscribe
    public void onLocationChangedEvent(LocationChangedEvent event){
        if (!isWaitingForGoodLocation()) //the dialog has already been dismissed.
            return;

        if (TrailBookState.alreadyGotEnoughGoodLocations())
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
            Log.d(Constants.TRAILBOOK_TAG, getLocalClassName() + ": refreshing from cloud");
            mWorkFragment.startGetPathSummariesRemote(null, 0);
        } else {
            toastNoNetwork();
        }
    }

    private boolean isTimeToRereshFromCloud() {
        long msSinceLastRefresh = ApplicationUtils.getCurrentTimeStamp() -TrailBookState.getLastRefreshedFromCloudTimeStamp();
        Log.d(Constants.TRAILBOOK_TAG, "Time since last refresh" + msSinceLastRefresh / 1000 / 60 + " min");
        if (msSinceLastRefresh > Constants.CLOUD_REFRESH_DEFAULT_TIME_DELTA) {
            return true;
        } else {
            return false;
        }
    }

    private void showNoPathsAlert() {
        DialogInterface.OnClickListener clickListenerOK = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int id) {
                Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + ": dismissing no paths dialog");
                //todo: launch searcher here
            }
        };
        Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + ": showing no paths alert");
        ApplicationUtils.showAlert(this, clickListenerOK, getString(R.string.info_no_paths_downloaded_title), getString(R.string.info_no_paths_downloaded_message), getString(R.string.OK), null);
    }

    private void showNoPathsWithinViewAlert() {
        DialogInterface.OnClickListener clickListenerOK = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int id) {
                Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + ": dismissing no paths dialog");
                //todo: launch searcher here
            }
        };
        Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + ": showing no paths alert");
        ApplicationUtils.showAlert(this, clickListenerOK, getString(R.string.info_no_paths_within_view_title), getString(R.string.no_paths_within_view_message), getString(R.string.OK), null);
    }

    private void showExitAlert(String reason) {
        DialogInterface.OnClickListener clickListenerOK = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int id) {
                Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + ": exiting application");
                TrailBookState.getInstance().stopLocationUpdates();
                TrailBookState.getInstance().removeAllNotificaions();
                finish();
            }
        };
        Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + ": showing exit confirm alert");
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
                Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + ": dismissing no paths dialog");
            }
        };
        ApplicationUtils.showAlert(this, clickListenerOK, getString(R.string.no_network_title), getString(R.string.no_network_message), getString(R.string.OK), null);
    }


    private void showErrorParsingKMLAlert() {
        DialogInterface.OnClickListener clickListenerOK = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int id) {
                Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + ": dismissing kml parse error dialog");
            }
        };
        ApplicationUtils.showAlert(this, clickListenerOK, getString(R.string.parse_error), getString(R.string.parse_error_message), getString(R.string.OK), null);
    }

    public void displayEditHelp() {
        DialogInterface.OnClickListener clickListenerOK = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int id) {

            }
        };

        ApplicationUtils.showAlert(this, clickListenerOK, getString(R.string.edit_point_help_title), getString(R.string.edit_point_help_text), getString(R.string.OK), null);

    }

    private void showPathExistsAlert(String pathName) {
        DialogInterface.OnClickListener clickListenerOK = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int id) {
                Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + ": dismissing kml parse error dialog");
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
                Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + ": deleting path " + pathId);
                mWorkFragment.startPathDeleteMongo(pathId);
                mPathManager.removeCloudCache(pathId);
            }
        };
        ApplicationUtils.showAlert(this, affirmCloudDeleteListener, "Delete From Cloud?", "This action is not reversible.", "Delete", "Cancel");
    }

    public void upload(String pathId) {
        if (isNetworkConnected()) {
            Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " Uploading path:" + pathId);
            //TODO: update path details and confirm
            Action uploadAction = new PathUploaderAction(mWorkFragment, mPathManager.getPathSummary(pathId));
            String userId = TrailBookState.getCurrentUser().userId;
            if (userId == null || userId == "-1") {
                LoginUtil.authenticateForAction(this, uploadAction);
            } else {
                Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + ": already has user id " + userId);
                PathSummary summary = mPathManager.getPathSummary(pathId);
                summary.setOwnerID(userId);
                uploadAction.execute();
            }
        } else {
            Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " no network connectivity");
            showNoNetworkStatusDialog();
        }
    }

    public void waitForLocation() {
        TrailBookState.resetConsecutiveGoodLocations();
        mWaitingForLocationDialog = new ProgressDialog(this);
        mWaitingForLocationDialog.setMessage(getString(R.string.waiting_for_location));
        mWaitingForLocationDialog.setCancelable(false);
        mWaitingForLocationDialog.show();
    }

    public void cancelWaitForLocationDialog() {
        try {
            if (mWaitingForLocationDialog != null) {
                mWaitingForLocationDialog.dismiss();
                mWaitingForLocationDialog = null;
            }
        } catch (Exception e) {
            Log.d(Constants.TRAILBOOK_TAG, "Exception closing dialog:", e);
        }
    }

    public boolean isWaitingForGoodLocation() {
        if (mWaitingForLocationDialog == null)
            return false;
        else
            return true;
    }

    @Override
    public void onNewPathCommentClick(TrailBookComment comment) {
        mPathManager.addPathComment(comment);
        mPathManager.saveComment(comment);
        BusProvider.getInstance().post(new PathCommentAddedEvent(comment));
        if (mPathManager.isPathInCloudCache(comment.getPathId())) {
            if (isNetworkConnected()) {
                Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " Uploading comment:" + comment);
                //TODO: show progress dialog
                Action uploadAction = new CommentUploaderAction(mWorkFragment, comment);
                String userId = TrailBookState.getCurrentUser().userId;

                if (userId == null || userId == "-1") {
                    LoginUtil.authenticateForAction(this, uploadAction);
                    //we don't actually need to connect.
                    //Authenticator.getInstance().connect();
                } else {
                    Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + ": already has user id " + userId);
                    uploadAction.execute();
                }
            } else {
                //todo: create service to upload when back on line
                Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " no network connectivity");
                showNoNetworkStatusDialog();
            }
        }
    }

    @Override
    public void onNewAttachedCommentClick(TrailBookComment comment) {
        if (isNetworkConnected()) {
            Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " Uploading comment:" + comment);
            //TODO: show progress dialog
            PointAttachedObject paoComment = attachNoteToCurrentLocation(comment);
            Action uploadAction = new AttachedCommentUploaderAction(mWorkFragment, paoComment);
            String userId = TrailBookState.getCurrentUser().userId;
            if (userId == null || userId == "-1") {
                LoginUtil.authenticateForAction(this, uploadAction);
                //we don't actually need to connect.
                //Authenticator.getInstance().connect();
            } else {
                Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + ": already has user id " + userId);
                uploadAction.execute();
            }
        } else {
            Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " no network connectivity");
            showNoNetworkStatusDialog();
        }
    }

    @Override
    public void onUploadPathClick(String pathId) {
        getFragmentManager().popBackStackImmediate();
        invalidateOptionsMenu();

        upload(pathId);
        collapseSlidingPanel();
    }

    private class WebServiceStateReceiver extends BroadcastReceiver
    {
        // Prevents instantiation
        private WebServiceStateReceiver() {
        }
        // Called when the BroadcastReceiver gets an Intent it's registered to receive
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " Recieved intent for " + action);
            if (UploadPathService.BROADCAST_ACTION.equalsIgnoreCase(action)) {
                int progress = intent.getIntExtra(UploadPathService.EXTENDED_DATA_STATUS_KEY, 0);
                Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + " upload progress is  " + progress);
                if (progress == UploadPathService.STATUS_FAILURE) {
                    Toast.makeText(TrailBookActivity.this, getString(R.string.upload_failed), Toast.LENGTH_LONG).show();
                    //todo: send failure notification
                    mMapFragment.hideMapMessage();
                    setProgressBarIndeterminateVisibility(false);
                    String pathId =  intent.getStringExtra(UploadPathService.PATH_ID_KEY);
                    sendUploadFailedNotification(pathId);
                } else if (progress == UploadPathService.STATUS_COMPLETE) {
                    //ApplicationUtils.toastGreen(TrailBookActivity.this, R.string.upload_completed);
                    Toast.makeText(TrailBookActivity.this, getString(R.string.upload_completed), Toast.LENGTH_LONG).show();
                    if (mMapFragment != null) {
                        setProgressBarIndeterminateVisibility(false);
                        mMapFragment.hideMapMessage();
                    }
                } else {
                    if (mMapFragment != null) {
                        mMapFragment.displayMessage(getString(R.string.path_uploading));
                        setProgressBarIndeterminateVisibility(true);
                    }
                }
            } else if (DownloadPathService.BROADCAST_ACTION.equalsIgnoreCase(action)) {
                int progress = intent.getIntExtra(DownloadPathService.EXTENDED_DATA_STATUS_KEY, 0);
                String pathId =  intent.getStringExtra(DownloadPathService.PATH_ID_KEY);
                if (progress == DownloadPathService.STATUS_COMPLETE) {
                    //ApplicationUtils.toastGreen(TrailBookActivity.this, R.string.download_completed);
                    Toast.makeText(TrailBookActivity.this, getString(R.string.download_completed), Toast.LENGTH_LONG).show();
                    setProgressBarIndeterminateVisibility(false);
                    mMapFragment.hideMapMessage();
                    Path path = mPathManager.getPath(pathId);
                    bus.post(new PathReceivedEvent(path));
                }
            }
        }
    }


    private void sendUploadFailedNotification(String pathId) {
        PathSummary summary = PathManager.getInstance().getPathSummary(pathId);

        Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + ": sending upload failed notification for path " + summary.getName());
        NotificationCompat.Builder builder = NotificationUtils.createUploadFailedNotifyBuilder(this, pathId);

        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(
                NotificationUtils.getNotificationId(pathId),
                builder.build());
        Log.d(Constants.TRAILBOOK_TAG, CLASS_NAME + ": sent upload failed notification for path " + summary.getName());
    }


}
