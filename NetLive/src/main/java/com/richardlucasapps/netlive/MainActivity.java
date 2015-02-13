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

import com.richardlucasapps.netlive.util.IabHelper;
import com.richardlucasapps.netlive.util.IabResult;
import com.richardlucasapps.netlive.util.Inventory;
import com.richardlucasapps.netlive.util.Purchase;

import java.util.concurrent.TimeUnit;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

public class MainActivity extends Activity implements AdFragment.OnFragmentInteractionListener{

    //TODO fix http://stackoverflow.com/questions/16777829/java-lang-runtimeexception-unable-to-start-activity-componentinfo-java-lang-nu


    IabHelper mHelper;

    SharedPreferences sharedPref;
    // SKUs for our products: the premium upgrade (non-consumable) and gas (consumable)
    static final String[] skuArray = {"1dollar99cents","3dollar99cents","5dollar99cents","7dollar99cents",
            "9dollar99cents","99centsperyear","2dollar99centsperyear","4dollar99centsperyear",
            "6dollar99centsperyear", "8dollar99centsperyear"};
    static final String SKU_0_99 = "0dollar99cents";  //TODO gotta get rid of decimal and money sign
    static final String SKU_1_99 = "1dollar99cents";
    static final String SKU_2_99 = "2dollar99cents";
    static final String SKU_3_99 = "3dollar99cents";
    static final String SKU_4_99 = "4dollar99cents";
    static final String SKU_5_99 = "5dollar99cents";
    static final String SKU_6_99 = "6dollar99cents";
    static final String SKU_7_99 = "7dollar99cents";
    static final String SKU_8_99 = "8dollar99cents";
    static final String SKU_9_99 = "9dollar99cents";

    // (arbitrary) request code for the purchase flow
    static final int RC_REQUEST = 10001;

    //TODO externalize the strings in this activity, show some class*
    //*denotes pun

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_no_ads);
//        getFragmentManager().beginTransaction()
//                .replace(android.R.id.content, new SettingsFragment())
//                .add(new AdFragment(), "AdFragment")
//                .commit();
        sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        //boolean showAds = sharedPref.getBoolean("SHOW_ADS", false);





        // Create new fragment and transaction
//        Fragment frag = new AdFragment();
//        FragmentTransaction transaction = getFragmentManager().beginTransaction();
//
//// Replace whatever is in the fragment_container view with this fragment,
//// and add the transaction to the back stack
//        transaction.add(R.id.LinearLayout1, frag);
//
//// Commit the transaction
//        transaction.commit();



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
                    .setTitle(getString(R.string.welcome_message_message)+" "+getString(R.string.app_name_with_version_number));
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




        String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvgiGgngCKPB1oRP7KojA9Ler9vDPnIVaRKZXq5/dn+4tRZSV4imuNyL8oMsWIMUFdkP3KMjJYAwRVMCqF5Jxg4XPiYvD4/FAb3ZqXDtcW9k/d7AgqcTEvqi3B4nnrGGgKvkE3ExWrD+vsFDBsMUT1zLRzXmel2KMitOTgN9UergWez/eRjJaCSYEidmMiR/XR+4L+dTjcyT4K28zgBt15OsgiP+C9cfmKswCJ9vZODqc4iLQYS7dzmns/s15X8w5UlyqHCvBp5mi8dohopSZIIX/Olc30xa8maJemkfY8vAtnnnCdvQ5ANejZstB/dINUWXOS/FvqPf7SyFQga+HRQIDAQAB";
        mHelper = new IabHelper(this, base64EncodedPublicKey);
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                Log.d("TAG", "Setup finished.");

                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
                    //complain("Problem setting up in-app billing: " + result);
                    Log.d("Problem setting up in-app billing:", "FAILED");
                    return;
                }

                // Have we been disposed of in the meantime? If so, quit.
                if (mHelper == null) return;

                // IAB is fully set up. Now, let's get an inventory of stuff we own.
                Log.d("NetLive", "Setup successful. Querying inventory.");
                if(savedInstanceState == null){  //this means we are first starting the Activity
                    mHelper.queryInventoryAsync(mGotInventoryListener);

                }

            }
        });

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

    private void openNetLiveInGooglePlay(){

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
                "\n\n Version Code: " + version  +"\n\nrichardlucasapps.com");
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
                .setTitle(getString(R.string.welcome_message_message)+" "+getString(R.string.app_name_with_version_number));
        AlertDialog dialog = builder.create();

        AlertDialog newFragment = dialog;
        newFragment.show();


    }

    public void showDonateDialogFragment(){
        DonateDialogFragment frag = new DonateDialogFragment();
        frag.show(getFragmentManager(),"Donate");
        return;
    }

    public void donateAmountClicked(int which){
        // The helper object

        Log.d("Donate Amount clicked", String.valueOf(which));

        //setWaitScreen(true);

        /* TODO: for security, generate your payload here for verification. See the comments on
         *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use
         *        an empty string, but on a production app you should carefully generate this. */
        String payload = "";

        Log.d("Item in SKUarray", skuArray[which]);

        mHelper.launchPurchaseFlow(this, skuArray[which], RC_REQUEST,
                mPurchaseFinishedListener, payload);


    }

    // Callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d("TAG", "Purchase finished: " + result + ", purchase: " + purchase);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            if (result.isFailure()) {
                //complain("Error purchasing: " + result);
                //setWaitScreen(false);
                Log.d("Result", "isFailure");
                return;
            }
            if (!verifyDeveloperPayload(purchase)) {
                Log.d("Did not verify", "Developer Payload");
                //complain("Error purchasing. Authenticity verification failed.");
                //setWaitScreen(false);
                return;
            }

            Log.d("TAG", "Purchase successful.");
            //purchase.getSku().equals(SKU_0_99);
            String purchaseSKU = purchase.getSku();
            for(String element : skuArray){
                if(purchaseSKU.equals(element)){
                    Log.d("SKU MATCH", "Element");
                }
            }



        }
    };

    /** Verifies the developer payload of a purchase. */
    boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();

        /*
         * TODO: verify that the developer payload of the purchase is correct. It will be
         * the same one that you sent when initiating the purchase.
         *
         * WARNING: Locally generating a random string when starting a purchase and
         * verifying it here might seem like a good approach, but this will fail in the
         * case where the user purchases an item on one device and then uses your app on
         * a different device, because on the other device you will not have access to the
         * random string you originally generated.
         *
         * So a good developer payload has these characteristics:
         *
         * 1. If two different users purchase an item, the payload is different between them,
         *    so that one user's purchase can't be replayed to another user.
         *
         * 2. The payload must be such that you can verify it even when the app wasn't the
         *    one who initiated the purchase flow (so that items purchased by the user on
         *    one device work on other devices owned by the user).
         *
         * Using your own server to store and verify developer payloads across app
         * installations is recommended.
         */

        return true;
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        return;
    }

    // Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d("NetLive", "Query inventory finished.");

            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) return;

            // Is it a failure?
            if (result.isFailure()) {
                Log.d("Failed to query inventory: " + result, "");
                return;
            }

            Log.d("NetLive", "Query inventory was successful.");

            /*
             * Check for items we own. Notice that for each purchase, we check
             * the developer payload to see if it's correct! See
             * verifyDeveloperPayload().
             */

            SharedPreferences.Editor edit = sharedPref.edit();
            // Do we have the premium upgrade?
            for(String element: skuArray){
                Purchase purchase = inventory.getPurchase(element);
                boolean isPurchased = (purchase != null && verifyDeveloperPayload(purchase));
                Log.d("Did Purchase " + element,(isPurchased ? "YES" : "NO"));

            }

            Fragment frag = new AdFragment();
            FragmentTransaction transaction = getFragmentManager().beginTransaction();

// Replace whatever is in the fragment_container view with this fragment,
// and add the transaction to the back stack
            transaction.add(R.id.LinearLayout1, frag);

// Commit the transaction
            transaction.commit();

            Log.d("NetLive", "Initial inventory query finished; enabling main UI.");
        }
    };
}
