package com.trailbook.kole.fragments.point_attached_object_create;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.Service;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.trailbook.kole.activities.R;
import com.trailbook.kole.data.Attachment;
import com.trailbook.kole.data.Climb;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.Grade;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.state_objects.PathManager;

import java.util.ArrayList;

public class CreateClimbFragment extends CreatePointAttachedObjectFragment implements AddPitchDescriptionDialogFragment.AddPitchDescriptionDialogListener{
    private static final String CLIMB_NAME = "CLIMB_NAME";
    private static final String GRADE = "GRADE";
    private static final String GRADE_SYSTEM_POSITION = "GRADE_SYSTEM_POSITION";
    private static final String TYPE_POSITION = "TYPE_POSITION";
    private static final String DESCRIPTION = "DESCRIPTION";
    private static final String RACK_DESCRIPTION = "RACK_DESCRIPTION";
    private static final String PITCH_COUNT = "PITCH_COUNT";
    private static final String PITCH_DESCRIPTIONS = "PITCH_DESCRIPTIONS";

    private Spinner mSpinnerType;
    private EditText mEditTextName;
    private EditText mEditTextDescription;
    private EditText mEditTextGrade;
    private EditText mEditTextRackDescription;
    private Spinner mSpinnerGradingSystem;
    private ArrayList<String> mPitchDescriptions;
    private EditText mEditTextPitchCount;
    private Button mAddPitchDescriptionButton;

    public static CreateClimbFragment newInstance(String paoId) {
        CreateClimbFragment fragment = new CreateClimbFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, paoId);
        fragment.setArguments(args);
        return fragment;
    }
    public CreateClimbFragment() {
        super();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mEditTextGrade != null) {
            outState.putString(GRADE, mEditTextGrade.getText().toString());
        }
        if (mEditTextDescription != null) {
            outState.putString(DESCRIPTION, mEditTextDescription.getText().toString());
        }
        if (mEditTextName != null) {
            outState.putString(CLIMB_NAME, mEditTextName.getText().toString());
        }
        if (mEditTextRackDescription != null) {
            outState.putString(RACK_DESCRIPTION, mEditTextRackDescription.getText().toString());
        }
        if (mSpinnerGradingSystem != null) {
            outState.putInt(GRADE_SYSTEM_POSITION, mSpinnerGradingSystem.getSelectedItemPosition());
        }
        if (mSpinnerType != null) {
            outState.putInt(TYPE_POSITION, mSpinnerType.getSelectedItemPosition());
        }
        if (mEditTextPitchCount != null) {
            outState.putString(PITCH_COUNT, mEditTextPitchCount.getText().toString());
        }
        if (mPitchDescriptions != null) {
            outState.putStringArrayList(PITCH_DESCRIPTIONS, mPitchDescriptions);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflateView(inflater, container, R.layout.create_climb);
        createViewObjects();

        mSpinnerGradingSystem = (Spinner)view.findViewById(R.id.cc_spinner_grade_system);
        mSpinnerType = (Spinner)view.findViewById(R.id.cc_spinner_type);

        mEditTextName = (EditText)view.findViewById(R.id.cc_et_name);
        mEditTextDescription = (EditText)view.findViewById(R.id.cc_et_description);
        mEditTextGrade = (EditText)view.findViewById(R.id.cc_et_grade);
        mEditTextRackDescription = (EditText)view.findViewById(R.id.cc_et_rack);
        mEditTextPitchCount = (EditText)view.findViewById(R.id.cc_et_pitch_count);
        mAddPitchDescriptionButton = (Button)view.findViewById(R.id.cc_b_add_pitch_descriptions);
        mAddPitchDescriptionButton.setOnClickListener(this);

        if (savedInstanceState == null) {
            populateValues(PathManager.getInstance().getPointAttachedObject(mNoteId));
        } else {
            restoreInstance(savedInstanceState);
        }

        return view;
    }

    @Override
    public void onClick(View view) {
        Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ": view clicked: " + view.getId());
        hideSoftKeyboard();
        if (view.getId() == R.id.cc_b_add_pitch_descriptions) {
            ArrayList<String> newPitchDescriptions = populatePitchDescriptionArray(mPitchDescriptions);
            Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + " new pitch descriptions: " + newPitchDescriptions);
            if (newPitchDescriptions!=null) {
                showEditPitchDescriptionDialog(newPitchDescriptions);
            }
           return;
        } else {
            super.onClick(view);
        }
    }

    private ArrayList<String> populatePitchDescriptionArray(ArrayList<String> existingDescriptions) {
        ArrayList<String> newDescriptionArray = new ArrayList<String>();
        Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + " edit text picth count contents:" + mEditTextPitchCount.getText());
        if (mEditTextPitchCount.getText() != null && mEditTextPitchCount.getText().toString().length()>0) {
            String pitchCount = mEditTextPitchCount.getText().toString();
            int totalPitchCount = Integer.parseInt(pitchCount);
            for (int pitchNum = 1; pitchNum <= totalPitchCount; pitchNum++) {
                if (existingDescriptions != null && existingDescriptions.size() >= pitchNum) {
                    newDescriptionArray.add(existingDescriptions.get(pitchNum - 1));
                } else {
                    newDescriptionArray.add("");
                }
            }
            return newDescriptionArray;
        }
        else
            return null;
    }

    private void showEditPitchDescriptionDialog(ArrayList<String> existingPitchDescriptions) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("new_path_dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        AddPitchDescriptionDialogFragment newFragment = AddPitchDescriptionDialogFragment.newInstance(existingPitchDescriptions);
        newFragment.setListener(this);
        newFragment.show(ft, "add_pitch_descriptions_dialog");
    }

    @Override
    public void populateValues(PointAttachedObject paoClimb) {
        super.populateValues(paoClimb);
        if (paoClimb != null) {
            Climb climb = (Climb)paoClimb.getAttachment();
            mEditTextName.setText(climb.name);
            mEditTextGrade.setText(climb.grade.grade);
            mEditTextDescription.setText(climb.description);
            mEditTextRackDescription.setText(climb.rackDescription);
            //todo: set spinners
            mPitchDescriptions = ((Climb) paoClimb.getAttachment()).getPitchDescriptions();
            mEditTextPitchCount.setText(String.valueOf(climb.getPitchCount()));
        }
    }

    @Override
    public void restoreInstance(Bundle savedInstanceState) {
        Log.d(Constants.TRAILBOOK_TAG, "restoring climb state");
        super.restoreInstance(savedInstanceState);
        if (savedInstanceState != null) {
            mEditTextGrade.setText(savedInstanceState.getString(GRADE));
            mEditTextName.setText(savedInstanceState.getString(CLIMB_NAME));
            mEditTextDescription.setText(savedInstanceState.getString(DESCRIPTION));
            mEditTextRackDescription.setText(savedInstanceState.getString(RACK_DESCRIPTION));
            mEditTextPitchCount.setText(savedInstanceState.getString(PITCH_COUNT));
            //todo: restore spinners
            mPitchDescriptions = savedInstanceState.getStringArrayList(PITCH_DESCRIPTIONS);
        }
    }

    @Override
    protected Attachment createAttachment() {
        Climb newClimb = new Climb();
        newClimb.setName(mEditTextName.getText().toString());
        newClimb.setDescription(mEditTextDescription.getText().toString());
        Grade grade = new Grade();
        grade.grade=mEditTextGrade.getText().toString();
        grade.gradingSystem = mSpinnerGradingSystem.getSelectedItem().toString();
        newClimb.setGrade(grade);
        newClimb.setName(mEditTextName.getText().toString());
        newClimb.setRackDescription(mEditTextRackDescription.getText().toString());
        newClimb.setClimbType(mSpinnerType.getSelectedItem().toString());
        newClimb.setPitchDescriptions(mPitchDescriptions);
        try {
            int pitchCount = Integer.parseInt(mEditTextPitchCount.getText().toString());
            newClimb.setPitchCount(pitchCount);
        } catch (Exception e) {
            Log.d(Constants.TRAILBOOK_TAG, "exception getting pitch count ", e);
        }

        return newClimb;
    }

    @Override
    protected void hideSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mEditTextName.getWindowToken(), 0);
    }

    @Override
    protected void showSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Service.INPUT_METHOD_SERVICE);
        imm.showSoftInput(mEditTextName, 0);
    }

    @Override
    public void onAddPitchDescriptions(ArrayList<String> pitchDescriptions) {
        Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ": got pitch descriptions:" + pitchDescriptions);
        mPitchDescriptions = pitchDescriptions;
    }
}
