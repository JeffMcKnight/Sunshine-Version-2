/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.sunshine.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.List;

public class MainActivity
        extends AppCompatActivity
        implements ForecastFragment.Listener {

    private final String LOG_TAG = MainActivity.class.getSimpleName();
    private final String FORECASTFRAGMENT_TAG = MainActivity.class.getName() + ".ForecastFragment";
    private String mLocation;
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Retrieve the location preference
        mLocation = Utility.getPreferredLocation(this);
        View detailContainer = findViewById(R.id.weather_detail_container);
        Log.i(LOG_TAG, "onCreate()"
            +"\t -- detailContainer: "+detailContainer
        );
        if (detailContainer !=  null){
            mTwoPane = true;
            if (savedInstanceState == null){
                /** Show forecast details for current day */
                DetailFragment.attach(
                        this,
                        mLocation,
                        System.currentTimeMillis()
                );
            }
        } else {
            mTwoPane = false;
        }
        createSupportActionBar(!mTwoPane);
    }

    /**
     * Configure the {@link android.support.v7.app.ActionBar} here because I couldn't get it to work
     * in XML.  Not sure if it's an emulator thing, or if something changed in the support lib since
     * Google discourages using the logo now, or if I just did something wrong.
     *
     * @param shouldHideShadow
     */
    private void createSupportActionBar(boolean shouldHideShadow) {
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null){
            if (shouldHideShadow) {
                supportActionBar.setElevation(0.0f);
            }
            supportActionBar.setDisplayUseLogoEnabled(true);
            supportActionBar.setDisplayShowHomeEnabled(true);
            supportActionBar.setIcon(R.drawable.ic_logo);
            supportActionBar.setDisplayShowTitleEnabled(false);
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        if (id == R.id.action_map) {
            openPreferredLocationInMap();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume(){
        super.onResume();
        String preferredLocation = Utility.getPreferredLocation(this);
        if (null != preferredLocation && !preferredLocation.equals(mLocation)){
            List<Fragment> fragmentList = getSupportFragmentManager().getFragments();
            for (Fragment eachFragment : fragmentList){
                if (eachFragment instanceof LocationPreferenceListener){
                    ((LocationPreferenceListener) eachFragment).onLocationChanged(preferredLocation);
                }
            }
            mLocation = preferredLocation;
        }

    }

    private void openPreferredLocationInMap() {

        // Using the URI scheme for showing a location found on a map.  This super-handy
        // intent can is detailed in the "Common Intents" page of Android's developer site:
        // http://developer.android.com/guide/components/intents-common.html#Maps
        Uri geoLocation = Uri.parse("geo:0,0?").buildUpon()
                .appendQueryParameter("q", mLocation)
                .build();

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Log.d(LOG_TAG, "Couldn't call " + mLocation + ", no receiving apps installed!");
        }
    }

    /**
     *
     * @param locationSetting
     * @param dateInMsec
     */
    @Override
    public void onListItemClick(String locationSetting, long dateInMsec) {
        Log.i(LOG_TAG, "onListItemClick()"
                +"\t -- mTwoPane: "+mTwoPane
                +"\t -- locationSetting: "+locationSetting
                +"\t -- dateInMsec: "+dateInMsec
        );
        if (mTwoPane){
            DetailFragment.attach(this, locationSetting, dateInMsec);
        } else {
            DetailActivity.launch(this, locationSetting, dateInMsec);
        }
    }

    /**
     * Phones use the special "today" layout for the first list item so the user sees a big fancy
     * icon, and tablets do not because we give the user the big fancy icon in the {@link DetailFragment}
     * @return false if the device is a tablet
     */
    @Override
    public boolean isUsingTodayLayout() {
        return !getResources().getBoolean(R.bool.is_tablet);
    }

}
