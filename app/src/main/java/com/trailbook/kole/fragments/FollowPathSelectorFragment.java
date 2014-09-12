package com.trailbook.kole.fragments;

import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;

import com.trailbook.kole.fragments.list_content.PathListContent;

/**
 * Created by kole on 9/7/2014.
 */
public class FollowPathSelectorFragment extends PathsOnDeviceSelectorFragment {
    public static FollowPathSelectorFragment newInstance() {
        FollowPathSelectorFragment fragment = new FollowPathSelectorFragment();
        return fragment;
    }

    public FollowPathSelectorFragment() {
    }

    public void addMenuItems(Menu m) {
        //no context menu for this list, when clicked just start following the path.
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        PathListContent.PathSummaryItem summaryItem = PathListContent.ITEMS.get(position);
        String pathId = summaryItem.id;
        sendActionToListener(FOLLOW, pathId);
    }
}
