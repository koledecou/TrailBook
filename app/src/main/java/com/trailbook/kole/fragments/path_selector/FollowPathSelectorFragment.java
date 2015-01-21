package com.trailbook.kole.fragments.path_selector;

import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ExpandableListView;

import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.helpers.ApplicationUtils;

import java.util.ArrayList;

/**
 * Created by kole on 9/7/2014.
 */
public class FollowPathSelectorFragment extends PathSelectorFragment {

    public static FollowPathSelectorFragment newInstance(ArrayList<String> pathIds, String title) {
        FollowPathSelectorFragment fragment = new FollowPathSelectorFragment();

        Bundle args = new Bundle();
        args.putStringArrayList(PATH_ID_LIST_ARG, pathIds);
        args.putString(TITLE_ARG, title);
        fragment.setArguments(args);

        return fragment;
    }

    public FollowPathSelectorFragment() {
    }

    public void addMenuItems(Menu m) {
        //no context menu for this list, when clicked just start following the path.
    }
/*
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        PathListContent.PathSummaryItem summaryItem = PathListContent.ITEMS.get(position);
        String pathId = summaryItem.id;
        mListener.executeAction(ApplicationUtils.MENU_CONTEXT_FOLLOW_ID, pathId);
    }*/

    @Override
    public boolean onChildClick(ExpandableListView parent, View view, int groupPosition, int childPosition, long id) {
        PathSummary summary = (PathSummary)listAdapter.getChild(groupPosition, childPosition);
        String pathId = summary.getId();
        mListener.executeAction(ApplicationUtils.MENU_CONTEXT_FOLLOW_ID, pathId);
        return true;
    }
}
