package com.trailbook.kole.helpers;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;

import com.trailbook.kole.state_objects.TrailBookState;

import java.util.Date;
import java.util.UUID;

/**
 * Created by kole on 9/8/2014.
 */
public class ApplicationUtils {

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
}
