package com.trailbook.kole.fragments.path_selector;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;

import com.trailbook.kole.activities.R;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.fragments.list_content.PathSelectorExpandableListAdapter;
import com.trailbook.kole.fragments.list_content.PathListContent;
import com.trailbook.kole.helpers.ApplicationUtils;
import com.trailbook.kole.state_objects.PathManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * A fragment representing a list of Items.
 * <p />
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p />
 * Activities containing this fragment MUST implement the OnFragmentInteractionListener
 * interface.
 */
public class PathSelectorFragment extends Fragment implements ExpandableListView.OnChildClickListener {
    public static final String UPLOAD = "UPLOAD";
    public static final String DELETE = "DELETE";
    public static final String FOLLOW = "FOLLOW";
    public static final String DISMISS = "DISMISS";
    public static final String TO_START = "TO_START";
    public static final String EDIT = "EDIT";

    public static final String  PATH_ID_LIST_ARG="PATH_IDS";
    public static final String  TITLE_ARG="TITLE";

    OnPathSelectorFragmentInteractionListener mListener;

    PathSelectorExpandableListAdapter listAdapter;
    ExpandableListView expListView;
    List<String> listDataHeader;
    HashMap<String, List<PathSummary>> listDataChild;

    public ArrayAdapter mAdapter;
    public ArrayList<String> mPathIds;
    String mTitle;

    final private Comparator<PathListContent.PathSummaryItem> stringComparator = new Comparator<PathListContent.PathSummaryItem>() {
        public int compare(PathListContent.PathSummaryItem s1, PathListContent.PathSummaryItem s2) {
            return s1.pathName.toLowerCase().compareTo(s2.pathName.toLowerCase());
        }
    };

    public static PathSelectorFragment newInstance(ArrayList<String> pathIds, String title) {
        PathSelectorFragment fragment = new PathSelectorFragment();

        Bundle args = new Bundle();
        args.putStringArrayList(PATH_ID_LIST_ARG, pathIds);
        args.putString(TITLE_ARG, title);
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
        mTitle = getArguments().getString(TITLE_ARG);
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
        View view = inflater.inflate(R.layout.fragment_pathselector_list, container, false);
        expListView = (ExpandableListView) view.findViewById(R.id.lvExp);
        listAdapter = new PathSelectorExpandableListAdapter(getActivity());
        prepareListData();

        expListView.setAdapter(listAdapter);
        expListView.setOnChildClickListener(this);
        registerForContextMenu(expListView);
        expListView.expandGroup(0);
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
        Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + " creating context menu");
        if (v.getId() == R.id.lvExp) {
            ExpandableListView.ExpandableListContextMenuInfo info  = (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;
            Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + " packed position=" + info.packedPosition);
            int groupPosition = ExpandableListView.getPackedPositionGroup(info.packedPosition);
            int childPosition = ExpandableListView.getPackedPositionChild(info.packedPosition);
            Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + " group position=" + groupPosition);
            Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + " child position=" + childPosition);
            PathSummary summary = (PathSummary) listAdapter.getChild(groupPosition, childPosition);

            menu.setHeaderTitle(summary.getName());
            ApplicationUtils.addPathActionMenuItems(menu, summary.getId());
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ExpandableListView.ExpandableListContextMenuInfo info  = (ExpandableListView.ExpandableListContextMenuInfo) item.getMenuInfo();
        int groupPosition = ExpandableListView.getPackedPositionGroup(info.packedPosition);
        int childPosition = ExpandableListView.getPackedPositionChild(info.packedPosition);
        PathSummary summary = (PathSummary) listAdapter.getChild(groupPosition, childPosition);
        String pathId = summary.getId();

        if (item.getItemId() == ApplicationUtils.MENU_CONTEXT_DELETE_ID) {
            mPathIds.remove(pathId);
            prepareListData();
        }
        mListener.processMenuAction(pathId, item);
        return true;
    }

/*    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        parent.showContextMenuForChild(view);
    }*/

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
        PathListContent.sort();
    }

    private void prepareListData() {
        listDataHeader = new ArrayList<String>();
        listDataChild = new HashMap<String, List<PathSummary>>();
        addPaths();
        listAdapter.setLists(listDataHeader, listDataChild);
        listAdapter.notifyDataSetChanged();
        listAdapter.notifyDataSetInvalidated();
    }

    private void addPaths() {
        for (String pathId:mPathIds) {
            addPathToMainBucket(pathId);

/* todo: implement tags or regions
           ArrayList<String> tags = PathManager.getInstance().getTags(pathId);
            Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + " adding tags to " + PathManager.getInstance().getPathSummary(pathId).getName() + ":" + tags);
            for (String tag:tags) {
                addPathToTag(pathId, tag);
            }*/
        }

        sortPaths(mTitle);
    }

    private void sortPaths(String groupKey) {
        List<PathSummary> list = listDataChild.get(groupKey);
        Collections.sort(list);
    }

    private void addPathToMainBucket(String pathId) {
        PathSummary p = PathManager.getInstance().getPathSummary(pathId);
        addTagHeaderIfNeeded(mTitle);
        List<PathSummary> memberList = addTagMember(mTitle, p);
        listDataChild.put(mTitle, memberList);
    }

    private void addPathToTag(String pathId, String tag) {
        PathSummary p = PathManager.getInstance().getPathSummary(pathId);
        addTagHeaderIfNeeded(tag);
        List<PathSummary> memberList = addTagMember(tag, p);
        listDataChild.put(tag, memberList);
    }

    private void addTagHeaderIfNeeded(String tag) {
        if (!listDataHeader.contains(tag)) {
            listDataHeader.add(tag);
        }
    }

    private List<PathSummary> addTagMember(String tag, PathSummary summary) {
        Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + " adding tag " + tag + " to " + summary.getName());
        List<PathSummary> tagMembers = listDataChild.get(tag);
        Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + " before members " + tagMembers);
        if (tagMembers == null) {
            tagMembers = new ArrayList<PathSummary>();
        }
        tagMembers.add(summary);
        Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + " after members " + tagMembers);
        return tagMembers;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View view, int groupPosition, int childPosition, long id) {
        PathSummary summary = (PathSummary)listAdapter.getChild(groupPosition, childPosition);
        Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + " child clicked: " + summary.getName());
        parent.showContextMenuForChild(view);
        return true;
    }

    public interface OnPathSelectorFragmentInteractionListener {
        public void processMenuAction(String pathId, MenuItem actionItem);
        public void executeAction(int action, String pathId);
    }

}
