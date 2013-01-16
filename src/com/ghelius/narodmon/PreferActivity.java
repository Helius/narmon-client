package com.ghelius.narodmon;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.*;
import android.util.Log;

public class PreferActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener{

    final MyPreferenceFragment settingsFragment = new MyPreferenceFragment();
    private static final String TAG = "narodmon-pref";

    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, settingsFragment).commit();
    }

    public static class MyPreferenceFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preference_screen);
        }

        public void updateSummary () {
            Preference loginPref =  findPreference(this.getText(R.string.pref_key_login));
            EditTextPreference el = (EditTextPreference) loginPref;
            el.setSummary(el.getText());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Set up a listener whenever a key changes
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        // Setup the initial values
        settingsFragment.updateSummary();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the listener whenever a key changes
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onSharedPreferenceChanged (SharedPreferences sharedPreferences, String key) {
        // Let's do something a preference value changes
        Log.d(TAG,"onSharedPreferenceChanged: " + key);
        settingsFragment.updateSummary();
    }


}