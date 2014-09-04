package com.trailbook.kole.fragments;

import android.app.Activity;
import android.content.Context;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.trailbook.kole.activities.R;
import com.trailbook.kole.data.ButtonActions;
import com.trailbook.kole.data.Path;
import com.trailbook.kole.data.PathSummary;
import com.trailbook.kole.tools.MapUtilities;
import com.trailbook.kole.tools.PathManager;
import com.trailbook.kole.worker_fragments.LocationServicesFragment;
import com.trailbook.kole.worker_fragments.WorkerFragment;


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
    private Button mResumeButton;
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
        mDescription=summary.getDescription();
        mName=summary.getName();
        //TODO: add summary image
        if (mName != null)
            mNameView.setText(mName);
        if (mDescription != null)
            mDescriptionView.setText(mDescription);

        buildButtonBar(mPathManager.getButtonActions(pathId));
        invalidate();
    }

    private void loadViews(){
        LayoutInflater inflater = LayoutInflater.from(this.getContext());
        inflater.inflate(R.layout.path_details, this);

        mNameView=(TextView)findViewById(R.id.summary_name);
        mDescriptionView=(TextView)findViewById(R.id.summary_description);
        mImageView=(ImageView)findViewById(R.id.summary_image);
        
        mDownloadButton = (Button)findViewById(R.id.pdv_download);
        mDownloadButton.setOnClickListener(this);

        mFollowButton = (Button)findViewById(R.id.pdv_follow);
        mFollowButton.setOnClickListener(this);

        mZoomButton = (Button)findViewById(R.id.pdv_zoom);
        mZoomButton.setOnClickListener(this);

        mResumeButton = (Button)findViewById(R.id.pdv_resume);
        mResumeButton.setOnClickListener(this);

        mNavToStartButton = (Button)findViewById(R.id.pdv_nav_to_start);
        mNavToStartButton.setOnClickListener(this);
    }

    private void buildButtonBar(ButtonActions actions) {
        if (actions.mCanDownloadPath)
            enableButton(mDownloadButton);
        else
            disableButton(mDownloadButton);

        if (actions.mCanFollowPath) {
            enableButton(mFollowButton);
            enableButton(mNavToStartButton);
        }
        else {
            disableButton(mFollowButton);
            disableButton(mFollowButton);
        }
    }

    private void disableButton(Button b) {
        b.setVisibility(View.INVISIBLE);
    }

    private void enableButton(Button b) {
        b.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.pdv_download) {
            Toast.makeText(getContext(), "downloading " + mName, Toast.LENGTH_LONG).show();
            actionListener.onDownloadRequested(mPathId);
        } else if (v.getId() == R.id.pdv_follow) {
            Toast.makeText(getContext(), "following " + mName, Toast.LENGTH_LONG).show();
            actionListener.onFollowRequested(mPathId);
        } else if (v.getId() == R.id.pdv_zoom) {
            Toast.makeText(getContext(), "zooming " + mName, Toast.LENGTH_LONG).show();
            actionListener.onZoomRequested(mPathId);
        } else if (v.getId() == R.id.pdv_resume) {
            Toast.makeText(getContext(), "resuming leading " + mName, Toast.LENGTH_LONG).show();
            actionListener.onResumeLeadingRequested(mPathId);
        } else if (v.getId() == R.id.pdv_nav_to_start) {
            actionListener.onNavigateToStart(mPathId);
        }
    }
}
