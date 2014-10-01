package com.trailbook.kole.fragments;

import android.view.View;

/**
 * Created by kole on 7/23/2014.
 */
public interface PathDetailsActionListener {
    void onDownloadRequested(String pathId);
    void onFollowRequested(String pathId);
    void onZoomRequested(String pathId);
    void onMoreActionsSelected(String pathId, View view);
    void onNavigateToStart(String mPathId);
}
