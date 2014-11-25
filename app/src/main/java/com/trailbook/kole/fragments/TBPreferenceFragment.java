package com.trailbook.kole.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.trailbook.kole.activities.R;
import com.trailbook.kole.activities.utils.LoginUtil;
import com.trailbook.kole.helpers.PreferenceUtilities;
import com.trailbook.kole.state_objects.TrailBookState;

/**
 * Created by kole on 7/19/2014.
 */
public class TBPreferenceFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preference);
        setCurrentUserIdPref();
        setCurrentUOM();
        setOffRouteTriggerDistance();
        setNoteTriggerDistance();
        setMapType();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister the listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
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

    private void setCurrentUserIdPref() {
        Preference savedUserIdDisplayPreference = (Preference)findPreference("SAVED_USER_ID_DISPLAY");
        savedUserIdDisplayPreference.setOnPreferenceClickListener(this);
        String userId = TrailBookState.getCurrentUserId();
        if (userId != null || userId == "-1")
            savedUserIdDisplayPreference.setSummary(userId);
        else
            savedUserIdDisplayPreference.setSummary(R.string.no_account);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey().equalsIgnoreCase("SAVED_USER_ID_DISPLAY")) {
            LoginUtil.authenticate(getActivity());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        setCurrentUserIdPref();
        setCurrentUOM();
        setOffRouteTriggerDistance();
        setNoteTriggerDistance();
        setMapType();
    }
}