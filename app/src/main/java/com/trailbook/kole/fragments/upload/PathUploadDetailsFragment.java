package com.trailbook.kole.fragments.upload;


import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.trailbook.kole.activities.R;
import com.trailbook.kole.activities.TrailBookActivity;
import com.trailbook.kole.data.Attachment;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.KeyWordGroup;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.fragments.list_content.KeyWordGroupsExpandableListAdapter;
import com.trailbook.kole.helpers.ApplicationUtils;
import com.trailbook.kole.helpers.NoteFactory;
import com.trailbook.kole.state_objects.PathManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PathUploadDetailsFragment extends Fragment implements View.OnClickListener, ExpandableListView.OnChildClickListener, AddKeywordsFragment.KeywordsAddedListener {
    private static final String PATH_NAME_KEY = "PATH_NAME";
    private static final String PATH_ID_KEY = "PATH_ID";
    private static final String KEY_WORDS_KEY = "KEY_WORDS";

    private static final String CLASSNAME = "PathUploadDetailsFragment";
    public static final String CLIMBS = "Climbs";
    public static final String REGIONS = "Regions";
    public static final String CRAGS = "Crags";
    private static final int MENU_CONTEXT_DELETE_ID = 1;
    private static final String CLIMBS_ADDED_KEY = "CLIMBS_ADDED";
    boolean mClimbsFromPathHaveBeenAdded = false;

    private ExpandableListView mListView;

    public interface UploadPathDialogListener {
        public void onUploadPathClick(String pathId);
    }

    private EditText mEditTextPathName;

    private static UploadPathDialogListener mListener;
    private String mPathId;
    private KeyWordGroup mKeyWords;
    private KeyWordGroupsExpandableListAdapter mListAdapter;

    public PathUploadDetailsFragment() {
        // Empty constructor required for DialogFragment
    }

    public static PathUploadDetailsFragment newInstance(String pathId) {
        PathUploadDetailsFragment frag = new PathUploadDetailsFragment();
        Bundle args = new Bundle();
        args.putString(PATH_ID_KEY, pathId);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (UploadPathDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement UploadPathDialogListener");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(Constants.TRAILBOOK_TAG, CLASSNAME + ": Saving state");
        if (mEditTextPathName != null)
            outState.putString(PATH_NAME_KEY, mEditTextPathName.getText().toString());

        outState.putString(PATH_ID_KEY, mPathId);
        outState.putBoolean(CLIMBS_ADDED_KEY, mClimbsFromPathHaveBeenAdded);
    }

    public void restoreState(Bundle savedState) {
        Log.d(Constants.TRAILBOOK_TAG, CLASSNAME + "Restoring state. ");
        mPathId = savedState.getString(PATH_ID_KEY);
        mClimbsFromPathHaveBeenAdded = savedState.getBoolean(CLIMBS_ADDED_KEY);
    }

    public void restoreViewValues(Bundle savedState) {
        mEditTextPathName.setText(savedState.getString(PATH_NAME_KEY));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mPathId = getArguments().getString(PATH_ID_KEY);
        if (mPathId == null) {
            getActivity().getFragmentManager().beginTransaction().remove(this).commit();
        }

        View view  = inflater.inflate(R.layout.upload_path_dialog, container, false);

        TextView pathNameLabel = (TextView) view.findViewById(R.id.name_label);
        pathNameLabel.setTypeface(null, Typeface.BOLD);

        mEditTextPathName = (EditText) view.findViewById(R.id.upd_et_path_name);
        mEditTextPathName.requestFocus();

        populatePathName(mPathId);
        mListView = (ExpandableListView) view.findViewById(R.id.lvExp);
        registerForContextMenu(mListView);
        refreshKeywords(mPathId);
        populateKeyWordListView();

        setButtons(view);

        if (savedInstanceState != null)
            restoreViewValues(savedInstanceState);

        return view;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        Log.d(Constants.TRAILBOOK_TAG, CLASSNAME + " creating context menu");
        if (v.getId() == R.id.lvExp) {
            ExpandableListView.ExpandableListContextMenuInfo info  = (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;
            Log.d(Constants.TRAILBOOK_TAG,CLASSNAME + " packed position=" + info.packedPosition);

            menu.setHeaderTitle("");
            menu.add(Menu.NONE, MENU_CONTEXT_DELETE_ID, Menu.NONE, getString(R.string.delete_key_word));
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ExpandableListView.ExpandableListContextMenuInfo info  = (ExpandableListView.ExpandableListContextMenuInfo) item.getMenuInfo();
        int groupPosition = ExpandableListView.getPackedPositionGroup(info.packedPosition);
        int childPosition = ExpandableListView.getPackedPositionChild(info.packedPosition);
        Log.d(Constants.TRAILBOOK_TAG, CLASSNAME + " group position=" + groupPosition);
        Log.d(Constants.TRAILBOOK_TAG, CLASSNAME + " child position=" + childPosition);

        if (item.getItemId() == ApplicationUtils.MENU_CONTEXT_DELETE_ID) {
            String keyWord = (String) mListAdapter.getChild(groupPosition, childPosition);
            Log.d(Constants.TRAILBOOK_TAG, CLASSNAME + " deleting " + keyWord);
            String groupName = (String) mListAdapter.getGroup(groupPosition);
            removeKeyword(keyWord, groupName);
            savePathSummaryKeywordGroup(mPathId, mKeyWords);
            refreshKeywords(mPathId);
            populateKeyWordListView();
        }

        return true;
    }

    private void removeKeyword(String keyWord, String groupName) {
        if (groupName.equals(CLIMBS)) {
            mKeyWords.removeClimb(keyWord);
        } else if (groupName.equals(CRAGS)) {
            mKeyWords.removeCrag(keyWord);
        }else if (groupName.equals(REGIONS)) {
            mKeyWords.removeRegion(keyWord);
        }
    }

    private void populatePathName(String pathId) {
        PathSummary summary = PathManager.getInstance().getPathSummary(pathId);
        if (summary != null) {
            String name = summary.getName();
            if (name != null)
                mEditTextPathName.setText(name);
        }
    }

    private void refreshKeywords(String pathId) {
        PathSummary summary = PathManager.getInstance().getPathSummary(pathId);
        if (summary != null)
            mKeyWords =summary.getKeyWordGroup();
        if (mKeyWords == null) {
            mKeyWords = new KeyWordGroup();
        }

        if (!mClimbsFromPathHaveBeenAdded)
            populateClimbsFromPath(pathId);
    }

    private void populateKeyWordListView() {

        mListAdapter = getListDataAdapter();

        mListView.setAdapter(mListAdapter);
        mListView.setOnChildClickListener(this);
        registerForContextMenu(mListView);
        mListView.expandGroup(0);
        mListView.expandGroup(1);
        mListView.expandGroup(2);
    }

    private void populateClimbsFromPath(String pathId) {
        Log.d(Constants.TRAILBOOK_TAG, CLASSNAME + ": populating climbs for path " + pathId);
        ArrayList<PointAttachedObject> pointAttachedObjects = PathManager.getInstance().getPointObjectsForPath(pathId);
        for (PointAttachedObject pao:pointAttachedObjects) {
            if (pao != null) {
                Attachment a = pao.getAttachment();
                Log.d(Constants.TRAILBOOK_TAG, CLASSNAME + ": type="+a.getType());
                if (a.getType().equals(NoteFactory.CLIMB)) {
                    Log.d(Constants.TRAILBOOK_TAG, CLASSNAME + ": adding climb "+a.getShortContent());
                    mKeyWords.addClimb(a.getShortContent());
                }
            }
        }
        savePathSummaryKeywordGroup(pathId, mKeyWords);
        mClimbsFromPathHaveBeenAdded = true;
    }

    private KeyWordGroupsExpandableListAdapter getListDataAdapter() {
        KeyWordGroupsExpandableListAdapter adapter =  new KeyWordGroupsExpandableListAdapter(getActivity());
        ArrayList<String> listDataHeader = new ArrayList<String>();
        HashMap<String, List<String>> listDataChild = new HashMap<String, List<String>>();
        listDataHeader.add(CLIMBS);
        listDataHeader.add(CRAGS);
        listDataHeader.add(REGIONS);

        KeyWordGroup group = PathManager.getInstance().getPathSummary(mPathId).getKeyWordGroup();
        if (group == null)
            group = new KeyWordGroup();

        addKeyWordToGroup(listDataChild, CLIMBS, "Add");
        addKeyWords(listDataChild, group.climbs, CLIMBS);

        addKeyWordToGroup(listDataChild, REGIONS, "Add");
        addKeyWords(listDataChild, group.regions, REGIONS);
        addKeyWordToGroup(listDataChild, CRAGS, "Add");
        addKeyWords(listDataChild, group.crags, CRAGS);

        adapter.setLists(listDataHeader, listDataChild);
        adapter.notifyDataSetChanged();
        adapter.notifyDataSetInvalidated();

        return adapter;
    }

    private void addKeyWords(HashMap<String, List<String>> listDataChild, ArrayList<String> words, String type) {
        if (words!=null) {
            for (String keyWord : words) {
                addKeyWordToGroup(listDataChild, type, keyWord);
            }
        }
    }

    private void addKeyWordToGroup(HashMap<String, List<String>> listDataChild, String groupName, String keyWord) {
        Log.d(Constants.TRAILBOOK_TAG, CLASSNAME +  " adding key word " + keyWord + " to " + groupName );
        List<String> keyWords = listDataChild.get(groupName);
        if (keyWords == null) {
            keyWords = new ArrayList<String>();
            listDataChild.put(groupName, keyWords);
        }
        keyWords.add(keyWord);

        Log.d(Constants.TRAILBOOK_TAG, CLASSNAME + " added key word " + keyWord + " to " + groupName );
    }


    public void addClimb(String climb) {
        mKeyWords.addClimb(climb);
    }

    public void addCrag(String crag) {
        mKeyWords.addCrag(crag);
    }

    public void addRegion(String region) {
        mKeyWords.addRegion(region);
    }

    private void setButtons(View v) {
        Button cancel = (Button)v.findViewById(R.id.upd_b_cancel);
        Button newButton = (Button) v.findViewById(R.id.upd_b_ok);

        cancel.setOnClickListener(this);
        newButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.upd_b_ok) {
            String pathName = mEditTextPathName.getText().toString();
            savePathName(pathName);
            mListener.onUploadPathClick(mPathId);
        } else if (v.getId() == R.id.upd_b_cancel) {
            getFragmentManager().popBackStackImmediate();
        }
    }

    private void savePathName(String pathName) {
        PathManager pathManager = PathManager.getInstance();
        PathSummary summary = pathManager.getPathSummary(mPathId);
        summary.setName(pathName);
        pathManager.savePath(mPathId);
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View view, int groupPosition, int childPosition, long id) {
        String keyWord = (String) mListAdapter.getChild(groupPosition, childPosition);
        Log.d(Constants.TRAILBOOK_TAG, CLASSNAME + " word clicked: " + keyWord + " position " + childPosition);
        //parent.showContextMenuForChild(view);
        if (childPosition == 0) {
            String groupName = (String) mListAdapter.getGroup(groupPosition);
            if (groupName.equals(CLIMBS)) {
                showAddKeywordFragment(AddKeywordsFragment.TYPE_CLIMB);
            } else if (groupName.equals(CRAGS)) {
                showAddKeywordFragment(AddKeywordsFragment.TYPE_CRAG);
            }else if (groupName.equals(REGIONS)) {
                showAddKeywordFragment(AddKeywordsFragment.TYPE_REGION);
            }
        } else {
            parent.showContextMenuForChild(view);
        }
        return true;
    }

    private void showAddKeywordFragment(int type) {
        AddKeywordsFragment newFrag = AddKeywordsFragment.newInstance(type, mPathId);
        newFrag.setListener(this);
        FragmentManager fm = getFragmentManager();
        fm.beginTransaction()
                .replace(R.id.map_container, newFrag, "ADD_KEY_WORDS")
                .addToBackStack(TrailBookActivity.MAPS_ACTIVITY_TAG)
                .commit();
    }

    @Override
    public void keywordsAdded(String pathId, ArrayList<String> keyWords, int type) {
        mPathId = pathId;
        refreshKeywords(pathId);

        if (keyWords != null) {
            for (String keyWord : keyWords) {
                if (type == AddKeywordsFragment.TYPE_CLIMB) {
                    addClimb(keyWord);
                } else if (type == AddKeywordsFragment.TYPE_CRAG) {
                    addCrag(keyWord);
                } else if (type == AddKeywordsFragment.TYPE_REGION) {
                    addRegion(keyWord);
                }
            }
            savePathSummaryKeywordGroup(mPathId, mKeyWords);
            if (mListView != null)
                populateKeyWordListView();
        }
    }

    private void savePathSummaryKeywordGroup(String pathId, KeyWordGroup keyWordGroup) {
        PathManager pathManager = PathManager.getInstance();
        PathSummary summary = pathManager.getPathSummary(pathId);
        if (summary != null) {
            summary.setKeyWordGroup(keyWordGroup);
        }
        pathManager.savePath(pathId);
        PathManager.hashKeywords(summary);
    }
}