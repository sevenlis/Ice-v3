package by.ingman.sevenlis.ice_v3.activities;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

import by.ingman.sevenlis.ice_v3.R;
import by.ingman.sevenlis.ice_v3.utils.SettingsUtils;

public class SettingsActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }
    
    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);
    
            final Preference intervalPreference = this.findPreference(SettingsUtils.PREF_LOCATION_TRACKING_INTERVAL);
            intervalPreference.setEnabled(SettingsUtils.Settings.getLocationTrackingType(getActivity().getApplication().getBaseContext()).equals(SettingsUtils.LOCATION_TRACKING_TYPE_PERIOD));
            final Preference typePreference = this.findPreference(SettingsUtils.PREF_LOCATION_TRACKING_TYPE);
            typePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    intervalPreference.setEnabled(newValue.equals(SettingsUtils.LOCATION_TRACKING_TYPE_PERIOD));
                    return true;
                }
            });
    
            /*if (Build.VERSION.SDK_INT <= 16) {
                this.getPreferenceScreen().removePreference(this.findPreference("orderDaysAhead"));
            }*/
        }
    }
}
