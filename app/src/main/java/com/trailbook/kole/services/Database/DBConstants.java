package com.trailbook.kole.services.database;

/**
 * Created by kole on 9/19/2014.
 */
public class DBConstants {
    public static String pathCollectionName = "paths";
    public static String segmentCollectionName = "segments";
    public static String generalCommentsCollectionName = "generalComments";
    public static String attachedCommentsCollectionName = "pointAttachedComments";
    public static String noteCollectionName = "pointObjects";

    public static String username = "trailbook";
    public static String database = "trailbook";
    public static char[] password = "trailbook".toCharArray();

    public static String serverAddress = "kahana.mongohq.com";
    public static int port = 10017;
}
