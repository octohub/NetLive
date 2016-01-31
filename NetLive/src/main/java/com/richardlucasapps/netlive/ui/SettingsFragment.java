package com.richardlucasapps.netlive.ui;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import com.richardlucasapps.netlive.MainService;
import com.richardlucasapps.netlive.R;

public class SettingsFragment extends PreferenceFragment {

    private ListPreference notificationDrawerUnitOfMeasurePreference;
    private CheckBoxPreference disableCheckBoxPreference;
    private ListPreference pollRatePreference;
    private CheckBoxPreference activeAppPreference;
    private CheckBoxPreference showTotalValuePreference;
    private CheckBoxPreference hideNotificationIconPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT == android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            addPreferencesFromResource(R.xml.preferences_for_jelly_bean_mr2);
        } else {
            addPreferencesFromResource(R.xml.preferences);
        }

        notificationDrawerUnitOfMeasurePreference = (ListPreference) findPreference("pref_key_measurement_unit");
        notificationDrawerUnitOfMeasurePreference.setOnPreferenceChangeListener(notificationDrawerUnitOfMeasurePreferenceListener);
        notificationDrawerUnitOfMeasurePreference.setSummary(notificationDrawerUnitOfMeasurePreference.getValue().toString());

        showTotalValuePreference = (CheckBoxPreference) findPreference("pref_key_show_total_value");
        showTotalValuePreference.setOnPreferenceChangeListener(showTotalValuePreferenceListener);

        pollRatePreference = (ListPreference) findPreference("pref_key_poll_rate");
        pollRatePreference.setOnPreferenceChangeListener(pollRatePreferenceListener);

        activeAppPreference = (CheckBoxPreference) findPreference("pref_key_active_app");
        activeAppPreference.setOnPreferenceChangeListener(activeAppPreferenceListener);

        hideNotificationIconPreference = (CheckBoxPreference) findPreference("pref_key_hide_notification");
        hideNotificationIconPreference.setOnPreferenceChangeListener(hideNotificationIconPreferenceListener);

        disableCheckBoxPreference = (CheckBoxPreference) findPreference("pref_key_auto_start");
        disableCheckBoxPreference.setOnPreferenceChangeListener(disableCheckBoxPreferenceListener);
    }

    @Override
    public void onResume() {
        /*
        This is important to have because if the system or someone manually kills the MainService by going into the running apps
        and killing it, then when they open the SettingsFragment, the disabled checkmark will be unchecked.  We need to make sure
        that if the MainService is not running, disable is checked.
         */
        super.onResume();
        if (!isMyServiceRunning(MainService.class)) {
            disableCheckBoxPreference.setChecked(true);
        }

    }


    private OnPreferenceChangeListener notificationDrawerUnitOfMeasurePreferenceListener = new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            ((ListPreference) preference).setValue(newValue.toString());
            preference.setSummary(newValue.toString());
            restartService();
            return true;
        }
    };

    private OnPreferenceChangeListener showTotalValuePreferenceListener = new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            ((CheckBoxPreference) preference).setChecked((Boolean) newValue);
            restartService();
            return true;
        }
    };


    private OnPreferenceChangeListener pollRatePreferenceListener = new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            ((ListPreference) preference).setValue(newValue.toString());
            int pollValue = Integer.parseInt(newValue.toString());

            if (5 > pollValue) {
                Toast.makeText(getActivity(), getString(R.string.poll_rate_warning), Toast.LENGTH_LONG).show();
            }

            restartService();
            return true;
        }
    };

    private OnPreferenceChangeListener activeAppPreferenceListener = new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            boolean boolVal = (Boolean) newValue;
            ((CheckBoxPreference) preference).setChecked(boolVal);

            if (Build.VERSION.SDK_INT == android.os.Build.VERSION_CODES.JELLY_BEAN_MR2 && boolVal) {
                Toast.makeText(getActivity(), getString(R.string.enable_active_app_feature_jelly_bean_mr2), Toast.LENGTH_LONG).show();
            }
            restartService();
            return true;
        }


    };

    private OnPreferenceChangeListener hideNotificationIconPreferenceListener = new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            ((CheckBoxPreference) preference).setChecked((Boolean) newValue);
            restartService();
            return true;
        }
    };


    private OnPreferenceChangeListener disableCheckBoxPreferenceListener = new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            boolean checked = (Boolean) newValue;
            ((CheckBoxPreference) preference).setChecked(checked);
            if (checked) {
                getActivity().stopService(new Intent(getActivity(), MainService.class));
            } else {
                getActivity().startService(new Intent(getActivity(), MainService.class));
            }
            return true;
        }
    };

    private void restartService() {
        getActivity().stopService(new Intent(getActivity(), MainService.class));
        getActivity().startService(new Intent(getActivity(), MainService.class));
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}