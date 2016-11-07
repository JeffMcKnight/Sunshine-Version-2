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

import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.concurrent.TimeUnit;

public class WeatherProvider extends ContentProvider {

    private static final String TAG = WeatherProvider.class.getSimpleName();
    // The URI Matcher used by this content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    public static final String MSG_UNKNOWN_URI = "Unknown uri: ";
    private WeatherDbHelper mOpenHelper;

    static final int WEATHER = 100;
    static final int WEATHER_WITH_LOCATION = 101;
    static final int WEATHER_WITH_LOCATION_AND_DATE = 102;
    static final int LOCATION = 300;
    static final int LOCATION_WITH_ID = 301;

    private static final SQLiteQueryBuilder sWeatherByLocationSettingQueryBuilder;

    static{
        sWeatherByLocationSettingQueryBuilder = new SQLiteQueryBuilder();
        
        //This is an inner join which looks like
        //weather INNER JOIN location ON weather.location_id = location._id
        String inTables
                = WeatherContract.WeatherEntry.TABLE_NAME
                + " INNER JOIN "
                + WeatherContract.LocationEntry.TABLE_NAME
                + " ON "
                + WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry.COLUMN_LOC_KEY +
                " = "
                + WeatherContract.LocationEntry.TABLE_NAME + "." + WeatherContract.LocationEntry._ID;
        sWeatherByLocationSettingQueryBuilder.setTables(inTables);
    }

    //location.location_setting = ?
    private static final String sLocationSettingSelection =
            WeatherContract.LocationEntry.TABLE_NAME+
                    "." + WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? ";

    //location.location_setting = ? AND date >= ?
    private static final String sLocationSettingWithStartDateSelection =
            WeatherContract.LocationEntry.TABLE_NAME+
                    "." + WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND " +
                    WeatherContract.WeatherEntry.COLUMN_DATE + " >= ? ";

    //location.location_setting = ? AND date = ?
    private static final String sLocationSettingAndDaySelection =
            WeatherContract.LocationEntry.TABLE_NAME +
                    "." + WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND " +
                    WeatherContract.WeatherEntry.COLUMN_DATE + " = ? ";

    /**
     *
     * @param uri
     * @param projection
     * @param sortOrder
     * @return
     */
    private Cursor getWeatherByLocationSetting(Uri uri, String[] projection, String sortOrder) {
        String locationSetting = WeatherContract.WeatherEntry.getLocationSettingFromUri(uri);
        long startDate = WeatherContract.WeatherEntry.getStartDateFromUri(uri);

        String[] selectionArgs;
        String selection;

        if (startDate == 0) {
            selection = sLocationSettingSelection;
            selectionArgs = new String[]{locationSetting};
        } else {
            selectionArgs = new String[]{locationSetting, Long.toString(startDate)};
            selection = sLocationSettingWithStartDateSelection;
        }

        return sWeatherByLocationSettingQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    /**
     *
     * @param uri
     * @param projection
     * @param sortOrder
     * @return
     */
    private Cursor getWeatherByLocationSettingAndDate(Uri uri, String[] projection, String sortOrder) {
        String locationSetting = WeatherContract.WeatherEntry.getLocationSettingFromUri(uri);
        long dateMsec = WeatherContract.WeatherEntry.getDateFromUri(uri);
        long normalizedDate = WeatherContract.normalizeDate(dateMsec);

        SQLiteDatabase database = mOpenHelper.getReadableDatabase();
        Cursor dumpAll = sWeatherByLocationSettingQueryBuilder.query(database, null, null, null, null, null, null);
        Log.d(TAG, "getWeatherByLocationSettingAndDate()"
                + "\n\t -- uri: " + uri
                + "\n\t -- locationSetting: " + locationSetting
                + "\t -- normalizedDate: " + normalizedDate
                + "\n\t -- sWeatherByLocationSettingQueryBuilder.getTables(): " + sWeatherByLocationSettingQueryBuilder.getTables()
                +"\n\t -- dumpAll.getCount(): " + dumpAll.getCount()
                +"\t -- dumpAll.getColumnCount(): " + dumpAll.getColumnCount()
        );

        return sWeatherByLocationSettingQueryBuilder.query(database,
                projection,
                sLocationSettingAndDaySelection,
                new String[]{locationSetting, Long.toString(normalizedDate)},
                null,
                null,
                sortOrder
        );
    }

    /**
     * Do a simple query with whatever query parameters we get
     *
     * @param tableName
     * @param columns
     * @param selection
     * @param selectionArgs
     * @param orderBy
     * @return
     */
    private Cursor getCursorForTable(String tableName, String[] columns, String selection, String[] selectionArgs, String orderBy) {
        SQLiteDatabase database = mOpenHelper.getReadableDatabase();
        Cursor cursor = database.query(tableName, columns, selection, selectionArgs, null, null, orderBy);
//        database.close();
        return cursor;
    }

    /*
        Students: Here is where you need to create the UriMatcher. This UriMatcher will
        match each URI to the WEATHER, WEATHER_WITH_LOCATION, WEATHER_WITH_LOCATION_AND_DATE,
        and LOCATION integer constants defined above.  You can test this by uncommenting the
        testUriMatcher test within TestUriMatcher.
     */
    static UriMatcher buildUriMatcher() {
        // 1) The code passed into the constructor represents the code to return for the root
        // URI.  It's common to use NO_MATCH as the code for this case. Add the constructor below.
        UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        // 2) Use the addURI function to match each of the types.  Use the constants from
        // WeatherContract to help define the types to the UriMatcher.
        uriMatcher.addURI(WeatherContract.CONTENT_AUTHORITY, WeatherContract.WeatherEntry.BASE_PATH, WeatherProvider.WEATHER);
        uriMatcher.addURI(WeatherContract.CONTENT_AUTHORITY, WeatherContract.WeatherEntry.BASE_PATH + "/*",WeatherProvider.WEATHER_WITH_LOCATION);
        uriMatcher.addURI(WeatherContract.CONTENT_AUTHORITY, WeatherContract.WeatherEntry.BASE_PATH + "/*/#", WeatherProvider.WEATHER_WITH_LOCATION_AND_DATE);
        uriMatcher.addURI(WeatherContract.CONTENT_AUTHORITY, WeatherContract.LocationEntry.PATH, WeatherProvider.LOCATION);
        uriMatcher.addURI(WeatherContract.CONTENT_AUTHORITY, WeatherContract.LocationEntry.PATH + "/#", WeatherProvider.LOCATION_WITH_ID);

        // 3) Return the new matcher!
        return uriMatcher;
    }

    /*
        Students: We've coded this for you.  We just create a new WeatherDbHelper for later use
        here.
     */
    @Override
    public boolean onCreate() {
        mOpenHelper = new WeatherDbHelper(getContext());
        return true;
    }

    /*

     */
    /**
     * Use this method to tell {@link ContentProvider} the MIME type of the resource specified by the URI
     *
     * Test this by uncommenting testGetType in TestProvider.
     *
     * @param uri
     * @return the MIME type of the resource specified by the URI
     */
    @Override
    public String getType(Uri uri) {

        // Use the Uri Matcher to determine what kind of URI this is.
        final int match = sUriMatcher.match(uri);

        switch (match) {
            // Student: Uncomment and fill out these two cases
            case WEATHER_WITH_LOCATION_AND_DATE:
                return WeatherContract.WeatherEntry.CONTENT_ITEM_TYPE;
            case WEATHER_WITH_LOCATION:
                return WeatherContract.WeatherEntry.CONTENT_TYPE;
            case WEATHER:
                return WeatherContract.WeatherEntry.CONTENT_TYPE;
            case LOCATION:
                return WeatherContract.LocationEntry.CONTENT_TYPE;
            case LOCATION_WITH_ID:
                return WeatherContract.LocationEntry.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri,
                        String[] projection,
                        String selection,
                        String[] selectionArgs,
                        String sortOrder) {
//        printTable(WeatherContract.WeatherEntry.TABLE_NAME);
//        printTable(WeatherContract.LocationEntry.TABLE_NAME);
        // Here's the switch statement that, given a URI, will determine what kind of request it is,
        // and query the database accordingly.
        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            // "weather/*/*"
            case WEATHER_WITH_LOCATION_AND_DATE: {
                Cursor rawCursor = getWeatherByLocationSettingAndDate(uri, projection, sortOrder);
                retCursor = new TimeUnitConvertingCursor(rawCursor);
                break;
            }
            // "weather/*"
            case WEATHER_WITH_LOCATION: {
                Cursor rawCursor = getWeatherByLocationSetting(uri, projection, sortOrder);
                retCursor = new TimeUnitConvertingCursor(rawCursor);
                break;
            }
            // "weather"
            case WEATHER: {
                Cursor rawCursor = getCursorForTable(
                        WeatherContract.WeatherEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        sortOrder);
                retCursor = new TimeUnitConvertingCursor(rawCursor);
                break;
            }
            // "location"
            case LOCATION: {
                retCursor = getCursorForTable(
                        WeatherContract.LocationEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        sortOrder);
                break;
            }

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        Log.d(TAG, "query()"
                +"\t -- uri: "+uri
                +"\n\t -- sUriMatcher.match(uri): "+sUriMatcher.match(uri)
                +"\t -- retCursor.getCount(): "+retCursor.getCount()
                +"\t -- getColumnCount(): "+retCursor.getColumnCount()
        );
//        WeatherDbHelper.printCursor(retCursor);
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    public void printTable(String tableName) {
        Log.i(TAG, "printTable() -- tableName: "+ tableName);
        Cursor cursor = getCursorForTable(tableName, null, null, null, null);
        WeatherDbHelper.printCursor(cursor);
    }

    /*
        Student: Add the ability to insert Locations to the implementation of this function.
     */
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        Log.i(TAG, "insert()"
                        + " -- match:" + match
                        + " -- uri:" + uri
                        + "\n\t -- values:" + values
        );
        printTable(WeatherContract.WeatherEntry.TABLE_NAME);
        switch (match) {
            case WEATHER: {
                validateWeatherContentValues(values);
                normalizeDate(values);
                long _id = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = WeatherContract.WeatherEntry.buildReturnUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case LOCATION: {
                validateLocationContentValues(values);
                long _id = db.insert(WeatherContract.LocationEntry.TABLE_NAME, null, values);
                Log.d(TAG, "insert()"
                    +"\t _id: "+_id
                );
                if ( _id > 0 ){
                    returnUri = WeatherContract.LocationEntry.buildReturnUri(_id);
                    Log.d(TAG, "insert()"
                                    +"\t returnUri: "+returnUri
                    );
                }
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            default:
                throw new UnsupportedOperationException(MSG_UNKNOWN_URI + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        db.close();
        return returnUri;
    }

    /**
     * Check all the fields that should be non-null in the Location table
     * @param values
     */
    private void validateWeatherContentValues(ContentValues values) {
        String keyName;
        keyName = WeatherContract.WeatherEntry.COLUMN_LOC_KEY;
        validateContentValue(values, keyName);
        keyName = WeatherContract.WeatherEntry.COLUMN_DATE;
        validateContentValue(values, keyName);
        keyName = WeatherContract.WeatherEntry.COLUMN_DEGREES;
        validateContentValue(values, keyName);
        keyName = WeatherContract.WeatherEntry.COLUMN_HUMIDITY;
        validateContentValue(values, keyName);
        keyName = WeatherContract.WeatherEntry.COLUMN_MAX_TEMP;
        validateContentValue(values, keyName);
        keyName = WeatherContract.WeatherEntry.COLUMN_MIN_TEMP;
        validateContentValue(values, keyName);
        keyName = WeatherContract.WeatherEntry.COLUMN_PRESSURE;
        validateContentValue(values, keyName);
        keyName = WeatherContract.WeatherEntry.COLUMN_SHORT_DESC;
        validateContentValue(values, keyName);
        keyName = WeatherContract.WeatherEntry.COLUMN_WEATHER_ID;
        validateContentValue(values, keyName);
        keyName = WeatherContract.WeatherEntry.COLUMN_WIND_SPEED;
        validateContentValue(values, keyName);
    }

    /**
     * Check all the fields that should be non-null in the Location table
     * @param values
     */
    private void validateLocationContentValues(ContentValues values) {
        String keyName;
        keyName = WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING;
        validateContentValue(values, keyName);
        keyName = WeatherContract.LocationEntry.COLUMN_CITY_NAME;
        validateContentValue(values, keyName);
        keyName = WeatherContract.LocationEntry.COLUMN_COORD_LAT;
        validateContentValue(values, keyName);
        keyName = WeatherContract.LocationEntry.COLUMN_COORD_LONG;
        validateContentValue(values, keyName);
    }

    /**
     * Log a warning if a required key is missing
     * @param values
     * @param keyName
     */
    private void validateContentValue(ContentValues values, String keyName) {
        if (!values.containsKey(keyName)){
            Log.w(TAG, "validateLocationContentValues()"
                            + " *** DOES NOT CONTAIN KEY: " + keyName
            );
        }
    }

    /**
     * Delete the rows specified by the {@code selection} parameter, or delete all rows if the
     * {@code selection} parameter is null
     * @param uri
     * @param selection
     * @param selectionArgs
     * @return
     */
    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        int rowsDeleted = 0;
        // Student: Start by getting a writable database
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();


        // Student: Use the uriMatcher to match the WEATHER and LOCATION URI's we are going to
        // handle.  If it doesn't match these, throw an UnsupportedOperationException.
        final int matchCode = sUriMatcher.match(uri);

        // We always substitute a "1" for a null selection parameter because this will cause a
        // SQLiteDatabase to perform the same action (delete all rows), but return a useful
        // value (number of rows deleted), instead of 0.
        String whereClause;
        if (selection == null) {
            whereClause = "1";
        } else {
            whereClause = selection;
        }
        switch (matchCode){
            case WEATHER:
                rowsDeleted = db.delete(WeatherContract.WeatherEntry.TABLE_NAME, whereClause, selectionArgs);
                break;
            case LOCATION:
                rowsDeleted = db.delete(WeatherContract.LocationEntry.TABLE_NAME, whereClause, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException(MSG_UNKNOWN_URI + uri);
        }

        // Student: A null value deletes all rows.  In my implementation of this, I only notified
        // the uri listeners (using the content resolver) if the rowsDeleted != 0 or the selection
        // is null.
        // Oh, and you should notify the listeners here.
        if(rowsDeleted != 0 ){
            getContext().getContentResolver().notifyChange(uri, null);
        }
        db.close();

        // Student: return the actual rows deleted
        return rowsDeleted;
    }

    /**
     * Expects dates in milliseconds
     * @param values
     */
    private void normalizeDate(ContentValues values) {
        // normalize the date value
        if (values.containsKey(WeatherContract.WeatherEntry.COLUMN_DATE)) {
            long dateMsec = values.getAsLong(WeatherContract.WeatherEntry.COLUMN_DATE);
            values.put(WeatherContract.WeatherEntry.COLUMN_DATE, WeatherContract.normalizeDate(dateMsec));
        }
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // Student: This is a lot like the delete function.  We return the number of rows impacted
        // by the update.
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int rowsUpdated=0;
        int matchCode = sUriMatcher.match(uri);
        switch (matchCode){
            case WEATHER:
                rowsUpdated = db.update(WeatherContract.WeatherEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            case LOCATION:
                rowsUpdated = db.update(WeatherContract.LocationEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException(MSG_UNKNOWN_URI+uri);
        }
        if (rowsUpdated > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        db.close();

        return rowsUpdated;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Log.i(TAG, "bulkInsert()"
                +"\t -- match: "+match
                +"\t -- uri: "+uri
        );
        printTable(WeatherContract.WeatherEntry.TABLE_NAME);
        int returnCount;
        switch (match) {
            case WEATHER:
                db.beginTransaction();
                returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        normalizeDate(value);
                        long _id = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                break;
            default:
            returnCount = super.bulkInsert(uri, values);
            break;
        }
        printTable(WeatherContract.WeatherEntry.TABLE_NAME);
        printTable(WeatherContract.LocationEntry.TABLE_NAME);
        return returnCount;
    }

    // You do not need to call this method. This is a method specifically to assist the testing
    // framework in running smoothly. You can read more at:
    // http://developer.android.com/reference/android/content/ContentProvider.html#shutdown()
    @Override
    @TargetApi(11)
    public void shutdown() {
        mOpenHelper.close();
        super.shutdown();
    }


    public static UriMatcher getUriMatcher() {
        return sUriMatcher;
    }

    /**
     * Use {@link TimeUnitConvertingCursor} to wrap the {@link Cursor} returned by a query of the
     * {@link com.example.android.sunshine.app.data.WeatherContract.WeatherEntry#TABLE_NAME} table
     * in order to convert the {@link com.example.android.sunshine.app.data.WeatherContract.WeatherEntry#COLUMN_DATE }
     * column from {@link TimeUnit#SECONDS} (which is the standard for date fields in a {@link SQLiteDatabase})
     * to {@link TimeUnit#MILLISECONDS} (which we use everywhere else in the app).
     */
    private class TimeUnitConvertingCursor extends CursorWrapper {

        /**
         * Creates a cursor wrapper.
         *
         * @param cursor The underlying cursor to wrap.
         */
        public TimeUnitConvertingCursor(Cursor cursor) {
            super(cursor);
        }

        /**
         * Convert the {@link WeatherContract#COL_WEATHER_DATE} to {@link TimeUnit#MILLISECONDS}
         * @param columnIndex
         * @return
         */
        @Override
        public long getLong(int columnIndex) {
            if (columnIndex == WeatherContract.COL_WEATHER_DATE){
                return TimeUnit.SECONDS.toMillis(super.getLong(columnIndex));
            } else {
                return super.getLong(columnIndex);
            }
        }

    }
}
