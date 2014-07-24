package com.trailbook.kole.fragments;

/**
 * Created by kole on 7/23/2014.
 */
public interface PathDetailsActionListener {
    void onDownloadRequested(String pathId);
    void onFollowRequested(String pathId);
    void onZoomRequested(String mPathId);
    void onResumeLeadingRequested(String mPathId);
}
