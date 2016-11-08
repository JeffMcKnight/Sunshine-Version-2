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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.android.sunshine.app.data.WeatherContract;
import com.example.android.sunshine.app.data.WeatherDbHelper;
import com.example.android.sunshine.app.sync.SunshineSyncAdapter;

import static com.example.android.sunshine.app.sync.SunshineSyncAdapter.LOG_TAG;

/**
 * Encapsulates fetching the forecast and displaying it as a {@link ListView} layout.
 */
public class ForecastFragment
        extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>, LocationPreferenceListener {

    private static final String TAG = ForecastFragment.class.getSimpleName();
    private static final String KEY_LISTVIEW_SELECTED_POSITION =
            ForecastFragment.class.getCanonicalName() + ".listview_selected_position";
    public static final int INVALID_LISTVIEW_POSITION = -1;
    private final int LOADER_ID = this.hashCode();
    private ForecastAdapter mForecastAdapter;
    private ListView mListView;
    private Listener mListener;
    private ContentObserver mContentObserver;
    private int mListViewPosition;
    private String mLocation;

    public ForecastFragment() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Listener) {
            mListener = (Listener) context;
        }
        /**
         * Listen for changes to the {@link com.example.android.sunshine.app.data.WeatherProvider}
         * database so we can update the UI by restarting the {@link CursorLoader}.
         */
        mContentObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                restartCursorLoader();
            }
        };
        context.getContentResolver().registerContentObserver(
                WeatherContract.WeatherEntry.CONTENT_URI,
                false,
                mContentObserver
        );
    }

    /**
     *
     */
    public void restartCursorLoader() {
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()"
                +"\t -- savedInstanceState: "+savedInstanceState
        );
        if (savedInstanceState != null){
            mListViewPosition = savedInstanceState.getInt(KEY_LISTVIEW_SELECTED_POSITION, INVALID_LISTVIEW_POSITION);
        } else {
            mListViewPosition = INVALID_LISTVIEW_POSITION;
        }
        // Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);
        updateWeather();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            updateWeather();
            return true;
        }
        if (id == R.id.action_map) {
            openPreferredLocationInMap();
            return true;
        }
        return super.onOptionsItemSelected(item);
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

        if (intent.resolveActivity(getContext().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Log.d(LOG_TAG, "Couldn't call " + mLocation + ", no receiving apps installed!");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Log.i(TAG, "onCreateView()"
                +"\t -- savedInstanceState: "+savedInstanceState
        );
        int flags = 0;
//        // The ArrayAdapter will take data from a source and
//        // use it to populate the ListView it's attached to.
//        String locationSetting = Utility.getPreferredLocation(getActivity());
//        Uri uri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(locationSetting, System.currentTimeMillis());
//        // Sort Cursor by forecast date in ascending order
//        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " " + WeatherContract.SORT_ORDER_ASCENDING;
//        Cursor cursor = getActivity().getContentResolver().query(uri, null, null, null, sortOrder);
//        mForecastAdapter = new ForecastAdapter(getActivity(), cursor, flags);
        mForecastAdapter = new ForecastAdapter(getActivity(), null, flags);
        mForecastAdapter.setUseTodayLayout(mListener.isUsingTodayLayout());
//                new ArrayAdapter<String>(
//                        getActivity(), // The current context (this activity)
//                        R.layout.list_item_forecast, // The name of the layout ID.
//                        R.id.list_item_forecast_textview, // The ID of the textview to populate.
//                        new ArrayList<String>());
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Get a reference to the ListView, and attach this adapter to it.
        mListView = (ListView) rootView.findViewById(R.id.listview_forecast);
        mListView.setAdapter(mForecastAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // TODO: override ForecastAdapter.getItem()
                // CursorAdapter returns a cursor at the correct position for getItem(), or null
                // if it cannot seek to that position.
                Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
                if (cursor != null) {
                    String locationSetting = Utility.getPreferredLocation(getActivity());
                    long dateMsec = cursor.getLong(WeatherContract.COL_WEATHER_DATE);
                    if (mListener != null) {
                        mListener.onListItemClick(locationSetting, dateMsec);
                    }
                }
                mListViewPosition = position;
                Log.i(TAG, "onItemClick()"
                        +"\t -- mListViewPosition: "+mListViewPosition
                );
            }

        });

        return rootView;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.v(TAG, "onActivityCreated()"
                + " -- savedInstanceState: " + savedInstanceState
                + " -- LOADER_ID: " + LOADER_ID
        );
        Bundle args = null;
        getLoaderManager().initLoader(LOADER_ID, args, this);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onViewStateRestored()"
                + "\t -- savedInstanceState: " +savedInstanceState
        );



        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null){
            mListViewPosition = savedInstanceState.getInt(KEY_LISTVIEW_SELECTED_POSITION, INVALID_LISTVIEW_POSITION);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
            if (mListViewPosition > INVALID_LISTVIEW_POSITION){
                savedInstanceState.putInt(KEY_LISTVIEW_SELECTED_POSITION, mListViewPosition);
            }
        Log.i(TAG, "onSaveInstanceState"
            +"\t -- mListViewPosition: "+mListViewPosition
                +"\t -- savedInstanceState: "+savedInstanceState
        );
    }

    /**
     * Unregister {@link #mListener} and {@link #mContentObserver} to prevent memory leaks and .
     */
    @Override
    public void onDetach() {
        mListener = null;
        Activity activity = getActivity();
        if (activity != null) {
            activity.getContentResolver().unregisterContentObserver(mContentObserver);
        }
        super.onDetach();
    }

    /**
     * Start fetching weather data for the current location specified by the user in
     * {@link SharedPreferences}.  We do not restart the {@link CursorLoader} here, and instead do
     * it in the {@link #mContentObserver} callback, when we know the {@link FetchWeatherTask} has
     * successfully updated the {@link com.example.android.sunshine.app.data.WeatherProvider} database.
     */
    private void updateWeather() {
        Log.i(TAG, "updateWeather()");
        SunshineSyncAdapter.syncImmediately(getContext());
    }

    /**
     * A {@link LoaderManager.LoaderCallbacks} method that creates a {@link Loader} if requested by
     * the {@link LoaderManager}
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader cursorLoader;
        String locationSetting = Utility.getPreferredLocation(getActivity());
        Uri uri =
//                WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(locationSetting, System.currentTimeMillis());
                WeatherContract.WeatherEntry.buildWeatherLocation(locationSetting);
        // Sort Cursor by forecast date in ascending order
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " " + WeatherContract.SORT_ORDER_ASCENDING;
        cursorLoader = new CursorLoader(getActivity(), uri, WeatherContract.FORECAST_COLUMNS, null, null, sortOrder);
        Log.d(TAG, "onCreateLoader()"
                + "\n\t -- id: " + id
                + "\n\t -- uri: " + uri
                + "\n\t -- cursorLoader: " + cursorLoader
        );
        if (id == LOADER_ID) {
        } else {
            Log.w(TAG, "onCreateLoader() -- UNRECOGNIZED id: " + id);
        }
        return cursorLoader;
    }

    /**
     * A {@link LoaderManager.LoaderCallbacks} method
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.v(TAG, "onLoadFinished()"
                + "\n\t -- loader.getId(): " + loader.getId()
                + "\n\t -- loader: " + loader
                + "\n\t -- mListView: " + mListView
        );
        mForecastAdapter.swapCursor(data);
        if (mListViewPosition > INVALID_LISTVIEW_POSITION){
            mListView.smoothScrollToPosition(mListViewPosition);
            mListView.setItemChecked(mListViewPosition, true);
        }
        mLocation = extractMapQueryParam(data);
//        WeatherDbHelper.printCursor(data);
//        mForecastAdapter.notifyDataSetChanged();

//        mListView.setAdapter(mForecastAdapter);
//        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//
//            @Override
//            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
//                // TODO: override ForecastAdapter.getItem()
//                Object forecast = mForecastAdapter.getItem(position);
//                Toast.makeText(getActivity(), "I don't do anything yet", Toast.LENGTH_SHORT).show();
////                Intent intent = new Intent(getActivity(), DetailActivity.class)
////                        .putExtra(Intent.EXTRA_TEXT, forecast);
////                startActivity(intent);
//            }
//        });
    }

    /**
     * Create the query parameter for the Map Location menu item by pulling the latitude, longitude,
     * and location_setting from the {@link Cursor} and building a {@link String} conforming to the
     * Map ACTION_VIEW URI scheme.
     * @param data
     * @return
     */
    private String extractMapQueryParam(Cursor data) {
        if (data.isAfterLast() || data.isBeforeFirst()){
            data.moveToFirst();
        }
        return new StringBuilder()
                .append(data.getFloat(WeatherContract.COL_COORD_LAT))
                .append(",")
                .append(data.getFloat(WeatherContract.COL_COORD_LONG))
                .append("(")
                .append(data.getString(WeatherContract.COL_LOCATION_SETTING))
                .append(")")
                .toString();
    }

    /**
     * A {@link LoaderManager.LoaderCallbacks} method
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.v(TAG, "onLoaderReset()"
                + "\n\t -- loader.getId(): " + loader.getId()
                + "\n\t -- loader: " + loader
        );
        mForecastAdapter.swapCursor(null);
    }

    /**
     * Update the {@link com.example.android.sunshine.app.data.WeatherProvider} database with the
     * fresh weather data for the new location. We restart the {@link CursorLoader} in case the
     * data is already cached in the database; it's better to use that than to wait for the network
     * task, even if the data is stale.
     *
     * @param location
     */
    public void onLocationChanged(String location) {
        updateWeather();
        restartCursorLoader();
    }


    /**
     * Use {@link Listener} to listen for list item clicks in the hosting {@link Activity}
     */
    public interface Listener {
        void onListItemClick(String locationSetting, long dateInSec);
        boolean isUsingTodayLayout();
    }

    //    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {
//
//        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
//
//        /* The date/time conversion code is going to be moved outside the asynctask later,
//         * so for convenience we're breaking it out into its own method now.
//         */
//        private String getReadableDateString(long time){
//            // Because the API returns a unix timestamp (measured in seconds),
//            // it must be converted to milliseconds in order to be converted to valid date.
//            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
//            return shortenedDateFormat.format(time);
//        }
//
//        /**
//         * Prepare the weather high/lows for presentation.
//         */
//        private String formatHighLows(double high, double low, String unitType) {
//
//            if (unitType.equals(getString(R.string.pref_units_imperial))) {
//                high = (high * 1.8) + 32;
//                low = (low * 1.8) + 32;
//            } else if (!unitType.equals(getString(R.string.pref_units_metric))) {
//                Log.d(LOG_TAG, "Unit type not found: " + unitType);
//            }
//
//            // For presentation, assume the user doesn't care about tenths of a degree.
//            long roundedHigh = Math.round(high);
//            long roundedLow = Math.round(low);
//
//            String highLowStr = roundedHigh + "/" + roundedLow;
//            return highLowStr;
//        }
//
//        /**
//         * Take the String representing the complete forecast in JSON Format and
//         * pull out the data we need to construct the Strings needed for the wireframes.
//         *
//         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
//         * into an Object hierarchy for us.
//         */
//        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
//                throws JSONException {
//
//            // These are the names of the JSON objects that need to be extracted.
//            final String OWM_LIST = "list";
//            final String OWM_WEATHER = "weather";
//            final String OWM_TEMPERATURE = "temp";
//            final String OWM_MAX = "max";
//            final String OWM_MIN = "min";
//            final String OWM_DESCRIPTION = "main";
//
//            JSONObject forecastJson = new JSONObject(forecastJsonStr);
//            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);
//
//            // OWM returns daily forecasts based upon the local time of the city that is being
//            // asked for, which means that we need to know the GMT offset to translate this data
//            // properly.
//
//            // Since this data is also sent in-order and the first day is always the
//            // current day, we're going to take advantage of that to get a nice
//            // normalized UTC date for all of our weather.
//
//            Time dayTime = new Time();
//            dayTime.setToNow();
//
//            // we start at the day returned by local time. Otherwise this is a mess.
//            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);
//
//            // now we work exclusively in UTC
//            dayTime = new Time();
//
//            String[] resultStrs = new String[numDays];
//
//            // Data is fetched in Celsius by default.
//            // If user prefers to see in Fahrenheit, convert the values here.
//            // We do this rather than fetching in Fahrenheit so that the user can
//            // change this option without us having to re-fetch the data once
//            // we start storing the values in a database.
//            SharedPreferences sharedPrefs =
//                    PreferenceManager.getDefaultSharedPreferences(getActivity());
//            String unitType = sharedPrefs.getString(
//                    getString(R.string.pref_units_key),
//                    getString(R.string.pref_units_metric));
//
//            for(int i = 0; i < weatherArray.length(); i++) {
//                // For now, using the format "Day, description, hi/low"
//                String day;
//                String description;
//                String highAndLow;
//
//                // Get the JSON object representing the day
//                JSONObject dayForecast = weatherArray.getJSONObject(i);
//
//                // The date/time is returned as a long.  We need to convert that
//                // into something human-readable, since most people won't read "1400356800" as
//                // "this saturday".
//                long dateTime;
//                // Cheating to convert this to UTC time, which is what we want anyhow
//                dateTime = dayTime.setJulianDay(julianStartDay+i);
//                day = getReadableDateString(dateTime);
//
//                // description is in a child array called "weather", which is 1 element long.
//                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
//                description = weatherObject.getString(OWM_DESCRIPTION);
//
//                // Temperatures are in a child object called "temp".  Try not to name variables
//                // "temp" when working with temperature.  It confuses everybody.
//                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
//                double high = temperatureObject.getDouble(OWM_MAX);
//                double low = temperatureObject.getDouble(OWM_MIN);
//
//                highAndLow = formatHighLows(high, low, unitType);
//                resultStrs[i] = day + " - " + description + " - " + highAndLow;
//            }
//            return resultStrs;
//
//        }
//        @Override
//        protected String[] doInBackground(String... params) {
//
//            // If there's no zip code, there's nothing to look up.  Verify size of params.
//            if (params.length == 0) {
//                return null;
//            }
//
//            // These two need to be declared outside the try/catch
//            // so that they can be closed in the finally block.
//            HttpURLConnection urlConnection = null;
//            BufferedReader reader = null;
//
//            // Will contain the raw JSON response as a string.
//            String forecastJsonStr = null;
//
//            String format = "json";
//            String units = "metric";
//            int numDays = 7;
//
//            try {
//                // Construct the URL for the OpenWeatherMap query
//                // Possible parameters are avaiable at OWM's forecast API page, at
//                // http://openweathermap.org/API#forecast
//                final String FORECAST_BASE_URL =
//                        "http://api.openweathermap.org/data/2.5/forecast/daily?";
//                final String QUERY_PARAM = "q";
//                final String FORMAT_PARAM = "mode";
//                final String UNITS_PARAM = "units";
//                final String DAYS_PARAM = "cnt";
//
//                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
//                        .appendQueryParameter(QUERY_PARAM, params[0])
//                        .appendQueryParameter(FORMAT_PARAM, format)
//                        .appendQueryParameter(UNITS_PARAM, units)
//                        .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
//                        .build();
//
//                URL url = new URL(builtUri.toString());
//
//                // Create the request to OpenWeatherMap, and open the connection
//                urlConnection = (HttpURLConnection) url.openConnection();
//                urlConnection.setRequestMethod("GET");
//                urlConnection.connect();
//
//                // Read the input stream into a String
//                InputStream inputStream = urlConnection.getInputStream();
//                StringBuffer buffer = new StringBuffer();
//                if (inputStream == null) {
//                    // Nothing to do.
//                    return null;
//                }
//                reader = new BufferedReader(new InputStreamReader(inputStream));
//
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
//                    // But it does make debugging a *lot* easier if you print out the completed
//                    // buffer for debugging.
//                    buffer.append(line + "\n");
//                }
//
//                if (buffer.length() == 0) {
//                    // Stream was empty.  No point in parsing.
//                    return null;
//                }
//                forecastJsonStr = buffer.toString();
//            } catch (IOException e) {
//                Log.e(LOG_TAG, "Error ", e);
//                // If the code didn't successfully get the weather data, there's no point in attempting
//                // to parse it.
//                return null;
//            } finally {
//                if (urlConnection != null) {
//                    urlConnection.disconnect();
//                }
//                if (reader != null) {
//                    try {
//                        reader.close();
//                    } catch (final IOException e) {
//                        Log.e(LOG_TAG, "Error closing stream", e);
//                    }
//                }
//            }
//
//            try {
//                return getWeatherDataFromJson(forecastJsonStr, numDays);
//            } catch (JSONException e) {
//                Log.e(LOG_TAG, e.getMessage(), e);
//                e.printStackTrace();
//            }
//
//            // This will only happen if there was an error getting or parsing the forecast.
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute(String[] result) {
//            if (result != null) {
//                mForecastAdapter.clear();
//                for(String dayForecastStr : result) {
//                    mForecastAdapter.add(dayForecastStr);
//                }
//                // New data is back from the server.  Hooray!
//            }
//        }
//    }

}
