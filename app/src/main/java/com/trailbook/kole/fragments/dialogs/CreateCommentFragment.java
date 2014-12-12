package com.trailbook.kole.fragments.dialogs;



import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.trailbook.kole.activities.R;
import com.trailbook.kole.data.TrailBookComment;
import com.trailbook.kole.state_objects.TrailBookState;

public class CreateCommentFragment extends DialogFragment implements View.OnClickListener {

    private static final String COMMENT = "COMMENT";
    private static final String PATH_ID = "PATH_ID";
    private static final String IS_ATTACHED="IS_ATTACHED";

    public interface CreateCommentDialogListener {
        public void onNewAttachedCommentClick(TrailBookComment comment);
        public void onNewPathCommentClick(TrailBookComment comment);
    }

    private boolean mIsAttached;
    private String mComment;
    private String mPathId;
    private EditText mEditText;
    private CheckBox mCheckBoxAttach;
    private static CreateCommentDialogListener mListener;

    public CreateCommentFragment() {
        // Empty constructor required for DialogFragment
    }

    public static CreateCommentFragment newInstance(int title, String pathId) {
        CreateCommentFragment frag = new CreateCommentFragment();
        Bundle args = new Bundle();
        args.putInt("title", title);
        args.putString("path_id", pathId);
        frag.setArguments(args);
        return frag;
    }

    public void setListener(CreateCommentDialogListener l) {
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
        mPathId = getArguments().getString("path_id");

        View view = inflater.inflate(R.layout.create_comment_dialog, container);
        mEditText = (EditText) view.findViewById(R.id.ccd_et_comment);
        mEditText.requestFocus();

        mCheckBoxAttach = (CheckBox) view.findViewById(R.id.ccd_cb_attach);
        mCheckBoxAttach.setVisibility(View.GONE);

        getDialog().setTitle(title);
        getDialog().getWindow().setSoftInputMode (WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        setButtons(view);

        if (savedInstanceState != null) {
            mIsAttached = savedInstanceState.getBoolean(IS_ATTACHED);
            mCheckBoxAttach.setChecked(mIsAttached);

            mComment = savedInstanceState.getString(COMMENT);
            mEditText.setText(mComment);
            mPathId = savedInstanceState.getString(PATH_ID);
        }

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(COMMENT, mEditText.getText().toString());
        outState.putString(PATH_ID, mPathId);
        outState.putBoolean(IS_ATTACHED, mCheckBoxAttach.isChecked());
    }



    private void setButtons(View v) {
        Button cancel = (Button)v.findViewById(R.id.ccd_b_cancel);
        Button newButton = (Button) v.findViewById(R.id.ccd_b_ok);

        cancel.setOnClickListener(this);
        newButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.ccd_b_ok) {
            String comment = mEditText.getText().toString();
            mIsAttached = mCheckBoxAttach.isChecked();
            TrailBookComment tbComment = new TrailBookComment(mPathId, comment, TrailBookState.getCurrentUser());
            if (mIsAttached) {
                mListener.onNewAttachedCommentClick(tbComment);
            } else {
                mListener.onNewPathCommentClick(tbComment);
            }
        }

        dismiss();
    }
}