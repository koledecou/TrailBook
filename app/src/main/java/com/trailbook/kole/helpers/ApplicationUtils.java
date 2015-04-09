package com.trailbook.kole.helpers;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.trailbook.kole.activities.R;
import com.trailbook.kole.activities.TrailBookActivity;
import com.trailbook.kole.data.KeyWord;
import com.trailbook.kole.data.Path;
import com.trailbook.kole.data.PathSegment;
import com.trailbook.kole.data.PointAttachedObject;
import com.trailbook.kole.events.MapObjectAddedEvent;
import com.trailbook.kole.events.SegmentUpdatedEvent;
import com.trailbook.kole.state_objects.BusProvider;
import com.trailbook.kole.state_objects.PathManager;

import java.io.File;
import java.util.ArrayList;
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
    public static final int MENU_CONTEXT_SHARE_ID = 11;

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
    private static final int SHARE_TEXT = R.string.share;


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

    public static void addPathActionMenuItemsForSearchResults(Menu menu, String id) {
        if (PathManager.getInstance().isStoredLocally(id)) {
            addDownloadedPathMenuItemsForSearchResults(menu, id);
        } else if (PathManager.getInstance().isPathInCloudCache(id)){
            addCloudPathMenuItemsForSearchResults(menu,id);
        }
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
        m.add(Menu.NONE, MENU_CONTEXT_SHARE_ID, Menu.NONE, SHARE_TEXT);
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

    public static void addDownloadedPathMenuItemsForSearchResults(Menu m, String id) {
        m.add(Menu.NONE, MENU_CONTEXT_FOLLOW_ID, Menu.NONE, FOLLOW_TEXT);
        m.add(Menu.NONE, MENU_CONTEXT_TO_START_ID, Menu.NONE, TO_START_TEXT);
        m.add(Menu.NONE, MENU_CONTEXT_ZOOM_ID, Menu.NONE, ZOOM_TEXT);
    }

    public static void addCloudPathMenuItemsForSearchResults(Menu m, String id) {
        m.add(Menu.NONE, MENU_CONTEXT_DOWNLOAD_ID, Menu.NONE, DOWNLOAD_TEXT);
        m.add(Menu.NONE, MENU_CONTEXT_ZOOM_ID, Menu.NONE, ZOOM_TEXT);
    }

    public static void showActionsPopupForPath(Context c, View v, PopupMenu.OnMenuItemClickListener listener, String pathId) {
        PopupMenu popup = new PopupMenu(c, v);
        popup.setOnMenuItemClickListener(listener);
        addPathActionMenuItems(popup.getMenu(), pathId);
        popup.show();
    }

    public static void sendFileViaEmail(Context c, File file) {
        try
        {
            file.setReadable(true, false);
            Uri uri = Uri.fromFile(file);

            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_EMAIL, "");
            i.putExtra(Intent.EXTRA_SUBJECT,"Trailbook Path");
            i.putExtra(Intent.EXTRA_TEXT,"I have sent you a path to follow.  The attachment can be opened by the Trailbook android app.  For instructions visit http://www.thetrailbook.com");
            i.putExtra(Intent.EXTRA_STREAM, uri);

            c.startActivity(Intent.createChooser(i, "Select application"));
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static String getLabelForKeywordType(int type) {
        switch (type) {
            case KeyWord.CLIMB:
                return "Climb: ";
            case KeyWord.CRAG:
                return "Crag: ";
            case KeyWord.REGION:
                return "Region: ";
            case KeyWord.PATH:
                return "Path: ";
            default:
                return "";
        }
    }

    public static boolean isFragmentShowing(FragmentManager fm, String tag) {
        Fragment f = fm.findFragmentByTag(tag);
        if (f != null && f.isVisible()) {
            return true;
        } else {
            return false;
        }
    }

    public static void toastGreen(Activity activity, int resId) {
        Toast toast = Toast.makeText(activity, resId, Toast.LENGTH_LONG);
        TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
        v.setBackgroundColor(Color.GREEN);
        toast.show();

    }

    public static boolean isCreateNoteDialogShowing(FragmentManager fm) {
        if (fm.findFragmentByTag(TrailBookActivity.ADD_NOTE_FRAG_TAG) != null){
            return true;
        } else {
            return false;
        }
    }

    public static boolean isNetworkConnected(Activity activity) {
        ConnectivityManager connMgr = (ConnectivityManager)
                activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        } else {
            return false;
        }
    }


    public static void showNoNetworkStatusDialog(Activity activity, String message) {
        DialogInterface.OnClickListener clickListenerOK = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int id) {
            }
        };
        ApplicationUtils.showAlert(activity, clickListenerOK, activity.getString(R.string.no_network_title),
                message,
                activity.getString(R.string.OK), null);
    }

    public static ArrayList<String> StringToArrayList(String string) {
        ArrayList result = new ArrayList<String>();
        result.add(string);
        return result;
    }

    public static void hideSoftKeyboard(Activity a) {
        if(a.getCurrentFocus()!=null) {
            InputMethodManager inputMethodManager = (InputMethodManager) a.getSystemService(Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(a.getCurrentFocus().getWindowToken(), 0);
        }
    }

    public static void postPathEvents(Path path) {
        if (path.paObjects != null) {
            for (PointAttachedObject pao : path.paObjects) {
                BusProvider.getInstance().post(new MapObjectAddedEvent(pao));
            }
        }

        if (path.segments != null) {
            for (PathSegment segment : path.segments) {
                BusProvider.getInstance().post(new SegmentUpdatedEvent(segment));
            }
        }
    }
}
