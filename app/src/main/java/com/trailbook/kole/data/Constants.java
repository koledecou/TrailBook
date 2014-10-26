package com.trailbook.kole.data;

import android.os.Environment;

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
    public static final String pathsRootDir = "paths";
    public static final String cachedPathsRootDir = "temp_cloud_paths";
    public static final String segmentsDir = "segments";
    public static final String imageDir = "images";
    public static final String notesDir = "notes";
    public static final String TRAILBOOK_TAG = "trailbook";
    public static final long CLOUD_REFRESH_DEFAULT_TIME_DELTA = 5*60*1000;

    private static final int UPDATE_INTERVAL_IN_SECONDS = 0;
    public static final long UPDATE_INTERVAL = 1000 * UPDATE_INTERVAL_IN_SECONDS;
    private static final int FASTEST_INTERVAL_IN_SECONDS = 0;
    public static final long FASTEST_INTERVAL = 1000 * FASTEST_INTERVAL_IN_SECONDS;
    public static final float MIN_ACCURACY_TO_START_LEADING = 50; //meters
    public static final int MIN_CONNSECUTIVE_GOOD_LOCATIONS_TO_LEAD = 10;

    public static int MEDIUM_DETAIL = 500;
    public static int FULL_DETAIL = -1;

    public static final int MIN_DISTANCE_BETWEEN_POINTS=3; //distance in meters
    public static final int MAX_DISTANCE_BETWEEN_POINTS = 1000;
    public static final int IMAGE_CAPTURE_WIDTH = 1024;
}
