package com.trailbook.kole.fragments;

import android.content.Context;
import android.media.Image;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.trailbook.kole.activities.R;
import com.trailbook.kole.data.ButtonActions;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.state_objects.PathManager;


public class PathDetailsView extends LinearLayout implements View.OnClickListener {

    public void setActionListener(PathDetailsActionListener actionListener) {
        this.actionListener = actionListener;
    }

    // the fragment initialization parameters
    private static final String ARG_PARAM1 = "path_id";
    private PathManager mPathManager;

    private String mPathId = "1";
    private String mDescription = "This is a sample path";
    private String mName = "Sample Path";
    private Image mImage = null;
    private TextView mNameView;
    private TextView mDescriptionView;
    private ImageView mImageView;
    private Button mNavToStartButton;
    private Button mDownloadButton;
    private Button mFollowButton;
    private Button mZoomButton;
    private ImageButton mMoreButton;
    private PathDetailsActionListener actionListener;

    public PathDetailsView(Context context) {
        super(context);
        loadViews();
    }

    public PathDetailsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        loadViews();
    }

    public PathDetailsView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        loadViews();
    }

    public void setPathId(String pathId) {
        mPathId=pathId;
        mPathManager = PathManager.getInstance();
        PathSummary summary = mPathManager.getPathSummary(pathId);
        if (summary == null) {
            Log.d(Constants.TRAILBOOK_TAG, "PathDetailsView: summary is null");
            return;
        }
        mDescription=summary.getDescription();
        mName=summary.getName();
        //TODO: add summary image
        if (mName != null)
            mNameView.setText(mName);
        if (mDescription != null) {
            Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ": description is " + mDescription);
            mDescriptionView.setText(mDescription);
        }
        buildButtonBar(mPathManager.getButtonActions(pathId));
        invalidate();
    }

    private void loadViews(){
        LayoutInflater inflater = LayoutInflater.from(this.getContext());
        inflater.inflate(R.layout.path_details, this);

        mNameView=(TextView)findViewById(R.id.summary_name);
        mDescriptionView=(TextView)findViewById(R.id.summary_description);
        mImageView=(ImageView)findViewById(R.id.summary_image);
        
        mDownloadButton = (Button)findViewById(R.id.pdv_button_download);
        mDownloadButton.setOnClickListener(this);

        mFollowButton = (Button)findViewById(R.id.pdv_button_follow);
        mFollowButton.setOnClickListener(this);
/*
        mNavToStartButton = (Button)findViewById(R.id.pdv_nav_to_start);
        mNavToStartButton.setOnClickListener(this);

        mZoomButton = (Button)findViewById(R.id.pdv_zoom);
        mZoomButton.setOnClickListener(this);*/

        mMoreButton = (ImageButton)findViewById(R.id.pdv_more);
        mMoreButton.setOnClickListener(this);
    }

    private void buildButtonBar(ButtonActions actions) {
        if (actions.mCanDownloadPath)
            mDownloadButton.setVisibility(VISIBLE);
        else
            mDownloadButton.setVisibility(GONE);

        if (actions.mCanFollowPath) {
            mFollowButton.setVisibility(VISIBLE);
        } else {
            mFollowButton.setVisibility(GONE);
        }
    }



    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.pdv_button_download) {
            Toast.makeText(getContext(), "downloading " + mName, Toast.LENGTH_LONG).show();
            actionListener.onDownloadRequested(mPathId);
        } else if (v.getId() == R.id.pdv_button_follow) {
            Toast.makeText(getContext(), "following " + mName, Toast.LENGTH_LONG).show();
            actionListener.onFollowRequested(mPathId);
        }

/*        else if (v.getId() == R.id.pdv_zoom) {
            actionListener.onZoomRequested(mPathId);
        } else if (v.getId() == R.id.pdv_more) {
            Toast.makeText(getContext(), "more actions for " + mName, Toast.LENGTH_LONG).show();
            actionListener.onMoreActionsSelected(mPathId, this);
        } else if (v.getId() == R.id.pdv_nav_to_start) {
            actionListener.onNavigateToStart(mPathId);
        }*/
        if (v.getId() == R.id.summary_layout || v.getId() == R.id.pdv_more) {
            //Toast.makeText(getContext(), "more actions for " + mName, Toast.LENGTH_LONG).show();
            actionListener.onMoreActionsSelected(mPathId, this);
        }
    }
}
