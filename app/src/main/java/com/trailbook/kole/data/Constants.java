package com.trailbook.kole.data;

import android.content.Context;
import android.os.Environment;

import java.io.File;

/**
 * Created by Fistik on 6/30/2014.
 */
public class Constants {
    public static final String delimiter = "--";
    public static final String BASE_URL = "http://www.thetrailbook.com/cgi-bin/trailbook";
    public static final String pathSummaryScript = "/getPathSummaries.py";
    public static final String getPathPointsScript = "/getPathPoints.py";
    public static final String getNotesScript = "/getNotes.py";
    public static final String uploadPathJson = "/uploadPathJson.py";
    public static final String uploadImage = "/uploadImage.py";
    public static final String webServerImageDir = "http://www.thetrailbook.com/trailbook/images";
    public static final String deviceExternalDir = Environment.getExternalStorageDirectory().getPath();
    public static final String pathsDir = "paths";
    public static final String imageDir = "images";
    public static final String TRAILBOOK_TAG = "trailbook";

    public static int SEARCH_MODE = 0;
    public static int FOLLOWER_MODE = 1;
    public static int LEADER_MODE = 2;

    public static int MEDIUM_DETAIL = 500;
    public static int FULL_DETAIL = -1;

    public static final int N = 1;
    public static final int NE = 2;
    public static final int E = 3;
    public static final int SE = 4;
    public static final int S = 5;
    public static final int SW = 6;
    public static final int W = 7;
    public static final int NW = 8;
    public static final int NO_BEARING = 0;
}
