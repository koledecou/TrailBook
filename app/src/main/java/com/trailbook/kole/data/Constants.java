package com.trailbook.kole.data;

/**
 * Created by Fistik on 6/30/2014.
 */
public class Constants {
    public static final String delimiter = "--";
    public static final String uploadImage = "/uploadImage.py";
    public static final String tempDir = "temp";
    public static final String pathsRootDir = "paths";
    public static final String cachedPathsRootDir = "temp_cloud_paths";
    public static final String segmentsDir = "segments";
    public static final String imageDir = "images";
    public static final String notesDir = "notes";
    public static final String commentsDir = "comments";
    public static final String keyHashFile = "keys.txt";
    public static final String TRAILBOOK_TAG = "trailbook";
    public static final long CLOUD_REFRESH_DEFAULT_TIME_DELTA = 24*60*60*1000; //24 hours

    private static final int UPDATE_INTERVAL_IN_SECONDS = 0;
    public static final long UPDATE_INTERVAL = 1000 * UPDATE_INTERVAL_IN_SECONDS;
    private static final int FASTEST_INTERVAL_IN_SECONDS = 0;
    public static final long FASTEST_INTERVAL = 1000 * FASTEST_INTERVAL_IN_SECONDS;
    public static final float MIN_ACCURACY_TO_START_LEADING = 50; //meters
    public static final int MIN_CONNSECUTIVE_GOOD_LOCATIONS_TO_LEAD = 10;
    public static final String DEFAULT_USER_NAME = "Anonymous";
    public static final String FILTER_PREFS_SHOW_MY_PATHS_KEY = "SHOW_MY_PATHS_KEY";
    public static final String FILTER_PREFS_SHOW_OTHER_PATHS_KEY = "FILTER_PREFS_SHOW_OTHER_PATHS_KEY";
    public static final String FILTER_PREFS_GROUPS_TO_SHOW_KEY = "FILTER_PREFS_GROUPS_TO_SHOW_KEY";

    public static final int MIN_DISTANCE_BETWEEN_POINTS=3; //distance in meters
    public static final int MAX_DISTANCE_BETWEEN_POINTS = 1000;
    public static final int IMAGE_CAPTURE_WIDTH = 1024;
}
