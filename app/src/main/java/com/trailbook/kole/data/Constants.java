package com.trailbook.kole.data;

import android.content.Context;
import android.os.Environment;

import java.io.File;

/**
 * Created by Fistik on 6/30/2014.
 */
public class Constants {
    public static final String delimiter = "--";
    public static final String BASE_CGIBIN_URL = "http://www.thetrailbook.com/cgi-bin/trailbook";
    public static final String BASE_WEBSERVERFILE_URL = "http://www.thetrailbook.com/trailbook_files";
    public static final String BASE_WEBSERVER_PATH_URL = BASE_WEBSERVERFILE_URL + "/paths";
    public static final String BASE_WEBSERVER_SEGMENT_URL =BASE_WEBSERVERFILE_URL + "/segments";
    public static final String pathSummaryScript = "/getPathSummariesFromFile.py";
    public static final String getSegmentsScript = "/getPathSegmentMapFromFile.py";
    public static final String getPointsScript = "/getSegmentPointsFromFile.py";
    public static final String getNotesScript = "/getNotesFromFile.py";
    public static final String uploadJson = "/uploadJson.py";
    public static final String uploadImage = "/uploadImage.py";
    public static final String deviceExternalDir = Environment.getExternalStorageDirectory().getPath();
    public static final String pathsDir = "paths";
    public static final String segmentsDir = "segments";
    public static final String imageDir = "images";
    public static final String TRAILBOOK_TAG = "trailbook";
    public static final long CLOUD_REFRESH_DEFAULT_TIME_DELTA = 5*60*1000;

    public static int MEDIUM_DETAIL = 500;
    public static int FULL_DETAIL = -1;

    public static final int MIN_DISTANCE_BETWEEN_POINTS=3; //distance in meters
    public static final int MAX_DISTANCE_BETWEEN_POINTS = 1000;
    public static final int IMAGE_CAPTURE_WIDTH = 640;
}
