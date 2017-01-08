package com.richardlucasapps.netlive.gauge;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;
import com.richardlucasapps.netlive.R;
import com.richardlucasapps.netlive.settings.SettingsActivity;
import com.richardlucasapps.netlive.widget.NetworkSpeedWidget;
import com.richardlucasapps.netlive.widget.WidgetSettings;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class GaugeService extends Service {

  private long previousBytesSentSinceBoot;
  private long previousBytesReceivedSinceBoot;

  private List<AppDataUsage> appDataUsageList;

  private Notification.Builder mBuilder;
  private NotificationManager mNotifyMgr;

  private SharedPreferences sharedPref;

  private UnitConverter converter;

  private String unitMeasurement;
  private boolean showActiveApp;

  private PowerManager pm;
  private boolean notificationEnabled;

  private List<UnitConverter> widgetUnitMeasurementConverters;
  private List<RemoteViews> widgetRemoteViews;

  private int[] ids;
  private AppWidgetManager manager;
  private int N;

  private boolean eitherNotificationOrWidgetRequestsActiveApp;
  private boolean showTotalValueNotification;
  private boolean hideNotification;

  private boolean widgetRequestsActiveApp;

  private boolean firstUpdate;
  private PackageManager packageManager;

  private boolean widgetExist;

  private ScheduledFuture updateHandler;

  private ArrayList<WidgetSettings> widgetSettingsOfAllWidgets;

  private long start = 0l;

  private double totalSecondsSinceLastPackageRefresh = 0d;
  private double totalSecondsSinceNotificaitonTimeUpdated = 0d;

  @Override public void onCreate() {
    super.onCreate();
    createService(this);
  }

  private void createService(final Service service) {
    firstUpdate = true;

    pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
    sharedPref = PreferenceManager.getDefaultSharedPreferences(service);

    notificationEnabled = !(sharedPref.getBoolean("pref_key_auto_start", false));
        /*
        Confirmed that this line: widgetExist = sharedPref.getBoolean("widget_exists", false);
        is a bad way to check if a widget exists. If using the auto backup feature in API 23 and above,
        this can cause an issue where the user ads a widget, they uninstall the device, and next time
        they install the app, before they even add a widget, this value will say that a widget exists,
        even though it does not. This will keep the service alive even if they disable the notification
        in the app, because that only affects the notification view.
         */
    widgetExist = sharedPref.getBoolean("widget_exists", false);

    if (!notificationEnabled && !widgetExist) {
      this.stopSelf();
      return;
    }

    unitMeasurement = sharedPref.getString("pref_key_measurement_unit", "Mbps");
    showTotalValueNotification = sharedPref.getBoolean("pref_key_show_total_value", false);
    long pollRate = Long.parseLong(sharedPref.getString("pref_key_poll_rate", "5"));
    showActiveApp = sharedPref.getBoolean("pref_key_active_app", true);
    hideNotification = sharedPref.getBoolean("pref_key_hide_notification", false);

    converter = getUnitConverter(unitMeasurement);

    widgetRequestsActiveApp = false;
    if (widgetExist) {
      setupWidgets();
    }
    if (showActiveApp || widgetRequestsActiveApp) {
      eitherNotificationOrWidgetRequestsActiveApp = true;
      packageManager = this.getPackageManager();
    }

    if (notificationEnabled) {
      mNotifyMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
      int mId = 1;
      mBuilder = new Notification.Builder(service).setSmallIcon(R.drawable.idle)
          .setContentTitle("")
          .setContentText("")
          .setOngoing(true);

      if (hideNotification) {
        mBuilder.setPriority(Notification.PRIORITY_MIN);
      } else {
        mBuilder.setPriority(Notification.PRIORITY_HIGH);
      }

      Intent resultIntent = new Intent(service, SettingsActivity.class);
      TaskStackBuilder stackBuilder = TaskStackBuilder.create(service);
      stackBuilder.addParentStack(SettingsActivity.class);
      stackBuilder.addNextIntent(resultIntent);
      PendingIntent resultPendingIntent =
          stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
      mBuilder.setContentIntent(resultPendingIntent);

      Notification notification = mBuilder.build();

      mNotifyMgr.notify(mId, notification);

      startForeground(mId, notification);
    }
    startUpdateService(pollRate);
  }

  @Override public IBinder onBind(Intent intent) {
    return null;
  }

  @Override public void onDestroy() {
    try {
      updateHandler.cancel(true);
    } catch (NullPointerException e) {
      //The only way there will be a null pointer, is if the disabled preference is checked.  Because if it is, onDestory() is called right away, without creating the updateHandler
    }
    super.onDestroy();
  }

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    Bundle extras = null;
    if (intent != null) {
      extras = intent.getExtras();
    }
    boolean wasPackageAdded = false;
    //int newAppUid = 0;
    if (extras != null) {
      wasPackageAdded = extras.getBoolean("PACKAGE_ADDED");
      //newAppUid = extras.getInt("EXTRA_UID");
    }
    if (wasPackageAdded && eitherNotificationOrWidgetRequestsActiveApp) {
      loadAllAppsIntoAppDataUsageList();
      //the uid in the EXTRA_UID from the packagewatcher broadcast receiver is always blank, to be fair, API says only that is "may" include it. Wtf Google, get your shit together.
      //so for now just leave this disabled and just reload the entire list of apps
      //            if(newAppUid!=0) {
      //                addSpecificPackageWithUID(newAppUid);
      //            } else {
      //                loadAllAppsIntoAppDataUsageList();
      //            }
    }
    return START_STICKY;
  }

  private void setupWidgets() {
    widgetSettingsOfAllWidgets = new ArrayList<>();

    ComponentName name = new ComponentName(getApplicationContext(), NetworkSpeedWidget.class);
    ids = AppWidgetManager.getInstance(getApplicationContext()).getAppWidgetIds(name);
    manager = AppWidgetManager.getInstance(this);

    widgetUnitMeasurementConverters = new ArrayList<>();
    widgetRemoteViews = new ArrayList<>();

    N = ids.length;

    for (int i = 0; i < N; i++) {
      int awID = ids[i];

      String colorOfFont = sharedPref.getString("pref_key_widget_font_color" + awID, "Black");
      String sizeOfFont = sharedPref.getString("pref_key_widget_font_size" + awID, "12");
      float floatSizeOfFont = Float.parseFloat(sizeOfFont);
      String measurementUnit =
          sharedPref.getString("pref_key_widget_measurement_unit" + awID, "Mbps");
      boolean displayActiveApp = sharedPref.getBoolean("pref_key_widget_active_app" + awID, true);
      boolean displayTotalValue = sharedPref.getBoolean("pref_key_widget_show_total" + awID, false);

      WidgetSettings widgetSettings =
          new WidgetSettings(measurementUnit, displayActiveApp, displayTotalValue);
      widgetSettingsOfAllWidgets.add(i, widgetSettings);

      if (displayActiveApp) {
        widgetRequestsActiveApp = true;
      }

      int widgetColor;
      widgetColor = Color.parseColor(colorOfFont);

      RemoteViews v = new RemoteViews(getPackageName(), R.layout.widget);
      v.setTextColor(R.id.widgetTextViewLineOne, widgetColor);

      v.setFloat(R.id.widgetTextViewLineOne, "setTextSize", floatSizeOfFont);

      widgetRemoteViews.add(v);
      UnitConverter converter = getUnitConverter(measurementUnit);
      widgetUnitMeasurementConverters.add(converter);
    }
  }

  private synchronized String getActiveAppWithTrafficApi() {
    long maxDelta = 0L;
    long delta;
    String appLabel = "";

    for (AppDataUsage currentApp : appDataUsageList) {
      delta = currentApp.getTransferRate();
      if (delta > maxDelta) {
        appLabel = currentApp.getAppName();
        maxDelta = delta;
      }
    }

    if (appLabel.equals("")) {
      return "(" + "..." + ")";
    }
    return "(" + appLabel + ")";
  }

  private UnitConverter getUnitConverter(String unitMeasurement) {

    if (unitMeasurement.equals("bps")) {
      return (new UnitConverter() {
        @Override public double convert(double bytesPerSecond) {
          return (bytesPerSecond * 8.0);
        }
      });
    }
    if (unitMeasurement.equals("Kbps")) {
      return (new UnitConverter() {
        @Override public double convert(double bytesPerSecond) {
          return (bytesPerSecond * 8.0) / 1000.0;
        }
      });
    }
    if (unitMeasurement.equals("Mbps")) {
      return (new UnitConverter() {
        @Override public double convert(double bytesPerSecond) {
          return (bytesPerSecond * 8.0) / 1000000.0;
        }
      });
    }
    if (unitMeasurement.equals("Gbps")) {
      return (new UnitConverter() {
        @Override public double convert(double bytesPerSecond) {
          return (bytesPerSecond * 8.0) / 1000000000.0;
        }
      });
    }
    if (unitMeasurement.equals("Bps")) {
      return (new UnitConverter() {
        @Override public double convert(double bytesPerSecond) {
          return bytesPerSecond;
        }
      });
    }
    if (unitMeasurement.equals("KBps")) {
      return (new UnitConverter() {
        @Override public double convert(double bytesPerSecond) {
          return bytesPerSecond / 1000.0;
        }
      });
    }
    if (unitMeasurement.equals("MBps")) {
      return (new UnitConverter() {
        @Override public double convert(double bytesPerSecond) {
          return bytesPerSecond / 1000000.0;
        }
      });
    }
    if (unitMeasurement.equals("GBps")) {
      return (new UnitConverter() {
        @Override public double convert(double bytesPerSecond) {
          return bytesPerSecond / 1000000000.0;
        }
      });
    }

    return (new UnitConverter() {
      @Override public double convert(double bytesPerSecond) {
        return (bytesPerSecond * 8.0) / 1000000.0;
      }
    });
  }

  private void startUpdateService(long pollRate) {
    final Runnable updater = new Runnable() {
      public void run() {
        update();
      }
    };
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    updateHandler = scheduler.scheduleAtFixedRate(updater, 0, pollRate, TimeUnit.SECONDS);
  }

  @SuppressWarnings("deprecation") private void update() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      if (!pm.isInteractive()) {
        return;
      }
    } else if (!pm.isScreenOn()) {
      return;
    }

    initiateUpdate();
  }

  private synchronized void initiateUpdate() {

    if (firstUpdate) {
      previousBytesSentSinceBoot =
          TrafficStats.getTotalTxBytes();//i dont initialize these to 0, because if i do, when app first reports, the rate will be crazy high
      previousBytesReceivedSinceBoot = TrafficStats.getTotalRxBytes();
      if (eitherNotificationOrWidgetRequestsActiveApp) {
        appDataUsageList = new ArrayList<>();
        loadAllAppsIntoAppDataUsageList();
      }
      firstUpdate = false;
    }

    if (firstUpdate
        && eitherNotificationOrWidgetRequestsActiveApp) {  //lazy initiazation, do it here so it is not done on the main thread, thus freezing the UI
      appDataUsageList = new ArrayList<>();
      loadAllAppsIntoAppDataUsageList();  //
      firstUpdate = false;
    }

    long end = System.nanoTime();
    long totalElapsed = end - start;
    long bytesSentSinceBoot = TrafficStats.getTotalTxBytes();
    long bytesReceivedSinceBoot = TrafficStats.getTotalRxBytes();
    start = System.nanoTime();

    double totalElapsedInSeconds = (double) totalElapsed / 1000000000.0;
    totalSecondsSinceLastPackageRefresh += totalElapsedInSeconds;
    totalSecondsSinceNotificaitonTimeUpdated += totalElapsedInSeconds;
    long bytesSentOverPollPeriod = bytesSentSinceBoot - previousBytesSentSinceBoot;
    long bytesReceivedOverPollPeriod = bytesReceivedSinceBoot - previousBytesReceivedSinceBoot;

    double bytesSentPerSecond = bytesSentOverPollPeriod / totalElapsedInSeconds;
    double bytesReceivedPerSecond = bytesReceivedOverPollPeriod / totalElapsedInSeconds;

    previousBytesSentSinceBoot = bytesSentSinceBoot;
    previousBytesReceivedSinceBoot = bytesReceivedSinceBoot;

    String activeApp = "";
    if (eitherNotificationOrWidgetRequestsActiveApp) {

      activeApp = getActiveAppWithTrafficApi();

      if (totalSecondsSinceLastPackageRefresh
          >= 86400) { //86400 once a day, just reload all the apps.

        loadAllAppsIntoAppDataUsageList();
        totalSecondsSinceLastPackageRefresh = 0;
      }
    }

    if (notificationEnabled) {
      updateNotification(bytesSentPerSecond, bytesReceivedPerSecond, activeApp);
    }
    if (widgetExist) {
      updateWidgets(bytesSentPerSecond, bytesReceivedPerSecond, activeApp);
    }
  }

  private void updateNotification(double bytesSentPerSecond, double bytesReceivedPerSecond,
      String activeApp) {

    String sentString = String.format("%.3f", (converter.convert(bytesSentPerSecond)));
    String receivedString = String.format("%.3f", (converter.convert(bytesReceivedPerSecond)));

    String displayValuesText = "";
    if (showTotalValueNotification) {
      double total =
          (converter.convert(bytesSentPerSecond) + converter.convert(bytesReceivedPerSecond));
      String totalString = String.format("%.3f", total);
      displayValuesText = "Total: " + totalString;
    }

    displayValuesText += " Up: " + sentString + " Down: " + receivedString;
    String contentTitleText = unitMeasurement;

    if (showActiveApp) {
      contentTitleText += " " + activeApp;
    }

    mBuilder.setContentText(displayValuesText);
    mBuilder.setContentTitle(contentTitleText);

    if (totalSecondsSinceNotificaitonTimeUpdated > 10800) { //10800 seconds is three hours
      mBuilder.setWhen(System.currentTimeMillis());
      totalSecondsSinceNotificaitonTimeUpdated = 0;
    }

    int mId = 1;
    if (!hideNotification) {

      if (bytesSentPerSecond < 13107 && bytesReceivedPerSecond < 13107) {
        mBuilder.setSmallIcon(R.drawable.idle);
        mNotifyMgr.notify(mId, mBuilder.build());
        return;
      }

      if (!(bytesSentPerSecond > 13107) && bytesReceivedPerSecond > 13107) {
        mBuilder.setSmallIcon(R.drawable.download);
        mNotifyMgr.notify(mId, mBuilder.build());
        return;
      }

      if (bytesSentPerSecond > 13107 && bytesReceivedPerSecond < 13107) {
        mBuilder.setSmallIcon(R.drawable.upload);
        mNotifyMgr.notify(mId, mBuilder.build());
        return;
      }

      if (bytesSentPerSecond > 13107
          && bytesReceivedPerSecond > 13107) {//1307 bytes is equal to .1Mbit
        mBuilder.setSmallIcon(R.drawable.both);
        mNotifyMgr.notify(mId, mBuilder.build());
      }
    }
    mNotifyMgr.notify(mId, mBuilder.build());
  }

  private void updateWidgets(double bytesSentPerSecond, double bytesReceivedPerSecond,
      String activeApp) {

    for (int i = 0; i < N; i++) {
      int awID = ids[i];

      WidgetSettings widgetSettings = widgetSettingsOfAllWidgets.get(i);

      String widgetTextViewLineOneText = "";

      if (widgetSettings.isDisplayActiveApp()) {
        widgetTextViewLineOneText = activeApp + "\n";
      }

      UnitConverter c = widgetUnitMeasurementConverters.get(i);

      String sentString = String.format("%.3f", c.convert(bytesSentPerSecond));
      String receivedString = String.format("%.3f", c.convert(bytesReceivedPerSecond));

      widgetTextViewLineOneText += widgetSettings.getMeasurementUnit() + "\n";
      if (widgetSettings.isDisplayTotalValue()) {
        double total =
            (converter.convert(bytesSentPerSecond) + converter.convert(bytesReceivedPerSecond));
        String totalString = String.format("%.3f", total);
        widgetTextViewLineOneText += "Total: " + totalString + "\n";
      }
      widgetTextViewLineOneText += "Up: " + sentString + "\n";
      widgetTextViewLineOneText += "Down: " + receivedString + "\n";

      RemoteViews v = widgetRemoteViews.get(i);
      v.setTextViewText(R.id.widgetTextViewLineOne, widgetTextViewLineOneText);
      manager.updateAppWidget(awID, v);
    }
  }

  private synchronized void loadAllAppsIntoAppDataUsageList() {
    if (appDataUsageList != null) {
      appDataUsageList.clear(); // clear before adding all the apps so we don't add duplicates
    } else {
      appDataUsageList = new ArrayList<>();
    }
    List<ApplicationInfo> appList = packageManager.getInstalledApplications(0);

    for (ApplicationInfo appInfo : appList) {
      addAppToAppDataUsageList(appInfo);
    }
  }

  private synchronized void addAppToAppDataUsageList(
      ApplicationInfo appInfo) {  //synchronized because both addSpecificPackageUID and loadAllAppsIntoAppDataUsageList may be changing the app list at the same time.
    String appLabel = (String) packageManager.getApplicationLabel(appInfo);
    int uid = appInfo.uid;
    AppDataUsage app = new AppDataUsage(appLabel, uid, this);
    appDataUsageList.add(app);
  }
}
