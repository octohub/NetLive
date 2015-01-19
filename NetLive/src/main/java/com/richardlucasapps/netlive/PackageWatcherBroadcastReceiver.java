package com.richardlucasapps.netlive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Created by richard on 1/19/15.
 */
public class PackageWatcherBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("PackageWatcherBroadcastReceiver","Package Added");
        //TODO make sure this doesn't trigger even when NetLive is disabled
        Intent startServiceIntent = new Intent(context, MainService.class);
        startServiceIntent.putExtra("PACKAGE_ADDED", true);
        context.startService(startServiceIntent);

    }
}
