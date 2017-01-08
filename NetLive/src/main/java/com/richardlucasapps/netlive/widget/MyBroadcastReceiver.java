package com.richardlucasapps.netlive.widget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.richardlucasapps.netlive.gauge.GaugeService;
import com.richardlucasapps.netlive.global.MyApplication;

public class MyBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        //this will always autostart and at least check if notification or widget enabled, if not, it destroys
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MyApplication.getInstance());
        boolean widgetExist = sharedPref.getBoolean("widget_exists", false);
        boolean autoStart = !(sharedPref.getBoolean("pref_key_auto_start", false));

        if (widgetExist || autoStart) {

            Intent startServiceIntent = new Intent(context, GaugeService.class);
            context.startService(startServiceIntent);
        }
    }
}
