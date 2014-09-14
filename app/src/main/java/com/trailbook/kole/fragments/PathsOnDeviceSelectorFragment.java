package com.trailbook.kole.fragments;



import android.app.Fragment;
import android.content.DialogInterface;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.trailbook.kole.activities.R;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.fragments.list_content.PathListContent;
import com.trailbook.kole.helpers.ApplicationUtils;
import com.trailbook.kole.state_objects.PathManager;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 *
 */
public class PathsOnDeviceSelectorFragment extends PathSelectorFragment {



    // TODO: Rename and change types of parameters
    public static PathsOnDeviceSelectorFragment newInstance() {
        PathsOnDeviceSelectorFragment fragment = new PathsOnDeviceSelectorFragment();
        return fragment;
    }

    public PathsOnDeviceSelectorFragment() {

    }

    @Override
    public ArrayAdapter getArrayAdapter() {
        addPathsFromDevice();

        mAdapter = new ArrayAdapter<PathListContent.PathSummaryItem>(getActivity(),
                android.R.layout.simple_list_item_1, android.R.id.text1, PathListContent.ITEMS);
        return mAdapter;
    }

    private void addPathsFromDevice() {
        PathListContent.removeAllItems();
        PathManager pathManager = PathManager.getInstance();
        ArrayList<PathSummary> pathSummaries=pathManager.getDownloadedPathSummaries();
        if (pathSummaries == null  || pathSummaries.size()==0) {
            showNoPathsAlert();
        }
        for (PathSummary summary: pathSummaries) {
            PathListContent.addItem(new PathListContent.PathSummaryItem(summary.getId(), summary.getName()));
        }
    }

    private void showNoPathsAlert() {
        DialogInterface.OnClickListener clickListenerOK = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int id) {
                Log.d(Constants.TRAILBOOK_TAG, "dismissing dialog");
                sendActionToListener(DISMISS, null);
            }
        };
        Log.d(Constants.TRAILBOOK_TAG, "showing no paths alert");
        ApplicationUtils.showAlert(getActivity(), clickListenerOK, getString(R.string.info_no_paths_downloaded_title), getString(R.string.info_no_paths_downloaded_message), getString(R.string.OK), null);
    }


    @Override
    public boolean onContextItemSelected(MenuItem item) {
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
}
