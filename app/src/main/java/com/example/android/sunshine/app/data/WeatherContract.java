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
package com.example.android.sunshine.app.data;

import android.content.ContentValues;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.text.format.Time;
import android.util.Log;

import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

/**
 * Defines table and column names for the weather database.
 */
public class WeatherContract {

    public static final String DATABASE_NAME = "weather.db";
    public static final String CONTENT_SCHEME = "content";
    public static final String CONTENT_AUTHORITY = "com.example.android.sunshine.app";
    private static final Uri BASE_CONTENT_URI = (new Uri.Builder())
            .scheme(CONTENT_SCHEME)
            .authority(CONTENT_AUTHORITY)
            .build();
    public static final String SORT_ORDER_ASCENDING = "ASC";
    public static final String DESCENDING = "DEC";
    public static final String[] FORECAST_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            WeatherEntry.TABLE_NAME + "." + WeatherEntry._ID,
            WeatherEntry.COLUMN_DATE,
            WeatherEntry.COLUMN_SHORT_DESC,
            WeatherEntry.COLUMN_MAX_TEMP,
            WeatherEntry.COLUMN_MIN_TEMP,
            LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherEntry.COLUMN_WEATHER_ID,
            LocationEntry.COLUMN_COORD_LAT,
            LocationEntry.COLUMN_COORD_LONG
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
    public static final int COL_COORD_LAT = 7;
    public static final int COL_COORD_LONG = 8;
    private static final String TAG = WeatherContract.class.getSimpleName();


    /**
     // To make it easy to query for the exact date, we normalize all dates that go into
     // the database to the start of the the Julian day at UTC.
     *
     * @param unnormallizedDateSec the unnormalized timedate stamp, in seconds
     * @return the normalized timedate stamp, in seconds
     */
    public static long normalizeDate(long unnormallizedDateSec) {
        // normalize the start date to the beginning of the (UTC) day
        Time time = new Time();
        /** Set time to current time so {@link Time#gmtoff} is correct */
        time.setToNow();
        /** {@link Time} wants time in milliseconds, so convert from seconds */
        long unnormallizedDateMsec = TimeUnit.SECONDS.toMillis(unnormallizedDateSec);
        int julianDay = Time.getJulianDay(unnormallizedDateMsec, time.gmtoff);
        long normalizedDateMsec = time.setJulianDay(julianDay);
        /** {@link android.database.sqlite.SQLiteDatabase} wants time in seconds, so convert back from msec */
        return TimeUnit.MILLISECONDS.toSeconds(normalizedDateMsec);
    }

    /*
        Inner class that defines the table contents of the location table
        Students: This is where you will add the strings.  (Similar to what has been
        done for WeatherEntry)
     */
    public static final class LocationEntry implements BaseColumns {
        public static final String TABLE_NAME = "location";
        public static final String PATH = TABLE_NAME;
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH).build();
        /**
         * The name of the columns in the location database
         */
        public static final String COLUMN_LOCATION_SETTING = "location_setting";
        public static final String COLUMN_COORD_LAT = "coord_lat";
        public static final String COLUMN_COORD_LONG = "coord_long";
        public static final String COLUMN_CITY_NAME = "city_name";
        public static final String[] COLUMN_NAMES = {
                COLUMN_LOCATION_SETTING,
                COLUMN_COORD_LAT,
                COLUMN_COORD_LONG,
                COLUMN_CITY_NAME
        };
        private static final String KEY = "id";
        /**
         * The MIME type; TODO: probably should not be the table's name
         */
        public static final String CONTENT_TYPE = TABLE_NAME;
        private static final String TAG = LocationEntry.class.getSimpleName();

        /**
         * Use this method to build a URI to query the location table
         * @param id
         * @return
         */
        public static Uri buildLocationUri(long id){
            return CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build();

        }

        public static Uri buildLocationUri(String locationSetting) {
            return CONTENT_URI.buildUpon().appendPath(locationSetting).build();
        }

        /**
         * Builds a URI that indicates the table and row affected by the {@link android.content.ContentProvider}
         * @param id
         * @return
         */
        public static Uri buildReturnUri(long id) {
            return CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build();
        }

        public static long getRowIdFromReturnUri(Uri uri){
            Long rowId = 0L;
            String lastPathSegment = null;
            if (WeatherProvider.buildUriMatcher().match(uri) == WeatherProvider.LOCATION_WITH_ID){
                lastPathSegment = uri.getLastPathSegment();
                rowId = Long.parseLong(lastPathSegment);
            }
            Log.i(TAG, "getRowIdFromReturnUri()"
                            +"\t -- uri: "+uri
                            +"\t -- lastPathSegment: "+lastPathSegment
                            +"\t -- rowId: "+rowId
            );
            try {
                if (rowId == null){
                    throw new ContractViolationException("*** Last path segment should be a long *** lastPathSegment: " + lastPathSegment);
                }
            } catch (ContractViolationException e) {
                e.printStackTrace();
            }
            return rowId;
        }

        @NonNull
        public static ContentValues buildContentValues(
                @NonNull String locationSetting,
                @NonNull String cityName,
                double lat,
                double lon) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(COLUMN_LOCATION_SETTING, locationSetting);
            contentValues.put(COLUMN_CITY_NAME, cityName);
            contentValues.put(COLUMN_COORD_LAT, lat);
            contentValues.put(COLUMN_COORD_LONG, lon);
            return contentValues;
        }
    }

    /* Inner class that defines the table contents of the weather table */
    public static final class WeatherEntry implements BaseColumns {
        private static final String TAG = WeatherEntry.class.getSimpleName();

        public static final String TABLE_NAME = "weather";
        public static final String PATH= WeatherEntry.TABLE_NAME;
        /**
         * The base URI for weather table queries.
         */
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH).build();

        // Column with the foreign key into the location table.
        public static final String COLUMN_LOC_KEY = "location_id";
        // Date, stored as long in milliseconds since the epoch
        public static final String COLUMN_DATE = "date";
        // Weather id as returned by API, to identify the icon to be used
        public static final String COLUMN_WEATHER_ID = "weather_id";

        // Short description and long description of the weather, as provided by API.
        // e.g "clear" vs "sky is clear".
        public static final String COLUMN_SHORT_DESC = "short_desc";

        // Min and max temperatures for the day (stored as floats)
        public static final String COLUMN_MIN_TEMP = "min";
        public static final String COLUMN_MAX_TEMP = "max";

        // Humidity is stored as a float representing percentage
        public static final String COLUMN_HUMIDITY = "humidity";

        // Humidity is stored as a float representing percentage
        public static final String COLUMN_PRESSURE = "pressure";

        // Wind speed is stored as a float representing wind speed  mph
        public static final String COLUMN_WIND_SPEED = "wind";

        // Degrees are meteorological degrees (e.g, 0 is north, 180 is south).  Stored as floats.
        public static final String COLUMN_DEGREES = "degrees";

        // MIME-type (?)
        public static final String CONTENT_TYPE = "x-" + TABLE_NAME;

        // MIME-type for location and data resource (?)
        public static final String CONTENT_ITEM_TYPE = CONTENT_TYPE + "/" + "x-location-date";
        public static final int PATH_SEGMENT_LOCATION = 1;

        /**
         * Use this method to build a URI to query the weather table for a specific location
         *
         * @param location
         * @return
         */
        public static Uri buildWeatherLocation(String location) {
            return CONTENT_URI.buildUpon().appendPath(location).build();
        }

        /**
         * Use this method to build a URI to query the weather table for a specific location on or after a specific date
         * TODO: convert or format timeMillis (or maybe it's ok as is?)
         *
         * @param location the location we want to query the database for
         * @param timeMillis  the time in msec of the start of the date range of forecasts we want;
         *                    convert to seconds, which is the standard format for SQLite
         * @return
         */
        public static Uri buildWeatherLocationWithStartDate(String location, long timeMillis) {
            Uri uri = buildWeatherLocation(location)
                    .buildUpon()
                    .appendPath(String.valueOf(TimeUnit.MILLISECONDS.toSeconds(timeMillis)))
                    .build();
            Log.i(TAG, "buildWeatherLocationWithStartDate()"
                +"\t -- uri.toString(): "+uri.toString()
            );
            return uri;
        }

        public static String getLocationSettingFromUri(Uri uri) {
            return uri.getPathSegments().get(PATH_SEGMENT_LOCATION);
        }

        /**
         * TODO: not sure what this should return; just make it the same as {@link #getDateFromUri(Uri)} for now
         * @param uri
         * @return the time in msec (since the start of the epoch)
         */
        public static long getStartDateFromUri(Uri uri) {
            return getDateFromUri(uri);
        }

        /**
         * FIXME: maybe it would be better to use WeatherProvider.sUriMatcher, but we'd still
         * want to throw an Exception if we couldn't extract the date, so this implementation is probably ok
         * @param uri
         * @return the extracted date if the uri contains a date, or zero if it does not
         */
        public static long getDateFromUri(Uri uri) {
            Long date = 0L;
            String lastPathSegment = null;
            if (WeatherProvider.buildUriMatcher().match(uri) == WeatherProvider.WEATHER_WITH_LOCATION_AND_DATE){
                lastPathSegment = uri.getLastPathSegment();
                date = Long.parseLong(lastPathSegment);
            }
            Log.i(TAG, "getDateFromUri()"
                            +"\t -- uri: "+uri
                            +"\t -- lastPathSegment: "+lastPathSegment
                            +"\t -- date: "+date
            );
            try {
                if (date == null){
                    throw new ContractViolationException("*** Last path segment should be a long *** lastPathSegment: " + lastPathSegment);
                }
            } catch (ContractViolationException e) {
                e.printStackTrace();
            }
            return date;
        }

        public static Uri buildWeatherUri(long id) {
            return CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build();
        }

        /**
         * Do we need this? Seems to duplicate {@link #buildWeatherLocationWithStartDate(String, long)}
         * TODO: convert or format the data parameter (or maybe it's ok as is?)
         * @param location
         * @param dateInSec
         * @return
         */
        public static Uri buildWeatherLocationWithDate(String location, long dateInSec) {
            return buildWeatherLocation(location).buildUpon().appendPath(String.valueOf(dateInSec)).build();
        }

        /**
         * Builds a URI that indicates the table and row affected by the {@link android.content.ContentProvider}
         * @param id
         * @return
         */
        public static Uri buildReturnUri(long id) {
            return CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build();
        }
    }

    private static class ContractViolationException extends Exception {
        public ContractViolationException(String detailMessage) {
            super(detailMessage);
        }
    }
}
