package com.trailbook.kole.helpers;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;

import com.trailbook.kole.activities.R;
import com.trailbook.kole.state_objects.PathManager;

import java.util.Date;
import java.util.UUID;

/**
 * Created by kole on 9/8/2014.
 */
public class ApplicationUtils {
    public static final int MENU_CONTEXT_DELETE_ID = 1;
    public static final int MENU_CONTEXT_UPLOAD_ID = 2;
    public static final int MENU_CONTEXT_FOLLOW_ID = 3;
    public static final int MENU_CONTEXT_TO_START_ID = 4;
    public static final int MENU_CONTEXT_EDIT_ID = 5;
    public static final int MENU_CONTEXT_RESUME_ID = 6;
    public static final int MENU_CONTEXT_ZOOM_ID = 7;
    public static final int MENU_CONTEXT_DOWNLOAD_ID = 8;
    public static final int MENU_CONTEXT_DELETE_FROM_CLOUD_ID = 9;
    public static final int MENU_CONTEXT_DISMISS_ID = 10;

    public static final int DELETE_TEXT = R.string.delete;
    public static final int UPLOAD_TEXT = R.string.upload;
    public static final int FOLLOW_TEXT = R.string.follow;
    public static final int TO_START_TEXT = R.string.to_start;
    public static final int EDIT_TEXT = R.string.edit;
    public static final int RESUME_TEXT = R.string.resume;
    public static final int ZOOM_TEXT = R.string.zoom;
    public static final int DOWNLOAD_TEXT = R.string.download;
    public static final int DELETE_FROM_CLOUD_TEXT = R.string.delete_from_cloud;
    public static final int REFRESH_TEXT = R.string.refresh;

    public static void showAlert(Context context,
                                 DialogInterface.OnClickListener clickListenerOK,
                                 String title,
                                 String message,
                                 String OK,
                                 String cancel) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

        // set title
        alertDialogBuilder.setTitle(title);

        // set dialog message
        alertDialogBuilder
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(OK,clickListenerOK);
        if (cancel != null) {
            alertDialogBuilder.setNegativeButton(cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    //just dismiss the dialog
                }
            });
        }
        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show(); 
    }

    public static String getDeviceId(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        String deviceId = prefs.getString("DEVICEID", "-1");
        if ("-1".equalsIgnoreCase(deviceId)) {
            UUID deviceUid = UUID.randomUUID();
            deviceId = deviceUid.toString();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("DEVICEID", deviceId);
            editor.commit();
        }
        return deviceId;
    }

    public static long getCurrentTimeStamp() {
        return new Date().getTime();
    }

    public static void disableButton(Button button) {
        button.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
        button.setEnabled(false);
    }

    public static void addPathActionMenuItems(Menu menu, String id) {
        if (PathManager.getInstance().isStoredLocally(id)) {
            addDownloadedPathMenuItems(menu, id);
        }

        if (PathManager.getInstance().isPathInCloudCache(id)){
            addCloudPathMenuItems(menu,id);
        }
    }

    public static void addDownloadedPathMenuItems(Menu m, String id) {
        m.add(Menu.NONE, MENU_CONTEXT_FOLLOW_ID, Menu.NONE, FOLLOW_TEXT);
        m.add(Menu.NONE, MENU_CONTEXT_TO_START_ID, Menu.NONE, TO_START_TEXT);
        m.add(Menu.NONE, MENU_CONTEXT_DELETE_ID, Menu.NONE, DELETE_TEXT);
        if (TrailbookPathUtilities.hasEditPermissions(id)) {
            m.add(Menu.NONE, MENU_CONTEXT_EDIT_ID, Menu.NONE, EDIT_TEXT);
            m.add(Menu.NONE, MENU_CONTEXT_RESUME_ID, Menu.NONE, RESUME_TEXT);
            m.add(Menu.NONE, MENU_CONTEXT_UPLOAD_ID, Menu.NONE, UPLOAD_TEXT);
        }

        m.add(Menu.NONE, MENU_CONTEXT_ZOOM_ID, Menu.NONE, ZOOM_TEXT);
    }

    public static void addCloudPathMenuItems(Menu m, String id) {
        if (PathManager.getInstance().isStoredLocally(id))
            m.add(Menu.NONE, MENU_CONTEXT_DOWNLOAD_ID, Menu.NONE, REFRESH_TEXT);
        else
            m.add(Menu.NONE, MENU_CONTEXT_DOWNLOAD_ID, Menu.NONE, DOWNLOAD_TEXT);

        if (m.findItem(MENU_CONTEXT_ZOOM_ID) == null) {
            m.add(Menu.NONE, MENU_CONTEXT_ZOOM_ID, Menu.NONE, ZOOM_TEXT);
        }
        if (m.findItem(MENU_CONTEXT_TO_START_ID) == null) {
            m.add(Menu.NONE, MENU_CONTEXT_TO_START_ID, Menu.NONE, TO_START_TEXT);
        }
        if (TrailbookPathUtilities.hasEditPermissions(id)) {
            m.add(Menu.NONE, MENU_CONTEXT_DELETE_FROM_CLOUD_ID, Menu.NONE, DELETE_FROM_CLOUD_TEXT);
        }
    }

    public static void showActionsPopupForPath(Context c, View v, PopupMenu.OnMenuItemClickListener listener, String pathId) {
        PopupMenu popup = new PopupMenu(c, v);
        popup.setOnMenuItemClickListener(listener);
        addPathActionMenuItems(popup.getMenu(), pathId);
        popup.show();
    }
}
