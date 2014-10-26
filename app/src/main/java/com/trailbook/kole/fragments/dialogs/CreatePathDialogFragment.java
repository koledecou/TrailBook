package com.trailbook.kole.fragments.dialogs;



import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.trailbook.kole.activities.R;

public class CreatePathDialogFragment extends DialogFragment implements View.OnClickListener {

    public interface CreatePathDialogListener {
        public void onNewPathClick(String pathName);
    }

    private EditText mEditText;
    private static CreatePathDialogListener mListener;

    public CreatePathDialogFragment() {
        // Empty constructor required for DialogFragment
    }

    public static CreatePathDialogFragment newInstance(int title) {
        CreatePathDialogFragment frag = new CreatePathDialogFragment();
        Bundle args = new Bundle();
        args.putInt("title", title);
        frag.setArguments(args);
        return frag;
    }

    public void setListener(CreatePathDialogListener l) {
        mListener = l;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        int title = getArguments().getInt("title");

        View view = inflater.inflate(R.layout.create_path_dialog, container);
        mEditText = (EditText) view.findViewById(R.id.cpd_et_path_name);
        mEditText.requestFocus();

        getDialog().setTitle(title);
        getDialog().getWindow().setSoftInputMode (WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        setButtons(view);

        return view;
    }

    private void setButtons(View v) {
        Button cancel = (Button)v.findViewById(R.id.cpd_b_cancel);
        Button newButton = (Button) v.findViewById(R.id.cpd_b_ok);

        cancel.setOnClickListener(this);
        newButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.cpd_b_ok) {
            String pathName = mEditText.getText().toString();
            mListener.onNewPathClick(pathName);
        }

        dismiss();
    }
}