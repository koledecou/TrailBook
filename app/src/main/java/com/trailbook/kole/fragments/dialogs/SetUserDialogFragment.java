package com.trailbook.kole.fragments.dialogs;


import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.trailbook.kole.activities.R;
import com.trailbook.kole.activities.utils.LoginUtil;
import com.trailbook.kole.data.User;
import com.trailbook.kole.events.UserUpdatedEvent;
import com.trailbook.kole.state_objects.BusProvider;

public class SetUserDialogFragment extends DialogFragment implements View.OnClickListener {

    public interface SetUserDialogListener {
        public void onUserUpdated(User user);
    }

    private EditText mEditTextUserName;
    private TextView mTextViewUserId;
    private static SetUserDialogListener mListener;

    private User mUser;

    public SetUserDialogFragment() {
        // Empty constructor required for DialogFragment
    }

    public static SetUserDialogFragment newInstance(int title, User user) {
        SetUserDialogFragment frag = new SetUserDialogFragment();
        Bundle args = new Bundle();
        args.putInt("title", title);
        args.putString("user_name", user.userName);
        args.putString("user_id", user.userId);
        frag.setArguments(args);

        return frag;
    }

    public void setListener(SetUserDialogListener l) {
        mListener = l;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUser = new User();
        mUser.userName = getArguments().getString("user_name");
        mUser.userId = getArguments().getString("user_id");
        Bus bus = BusProvider.getInstance();
        bus.register(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        int title = getArguments().getInt("title");


        View view = inflater.inflate(R.layout.set_user_dialog, container);
        mEditTextUserName = (EditText) view.findViewById(R.id.sud_et_user_name);
        mEditTextUserName.setText(mUser.userName);
        mEditTextUserName.requestFocus();

        mTextViewUserId = (TextView) view.findViewById(R.id.sud_login_id);
        mTextViewUserId.setText(mUser.userId);

        getDialog().setTitle(title);
        getDialog().getWindow().setSoftInputMode (WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        setButtons(view);
        return view;
    }

    private void setButtons(View v) {
        ImageButton mPickAccountButton = (ImageButton) v.findViewById(R.id.sud_b_pick_account);
        mPickAccountButton.setOnClickListener(this);

        Button mOkButton = (Button) v.findViewById(R.id.sud_b_ok);
        mOkButton.setOnClickListener(this);

        Button mCancelButton = (Button) v.findViewById(R.id.sud_b_cancel);
        mCancelButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.sud_b_ok) {
            mUser.userName = mEditTextUserName.getText().toString();;
            mListener.onUserUpdated(mUser);
            dismiss();
        } else if (v.getId() == R.id.sud_b_cancel) {
            dismiss();
        } else if (v.getId() == R.id.sud_b_pick_account) {
            LoginUtil.authenticate(getActivity());
        }
    }

    @Subscribe
    public void onUserUpdatedEvent(UserUpdatedEvent event) {
        mUser = event.getUser();
        setCurrentUserIdDisplay(mUser);
    }

    private void setCurrentUserIdDisplay(User user) {
        String userId = user.userId;
        if (userId == null || userId.equalsIgnoreCase("-1") || userId.equalsIgnoreCase("")) {
            mTextViewUserId.setText("Please Choose Account");
        }else {
            mTextViewUserId.setText(user.userId);
        }
    }
}