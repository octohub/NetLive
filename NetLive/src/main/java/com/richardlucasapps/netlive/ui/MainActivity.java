package com.richardlucasapps.netlive.ui;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.richardlucasapps.netlive.MainService;
import com.richardlucasapps.netlive.R;


public class MainActivity extends Activity {

    private AlertDialog aboutDialog;
    private AlertDialog helpDialog;
    private AlertDialog welcomeDialog;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        if (Build.VERSION.SDK_INT == android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            PreferenceManager.setDefaultValues(this, R.xml.preferences_for_jelly_bean_mr2, false);
        } else {
            PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        }

        boolean firstRun = getSharedPreferences("START_UP_PREFERENCE", MODE_PRIVATE).getBoolean("firstRun", true);

        if (firstRun) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setPositiveButton(getString(R.string.welcome_message_dismiss), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User clicked OK button
                }
            });
            builder.setMessage(getString(R.string.welcome) + getString(R.string.welcome_para))
                    .setTitle(getString(R.string.welcome_message_message) + " " + getString(R.string.app_name_with_version_number));

            welcomeDialog = builder.create();
            welcomeDialog.show();

            getSharedPreferences("START_UP_PREFERENCE", MODE_PRIVATE)
                    .edit()
                    .putBoolean("firstRun", false)
                    .commit();

        }
        Intent intent = new Intent(getApplicationContext(), MainService.class); //getApp
        startService(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (aboutDialog != null) {
            aboutDialog.dismiss();
        }
        if (helpDialog != null) {
            helpDialog.dismiss();
        }
        if (welcomeDialog != null) {
            welcomeDialog.dismiss();
        }
        if (rateDialog != null) {
            rateDialog.dismiss();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_settings_rate_netlive:
                openNetLiveInGooglePlay();
                return true;
            case R.id.action_settings_share:

                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                String url = getString(R.string.app_url_if_uri_fails);
                String shareBody = getString(R.string.netlive_share_body) + " " + url;
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
                startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_via)));
                return true;

            case R.id.action_settings_help:
                showHelpDialog();
                return true;

            case R.id.action_settings_about:
                showAboutDialog();
                return true;

            case R.id.action_settings_send_feedback:
                Intent Email = new Intent(Intent.ACTION_SEND);
                Email.setType("message/rfc822");
                Email.putExtra(Intent.EXTRA_EMAIL, new String[]{"richardlucasapps@gmail.com"});
                Email.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback)); //feedback
                startActivity(Intent.createChooser(Email, getString(R.string.send_feedback)));
                return true;
            case android.R.id.home:
                startActivity(new Intent(MainActivity.this, MainActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void openNetLiveInGooglePlay() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        //Try Google play
        intent.setData(Uri.parse(getString(R.string.app_uri_for_google_play)));
        if (noActivityFoundForIntent(intent)) {
            //Market (Google play) app seems not installed, let's try to open a webbrowser
            intent.setData(Uri.parse(getString(R.string.app_url_if_uri_fails)));
            if (noActivityFoundForIntent(intent)) {
                //Well if this also fails, we have run out of options, inform the user.
                Toast.makeText(this, getString(R.string.could_not_open_app_in_Google_play), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean noActivityFoundForIntent(Intent aIntent) {
        try {
            startActivity(aIntent);
            return false;
        } catch (ActivityNotFoundException e) {
            return true;
        }
    }

    private void showAboutDialog() {
        AlertDialog.Builder aboutBuilder = new AlertDialog.Builder(this);
        TextView myMsg = new TextView(this);
        String version;
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = Integer.toString(packageInfo.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            version = getString(R.string.version_code_not_found);
        }
        SpannableString s = new SpannableString(getString(R.string.app_name_with_version_number) +
                "\n\n" + " " + getString(R.string.heading_version_code) + " " + version + "\n\nrichardlucasapps.com");
        Linkify.addLinks(s, Linkify.WEB_URLS);
        myMsg.setText(s);
        myMsg.setTextSize(15);
        myMsg.setMovementMethod(LinkMovementMethod.getInstance());
        myMsg.setGravity(Gravity.CENTER);
        aboutBuilder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
            }
        });
        aboutBuilder.setView(myMsg)
                .setTitle(getString(R.string.about));
        AlertDialog dialog = aboutBuilder.create();

        aboutDialog = dialog;
        aboutDialog.show();

    }


    private void showHelpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String s = getString(R.string.help_dialog_para_1);

        String overviewTitle = getString(R.string.overview);
        String overviewContent = getString(R.string.help_dialog_para_2);
        String batteryLifeTitle = getString(R.string.battery_life_help_title);
        String batteryLifeAdvice = getString(R.string.battery_life_help_advice);

        String androidMR2Title = getString(R.string.help_dialog_android_jelly_bean_mr2_title);
        String androidMR2Body = getString(R.string.help_dialog_android_jelly_bean_mr2_body);

        String si = getString(R.string.help_dialog_para_3);


        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.help_dialog, null);
        TextView textview = (TextView) view.findViewById(R.id.textmsg);
        textview.setText((Html.fromHtml(s + "<br>" + "<br>" + "<b>" + overviewTitle + "</b>"
                        + "<br>" + "<br>" + overviewContent + "<br>" + "<br>"
                        + si + "<br>" + "<br>" + "<b>" + batteryLifeTitle + "</b>" + "<br>" + "<br>"
                        + batteryLifeAdvice + "<b>" + "<br>" + "<br>" + androidMR2Title + "</b>" + "<br>" + "<br>"
                        + androidMR2Body + "<a href=\"https://code.google.com/p/android/issues/detail?id=58210\">https://code.google.com/p/android/issues/detail?id=58210</a>"
        )));

        textview.setTextSize(17);
        textview.setPadding(15, 15, 15, 15);
        textview.setMovementMethod(LinkMovementMethod.getInstance());

        builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
            }
        });
        builder.setView(view)
                .setTitle(getString(R.string.welcome_message_message) + " " + getString(R.string.app_name_with_version_number));

        helpDialog = builder.create();
        helpDialog.show();
    }

}
