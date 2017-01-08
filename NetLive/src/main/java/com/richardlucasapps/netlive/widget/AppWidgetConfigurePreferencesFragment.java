package com.richardlucasapps.netlive.widget;

import android.app.Activity;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.richardlucasapps.netlive.gauge.GaugeService;
import com.richardlucasapps.netlive.global.MyApplication;
import com.richardlucasapps.netlive.R;

public class AppWidgetConfigurePreferencesFragment extends PreferenceFragment {

    private int mAppWidgetId;
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor edit;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.widget_preference);

        PreferenceScreen addWidgetPreference = (PreferenceScreen) findPreference("pref_key_widget_add_widget_preference_screen");
        addWidgetPreference.setOnPreferenceClickListener(addWidgetPreferenceListener);
        mAppWidgetId = AppWidgetConfigure.getmAppWidgetId();

        sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        edit = sharedPref.edit();

        ListPreference widgetUnitOfMeasure = (ListPreference) findPreference("pref_key_widget_measurement_unit");
        widgetUnitOfMeasure.setOnPreferenceChangeListener(widgetUnitOfMeasureListener);
        widgetUnitOfMeasure.setSummary(widgetUnitOfMeasure.getValue());

        ListPreference widgetFontColor = (ListPreference) findPreference("pref_key_widget_font_color");
        widgetFontColor.setOnPreferenceChangeListener(widgetFontColorListener);
        widgetFontColor.setSummary(widgetFontColor.getEntry().toString());

        ListPreference widgetFontSize = (ListPreference) findPreference("pref_key_widget_font_size");
        widgetFontSize.setOnPreferenceChangeListener(widgetFontSizeListener);
        widgetFontSize.setSummary(widgetFontSize.getEntry());
    }

    private final OnPreferenceChangeListener widgetFontSizeListener = new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            ((ListPreference) preference).setValue(newValue.toString());
            String fontSizeEntry = (String) ((ListPreference) preference).getEntry();
            preference.setSummary(fontSizeEntry);
            return true;
        }
    };

    private final OnPreferenceChangeListener widgetUnitOfMeasureListener = new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            ((ListPreference) preference).setValue(newValue.toString());
            preference.setSummary(newValue.toString());
            return true;
        }
    };

    private final OnPreferenceChangeListener widgetFontColorListener = new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            ((ListPreference) preference).setValue(newValue.toString());
            String fontColorEntry = (String) ((ListPreference) preference).getEntry();
            preference.setSummary(fontColorEntry);
            return true;
        }
    };

    private final OnPreferenceClickListener addWidgetPreferenceListener = new OnPreferenceClickListener() {

        @Override
        public boolean onPreferenceClick(Preference arg0) {
            String unitOfMeasure = sharedPref.getString("pref_key_widget_measurement_unit", "Mbps");

            boolean displayActiveApp = sharedPref.getBoolean("pref_key_widget_active_app", true);
            String styleOfFont = sharedPref.getString("pref_key_widget_font_style", null);
            String sizeOfFont = sharedPref.getString("pref_key_widget_font_size", "12");
            String colorOfFont = sharedPref.getString("pref_key_widget_font_color", null);
            boolean showTotalValue = sharedPref.getBoolean("pref_key_widget_show_total", false);

            edit.putString("pref_key_widget_measurement_unit" + mAppWidgetId, unitOfMeasure);

            edit.putBoolean("pref_key_widget_active_app" + mAppWidgetId, displayActiveApp);
            edit.putBoolean("pref_key_widget_show_total" + mAppWidgetId, showTotalValue);

            edit.putString("pref_key_widget_font_style" + mAppWidgetId, styleOfFont);
            edit.putString("pref_key_widget_font_size" + mAppWidgetId, sizeOfFont);
            edit.putString("pref_key_widget_font_color" + mAppWidgetId, colorOfFont);

            edit.putBoolean("widget_exists", true);
            edit.commit();
            Intent result = new Intent();
            result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            getActivity().setResult(Activity.RESULT_OK, result);
            getActivity().finish();
            MyApplication.getInstance().stopService(new Intent(MyApplication.getInstance(), GaugeService.class));
            MyApplication.getInstance().startService(new Intent(MyApplication.getInstance(), GaugeService.class));

            return true;
        }

    };

    private static void initializeActionBar(PreferenceScreen preferenceScreen) {
        final Dialog dialog = preferenceScreen.getDialog();

        if (dialog != null) {
            // Inialize the action bar
            dialog.getActionBar().setDisplayHomeAsUpEnabled(true);

            // Apply custom home button area click listener to close the PreferenceScreen because PreferenceScreens are dialogs which swallow
            // events instead of passing to the activity
            // Related Issue: https://code.google.com/p/android/issues/detail?id=4611
            View homeBtn = dialog.findViewById(android.R.id.home);

            if (homeBtn != null) {
                OnClickListener dismissDialogClickListener = new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                };

                // Prepare yourselves for some hacky programming
                ViewParent homeBtnContainer = homeBtn.getParent();

                // The home button is an ImageView inside a FrameLayout
                if (homeBtnContainer instanceof FrameLayout) {
                    ViewGroup containerParent = (ViewGroup) homeBtnContainer.getParent();

                    if (containerParent instanceof LinearLayout) {
                        // This view also contains the title text, set the whole view as clickable
                        ((LinearLayout) containerParent).setOnClickListener(dismissDialogClickListener);
                    } else {
                        // Just set it on the home button
                        ((FrameLayout) homeBtnContainer).setOnClickListener(dismissDialogClickListener);
                    }
                } else {
                    // The 'If all else fails' default case
                    homeBtn.setOnClickListener(dismissDialogClickListener);
                }
            }
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        super.onPreferenceTreeClick(preferenceScreen, preference);

        // If the user has clicked on a preference screen, set up the action bar
        if (preference instanceof PreferenceScreen) {
            initializeActionBar((PreferenceScreen) preference);
        }

        return false;
    }

}