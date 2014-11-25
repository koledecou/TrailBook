package com.trailbook.kole.fragments.point_attached_object_create;

import android.app.DialogFragment;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.trailbook.kole.activities.R;

import java.util.ArrayList;

/**
 * Created by kole on 11/24/2014.
 */
public class AddPitchDescriptionDialogFragment extends DialogFragment implements View.OnClickListener {
    private static final String PITCH_DESCRIPTIONS = "PITCH_DESCRIPTIONS";

    public interface AddPitchDescriptionDialogListener {
        public void onAddPitchDescriptions(ArrayList<String> pitchDescriptions);
    }

    private static AddPitchDescriptionDialogListener mListener;
    private ArrayList<String> mPitchDescriptions;
    private ArrayList<EditText> mDescriptionEditTextArray;
    LinearLayout mPitchDescriptionContainer;

    public AddPitchDescriptionDialogFragment() {
        // Empty constructor required for DialogFragment
    }

    public static AddPitchDescriptionDialogFragment newInstance(ArrayList<String> existingPitchDescriptions) {
        AddPitchDescriptionDialogFragment frag = new AddPitchDescriptionDialogFragment();
        Bundle args = new Bundle();
        args.putStringArrayList(PITCH_DESCRIPTIONS, existingPitchDescriptions);
        frag.setArguments(args);
        return frag;
    }


    public void setListener(AddPitchDescriptionDialogListener l) {
        mListener = l;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        int title = R.string.add_pitch_description_dialog_title;
        mPitchDescriptions = getArguments().getStringArrayList(PITCH_DESCRIPTIONS);
        mDescriptionEditTextArray = new ArrayList<EditText>();

        View view = inflater.inflate(R.layout.create_climb_pitch_descriptions, container);
        mPitchDescriptionContainer = (LinearLayout)view.findViewById(R.id.apd_pitch_descriptions_dialog_layout);
        createPitchDescriptionViews();

        getDialog().setTitle(title);
        getDialog().getWindow().setSoftInputMode (WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        setButtons(view);

        return view;
    }

    private void createPitchDescriptionViews() {
        int pitchNum = 1;
        mPitchDescriptionContainer.removeAllViews();
        for (String description:mPitchDescriptions) {
            LinearLayout layout = createPitchDescriptionView(pitchNum, description);
            mPitchDescriptionContainer.addView(layout);
            pitchNum++;
        }
    }

    private LinearLayout createPitchDescriptionView(int pitchNum, String description){
        RelativeLayout.LayoutParams params=new RelativeLayout.LayoutParams
                ((int) LinearLayout.LayoutParams.WRAP_CONTENT,(int) LinearLayout.LayoutParams.WRAP_CONTENT);

        LinearLayout layout = new LinearLayout(getActivity());

        layout.setOrientation(LinearLayout.HORIZONTAL);
        TextView tvLabel = getPitchNumberTextView(pitchNum, params);
        layout.addView(tvLabel);

        EditText etDescription = getPitchDescriptionEditText(description, params);
        mDescriptionEditTextArray.add(etDescription);
        layout.addView(etDescription);

        return layout;
    }

    private EditText getPitchDescriptionEditText(String description, RelativeLayout.LayoutParams params) {
        EditText etDescription = new EditText(getActivity());
        etDescription.setText(description);
        etDescription.setTextColor(Color.BLACK);
        etDescription.setLayoutParams(params);
        return etDescription;
    }

    private TextView getPitchNumberTextView(int pitchNum, RelativeLayout.LayoutParams params) {
        TextView tvLabel=new TextView(getActivity());
        tvLabel.setText("Pitch " + pitchNum + " ");
        tvLabel.setTypeface(null, Typeface.BOLD);
        tvLabel.setTextColor(Color.BLACK);
        tvLabel.setLayoutParams(params);
        return tvLabel;
    }

    private void setButtons(View v) {
        Button cancel = (Button)v.findViewById(R.id.apd_b_cancel);
        Button newButton = (Button) v.findViewById(R.id.apd_b_ok);

        cancel.setOnClickListener(this);
        newButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.apd_b_ok) {
            int pitchNum = 1;
            for (EditText et:mDescriptionEditTextArray){
                mPitchDescriptions.set(pitchNum-1, et.getText().toString());
                pitchNum++;
            }
            mListener.onAddPitchDescriptions(mPitchDescriptions);
        }

        dismiss();
    }
}
