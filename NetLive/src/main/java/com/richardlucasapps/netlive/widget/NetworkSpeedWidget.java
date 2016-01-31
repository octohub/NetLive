package com.richardlucasapps.netlive.widget;

import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.richardlucasapps.netlive.MainService;
import com.richardlucasapps.netlive.MyApplication;


public class NetworkSpeedWidget extends AppWidgetProvider {

    /*This onUpdate is called when the widget is first configured, and also updates over
    the interval defined in the updatePeriodMillis in my widget_details.xml file.
    Though, remember, this can only work every 15 minutes, which is why I must
    start a service, to update every second.
    */


    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {

        MyApplication.getInstance().stopService(new Intent(MyApplication.getInstance(), MainService.class));
        MyApplication.getInstance().startService(new Intent(MyApplication.getInstance(), MainService.class));
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onDisabled(Context context) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MyApplication.getInstance());
        SharedPreferences.Editor edit = sharedPref.edit();
        edit.putBoolean("widget_exists",false);
        edit.apply();
        MyApplication.getInstance().stopService(new Intent(MyApplication.getInstance(),MainService.class));
        MyApplication.getInstance().startService(new Intent(MyApplication.getInstance(), MainService.class));
        super.onDisabled(context);
    }
}	