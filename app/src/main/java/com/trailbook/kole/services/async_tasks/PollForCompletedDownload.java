package com.trailbook.kole.services.async_tasks;

import android.os.Handler;
import android.util.Log;

import com.trailbook.kole.data.Constants;
import com.trailbook.kole.state_objects.PathManager;

import java.util.ArrayList;


/**
 * Created by kole on 3/16/2015.
 */
public class PollForCompletedDownload extends Thread {

    public void setPathIds(ArrayList<String> pathIds) {
        this.mPathIds = mPathIds;
    }

    private ArrayList<String> mPathIds;
    private PathManager pathManager;
    private Handler mHandler;
    private Runnable mUpdateResultsRunnable;

    public PollForCompletedDownload(ArrayList<String> pathIds, Handler handler, Runnable updateResultsRunnable) {
        this.mPathIds = pathIds;
        this.mHandler = handler;
        pathManager = PathManager.getInstance();
        mUpdateResultsRunnable = updateResultsRunnable;
    }

    /**
     * Starts executing the active part of the class' code. This method is
     * called when a thread is started that has been created with a class which
     * implements {@code Runnable}.
     */
    @Override
    public void run() {
        //todo: put in a timeout
        while (true) {
            try {
                Thread.sleep(5000);
                if (allPathsAreComplete()) {
                    Log.d(Constants.TRAILBOOK_TAG, "PollForCompletedDownload: done.");
                    mHandler.post(mUpdateResultsRunnable);
                    return;
                }
                Log.d(Constants.TRAILBOOK_TAG, "PollForCompletedDownload: not done yet");
            } catch (InterruptedException e) {
                mHandler.post(mUpdateResultsRunnable);
                return;
            }
        }
    }

    private boolean allPathsAreComplete() {
        for (String pathId : mPathIds) {
            if (!pathManager.isPathComplete(pathId)) {
                return false;
            }
        }
        return true;
    }
}
