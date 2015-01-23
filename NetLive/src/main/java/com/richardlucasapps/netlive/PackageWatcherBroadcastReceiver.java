package com.richardlucasapps.netlive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Created by richard on 1/19/15.
 */
public class PackageWatcherBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("PackageWatcherBroadcastReceiver","Package Added");
        Intent startServiceIntent = new Intent(context, MainService.class);
        Bundle extras = intent.getExtras();
        String uid = null;
        if(extras!=null){
            uid = extras.getString("EXTRA_UID",null);

        }
        if(uid!=null && !uid.isEmpty()){
            startServiceIntent.putExtra("EXTRA_UID", uid);
        }
        //TODO make sure this doesn't trigger even when NetLive is disabled

        startServiceIntent.putExtra("PACKAGE_ADDED", true);
        context.startService(startServiceIntent);

    }
}
