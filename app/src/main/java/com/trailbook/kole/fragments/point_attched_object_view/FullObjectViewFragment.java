package com.trailbook.kole.fragments.point_attched_object_view;


import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.trailbook.kole.activities.R;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.helpers.NoteFactory;
import com.trailbook.kole.state_objects.PathManager;

public class FullObjectViewFragment extends Fragment {
    String mPaoId;
    int mLayoutId;
    PointAttachedObjectView mPaoView;

    public static FullObjectViewFragment newInstance(String paoId) {
        FullObjectViewFragment fragment = new FullObjectViewFragment();
        Bundle args = new Bundle();
        args.putString("pao_id", paoId);

        fragment.setArguments(args);
        return fragment;
    }

    public FullObjectViewFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPaoId = getArguments().getString("pao_id");
        mLayoutId = getArguments().getInt("layout_id");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_full_pao, container, false);
        LinearLayout layout = (LinearLayout)view.findViewById(R.id.full_pao_layout);
        PointAttachedObject pao = PathManager.getInstance().getPointAttachedObject(mPaoId);
        if (pao != null) {
            mPaoView = NoteFactory.getFullScreenView(pao);
            layout.addView(mPaoView);
        }

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
