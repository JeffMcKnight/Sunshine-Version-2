package com.example.android.sunshine.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.android.sunshine.app.data.WeatherContract;
import com.example.android.sunshine.app.data.WeatherDbHelper;

import java.util.concurrent.TimeUnit;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailFragment
        extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>, FetchWeatherTask.Listener {

    private static final String LOG_TAG = DetailFragment.class.getSimpleName();

    private static final String FORECAST_SHARE_HASHTAG = " #SunshineApp";
    public static final String ID_TAG = DetailFragment.class.getCanonicalName() + ".id_tag";
    private static final String ARG_LOCATION = DetailFragment.class.getCanonicalName() + ".arg_location";
    private static final String ARG_DATE = DetailFragment.class.getCanonicalName() + ".arg_date";
    private final int LOADER_ID = hashCode();
    public static final String[] DETAILS_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_HUMIDITY,
            WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,
            WeatherContract.WeatherEntry.COLUMN_DEGREES,
            WeatherContract.WeatherEntry.COLUMN_PRESSURE
    };

    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    public static final int COL_WEATHER_ID = 0;
    public static final int COL_WEATHER_DATE = 1;
    public static final int COL_WEATHER_DESC = 2;
    public static final int COL_WEATHER_MAX_TEMP = 3;
    public static final int COL_WEATHER_MIN_TEMP = 4;
    public static final int COL_LOCATION_SETTING = 5;
    public static final int COL_WEATHER_CONDITION_ID = 6;
    public static final int COL_HUMIDITY = 7;
    public static final int COL_WIND_SPEED = 8;
    public static final int COL_WIND_DIRECTION = 9;
    public static final int COL_PRESSURE = 10;

    private CharSequence mForecastStr;
    private TextView mDetailTextView;
    private Uri mDetailsUri;
    private ShareActionProvider mShareActionProvider;
    private ViewHolder mViewHolder;

    public DetailFragment() {
        setHasOptionsMenu(true);
    }

    public static void attach(FragmentActivity activity, String location, long dateInSec) {
        DetailFragment detailFragment = new DetailFragment();
        Bundle fragmentArguments = new Bundle();
        fragmentArguments.putString(ARG_LOCATION, location);
        fragmentArguments.putLong(ARG_DATE, dateInSec);
        detailFragment.setArguments(fragmentArguments);
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.weather_detail_container, detailFragment, ID_TAG)
                .commit();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        Intent intent = activity.getIntent();
        if (intent != null && intent.getData() != null) {
            mDetailsUri = intent.getData();
        } else {
            mDetailsUri = parseArguments(getArguments());
        }
        Log.i(LOG_TAG, "onAttach()"
            +"\t -- mDetailsUri: "+mDetailsUri
        );
        getLoaderManager().initLoader(LOADER_ID, null, this);
    }

    public Uri parseArguments(Bundle fragmentArguments) {
        String locationSetting;
        if (fragmentArguments.containsKey(ARG_LOCATION)){
            locationSetting = fragmentArguments.getString(ARG_LOCATION);
        } else {
            locationSetting = Utility.getPreferredLocation(getActivity());
        }
        long dateInSec;
        if (fragmentArguments.containsKey(ARG_DATE)){
            dateInSec = fragmentArguments.getLong(ARG_DATE);
        } else {
            dateInSec = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
        }
        return WeatherContract.WeatherEntry.buildWeatherLocationWithDate(locationSetting, dateInSec);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(LOG_TAG, "onCreateView()");

        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        // The detail Activity called via intent.  Inspect the intent for forecast data.
//            mDetailTextView = (TextView) rootView.findViewById(R.id.detail_text);
        mViewHolder = new ViewHolder(rootView);


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
        Log.i(LOG_TAG, "onActivityCreated()");
        super.onActivityCreated(savedInstanceState);
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
        cursorLoader = new CursorLoader(getActivity(), mDetailsUri, DETAILS_COLUMNS, null, null, sortOrder);
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
//        WeatherDbHelper.printCursor(data);
        mForecastStr = convertCursorRowToDetailsFormat(data);
        mViewHolder.updateViewHolder(data, this.getActivity());
//            mDetailTextView.setText(mForecastStr);
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
            double maxTemp = cursor.getDouble(COL_WEATHER_MAX_TEMP);
            double minTemp = cursor.getDouble(COL_WEATHER_MIN_TEMP);

            boolean isMetric = Utility.isMetric(getActivity());
            String highLowStr
                    = Utility.formatTemperature(maxTemp, isMetric, getActivity())
                    + "/"
                    + Utility.formatTemperature(minTemp, isMetric, getActivity());

            uxFormat = Utility.formatDate(cursor.getLong(COL_WEATHER_DATE)) +
                    " - " + cursor.getString(COL_WEATHER_DESC) +
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
        Log.i(LOG_TAG, "onLoaderReset()");

    }

    /**
     * Initialize or restart the {@link CursorLoader} so we can display the updated weather data
     */
    @Override
    public void onWeatherUpdate() {
        if (getLoaderManager().getLoader(LOADER_ID) == null){
            getLoaderManager().initLoader(LOADER_ID, null, this);
        } else {
            getLoaderManager().restartLoader(LOADER_ID, null, this);
        }
    }

    public static class ViewHolder {
        final ImageView mIconView;
        final TextView mDayView;
        final TextView mDateView;
        final TextView mForecastDescriptionView;
        final TextView mMaxTempView;
        final TextView mMinTempView;
        final TextView mHumidityView;
        final TextView mWindView;
        final TextView mPressureView;

        public ViewHolder(View view){
            mIconView = (ImageView) view.findViewById(R.id.detail_item_icon);
            mDayView = (TextView) view.findViewById(R.id.detail_item_day_textview);
            mDateView = (TextView) view.findViewById(R.id.detail_item_date_textview);
            mForecastDescriptionView = (TextView)view.findViewById(R.id.detail_item_forecast_textview);
            mMaxTempView = (TextView)view.findViewById(R.id.detail_item_high_textview);
            mMinTempView = (TextView)view.findViewById(R.id.detail_item_low_textview);
            mHumidityView = (TextView)view.findViewById(R.id.detail_item_humidity_textview);
            mWindView = (TextView)view.findViewById(R.id.detail_item_wind_textview);
            mPressureView = (TextView)view.findViewById(R.id.detail_item_pressure_textview);
        }

        public void updateViewHolder(Cursor data, Context context) {
            if (data.moveToFirst()){
                Log.i(DetailFragment.LOG_TAG, "onLoadFinished()"
                        +"\n\t -- data.getPosition(): "+data.getPosition()
                );
                mIconView.setImageDrawable(
                        ContextCompat.getDrawable(
                                context,
                                Utility.getArtResourceForWeatherCondition(
                                        data.getInt(COL_WEATHER_CONDITION_ID)
                                )
                        )
                );
                mDayView.setText(Utility.getDayName(context, data.getLong(DetailFragment.COL_WEATHER_DATE)));
                mDateView.setText(Utility.formatDate(data.getLong(DetailFragment.COL_WEATHER_DATE)));
                boolean isMetric = Utility.isMetric(context);
                mMaxTempView.setText(Utility.formatTemperature(
                        data.getLong(DetailFragment.COL_WEATHER_MAX_TEMP),
                        isMetric,
                        context
                ));
                mMinTempView.setText(Utility.formatTemperature(
                        data.getLong(DetailFragment.COL_WEATHER_MIN_TEMP),
                        isMetric,
                        context
                ));
                mForecastDescriptionView.setText(data.getString(DetailFragment.COL_WEATHER_DESC));
                mHumidityView.setText(Utility.formatHumidity(data.getInt(DetailFragment.COL_HUMIDITY), context));
                mWindView.setText(Utility.formatWindSpeed(
                        data.getDouble(DetailFragment.COL_WIND_SPEED),
                        data.getDouble(DetailFragment.COL_WIND_DIRECTION),
                        context));
                mPressureView.setText(Utility.formatPressure(data.getDouble(DetailFragment.COL_PRESSURE), context));
            }
        }
    }

}
