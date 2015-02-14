package com.richardlucasapps.netlive;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


import java.util.concurrent.TimeUnit;


public class MainActivity extends Activity {

    SharedPreferences sharedPref;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_no_ads);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());


        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        boolean firstRun = getSharedPreferences("START_UP_PREFERENCE", MODE_PRIVATE).getBoolean("firstRun", true);


        SharedPreferences.Editor edit = sharedPref.edit();

        boolean alreadyPrompted = sharedPref.getBoolean("ALREADY_PROMPTED", false);

        if (!alreadyPrompted) {

            Long date_firstLaunch = sharedPref.getLong("PREF_DATE_FIRST_LAUNCH", 0);
            if (date_firstLaunch == 0) {
                date_firstLaunch = System.currentTimeMillis();
                edit.putLong("PREF_DATE_FIRST_LAUNCH", date_firstLaunch);
                edit.commit();
            }

            long days = TimeUnit.MILLISECONDS.toDays(date_firstLaunch - System.currentTimeMillis());


            if (days > 7) {
                edit.putBoolean("ALREADY_PROMPTED", true);
                edit.commit();
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setNegativeButton(getString(R.string.negative_option_for_rate_app_dialog), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });
                builder.setPositiveButton(getString(R.string.positive_option_for_rate_app_dialog), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        openNetLiveInGooglePlay();


                    }
                });
                builder.setMessage(getString(R.string.rate_app_dialog_explanation))
                        .setTitle(getString(R.string.rate_app_dialog_title));
                AlertDialog dialog = builder.create();

                AlertDialog newFragment = dialog;
                newFragment.show();

            }
        }

        if (firstRun) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setPositiveButton(getString(R.string.welcome_message_dismiss), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User clicked OK button
                }
            });
            builder.setMessage(getString(R.string.welcome))
                    .setTitle(getString(R.string.welcome_message_message) + " " + getString(R.string.app_name_with_version_number));
            AlertDialog dialog = builder.create();

            AlertDialog newFragment = dialog;
            newFragment.show();

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
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_settings_rate_netlive:
                openNetLiveInGooglePlay();
                return true;
            case R.id.action_settings_share:

                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                String shareBody = getString(R.string.app_url_if_uri_fails);
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
                Email.putExtra(Intent.EXTRA_SUBJECT, "Feedback");
                startActivity(Intent.createChooser(Email, "Send Feedback:"));
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
        if (MyStartActivity(intent) == false) {
            //Market (Google play) app seems not installed, let's try to open a webbrowser
            intent.setData(Uri.parse(getString(R.string.app_url_if_uri_fails)));
            if (MyStartActivity(intent) == false) {
                //Well if this also fails, we have run out of options, inform the user.
                Toast.makeText(this, getString(R.string.could_not_open_app_in_Google_play), Toast.LENGTH_SHORT).show();
            }
        }

    }

    private boolean MyStartActivity(Intent aIntent) {
        try {
            startActivity(aIntent);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }


    private void showAboutDialog() {
        AlertDialog.Builder aboutBuilder = new AlertDialog.Builder(this);
        TextView myMsg = new TextView(this);
        PackageInfo pInfo = null;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        int version = pInfo.versionCode;
        SpannableString s = new SpannableString(getString(R.string.app_name_with_version_number) +
                "\n\n Version Code: " + version + "\n\nrichardlucasapps.com");
        Linkify.addLinks(s, Linkify.WEB_URLS);
        myMsg.setText(s);
        myMsg.setTextSize(15);
        myMsg.setMovementMethod(LinkMovementMethod.getInstance());
        myMsg.setGravity(Gravity.CENTER);
        aboutBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
            }
        });
        aboutBuilder.setView(myMsg)
                .setTitle("About");
        AlertDialog dialog = aboutBuilder.create();

        AlertDialog newFragment = dialog;
        newFragment.show();


    }


    private void showHelpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String s = getString(R.string.help_dialog_para_1);

        String overviewTitle = "Overview";
        String overviewContent = getString(R.string.help_dialog_para_2);

        String si = getString(R.string.help_dialog_para_3);
        ;

        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.help_dialog, null);
        TextView textview = (TextView) view.findViewById(R.id.textmsg);
        textview.setText((Html.fromHtml(s + "<br>" + "<br>" + "<b>" + overviewTitle + "</b>" + "<br>" + "<br>" + overviewContent + "<br>" + "<br>"
                        + si
        )));

        textview.setTextSize(17);
        textview.setPadding(15, 15, 15, 15);


        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
            }
        });
        builder.setView(view)
                .setTitle(getString(R.string.welcome_message_message) + " " + getString(R.string.app_name_with_version_number));
        AlertDialog dialog = builder.create();

        AlertDialog newFragment = dialog;
        newFragment.show();


    }

}
