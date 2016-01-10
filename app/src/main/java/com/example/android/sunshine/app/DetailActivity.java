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

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.android.sunshine.app.data.WeatherContract;

public class DetailActivity extends ActionBarActivity {

    private String mForecastStr;

    /**
     * Use this method to launch {@link DetailActivity}. We pass the location and date as a URI
     * @param context
     * @param locationSetting
     * @param dateMsec
     */
    public static void launch(Context context, String locationSetting, long dateMsec) {
        Intent intent = new Intent(context, DetailActivity.class);
        intent.setData(WeatherContract.WeatherEntry.buildWeatherLocationWithDate(locationSetting, dateMsec));
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new DetailFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.detail, menu);
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

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class DetailFragment
            extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

        private static final String LOG_TAG = DetailFragment.class.getSimpleName();

        private static final String FORECAST_SHARE_HASHTAG = " #SunshineApp";
        private final int LOADER_ID = hashCode();
        private CharSequence mForecastStr;
        private TextView mDetailTextView;
        private Uri mDetailsUri;
        private ShareActionProvider mShareActionProvider;

        public DetailFragment() {
            setHasOptionsMenu(true);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

            // The detail Activity called via intent.  Inspect the intent for forecast data.
            mDetailsUri = parseIntent(getActivity().getIntent());
            mDetailTextView = (TextView) rootView.findViewById(R.id.detail_text);

            return rootView;
        }

        /**
         * Start the CursorLoader as soon as the Activity has been created because we want to be
         * sure the UI elements are non-null and ready to be populated with data when
         * {@link #onLoadFinished(Loader, Cursor)} is called.
         *
         * @param savedInstanceState
         */
        @Override
        public void onActivityCreated(@Nullable Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            getLoaderManager().initLoader(LOADER_ID, null, this);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            // Inflate the menu; this adds items to the action bar if it is present.
            inflater.inflate(R.menu.detailfragment, menu);

            // Retrieve the share menu item
            MenuItem menuItem = menu.findItem(R.id.action_share);

            // Get the provider and hold onto it to set/change the share intent.
            mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

            // Attach an intent to this ShareActionProvider.  You can update this at any time,
            // like when the user selects a new piece of data they might like to share.
            if (mShareActionProvider != null ) {
                mShareActionProvider.setShareIntent(createShareForecastIntent(mForecastStr));
            } else {
                Log.d(LOG_TAG, "Share Action Provider is null?");
            }
        }

        private Intent createShareForecastIntent(CharSequence forecastStr) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            shareIntent.setType("text/plain");
            String shareString
                    = (forecastStr != null)
                    ? (forecastStr + FORECAST_SHARE_HASHTAG)
                    : FORECAST_SHARE_HASHTAG;
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareString);
            return shareIntent;
        }

        /**
         * Extract the location/date URI if it exists
         * @param intent
         */
        @Nullable
        private Uri parseIntent(Intent intent) {
            if (intent != null) {
                return intent.getData();
            }
            return null;
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            CursorLoader cursorLoader;
            // Sort Cursor by forecast date in ascending order
            String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " " + WeatherContract.SORT_ORDER_ASCENDING;
            cursorLoader = new CursorLoader(getActivity(), mDetailsUri, WeatherContract.FORECAST_COLUMNS, null, null, sortOrder);
            Log.d(LOG_TAG, "onCreateLoader()"
                    + "\n\t -- id: " + id
                    + "\n\t -- mDetailsUri: " + mDetailsUri
                    + "\n\t -- cursorLoader: " + cursorLoader
            );
            if (id == LOADER_ID) {
            } else {
                Log.w(LOG_TAG, "onCreateLoader() -- UNRECOGNIZED id: " + id);
            }
            return cursorLoader;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            mForecastStr = convertCursorRowToDetailsFormat(data);
            Log.i(LOG_TAG, "onLoadFinished()"
                    +"\n\t -- mForecastStr: "+mForecastStr
                    +"\n\t -- mShareActionProvider: "+mShareActionProvider
            );
            mDetailTextView.setText(mForecastStr);
            // Attach an intent to this ShareActionProvider.  You can update this at any time,
            // like when the user selects a new piece of data they might like to share.
            if (mShareActionProvider != null ) {
                mShareActionProvider.setShareIntent(createShareForecastIntent(mForecastStr));
            } else {
                Log.w(LOG_TAG, "Share Action Provider is null?");
            }
        }

        private CharSequence convertCursorRowToDetailsFormat(Cursor cursor) {
            Log.i(LOG_TAG, "convertCursorRowToDetailsFormat()"
                    +"\t -- cursor: "+cursor
                    +"\t -- cursor.getCount(): "+cursor.getCount()
                    +"\t -- cursor.getPosition(): "+cursor.getPosition()
            );
            String uxFormat;
            if (cursor.moveToFirst()){
                double maxTemp = cursor.getDouble(WeatherContract.COL_WEATHER_MAX_TEMP);
                double minTemp = cursor.getDouble(WeatherContract.COL_WEATHER_MIN_TEMP);

                boolean isMetric = Utility.isMetric(getActivity());
                String highLowStr
                        = Utility.formatTemperature(maxTemp, isMetric, getActivity())
                        + "/"
                        + Utility.formatTemperature(minTemp, isMetric, getActivity());

                uxFormat = Utility.formatDate(cursor.getLong(WeatherContract.COL_WEATHER_DATE)) +
                        " - " + cursor.getString(WeatherContract.COL_WEATHER_DESC) +
                        " - " + highLowStr;
            } else {
                uxFormat = "Weather Data Unavailable";
            }
            Log.i(LOG_TAG, "convertCursorRowToDetailsFormat()"
                    +"\t -- uxFormat: "+uxFormat
            );
            return uxFormat;
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {

        }
    }
}
