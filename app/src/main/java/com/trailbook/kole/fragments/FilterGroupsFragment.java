package com.trailbook.kole.fragments;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.TextView;

import com.trailbook.kole.activities.R;
import com.trailbook.kole.data.FilterSet;
import com.trailbook.kole.data.PathGroup;
import com.trailbook.kole.events.FilterChangedEvent;
import com.trailbook.kole.fragments.dialogs.CreateCommentFragment;
import com.trailbook.kole.helpers.TrailbookPathUtilities;
import com.trailbook.kole.state_objects.BusProvider;
import com.trailbook.kole.state_objects.TrailBookState;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

public class FilterGroupsFragment extends DialogFragment implements View.OnClickListener {
    private Button mUnlockGroupButton;
    private ScrollView mGroupsContainer;
    private ArrayList<PathGroup> mGroups;

    public static FilterGroupsFragment newInstance() {
        FilterGroupsFragment fragment = new FilterGroupsFragment();
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FilterGroupsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BusProvider.getInstance().register(this);
        mGroups = TrailbookPathUtilities.getMyGroups();
        if (mGroups == null)
            mGroups = new ArrayList<PathGroup>();
    }

    @Override
    public void onDestroy() {
        BusProvider.getInstance().unregister(this);
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.filter_dialog, container, false);

        mUnlockGroupButton = (Button) view.findViewById(R.id.fd_b_add_group);
        mUnlockGroupButton.setOnClickListener(this);

        TextView tvNoGroupsMessage=(TextView)view.findViewById(R.id.tv_no_groups);
        if (mGroups == null || mGroups.size()<1)
            tvNoGroupsMessage.setVisibility(View.GONE);
        else
            tvNoGroupsMessage.setVisibility(View.VISIBLE);

        mGroupsContainer = (ScrollView)view.findViewById(R.id.scroll_groups);
        putGroupsInContainer(inflater, mGroupsContainer,  mGroups);

        restoreSavedValues();
        return view;
    }

    private void restoreSavedValues() {
        FilterSet filters = TrailbookPathUtilities.getFilters();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    private void putGroupsInContainer(LayoutInflater inflater, ScrollView groupsContainer, ArrayList<PathGroup> groups) {
        groupsContainer.removeAllViews();
        if (groups != null && groups.size()>0) {
            for (PathGroup group : groups) {
                putGroupInContainer(inflater, groupsContainer, group);
            }
        } else {
            putEmptyMessageInContainer();
        }

    }

    private void putEmptyMessageInContainer() {
        //todo: give some message here
    }

    private void putGroupInContainer(LayoutInflater inflater, ScrollView groupsContainer, PathGroup group) {
        View singleGropView = inflateSingleGroupView(inflater, groupsContainer);
        populateGroupView(singleGropView, group);
        groupsContainer.addView(singleGropView);
    }

    private View inflateSingleGroupView(LayoutInflater inflater, ScrollView groupsContainer) {
        View singleGroupView = inflater.inflate(R.layout.single_group, groupsContainer, false);
        return singleGroupView;
    }

    private void populateGroupView(View singleGroupView, PathGroup group) {
        TextView tvGroupName = (TextView)singleGroupView.findViewById(R.id.tv_group_name);
        tvGroupName.setText(group.groupName);
        singleGroupView.setTag(group.groupId);
    }

    @Override
    public void onClick(View v) {
       if (v.getId() == R.id.fd_b_add_group) {
           showJoinGroupDialog();
       } else {
           saveFilters();
       }
    }

    private void saveFilters() {
        FilterSet filters = getFilters();
        TrailbookPathUtilities.saveFilters(filters);
        BusProvider.getInstance().post(new FilterChangedEvent(filters));
    }

    private FilterSet getFilters() {
        FilterSet filters = new FilterSet();
        filters.groupIdsToShow = getGroupIdsToShow();

        return filters;
    }

    private Set<String> getGroupIdsToShow() {
        Set<String> groupIds = new LinkedHashSet<String>();
        for (PathGroup group: mGroups) {
            CheckBox cb = (CheckBox)mGroupsContainer.findViewWithTag(group.groupId);
            if (cb != null && cb.isChecked()) {
                groupIds.add(group.groupId);
            }
        }
        return groupIds;
    }

    private void showJoinGroupDialog() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("join_group_dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        CreateCommentFragment newFragment = CreateCommentFragment.newInstance(R.string.add_comment_dialog_title, TrailBookState.getActivePathId());
        newFragment.setListener((CreateCommentFragment.CreateCommentDialogListener) getActivity());
        newFragment.show(ft, "join_group_dialog");
    }
}
