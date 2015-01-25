package com.richardlucasapps.netlive;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.preference.PreferenceManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.RemoteViews;

public class MainService extends Service {

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);

    private Long bytesSentSinceBoot;
    private Long bytesReceivedSinceBoot;

    private Long previousBytesSentSinceBoot;
    private Long previousBytesReceivedSinceBoot;

    private Long bytesSentPerSecond;
    private Long bytesReceivedPerSecond;

    private String sentString;
    private String receivedString;

    private String activeApp = "";
    List<AppDataUsage> appDataUsageList;
    int appMonitorCounter;
    int setWhenCounter;

    int mId;

    NotificationCompat.Builder mBuilder;
    NotificationManager mNotifyMgr;
    Notification notification;
    SharedPreferences sharedPref;

    ScheduledFuture updateHandler;
    Intent resultIntent;

    Context context;
    ComponentName name;

    UnitConverter converter;
    long pollRate;

    String displayValuesText = "";
    String unitMeasurement;
    boolean showActiveApp;
    String contentTitleText = "";


    PowerManager pm;
    boolean notificationEnabled;

    List<UnitConverter> widgetUnitMeasurementConverters;
    List<RemoteViews> widgetRemoteViews;

    int[] ids;
    AppWidgetManager manager;
    int N;

    long correctedPollRate;

    boolean eitherNotificationOrWidgetRequestsActiveApp;
    boolean showTotalValueNotification;
    boolean hideNotification;


    boolean widgetRequestsActiveApp;

    private int updatesMissed = 1;

    private boolean firstUpdate;
    private PackageManager packageManager;




    //TODO show total value as an option

    //TODO allow no notification to be displayed

    boolean widgetExist;

    @Override
    public void onCreate() {
        super.onCreate();
        createService(this);


    }

    private void createService(final Service service) {
        firstUpdate = true;

        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(service);

        notificationEnabled = !(sharedPref.getBoolean("pref_key_auto_start", false));
        widgetExist = sharedPref.getBoolean("widget_exists", false);

        if (!notificationEnabled && !widgetExist) {
            this.stopSelf();
            return;
        }


        unitMeasurement = sharedPref.getString("pref_key_measurement_unit", "Mbps");
        showTotalValueNotification = sharedPref.getBoolean("pref_key_show_total_value", false);
        pollRate = Long.parseLong(sharedPref.getString("pref_key_poll_rate", "1"));
        showActiveApp = sharedPref.getBoolean("pref_key_active_app", true);
        hideNotification = sharedPref.getBoolean("pref_key_hide_notification", false);

        converter = getUnitConverter(unitMeasurement);


        context = getApplicationContext();
        widgetRequestsActiveApp = false;
        if (widgetExist) {
            setupWidgets();
        }
        if (showActiveApp || widgetRequestsActiveApp) {
            eitherNotificationOrWidgetRequestsActiveApp = true;
            packageManager = this.getPackageManager();

        }


        appMonitorCounter = 0;
        setWhenCounter = 0;

        previousBytesSentSinceBoot = TrafficStats.getTotalTxBytes();//i dont initialize these to 0, because if i do, when app first reports, the rate will be crazy high
        previousBytesReceivedSinceBoot = TrafficStats.getTotalRxBytes();



        if (notificationEnabled) {
            mNotifyMgr =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mId = 1;
            mBuilder = new NotificationCompat.Builder(service)
                    .setSmallIcon(R.drawable.idle)
                    .setContentTitle("")
                    .setContentText("")
                    .setOngoing(true);


            if (hideNotification) {
                mBuilder.setPriority(Notification.PRIORITY_MIN);
            } else {
                mBuilder.setPriority(Notification.PRIORITY_HIGH);
            }


            resultIntent = new Intent(service, MainActivity.class);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(service);
            stackBuilder.addParentStack(MainActivity.class);
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(
                            0,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
            mBuilder.setContentIntent(resultPendingIntent);


            notification = mBuilder.build();


            mNotifyMgr.notify(
                    mId,
                    notification);

            startForeground(mId, notification);
        }

        startUpdateService(pollRate);

    }



    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        try {
            updateHandler.cancel(true); //TODO can probabaly get rid of this stuff, only call superOnDestory because it nothing is bound to service, it will stop.
        } catch (NullPointerException e) {
            //The only way there will be a null pointer, is if the disabled preference is checked.  Because if it is, onDestory() is called right away, without creating the updateHandler
        }
        super.onDestroy();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle extras = intent.getExtras();
        boolean wasPackageAdded = false;
        int newAppUid = 0;
        if(extras!=null){
            wasPackageAdded = extras.getBoolean("PACKAGE_ADDED");
            newAppUid = extras.getInt("EXTRA_UID");

        }
        if(wasPackageAdded && eitherNotificationOrWidgetRequestsActiveApp){
            Log.d("onStartCommand", "loadAllApps");

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

    public synchronized String getActiveAppWithTrafficApi() {

        long maxDelta = 0L;
        long delta = 0L;
        String appLabel = "";

        int count = 0;
        for (AppDataUsage currentApp : appDataUsageList) {
            count++;
            delta = currentApp.getRateWithTrafficStatsAPI();
            if (delta > maxDelta) {
                appLabel = currentApp.getAppName();
                maxDelta = delta;
            }
        }
        Log.d("Iterated", count + " times");
        if (appLabel.equals("")) {
            return "(" + "..." + ")";
        }
        return "(" + appLabel + ")";

    }


    private UnitConverter getUnitConverter(String unitMeasurement) {

        if (unitMeasurement.equals("bps")) {
            return (new UnitConverter() {
                @Override
                public double convert(long bytesPerSecond) {
                    return (bytesPerSecond * 8.0);
                }
            });
        }
        if (unitMeasurement.equals("Kbps")) {
            return (new UnitConverter() {
                @Override
                public double convert(long bytesPerSecond) {
                    return (bytesPerSecond * 8.0) / 1000.0;
                }
            });
        }
        if (unitMeasurement.equals("Mbps")) {
            return (new UnitConverter() {
                @Override
                public double convert(long bytesPerSecond) {
                    return (bytesPerSecond * 8.0) / 1000000.0;
                }
            });
        }
        if (unitMeasurement.equals("Gbps")) {
            return (new UnitConverter() {
                @Override
                public double convert(long bytesPerSecond) {
                    return (bytesPerSecond * 8.0) / 1000000000.0;
                }
            });
        }
        if (unitMeasurement.equals("Bps")) {
            return (new UnitConverter() {
                @Override
                public double convert(long bytesPerSecond) {
                    return bytesPerSecond;
                }
            });
        }
        if (unitMeasurement.equals("KBps")) {
            return (new UnitConverter() {
                @Override
                public double convert(long bytesPerSecond) {
                    return bytesPerSecond / 1000.0;
                }
            });
        }
        if (unitMeasurement.equals("MBps")) {
            return (new UnitConverter() {
                @Override
                public double convert(long bytesPerSecond) {
                    return bytesPerSecond / 1000000.0;
                }
            });
        }
        if (unitMeasurement.equals("GBps")) {
            return (new UnitConverter() {
                @Override
                public double convert(long bytesPerSecond) {
                    return bytesPerSecond / 1000000000.0;
                }
            });
        }

        return (new UnitConverter() {
            @Override
            public double convert(long bytesPerSecond) {
                return (bytesPerSecond * 8.0) / 1000000.0;
            }
        });


    }


    public void startUpdateService(long pollRate) {
        final Runnable beeper = new Runnable() {
            public void run() {
                update();
            }
        };
        updateHandler =
                scheduler.scheduleAtFixedRate(beeper, 1, pollRate, TimeUnit.SECONDS);
    }


    private void update() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Log.d("isLolipop","device");
            if (!pm.isInteractive()) {
                updatesMissed += 1;
                return;
            }
        } else if (!pm.isScreenOn()) {
            updatesMissed += 1;
            return;
        }


        prepareUpdate();

        if (notificationEnabled) {
            updateNotification();

        }
        if (widgetExist) {
            updateWidgets();

        }


    }


    private void updateNotification() {

        sentString = String.format("%.3f", converter.convert(bytesSentPerSecond) / correctedPollRate);
        receivedString = String.format("%.3f", converter.convert(bytesReceivedPerSecond) / correctedPollRate);

        previousBytesSentSinceBoot = bytesSentSinceBoot;
        previousBytesReceivedSinceBoot = bytesReceivedSinceBoot;

        if (showTotalValueNotification) {
            double total = (converter.convert(bytesSentPerSecond) + converter.convert(bytesReceivedPerSecond)) / correctedPollRate;
            String totalString = String.format("%.3f", total);
            displayValuesText = "Total: " + totalString;
        }

        displayValuesText += " Up: " + sentString + " Down: " + receivedString;
        contentTitleText = unitMeasurement;

        if (showActiveApp) {
            contentTitleText += " " + activeApp;
        }

        mBuilder.setContentText(displayValuesText);
        mBuilder.setContentTitle(contentTitleText);
        setWhenCounter++;
        if (setWhenCounter > 10800 / pollRate) { //10800 seconds is three hours, but in reality this will be greater because the device will not be awake for that whole time
            mBuilder.setWhen(System.currentTimeMillis());
            setWhenCounter = 0;
        }

        displayValuesText = "";

        //TODO Report issue to AOSP where if the notification is set to minimum priority, and you update it after having called setWhen(), it will reissue it like a new notification, wont just update it


        if (!hideNotification) {

            if (bytesSentPerSecond / correctedPollRate < 13107 && bytesReceivedPerSecond / correctedPollRate < 13107) {
                mBuilder.setSmallIcon(R.drawable.idle);
                mNotifyMgr.notify(mId, mBuilder.build());
                return;
            }

            if (!(bytesSentPerSecond / correctedPollRate > 13107) && bytesReceivedPerSecond / correctedPollRate > 13107) {
                mBuilder.setSmallIcon(R.drawable.download);
                mNotifyMgr.notify(mId, mBuilder.build());
                return;
            }

            if (bytesSentPerSecond / correctedPollRate > 13107 && bytesReceivedPerSecond / correctedPollRate < 13107) {
                mBuilder.setSmallIcon(R.drawable.upload);
                mNotifyMgr.notify(mId, mBuilder.build());
                return;
            }

            if (bytesSentPerSecond / correctedPollRate > 13107 && bytesReceivedPerSecond / correctedPollRate > 13107) {//1307 bytes is equal to .1Mbit
                mBuilder.setSmallIcon(R.drawable.both);
                mNotifyMgr.notify(mId, mBuilder.build());
            }
        }
        mNotifyMgr.notify(mId, mBuilder.build());
    }

    private void setupWidgets() {

        name = new ComponentName(context, NetworkSpeedWidget.class);
        ids = AppWidgetManager.getInstance(context).getAppWidgetIds(name);
        manager = AppWidgetManager.getInstance(this);


        widgetUnitMeasurementConverters = new ArrayList<UnitConverter>();
        widgetRemoteViews = new ArrayList<RemoteViews>();

        N = ids.length;

        for (int i = 0; i < N; i++) {
            int awID = ids[i];

            String colorOfFont = sharedPref.getString("pref_key_widget_font_color" + awID, "Black");
            String sizeOfFont = sharedPref.getString("pref_key_widget_font_size" + awID,"12");
            float floatSizeOfFont = Float.parseFloat(sizeOfFont);
            String measurementUnit = sharedPref.getString("pref_key_widget_measurement_unit" + awID, "Mbps");
            boolean displayActiveApp = sharedPref.getBoolean("pref_key_widget_active_app" + awID, true);


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


    private void addSpecificPackageWithUID(int uid){
        String[] packagesForUid;
         //TODO don't reinstantiate every time

        Log.d("uid",String.valueOf(uid));
        //check what the uid is coming back, also check if need to make new instance of paclageManager in order to make it work
        packagesForUid = packageManager.getPackagesForUid(uid);
        for (String element : packagesForUid) {
            try {
                ApplicationInfo appInfo = packageManager.getApplicationInfo(element, 0);
                addAppToAppDataUsageList(appInfo);
            } catch (PackageManager.NameNotFoundException e) {
                //e.printStackTrace();
            }

        }

    }
    private void loadAllAppsIntoAppDataUsageList() {
        appDataUsageList.clear(); // clear before adding all the apps so we don't add duplicates
        List<ApplicationInfo> appList = packageManager.getInstalledApplications(0);

        for (ApplicationInfo appInfo : appList) {
            addAppToAppDataUsageList(appInfo);

        }
        //TODO NEED to add back in the correction so when you awake device from sleep values are not messed up
        //See if can register for installed app events, then can loadallappslist again

    }

    private synchronized void addAppToAppDataUsageList(ApplicationInfo appInfo){  //synchronized because both addSpecificPackageUID and loadAllAppsIntoAppDataUsageList may be changing the app list at the same time.
        String appLabel = (String) packageManager.getApplicationLabel(appInfo);
        int uid = appInfo.uid;
        AppDataUsage app = new AppDataUsage(appLabel, uid);
        appDataUsageList.add(app);

    }


    private void updateWidgets() {

        for (int i = 0; i < N; i++) {
            int awID = ids[i];

            boolean displayActiveApp = sharedPref.getBoolean("pref_key_widget_active_app" + awID, true);
            boolean displayTotalValue = sharedPref.getBoolean("pref_key_widget_show_total" + awID, false);
            String widgetUnitMeasurement = sharedPref.getString("pref_key_widget_measurement_unit" + awID, unitMeasurement);


            String widgetTextViewLineOneText = "";

            if (displayActiveApp) {
                widgetTextViewLineOneText = activeApp + "\n";
            }

            UnitConverter c = widgetUnitMeasurementConverters.get(i);

            String sentString = String.format("%.3f", c.convert(bytesSentPerSecond) / correctedPollRate);
            String receivedString = String.format("%.3f", c.convert(bytesReceivedPerSecond) / correctedPollRate);

            widgetTextViewLineOneText += widgetUnitMeasurement + "\n";
            if (displayTotalValue) {
                double total = (converter.convert(bytesSentPerSecond) + converter.convert(bytesReceivedPerSecond)) / correctedPollRate;
                String totalString = String.format("%.3f", total);
                displayValuesText = "Total: " + totalString;
                widgetTextViewLineOneText += "Total: " + totalString + "\n";
            }
            widgetTextViewLineOneText += "Up: " + sentString + "\n";
            widgetTextViewLineOneText += "Down: " + receivedString + "\n";

            RemoteViews v = widgetRemoteViews.get(i);
            v.setTextViewText(R.id.widgetTextViewLineOne, widgetTextViewLineOneText);
            manager.updateAppWidget(awID, v);

        }

    }

    private void prepareUpdate() {

        if(firstUpdate && eitherNotificationOrWidgetRequestsActiveApp){  //lazy initiazation, do it here so it is not done on the main thread, thus freezing the UI
            appDataUsageList = new ArrayList<AppDataUsage>();
            loadAllAppsIntoAppDataUsageList();  //
            firstUpdate = false;
        }

        correctedPollRate = pollRate * updatesMissed;
        updatesMissed = 1;


        bytesSentSinceBoot = TrafficStats.getTotalTxBytes();
        bytesReceivedSinceBoot = TrafficStats.getTotalRxBytes();

        bytesSentPerSecond = bytesSentSinceBoot - previousBytesSentSinceBoot;
        bytesReceivedPerSecond = bytesReceivedSinceBoot - previousBytesReceivedSinceBoot;

        previousBytesSentSinceBoot = bytesSentSinceBoot;
        previousBytesReceivedSinceBoot = bytesReceivedSinceBoot;


        if (eitherNotificationOrWidgetRequestsActiveApp) {
            activeApp = getActiveAppWithTrafficApi();


            appMonitorCounter += 1;  //TODO perhaps just get rid of this, or increase it by more. If a user installs another app, it updates app list
            if (appMonitorCounter >= (1000 / pollRate)) {//divide by pollRate so that if you have a pollRate of 10, that will end up being 500 seconds, not 5000
                Log.d("loading new apps list", "doing it");
                loadAllAppsIntoAppDataUsageList();
                appMonitorCounter = 0;
            }
        }

    }

}
