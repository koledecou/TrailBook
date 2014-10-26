package com.trailbook.kole.fragments.path_selector;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.trailbook.kole.activities.R;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.fragments.list_content.PathListContent;
import com.trailbook.kole.helpers.ApplicationUtils;
import com.trailbook.kole.state_objects.PathManager;

import java.util.ArrayList;
import java.util.Comparator;

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
    public static final String UPLOAD = "UPLOAD";
    public static final String DELETE = "DELETE";
    public static final String FOLLOW = "FOLLOW";
    public static final String DISMISS = "DISMISS";
    public static final String TO_START = "TO_START";
    public static final String EDIT = "EDIT";

    public static final String  PATH_ID_LIST_ARG="PATH_IDS";

    OnPathSelectorFragmentInteractionListener mListener;

    private AbsListView mListView;
    public ArrayAdapter mAdapter;
    public ArrayList<String> mPathIds;

    final private Comparator<PathListContent.PathSummaryItem> stringComparator = new Comparator<PathListContent.PathSummaryItem>() {
        public int compare(PathListContent.PathSummaryItem s1, PathListContent.PathSummaryItem s2) {
            return s1.pathName.toLowerCase().compareTo(s2.pathName.toLowerCase());
        }
    };

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
        mAdapter.sort(stringComparator);
        mListView.setAdapter(mAdapter);

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
            PathListContent.PathSummaryItem summaryItem = (PathListContent.PathSummaryItem) lv.getItemAtPosition(info.position);

            menu.setHeaderTitle(summaryItem.pathName);
            ApplicationUtils.addPathActionMenuItems(menu, summaryItem.id);
            //ApplicationUtils.addDownloadedPathMenuItems(menu, summaryItem.id);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        //todo: menu items depend on path status

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        PathListContent.PathSummaryItem summaryItem = PathListContent.ITEMS.get(info.position);
        String pathId = summaryItem.id;
        if (item.getItemId() == ApplicationUtils.MENU_CONTEXT_DELETE_ID) {
            mAdapter.remove(summaryItem);
        }
        mListener.processMenuAction(pathId, item);
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        parent.showContextMenuForChild(view);
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

    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnPathSelectorFragmentInteractionListener {
        public void processMenuAction(String pathId, MenuItem actionItem);
        public void executeAction(int action, String pathId);
    }

}
