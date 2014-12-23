package com.richardlucasapps.netlive;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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

import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {

    MyApplication app;

    SharedPreferences sharedPref;

    //TODO externalize the strings in this activity, show some class*
    //*denotes pun

	@Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction()
        .replace(android.R.id.content, new SettingsFragment())
        .commit();

        app = new MyApplication();
        sharedPref = PreferenceManager.getDefaultSharedPreferences(app.getInstance());

        Intent intent = new Intent(this, MainService.class);
        startService(intent);

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
                builder.setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });
                builder.setPositiveButton("Rate NetLive", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        //Try Google play
                        intent.setData(Uri.parse("market://details?id=com.richardlucasapps.netlive"));
                        if (MyStartActivity(intent) == false) {
                            //Market (Google play) app seems not installed, let's try to open a webbrowser
                            intent.setData(Uri.parse("https://play.google.com/store/apps/details?com.richardlucasapps.netlive"));
                            if (MyStartActivity(intent) == false) {
                                //Well if this also fails, we have run out of options, inform the user.
                            }
                        }


                    }
                });
                builder.setMessage("Enjoy NetLive? Please take a moment to rate it.")
                        .setTitle("Rate NetLive");
                AlertDialog dialog = builder.create();

                AlertDialog newFragment = dialog;
                newFragment.show();

            }
        }

		 if (firstRun){
			 AlertDialog.Builder builder = new AlertDialog.Builder(this);
			 builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		               // User clicked OK button
		           }
		       });
			 builder.setMessage(getString(R.string.welcome))
		       .setTitle("Welcome to NetLive");
			 AlertDialog dialog = builder.create();
			 
			 AlertDialog newFragment = dialog;
			 newFragment.show();

             getSharedPreferences("START_UP_PREFERENCE", MODE_PRIVATE)
		        .edit()
		        .putBoolean("firstRun", false)
		        .commit();
			    
		 }

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
                Intent intent = new Intent(Intent.ACTION_VIEW);
                //Try Google play
                intent.setData(Uri.parse("market://details?id=com.richardlucasapps.netlive"));
                if (MyStartActivity(intent) == false) {
                    //Market (Google play) app seems not installed, let's try to open a webbrowser
                    intent.setData(Uri.parse("https://play.google.com/store/apps/details?com.richardlucasapps.netlive"));
                    if (MyStartActivity(intent) == false) {
                        //Well if this also fails, we have run out of options, inform the user.
                        Toast.makeText(this, "Could not open Google Play", Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            case R.id.action_settings_share:

                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                String shareBody = "https://play.google.com/store/apps/details?id=com.richardlucasapps.netlive&hl=en";
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "NetLive");
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
                startActivity(Intent.createChooser(sharingIntent, "Share via"));
                return true;

            case R.id.action_settings_help:
                showHelpDialog();
                return true;

            case R.id.action_settings_about:
                showAboutDialog();
                return true;
                
            case R.id. action_settings_send_feedback:
            	 Intent Email = new Intent(Intent.ACTION_SEND);
                 Email.setType("message/rfc822");
                 Email.putExtra(Intent.EXTRA_EMAIL, new String[] { "richardlucasapps@gmail.com" });
                 Email.putExtra(Intent.EXTRA_SUBJECT, "Feedback");
                 //Email.putExtra(Intent.EXTRA_TEXT, "Dear ...," + "");
                 startActivity(Intent.createChooser(Email, "Send Feedback:"));
                 return true;
            case android.R.id.home:
            	startActivity(new Intent(MainActivity.this, MainActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    
    }

    private boolean MyStartActivity(Intent aIntent) {
        try
        {
            startActivity(aIntent);
            return true;
        }
        catch (ActivityNotFoundException e)
        {
            return false;
        }
    }


	private void showAboutDialog() {
		 AlertDialog.Builder aboutBuilder = new AlertDialog.Builder(this);
		 TextView myMsg = new TextView(this);
		 SpannableString s = new SpannableString("NetLive v2.5\n\nrichardlucasapps.com");
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

		String si =   getString(R.string.help_dialog_para_3);;
		
		LayoutInflater inflater= LayoutInflater.from(this);
		View view=inflater.inflate(R.layout.help_dialog, null);
		TextView textview = (TextView)view.findViewById(R.id.textmsg);
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
	       .setTitle("Welcome to NetLive");
		 AlertDialog dialog = builder.create();
		 
		 AlertDialog newFragment = dialog;
		 newFragment.show();

		
	}
}
