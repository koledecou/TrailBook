package com.trailbook.kole.fragments;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.trailbook.kole.activities.R;

/**
 * Created by kole on 7/19/2014.
 */
public class TBPreferenceFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preference);
    }
}