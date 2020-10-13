package by.ingman.sevenlis.ice_v3.activities;

import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import by.ingman.sevenlis.ice_v3.R;
import by.ingman.sevenlis.ice_v3.utils.SettingsUtils;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }
    
    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    
    public static class SettingsFragment extends PreferenceFragment {
        @SuppressWarnings("deprecation")
        public SettingsFragment() {}

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);
    
            final Preference intervalPreference = this.findPreference(SettingsUtils.PREF_LOCATION_TRACKING_INTERVAL);
            intervalPreference.setEnabled(SettingsUtils.Settings.getLocationTrackingType(getActivity().getApplication().getBaseContext()).equals(SettingsUtils.LOCATION_TRACKING_TYPE_PERIOD));
            final Preference typePreference = this.findPreference(SettingsUtils.PREF_LOCATION_TRACKING_TYPE);
            typePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                intervalPreference.setEnabled(newValue.equals(SettingsUtils.LOCATION_TRACKING_TYPE_PERIOD));
                return true;
            });

            final MultiSelectListPreference locationTrackerProviders = (MultiSelectListPreference) this.findPreference(SettingsUtils.PREF_LOCATION_TRACKING_PROVIDERS);
            locationTrackerProviders.setEntries(getActivity().getApplicationContext().getResources().getTextArray(R.array.location_tracking_providers_names));
            locationTrackerProviders.setEntryValues(getActivity().getApplicationContext().getResources().getTextArray(R.array.location_tracking_providers_values));
            locationTrackerProviders.setDefaultValue(SettingsUtils.LOCATION_TRACKING_PROVIDER_GPS);
    
            /*if (Build.VERSION.SDK_INT <= 16) {
                this.getPreferenceScreen().removePreference(this.findPreference("orderDaysAhead"));
            }*/
        }
    
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            setHasOptionsMenu(true);
            return super.onCreateView(inflater, container, savedInstanceState);
        }
    }
}
