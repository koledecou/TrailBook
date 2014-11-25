package com.trailbook.kole.activities.utils;

import android.app.Activity;
import android.util.Log;

import com.trailbook.kole.data.Constants;
import com.trailbook.kole.state_objects.Authenticator;

/**
 * Created by kole on 11/25/2014.
 */
public class LoginUtil {
    public static void authenticateForAction(Activity activity, Action uploadAction) {
        Authenticator.getInstance().setActionOnAccountReceived(uploadAction);
        authenticate(activity);
    }

    public static void authenticate(Activity activity) {
        Log.d(Constants.TRAILBOOK_TAG, "LoginUtil: getting account");
        Authenticator.getInstance().initializeAuthentication(activity);
        Authenticator.getInstance().pickUserAccount();
    }
}
