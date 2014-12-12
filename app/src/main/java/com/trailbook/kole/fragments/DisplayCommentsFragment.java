package com.trailbook.kole.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TextView;

import com.squareup.otto.Subscribe;
import com.trailbook.kole.activities.R;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.TrailBookComment;
import com.trailbook.kole.events.PathCommentAddedEvent;
import com.trailbook.kole.fragments.dialogs.CreateCommentFragment;
import com.trailbook.kole.state_objects.BusProvider;
import com.trailbook.kole.state_objects.PathManager;
import com.trailbook.kole.state_objects.TrailBookState;

import java.util.ArrayList;

/**
 * A fragment representing a list of Items.
 * <p />
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p />
 * Activities containing this fragment MUST implement the OnFragmentInteractionListener
 * interface.
 */
public class DisplayCommentsFragment extends Fragment implements View.OnClickListener {
    public static final String  PATH_ID_ARG="PATH_ID";
    public String mPathId;

    private Button mAddCommentButton;
    private TableLayout mCommentsContainer;

    public static DisplayCommentsFragment newInstance(String pathId) {
        DisplayCommentsFragment fragment = new DisplayCommentsFragment();

        Bundle args = new Bundle();
        args.putString(PATH_ID_ARG, pathId);
        fragment.setArguments(args);

        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public DisplayCommentsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BusProvider.getInstance().register(this);
        mPathId = getPathIdFromArg();
    }

    @Override
    public void onDestroy() {
        BusProvider.getInstance().unregister(this);
        super.onDestroy();
    }

    private String getPathIdFromArg () {
        Bundle args = getArguments();
        return args.getString(PATH_ID_ARG);
    }

    @Subscribe
    public void onPathCommentAddedEvent(PathCommentAddedEvent event) {
        TrailBookComment comment = event.getComment();
        putCommentInContainer(getActivity().getLayoutInflater(), mCommentsContainer, comment);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.comment_list, container, false);
        mAddCommentButton = (Button) view.findViewById(R.id.b_add_comment);
        mAddCommentButton.setOnClickListener(this);
        mCommentsContainer = (TableLayout)view.findViewById(R.id.comments_container);
        putCommentsInContainer(inflater, mCommentsContainer, mPathId);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    private void putCommentsInContainer(LayoutInflater inflater, TableLayout commentContainer, String pathId) {
        commentContainer.removeAllViews();
        PathManager pathManager = PathManager.getInstance();
        ArrayList<TrailBookComment> comments = pathManager.getComments(pathId);
        for (TrailBookComment comment:comments) {
            //todo: order by date
            putCommentInContainer(inflater, commentContainer, comment);
        }
    }

    private void putCommentInContainer(LayoutInflater inflater, TableLayout commentContainer, TrailBookComment comment) {
        String userName = getUserNameForDisplay(comment);
        String commentText = comment.getComment();

        View singleCommentView = inflateSingleCommentView(inflater, commentContainer);
        populateCommentView(singleCommentView, userName, commentText);
        commentContainer.addView(singleCommentView);
    }

    private String getUserNameForDisplay(TrailBookComment comment) {
        if (comment.getUser() ==null || comment.getUser().userName == null) {
            return Constants.DEFAULT_USER_NAME;
        } else {
            return comment.getUser().userName;
        }
    }

    private View inflateSingleCommentView(LayoutInflater inflater, TableLayout commentContainer) {
        View singleCommentView = inflater.inflate(R.layout.single_comment, commentContainer, false);
        return singleCommentView;
    }

    private void populateCommentView(View singleCommentView, String userName, String commentText) {
        TextView tvUserName = (TextView)singleCommentView.findViewById(R.id.tv_user_name);
        TextView tvComment = (TextView)singleCommentView.findViewById(R.id.tv_comment_text);
        if (userName == null || userName.length()<1) {
            userName = Constants.DEFAULT_USER_NAME;
        }
        tvUserName.setText(userName);
        tvComment.setText(commentText);
    }

    @Override
    public void onClick(View v) {
        showAddCommentDialog();
    }

    private void showAddCommentDialog() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("add_comment_dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        CreateCommentFragment newFragment = CreateCommentFragment.newInstance(R.string.add_comment_dialog_title, TrailBookState.getActivePathId());
        newFragment.setListener((CreateCommentFragment.CreateCommentDialogListener) getActivity());
        newFragment.show(ft, "add_comment_dialog");
    }
}
