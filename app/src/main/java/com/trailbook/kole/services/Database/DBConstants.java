package com.trailbook.kole.services.database;

import org.json.JSONException;
import org.json.JSONObject;

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

    public static void populateFromJsonString(String jsonDbConstants) {
        try {
            JSONObject jsonObject = new JSONObject(jsonDbConstants);
            pathCollectionName = jsonObject.getString("pathCollectionName");
            segmentCollectionName = jsonObject.getString("segmentCollectionName");
            generalCommentsCollectionName = jsonObject.getString("generalCommentsCollectionName");
            attachedCommentsCollectionName = jsonObject.getString("attachedCommentsCollectionName");
            noteCollectionName = jsonObject.getString("noteCollectionName");
            username = jsonObject.getString("username");
            database = jsonObject.getString("database");
            password = jsonObject.getString("password").toCharArray();
            serverAddress = jsonObject.getString("serverAddress");
            port = jsonObject.getInt("port");
        } catch (JSONException e) {
        }
    }
}
