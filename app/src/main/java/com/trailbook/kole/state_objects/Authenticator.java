package com.trailbook.kole.state_objects;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;

import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.trailbook.kole.activities.utils.Action;
import com.trailbook.kole.data.User;
import com.trailbook.kole.events.UserUpdatedEvent;

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

    @Override
    public void onConnected(Bundle bundle) {
        setUserDetails();
    }

    private void setUserDetails() {
        if (mGoogleApiClient.isConnected() && Plus.PeopleApi.getCurrentPerson(mGoogleApiClient) != null) {
            Person currentPerson = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
            String personName = currentPerson.getDisplayName();
            Person.Image personPhoto = currentPerson.getImage();
            User user = TrailBookState.getCurrentUser();
            if (personPhoto != null)
                user.profilePhotoUrl=personPhoto.getUrl();

            user.userName=personName;
            TrailBookState.getInstance().setUser(user);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (!mIntentInProgress && result.hasResolution()) {
            try {
                mIntentInProgress = true;
                result.startResolutionForResult(mActivity, REQUEST_CODE_SIGN_IN);
            } catch (IntentSender.SendIntentException e) {
                mIntentInProgress = false;
            }
        }
    }

    public void onReturnFromIntent() {
        mIntentInProgress = false;
    }

    public void connect() {
        mGoogleApiClient.connect();
    }

    public void disconnect() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    public void onGotAccount(String userId) {
        User user = TrailBookState.getCurrentUser();
        if (user == null) {
            user = new User();
        }
        user.userId = userId;
        BusProvider.getInstance().post(new UserUpdatedEvent(user));
        if (mActionOnAccountReceived != null)
            mActionOnAccountReceived.execute();
    }

    public void setActionOnAccountReceived(Action action) {
        this.mActionOnAccountReceived = action;
    }
}
