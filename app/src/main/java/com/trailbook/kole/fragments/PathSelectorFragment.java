package com.trailbook.kole.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.trailbook.kole.activities.R;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.fragments.list_content.PathListContent;
import com.trailbook.kole.state_objects.PathManager;

import java.util.ArrayList;

/**
 * A fragment representing a list of Items.
 * <p />
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p />
 * Activities containing this fragment MUST implement the OnFragmentInteractionListener
 * interface.
 */
public class PathSelectorFragment extends Fragment implements AbsListView.OnItemClickListener {

    static final int MENU_CONTEXT_DELETE_ID = 1;
    static final int MENU_CONTEXT_UPLOAD_ID = 2;
    static final int MENU_CONTEXT_FOLLOW_ID = 3;
    static final int MENU_CONTEXT_TO_START_ID = 4;

    static final int DELETE_TEXT = R.string.delete;
    static final int UPLOAD_TEXT = R.string.upload;
    static final int FOLLOW_TEXT = R.string.follow;
    static final int TO_START_TEXT = R.string.to_start;

    public static final String UPLOAD = "UPLOAD";
    public static final String DELETE = "DELETE";
    public static final String FOLLOW = "FOLLOW";
    public static final String DISMISS = "DISMISS";
    public static final String TO_START = "TO_START";

    public static final String  PATH_ID_LIST_ARG="PATH_IDS";

    private OnPathSelectorFragmentInteractionListener mListener;

    private AbsListView mListView;
    public ArrayAdapter mAdapter;
    public ArrayList<String> mPathIds;

    public static PathSelectorFragment newInstance(ArrayList<String> pathIds) {
        PathSelectorFragment fragment = new PathSelectorFragment();

        Bundle args = new Bundle();
        args.putStringArrayList(PATH_ID_LIST_ARG, pathIds);
        fragment.setArguments(args);

        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public PathSelectorFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPathIds = getPathIdsFromArg();
        mAdapter = getArrayAdapter();
    }

    private ArrayList<String> getPathIdsFromArg () {
        Bundle args = getArguments();
        return args.getStringArrayList(PATH_ID_LIST_ARG);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pathselectorfragment, container, false);

        // Set the adapter
        mListView = (AbsListView) view.findViewById(R.id.psf_list);
        ((AdapterView<ListAdapter>) mListView).setAdapter(mAdapter);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);
        registerForContextMenu(mListView);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnPathSelectorFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.psf_list) {
            ListView lv = (ListView) v;
            AdapterView.AdapterContextMenuInfo info  = (AdapterView.AdapterContextMenuInfo) menuInfo;
            PathListContent.PathSummaryItem obj = (PathListContent.PathSummaryItem) lv.getItemAtPosition(info.position);

            menu.setHeaderTitle(obj.pathName);
            addMenuItems(menu);
        }
    }


    @Override
    public boolean onContextItemSelected(MenuItem item) {
        //todo: menu items depend on path status

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        PathListContent.PathSummaryItem summaryItem = PathListContent.ITEMS.get(info.position);
        String pathId = summaryItem.id;
        switch (item.getItemId()) {
            case MENU_CONTEXT_DELETE_ID:
                Log.d(Constants.TRAILBOOK_TAG, "deleting item pos=" + info.position + " pathid=" + pathId);
                sendActionToListener(DELETE, pathId);
                mAdapter.remove(summaryItem);
                return true;
            case MENU_CONTEXT_UPLOAD_ID:
                Log.d(Constants.TRAILBOOK_TAG, "uploading item pos=" + info.position + " pathid=" + pathId);
                sendActionToListener(UPLOAD, pathId);
                return true;
            case MENU_CONTEXT_FOLLOW_ID:
                Log.d(Constants.TRAILBOOK_TAG, "following item pos=" + info.position + " pathid=" + pathId);
                sendActionToListener(FOLLOW, pathId);
                return true;
            case MENU_CONTEXT_TO_START_ID:
                Log.d(Constants.TRAILBOOK_TAG, "going to item pos=" + info.position + " pathid=" + pathId);
                sendActionToListener(TO_START, pathId);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        parent.showContextMenuForChild(view);
    }

    public void addMenuItems(Menu m) {
        m.add(Menu.NONE, MENU_CONTEXT_DELETE_ID, Menu.NONE, DELETE_TEXT);
        m.add(Menu.NONE, MENU_CONTEXT_UPLOAD_ID, Menu.NONE, UPLOAD_TEXT);
        m.add(Menu.NONE, MENU_CONTEXT_FOLLOW_ID, Menu.NONE, FOLLOW_TEXT);
        m.add(Menu.NONE, MENU_CONTEXT_TO_START_ID, Menu.NONE, TO_START_TEXT);
    }

    public ArrayAdapter getArrayAdapter() {
        addPathsToListContent();

        mAdapter = new ArrayAdapter<PathListContent.PathSummaryItem>(getActivity(),
                android.R.layout.simple_list_item_1, android.R.id.text1, PathListContent.ITEMS);
        return mAdapter;
    }

    private void addPathsToListContent() {
        PathListContent.removeAllItems();
        PathManager pathManager = PathManager.getInstance();
        for (String pathId:mPathIds) {
            PathSummary summary = pathManager.getPathSummary(pathId);
            PathListContent.addItem(new PathListContent.PathSummaryItem(summary.getId(), summary.getName()));
        }
/*        ArrayList<PathSummary> pathSummaries=pathManager.getPathSummaries();
        if (pathSummaries == null  || pathSummaries.size()==0) {
            showNoPathsAlert();
        }
        for (PathSummary summary: pathSummaries) {
            PathListContent.addItem(new PathListContent.PathSummaryItem(summary.getId(), summary.getName()));
        }*/
    }

    public void sendActionToListener(String action, String pathId) {
        if (null != mListener) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.
            mListener.onPathSelectorFragmentResult(action, pathId);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnPathSelectorFragmentInteractionListener {
        public void onPathSelectorFragmentResult(String action, String pathId);
    }

}
