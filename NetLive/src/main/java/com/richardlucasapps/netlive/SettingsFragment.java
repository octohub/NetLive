package com.richardlucasapps.netlive;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.util.Log;


public class SettingsFragment extends PreferenceFragment {

	ListPreference notificationDrawerUnitOfMeasurePreference;
    CheckBoxPreference disableCheckBoxPreference;
    ListPreference pollRatePreference;
    CheckBoxPreference activeAppPreference;
    CheckBoxPreference showTotalValuePreference;
    CheckBoxPreference hideNotificationIconPreference;


    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		
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
        Log.d("onResume SettingsFragment", "here");
        super.onResume();
        if(!isMyServiceRunning(MainService.class)){
            Log.d("Service Not Running", "about to set disabled as checked");
            disableCheckBoxPreference.setChecked(true);
        }

    }
	

	
	private OnPreferenceChangeListener notificationDrawerUnitOfMeasurePreferenceListener = new OnPreferenceChangeListener(){

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {

            ((ListPreference) preference).setValue(newValue.toString());
            preference.setSummary(newValue.toString());
            restartService();
            return true;
        }


    };

    private OnPreferenceChangeListener showTotalValuePreferenceListener = new OnPreferenceChangeListener(){

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            ((CheckBoxPreference) preference).setChecked((Boolean)newValue);
            restartService();


            return true;
        }


    };


    private OnPreferenceChangeListener pollRatePreferenceListener = new OnPreferenceChangeListener(){

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            ((ListPreference) preference).setValue(newValue.toString());
            restartService();

            return true;
        }


    };

    private OnPreferenceChangeListener activeAppPreferenceListener = new OnPreferenceChangeListener(){

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            ((CheckBoxPreference) preference).setChecked((Boolean)newValue);
            restartService();


            return true;
        }


    };

    private OnPreferenceChangeListener hideNotificationIconPreferenceListener = new OnPreferenceChangeListener(){

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            ((CheckBoxPreference) preference).setChecked((Boolean)newValue);
            restartService();


            return true;
        }


    };



    private OnPreferenceChangeListener disableCheckBoxPreferenceListener = new OnPreferenceChangeListener(){

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            Log.d("Settings Fragment", "disabled checkbox, in onPreferenceChange");
            ((CheckBoxPreference) preference).setChecked((Boolean)newValue);
            restartService();

            return true;

        }
    };

    private void restartService(){
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