package com.trailbook.kole.fragments.dialogs;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.state_objects.PathManager;

public class PathActionDialogFragment extends DialogFragment {
    String mPathId;

    public PathActionDialogFragment() {
    }

    public static PathActionDialogFragment newInstance(String pathId) {
        PathActionDialogFragment frag = new PathActionDialogFragment();
        Bundle args = new Bundle();
        args.putString("path_id", pathId);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mPathId = getArguments().getString("path_id");
        PathSummary summary = PathManager.getInstance().getPathSummary(mPathId);
        getDialog().setTitle(summary.getName());


        return super.onCreateView(inflater, container, savedInstanceState);
    }
}
