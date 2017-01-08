package com.richardlucasapps.netlive.packagewatcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import com.richardlucasapps.netlive.gauge.GaugeService;

public class PackageWatcherBroadcastReceiver extends BroadcastReceiver {

  @Override public void onReceive(Context context, Intent intent) {
    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
    boolean notificationEnabled = !(sharedPref.getBoolean("pref_key_auto_start", false));
    boolean widgetExist = sharedPref.getBoolean("widget_exists", false);
    if (!notificationEnabled && !widgetExist) {
      return;
    }
    Intent startServiceIntent = new Intent(context, GaugeService.class);
    Bundle extras = intent.getExtras();
    String uid = null;

    if (extras != null) {
      uid = extras.getString("EXTRA_UID", null);
    }
    if (uid != null && !uid.isEmpty()) {
      startServiceIntent.putExtra("EXTRA_UID", Integer.parseInt(uid));
    }
    //TODO make sure this doesn't trigger even when NetLive is disabled

    startServiceIntent.putExtra("PACKAGE_ADDED", true);
    context.startService(startServiceIntent);
  }
}
