package com.trailbook.kole.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

import com.trailbook.kole.activities.R;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.fragments.list_content.CheckablePathAdapter;
import com.trailbook.kole.state_objects.PathManager;

import java.util.ArrayList;

public class UpdatePathFragment extends Fragment implements View.OnClickListener {

    private static final String PATH_IDS_KEY = "PATH_IDS";
    private static final String CLASSNAME = "UpdatePathFragment";
    private static UpdatePathsDialogListener mListener;
    private ArrayList<String> mPathIds;
    private CheckablePathAdapter mAdapter;

    public interface UpdatePathsDialogListener {
        public void onUpdatePathsClick(ArrayList<String> pathIds);
    }

    public UpdatePathFragment() {
        // Empty constructor required for DialogFragment
    }

    public static UpdatePathFragment newInstance(ArrayList<String> pathIds) {
        UpdatePathFragment frag = new UpdatePathFragment();
        Bundle args = new Bundle();
        args.putStringArrayList(PATH_IDS_KEY, pathIds);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (UpdatePathsDialogListener) activity;
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

        outState.putStringArrayList(PATH_IDS_KEY, mPathIds);
        //todo: save checkbox states
    }

    public void restoreState(Bundle savedState) {
        mPathIds = savedState.getStringArrayList(PATH_IDS_KEY);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mPathIds = getArguments().getStringArrayList(PATH_IDS_KEY);
        if (mPathIds == null) {
            getActivity().getFragmentManager().beginTransaction().remove(this).commit();
            return null;
        }

        ArrayList<PathSummary> summaries = PathManager.getInstance().getPathSummariesFromPathIds(mPathIds);

        View view  = inflater.inflate(R.layout.update_paths_dialog, container, false);

        mAdapter = new CheckablePathAdapter(getActivity(),
                R.layout.checkbox_list_item, summaries);
        ListView listView = (ListView) view.findViewById(R.id.listView1);
        // Assign adapter to ListView
        listView.setAdapter(mAdapter);
        Button bCancel = (Button)view.findViewById(R.id.b_cancel);
        bCancel.setOnClickListener(this);
        Button bOk = (Button)view.findViewById(R.id.b_ok);
        bOk.setOnClickListener(this);

/*        listView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // When clicked, show a toast with the TextView text
                Country country = (Country) parent.getItemAtPosition(position);
                Toast.makeText(getApplicationContext(),
                        "Clicked on Row: " + country.getName(),
                        Toast.LENGTH_LONG).show();
            }
        });*/

        return view;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.b_cancel) {
            getFragmentManager().popBackStackImmediate();
        } else if (v.getId() == R.id.b_ok) {
            ArrayList<String> pathSummariesToUpdate = mAdapter.getPathSummariesToUpdate();
            mListener.onUpdatePathsClick(pathSummariesToUpdate);
        }
    }
}
