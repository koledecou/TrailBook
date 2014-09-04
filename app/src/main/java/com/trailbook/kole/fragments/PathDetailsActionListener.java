package com.trailbook.kole.fragments;

/**
 * Created by kole on 7/23/2014.
 */
public interface PathDetailsActionListener {
    void onDownloadRequested(String pathId);
    void onFollowRequested(String pathId);
    void onZoomRequested(String pathId);
    void onResumeLeadingRequested(String pathId);
    void onNavigateToStart(String mPathId);
}
