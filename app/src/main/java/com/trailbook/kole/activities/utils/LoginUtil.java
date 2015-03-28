package com.trailbook.kole.activities.utils;

import android.app.Activity;

import com.trailbook.kole.state_objects.Authenticator;

/**
 * Created by kole on 11/25/2014.
 */
public class LoginUtil {
    public static void authenticateForAction(Activity activity, Action action) {
        Authenticator.getInstance().setActionOnAccountReceived(action);
        authenticate(activity);
    }

    public static void authenticate(Activity activity) {
        Authenticator.getInstance().initializeAuthentication(activity);
        Authenticator.getInstance().pickUserAccount();
    }
}
