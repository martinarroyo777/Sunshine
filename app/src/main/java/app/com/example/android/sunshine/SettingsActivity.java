package app.com.example.android.sunshine;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class SettingsActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener {

   @Override
    public void onCreate(Bundle savedInstanceState){
       super.onCreate(savedInstanceState);
       //Get information from pref_general file
       addPreferencesFromResource(R.xml.pref_general);
       //Find the default value for our location setting and bind it
       bindPreferenceSummaryToValue(findPreference(getString(R.string.key_location)));
       bindPreferenceSummaryToValue(findPreference(getString(R.string.key_units)));
   }

    /*
        Attaches a listener so the summary is always updated with the preference value.
        Also fires the listener once, to initialize the summary (so it shows up before the
        value is changed.)
     */

    private void bindPreferenceSummaryToValue(Preference preference){
        // Set the listener to watch for value changes
        preference.setOnPreferenceChangeListener(this);

        //Trigger the listener immediately with the preference's current value
        onPreferenceChange(preference,
                PreferenceManager
                    .getDefaultSharedPreferences(preference.getContext())
                    .getString(preference.getKey(), ""));


    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        String stringValue = o.toString();

        if (preference instanceof ListPreference){
            // For list preferences, look up the correct displa value in
            // the preference's 'entres' list (since they have separate labels/values
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(stringValue);
            if (prefIndex >= 0){
                preference.setSummary(listPreference.getEntries()[prefIndex]);
            }
            else{
                // For other preferences, set the summary to the value's simple string representation
                preference.setSummary(stringValue);
            }

        }
        return true;
    }
}
