package com.trailbook.kole.fragments;



import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;
import com.trailbook.kole.activities.R;
import com.trailbook.kole.data.Path;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.fragments.list_content.PathListContent;
import com.trailbook.kole.tools.PathManager;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 *
 */
public class PathsToUploadSelectorFragment extends PathSelectorFragment {

    // TODO: Rename and change types of parameters
    public static PathsToUploadSelectorFragment newInstance() {
        PathsToUploadSelectorFragment fragment = new PathsToUploadSelectorFragment();
        return fragment;
    }

    public PathsToUploadSelectorFragment() {
        mAction = UPLOAD;
    }

    @Override
    public ListAdapter getArrayAdapter() {
        addItems();
        
        mAdapter = new ArrayAdapter<PathListContent.PathSummaryItem>(getActivity(),
                android.R.layout.simple_list_item_1, android.R.id.text1, PathListContent.ITEMS);
        return mAdapter;
    }

    private void addItems() {
        PathListContent.removeAllItems();
        PathManager pathManager = PathManager.getInstance();
        ArrayList<PathSummary> pathSummaries=pathManager.getDownloadedPathSummaries();
        for (PathSummary summary: pathSummaries) {
            PathListContent.addItem(new PathListContent.PathSummaryItem(summary.getId(), summary.getName()));
        }
    }


}
