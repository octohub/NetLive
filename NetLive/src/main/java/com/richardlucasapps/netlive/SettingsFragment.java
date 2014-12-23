package com.richardlucasapps.netlive;

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;



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
	

	
	private OnPreferenceChangeListener notificationDrawerUnitOfMeasurePreferenceListener = new OnPreferenceChangeListener(){

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {

            ((ListPreference) preference).setValue(newValue.toString());
            preference.setSummary(newValue.toString());
            getActivity().stopService(new Intent(getActivity(), MainService.class));
            getActivity().startService(new Intent(getActivity(), MainService.class));
            return false;
        }


    };

    private OnPreferenceChangeListener showTotalValuePreferenceListener = new OnPreferenceChangeListener(){

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            ((CheckBoxPreference) preference).setChecked((Boolean)newValue);
            getActivity().stopService(new Intent(getActivity(), MainService.class));
            getActivity().startService(new Intent(getActivity(), MainService.class));


            return false;
        }


    };


    private OnPreferenceChangeListener pollRatePreferenceListener = new OnPreferenceChangeListener(){

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            ((ListPreference) preference).setValue(newValue.toString());
            getActivity().stopService(new Intent(getActivity(), MainService.class));
            getActivity().startService(new Intent(getActivity(), MainService.class));

            return false;
        }


    };

    private OnPreferenceChangeListener activeAppPreferenceListener = new OnPreferenceChangeListener(){

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            ((CheckBoxPreference) preference).setChecked((Boolean)newValue);
            getActivity().stopService(new Intent(getActivity(), MainService.class));
            getActivity().startService(new Intent(getActivity(), MainService.class));


            return false;
        }


    };

    private OnPreferenceChangeListener hideNotificationIconPreferenceListener = new OnPreferenceChangeListener(){

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            ((CheckBoxPreference) preference).setChecked((Boolean)newValue);
            getActivity().stopService(new Intent(getActivity(), MainService.class));
            getActivity().startService(new Intent(getActivity(), MainService.class));


            return false;
        }


    };



    private OnPreferenceChangeListener disableCheckBoxPreferenceListener = new OnPreferenceChangeListener(){

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            ((CheckBoxPreference) preference).setChecked((Boolean)newValue);
            getActivity().stopService(new Intent(getActivity(), MainService.class));
            getActivity().startService(new Intent(getActivity(), MainService.class));

            return false;

        }
    };



}