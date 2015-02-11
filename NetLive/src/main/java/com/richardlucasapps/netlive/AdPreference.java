package com.richardlucasapps.netlive;

//TODO http://developer.android.com/guide/topics/ui/settings.html
//TODO look at building a custom preference

import android.app.Activity;
import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

public class AdPreference extends Preference {


    public AdPreference(Context context, AttributeSet attrs, int defStyle) {super    (context, attrs, defStyle);}
    public AdPreference(Context context, AttributeSet attrs) {super(context, attrs);}
    public AdPreference(Context context) {super(context);}

    @Override
    protected View onCreateView(ViewGroup parent) {
        // this will create the linear layout defined in ads_layout.xml
        View view = super.onCreateView(parent);

        // the context is a PreferenceActivity
        //Activity activity = (Activity)getContext();

//        AdRequest adRequest = new AdRequest.Builder().build();
//        mAdView.loadAd(adRequest);
        int count  = parent.getChildCount();

        Log.d("count", String.valueOf(count));

        // Create the adView
        //AdView adView = new AdView(activity, AdSize.BANNER, "<your add id>");
        AdView mAdView = (AdView) view.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);



        return view;
    }
}
