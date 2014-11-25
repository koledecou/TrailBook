package com.trailbook.kole.state_objects;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;
import com.trailbook.kole.activities.utils.Action;
import com.trailbook.kole.data.Constants;

/**
 * Created by kole on 10/4/2014.
 */
public class Authenticator  implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener  {
    public static final int REQUEST_CODE_SIGN_IN = 12;
    public static final int REQUEST_CODE_PICK_ACCOUNT= 13;
    private GoogleApiClient mGoogleApiClient;
    private boolean mIntentInProgress;
    private Activity mActivity;

    private static final Authenticator INSTANCE = new Authenticator();
    private Action mActionOnAccountReceived;

    public static Authenticator getInstance() { return INSTANCE;}

    public void initializeAuthentication(Activity activity) {
        this.mActivity = activity;
        mGoogleApiClient = new GoogleApiClient.Builder(activity)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .build();
    }

    public void pickUserAccount() {
        String[] accountTypes = new String[]{"com.google"};
        Intent intent = AccountPicker.newChooseAccountIntent(null, null,
                accountTypes, false, null, null, null, null);
        mActivity.startActivityForResult(intent, REQUEST_CODE_PICK_ACCOUNT);
    }

    public String getUserName () {
        return TrailBookState.getInstance().getCurrentUserId();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ": connected." + bundle);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ": connection suspended, trying again");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ": connection failed." + result  + "error code: " + result.getErrorCode());
        if (!mIntentInProgress && result.hasResolution()) {
            Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ": has resolution.  fix?");
            try {
                mIntentInProgress = true;
                result.startResolutionForResult(mActivity, REQUEST_CODE_SIGN_IN);
            } catch (IntentSender.SendIntentException e) {
                Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + "sendIntentException", e);
                mIntentInProgress = false;
            }
        }
    }

    public void onReturnFromIntent() {
        Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ": onReturnFromIntent()");
        mIntentInProgress = false;

/*        if (!mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.connect();
        }*/
    }

    public void connect() {
        Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ": connecting");
        mGoogleApiClient.connect();
    }

    public void disconnect() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ": disconnecting");
            mGoogleApiClient.disconnect();
        }
    }

    public void onGotAccount(String userName) {
        Log.d(Constants.TRAILBOOK_TAG, getClass().getSimpleName() + ": got user name:" + userName);
        TrailBookState.getInstance().setUserId(userName);
        if (mActionOnAccountReceived != null)
            mActionOnAccountReceived.execute();
    }

    public void setActionOnAccountReceived(Action action) {
        this.mActionOnAccountReceived = action;
    }
}
