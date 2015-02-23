package com.trailbook.kole.fragments;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

import com.squareup.otto.Bus;
import com.trailbook.kole.activities.R;
import com.trailbook.kole.data.Constants;
import com.trailbook.kole.data.User;
import com.trailbook.kole.fragments.dialogs.SetUserDialogFragment;
import com.trailbook.kole.helpers.PreferenceUtilities;
import com.trailbook.kole.state_objects.BusProvider;
import com.trailbook.kole.state_objects.TrailBookState;

/**
 * Created by kole on 7/19/2014.
 */
public class TBPreferenceFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener, SharedPreferences.OnSharedPreferenceChangeListener, SetUserDialogFragment.SetUserDialogListener {
    private Bus bus;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preference);
        setCurrentUserIdPrefFromSavedState();
        setCurrentUOM();
        setOffRouteTriggerDistance();
        setNoteTriggerDistance();
        setMapType();
        bus = BusProvider.getInstance();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
        bus.register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister the listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
        bus.register(this);
    }

    private void setMapType() {
        ListPreference mapTypePref = (ListPreference)findPreference("map_type_pref");
        mapTypePref.setSummary(mapTypePref.getEntry());
    }

    private void setOffRouteTriggerDistance() {
        Preference distPref = (Preference)findPreference("off_route_distance_pref");
        int dist = PreferenceUtilities.getStrayFromPathTriggerDistance(getActivity());
        String displayDist = "";
        if (dist == Integer.MAX_VALUE) {
            displayDist = getString(R.string.not_enabled);
        } else {
            displayDist = String.valueOf(dist);
            displayDist = getDistanceDisplayString(displayDist);
        }

        distPref.setSummary(displayDist);
    }

    private String getDistanceDisplayString(String displayDist) {
        if (PreferenceUtilities.isUSUnitsPreferred(getActivity())) {
            displayDist = displayDist + " " + getString(R.string.feet);
        } else {
            displayDist = displayDist + " " + getString(R.string.meters);
        }
        return displayDist;
    }

    private void setNoteTriggerDistance() {
        Preference distPref = (Preference)findPreference("triggerRadiusClose");
        int dist = PreferenceUtilities.getNoteAlertDistance(getActivity());
        String displayDist = "";
        if (dist == Integer.MIN_VALUE) {
            displayDist = getString(R.string.not_enabled);
        } else {
            displayDist = String.valueOf(dist);
            displayDist = getDistanceDisplayString(displayDist);
        }

        distPref.setSummary(displayDist);
    }

    private void setCurrentUOM() {
        Preference uomPreference = (Preference)findPreference("unit_of_measure_pref");
        if (PreferenceUtilities.isUSUnitsPreferred(getActivity())) {
            uomPreference.setSummary(R.string.uom_items_us);
        } else {
            uomPreference.setSummary(R.string.uom_items_metric);
        }
    }

    private void setCurrentUserIdPrefFromSavedState() {
        User user = TrailBookState.getCurrentUser();
        setCurrentUserIdDisplay(user);
    }

    private void setCurrentUserIdDisplay(User user) {
        if (user != null) {
            Preference savedUserIdDisplayPreference = (Preference) findPreference("SAVED_USER_ID_DISPLAY");
            savedUserIdDisplayPreference.setOnPreferenceClickListener(this);
            String userId = user.userId;
            Log.d(Constants.TRAILBOOK_TAG, "TBPreferenceFragment: setting user " + userId);
            //savedUserIdDisplayPreference.setIcon(R.drawable.ic_google_plus);
            savedUserIdDisplayPreference.setSummary(
                    getUserNameForDisplay(user) +
                            System.getProperty("line.separator") +
                            getUserIdForDisplay(user)
            );
        }
    }

    private String getUserNameForDisplay(User u) {
        if (u.userName == null || u.userName.length()<1)
            return Constants.DEFAULT_USER_NAME;
        else
            return u.userName;
    }

    private String getUserIdForDisplay(User u) {
        if (u.userId == null || u.userId.equalsIgnoreCase("-1") || u.userId.equalsIgnoreCase("")) {
            return getResources().getString(R.string.no_account);
        } else {
            return u.userId;
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey().equalsIgnoreCase("SAVED_USER_ID_DISPLAY")) {
            showSetUserDialog();
            return true;
        } else {
            return false;
        }
    }

    private void showSetUserDialog() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("edit_user_dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        SetUserDialogFragment newFragment = SetUserDialogFragment.newInstance(R.string.set_user_title, TrailBookState.getCurrentUser());
        newFragment.setListener(this);
        newFragment.show(ft, "edit_user_dialog");
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        setCurrentUserIdPrefFromSavedState();
        setCurrentUOM();
        setOffRouteTriggerDistance();
        setNoteTriggerDistance();
        setMapType();
    }

    @Override
    public void onUserUpdated(User user) {
        TrailBookState.getInstance().setUser(user);
        setCurrentUserIdDisplay(user);
    }
}