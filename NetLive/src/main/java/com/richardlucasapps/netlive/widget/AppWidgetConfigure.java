package com.richardlucasapps.netlive.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;

public class AppWidgetConfigure extends Activity {
	
	private static int mAppWidgetId;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, 
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        getFragmentManager().beginTransaction()
        .replace(android.R.id.content, new AppWidgetConfigurePreferencesFragment())
        .commit();
	}

    public static int getmAppWidgetId() {
		return mAppWidgetId;
	}
}
