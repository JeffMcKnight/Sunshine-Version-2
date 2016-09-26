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
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.format.Time;
import android.util.Log;

import com.example.android.sunshine.app.FetchWeatherTask;
import com.example.android.sunshine.app.data.WeatherContract.LocationEntry;
import com.example.android.sunshine.app.data.WeatherContract.WeatherEntry;
import com.example.android.sunshine.app.service.SunshineService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Vector;

/**
 * Manages a local database for weather data.
 */
public class WeatherDbHelper extends SQLiteOpenHelper {
    private static final String TAG = WeatherDbHelper.class.getSimpleName();

    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 2;
    private static final String LOG_TAG = WeatherDbHelper.class.getSimpleName();

    public WeatherDbHelper(Context context) {
        super(context, WeatherContract.DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Debugging tool to display
     * @param locationSetting
     * @param context
     */
    public static void displayWeatherLocationTable(String locationSetting, Context context) {
        Vector<ContentValues> cVVector;// Sort order:  Ascending, by date.
        String sortOrder = WeatherEntry.COLUMN_DATE + " ASC";
//            FIXME: how is buildWeatherLocationWithStartDate supposed to work? all weather forecasts with specified date and later?
        Uri weatherForLocationUri =
                WeatherEntry.buildWeatherLocationWithStartDate(locationSetting, System.currentTimeMillis());
//                    WeatherEntry.buildWeatherLocation(locationSetting);

        // Students: Uncomment the next lines to display what what you stored in the bulkInsert

        Cursor cur = context.getContentResolver().query(weatherForLocationUri,
                null, null, null, sortOrder);

        Log.i(LOG_TAG, "getWeatherDataFromJson()"
                    +"\t -- cur.getCount(): "+cur.getCount()
        );
        cVVector = new Vector<ContentValues>(cur.getCount());
        if ( cur.moveToFirst() ) {
            do {
                ContentValues cv = new ContentValues();
                DatabaseUtils.cursorRowToContentValues(cur, cv);
                Log.i(LOG_TAG, "getWeatherDataFromJson()"
                                + "\t -- cv: " + cv
                );
                cVVector.add(cv);
            } while (cur.moveToNext());
        }

        Log.d(LOG_TAG, "FetchWeatherTask Complete. " + cVVector.size() + " Rows Inserted");
    }

    /**
     * Helper method to handle insertion of a new location in the weather database.
     *
     * @param locationSetting The location string used to request updates from the server.
     * @param cityName A human-readable city name, e.g "Mountain View"
     * @param lat the latitude of the city
     * @param lon the longitude of the city
     * @return the row ID of the added location.
     */
    private static long addLocation(String locationSetting, String cityName, double lat, double lon, ContentResolver contentResolver) {
        long rowId;
        // Students: First, check if the location with this city name exists in the db
        String selection = LocationEntry.COLUMN_LOCATION_SETTING + " = ?";
        String[] selectionArgs = {locationSetting};
        String sortOrder = WeatherContract.SORT_ORDER_ASCENDING;
        Cursor cursor = contentResolver.query(LocationEntry.CONTENT_URI, null, selection, selectionArgs, null);
        Uri returnUri = null;
        if (cursor != null && cursor.moveToFirst()){
            // If it exists, return the current ID
            int cursorCount = cursor.getCount();
            if (cursorCount >1){
                Log.w(LOG_TAG, "addLocation()"
                        +"\t *** locationSetting: "+ locationSetting + " NOT UNIQUE"
                        +"\t *** cursorCount: "+ cursorCount
                );
            }
            int idColumnIndex = cursor.getColumnIndex(LocationEntry._ID);
            rowId = cursor.getLong(idColumnIndex);
        } else {
            // Otherwise, insert it using the content resolver and the base URI
            ContentValues contentValues =
                    LocationEntry.buildContentValues(locationSetting, cityName, lat, lon);
            returnUri = contentResolver.insert(LocationEntry.CONTENT_URI, contentValues);
            rowId = LocationEntry.getRowIdFromReturnUri(returnUri);
        }

        return rowId;
    }

    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     *
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    public static String[] getWeatherDataFromJson(String forecastJsonStr,
                                                  String locationSetting,
                                                  Context context)
            throws JSONException {

        // Now we have a String representing the complete forecast in JSON Format.
        // Fortunately parsing is easy:  constructor takes the JSON string and converts it
        // into an Object hierarchy for us.

        // These are the names of the JSON objects that need to be extracted.

        // Location information
        final String OWM_CITY = "city";
        final String OWM_CITY_NAME = "name";
        final String OWM_COORD = "coord";

        // Location coordinate
        final String OWM_LATITUDE = "lat";
        final String OWM_LONGITUDE = "lon";

        // Weather information.  Each day's forecast info is an element of the "list" array.
        final String OWM_LIST = "list";

        final String OWM_PRESSURE = "pressure";
        final String OWM_HUMIDITY = "humidity";
        final String OWM_WINDSPEED = "speed";
        final String OWM_WIND_DIRECTION = "deg";

        // All temperatures are children of the "temp" object.
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";

        final String OWM_WEATHER = "weather";
        final String OWM_DESCRIPTION = "main";
        final String OWM_WEATHER_ID = "id";

        try {
            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            JSONObject cityJson = forecastJson.getJSONObject(OWM_CITY);
            String cityName = cityJson.getString(OWM_CITY_NAME);

            JSONObject cityCoord = cityJson.getJSONObject(OWM_COORD);
            double cityLatitude = cityCoord.getDouble(OWM_LATITUDE);
            double cityLongitude = cityCoord.getDouble(OWM_LONGITUDE);

            long locationId = addLocation(locationSetting, cityName, cityLatitude, cityLongitude, context.getContentResolver());

            // Insert the new weather information into the database
            Vector<ContentValues> cVVector = new Vector<ContentValues>(weatherArray.length());

            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.

            Time dayTime = new Time();
            dayTime.setToNow();

            // we start at the day returned by local time. Otherwise this is a mess.
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // now we work exclusively in UTC
            dayTime = new Time();

            for(int i = 0; i < weatherArray.length(); i++) {
                // These are the values that will be collected.
                /** The date-time in seconds */
                long dateTimeMsec;
                double pressure;
                int humidity;
                double windSpeed;
                double windDirection;

                double high;
                double low;

                String description;
                int weatherId;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // Cheating to convert this to UTC time, which is what we want anyhow
                dateTimeMsec = dayTime.setJulianDay(julianStartDay + i);
                pressure = dayForecast.getDouble(OWM_PRESSURE);
                humidity = dayForecast.getInt(OWM_HUMIDITY);
                windSpeed = dayForecast.getDouble(OWM_WINDSPEED);
                windDirection = dayForecast.getDouble(OWM_WIND_DIRECTION);

                // Description is in a child array called "weather", which is 1 element long.
                // That element also contains a weather code.
                JSONObject weatherObject =
                        dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);
                weatherId = weatherObject.getInt(OWM_WEATHER_ID);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                high = temperatureObject.getDouble(OWM_MAX);
                low = temperatureObject.getDouble(OWM_MIN);

                ContentValues weatherValues = new ContentValues();

                weatherValues.put(WeatherEntry.COLUMN_LOC_KEY, locationId);
                weatherValues.put(WeatherEntry.COLUMN_DATE, dateTimeMsec);
                weatherValues.put(WeatherEntry.COLUMN_HUMIDITY, humidity);
                weatherValues.put(WeatherEntry.COLUMN_PRESSURE, pressure);
                weatherValues.put(WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
                weatherValues.put(WeatherEntry.COLUMN_DEGREES, windDirection);
                weatherValues.put(WeatherEntry.COLUMN_MAX_TEMP, high);
                weatherValues.put(WeatherEntry.COLUMN_MIN_TEMP, low);
                weatherValues.put(WeatherEntry.COLUMN_SHORT_DESC, description);
                weatherValues.put(WeatherEntry.COLUMN_WEATHER_ID, weatherId);

                Log.d(LOG_TAG, "getWeatherDataFromJson()"
                        + "\t -- i: " + i
                        + "\t -- weatherValues: " + weatherValues
                );
                cVVector.add(weatherValues);
            }

            // add to database
            ContentValues[] contentValues = cVVector.toArray(new ContentValues[cVVector.size()]);
            Log.i(LOG_TAG, "getWeatherDataFromJson()"
                    +"\t -- cVVector.size(): "+ cVVector.size()
                    +"\t -- contentValues.length: "+ contentValues.length
                    +"\n\t -- Arrays.toString(contentValues): "+ Arrays.toString(contentValues)
            );
            if ( cVVector.size() > 0 ) {
                // Student: call bulkInsert to add the weatherEntries to the database here
                context.getContentResolver().delete(WeatherEntry.CONTENT_URI, null, null);
                context.getContentResolver().bulkInsert(WeatherEntry.CONTENT_URI, contentValues);
            }

            displayWeatherLocationTable(locationSetting, context);


//            String[] resultStrs = convertContentValuesToUXFormat(cVVector);
            String[] resultStrs = null;
            return resultStrs;

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        createWeatherTable(sqLiteDatabase);
        createLocationTable(sqLiteDatabase);
    }

    private void createLocationTable(SQLiteDatabase sqLiteDatabase) {
        TableBuilder locTableBuilder = new TableBuilder(LocationEntry.TABLE_NAME)
                .addColumn(LocationEntry.COLUMN_LOCATION_SETTING, TableBuilder.SQL_TYPE_TEXT, TableBuilder.CONSTRAINT_UNIQUE, TableBuilder.CONSTRAINT_NOT_NULL)
                .addColumn(LocationEntry.COLUMN_CITY_NAME, TableBuilder.SQL_TYPE_TEXT, TableBuilder.CONSTRAINT_NOT_NULL)
                .addColumn(LocationEntry.COLUMN_COORD_LAT, TableBuilder.SQL_TYPE_REAL, TableBuilder.CONSTRAINT_NOT_NULL)
                .addColumn(LocationEntry.COLUMN_COORD_LONG, TableBuilder.SQL_TYPE_REAL, TableBuilder.CONSTRAINT_NOT_NULL)
                ;
//        StringBuilder locationTableBuilder = new StringBuilder(TableBuilder.CREATE_TABLE_IF_NOT_EXISTS);
//        locationTableBuilder
//                .append(TableBuilder.WHITE_SPACE).append(LocationEntry.TABLE_NAME)
//                .append(" (").append(LocationEntry._ID)
//                .append(TableBuilder.WHITE_SPACE).append(TableBuilder.SQL_TYPE_INTEGER)
//                .append(TableBuilder.WHITE_SPACE).append(TableBuilder.CONSTRAINT_PRIMARY_KEY)
//
//                .append(TableBuilder.COLUMN_SEPARATOR).append(LocationEntry.COLUMN_LOCATION_SETTING)
//                .append(TableBuilder.WHITE_SPACE).append(TableBuilder.SQL_TYPE_TEXT)
//                .append(TableBuilder.WHITE_SPACE).append(TableBuilder.CONSTRAINT_UNIQUE)
//                .append(TableBuilder.WHITE_SPACE).append(TableBuilder.CONSTRAINT_NOT_NULL)
//
//                .append(TableBuilder.COLUMN_SEPARATOR).append(LocationEntry.COLUMN_CITY_NAME)
//                .append(TableBuilder.WHITE_SPACE).append(TableBuilder.SQL_TYPE_TEXT)
//                .append(TableBuilder.WHITE_SPACE).append(TableBuilder.CONSTRAINT_NOT_NULL)
//
//                .append(TableBuilder.COLUMN_SEPARATOR).append(LocationEntry.COLUMN_COORD_LAT)
//                .append(TableBuilder.WHITE_SPACE).append(TableBuilder.SQL_TYPE_REAL)
//                .append(TableBuilder.WHITE_SPACE).append(TableBuilder.CONSTRAINT_NOT_NULL)
//
//                .append(TableBuilder.COLUMN_SEPARATOR).append(LocationEntry.COLUMN_COORD_LONG)
//                .append(TableBuilder.WHITE_SPACE).append(TableBuilder.SQL_TYPE_REAL)
//                .append(TableBuilder.WHITE_SPACE).append(TableBuilder.CONSTRAINT_NOT_NULL)
//                .append(" )")
//        ;

        Log.i(TAG, "createLocationTable()"
//                        + "\n\t -- locationTableBuilder.toString(): " + locationTableBuilder.toString()
                        + "\n\t -- locTableBuilder.build():         " + locTableBuilder.build()
        );
        sqLiteDatabase.execSQL(locTableBuilder.build());
    }

    private void createWeatherTable(SQLiteDatabase sqLiteDatabase) {
        TableBuilder weatherTableBuilder = new TableBuilder(WeatherEntry.TABLE_NAME)
                .addColumn(WeatherEntry.COLUMN_LOC_KEY, TableBuilder.SQL_TYPE_INTEGER, TableBuilder.CONSTRAINT_NOT_NULL)
                .addColumn(WeatherEntry.COLUMN_DATE, TableBuilder.SQL_TYPE_INTEGER, TableBuilder.CONSTRAINT_NOT_NULL)
                .addColumn(WeatherEntry.COLUMN_SHORT_DESC, TableBuilder.SQL_TYPE_TEXT, TableBuilder.CONSTRAINT_NOT_NULL)
                .addColumn(WeatherEntry.COLUMN_WEATHER_ID, TableBuilder.SQL_TYPE_INTEGER, TableBuilder.CONSTRAINT_NOT_NULL)
                .addColumn(WeatherEntry.COLUMN_MIN_TEMP, TableBuilder.SQL_TYPE_REAL, TableBuilder.CONSTRAINT_NOT_NULL)
                .addColumn(WeatherEntry.COLUMN_MAX_TEMP, TableBuilder.SQL_TYPE_REAL, TableBuilder.CONSTRAINT_NOT_NULL)
                .addColumn(WeatherEntry.COLUMN_HUMIDITY, TableBuilder.SQL_TYPE_REAL, TableBuilder.CONSTRAINT_NOT_NULL)
                .addColumn(WeatherEntry.COLUMN_PRESSURE, TableBuilder.SQL_TYPE_REAL, TableBuilder.CONSTRAINT_NOT_NULL)
                .addColumn(WeatherEntry.COLUMN_WIND_SPEED, TableBuilder.SQL_TYPE_REAL, TableBuilder.CONSTRAINT_NOT_NULL)
                .addColumn(WeatherEntry.COLUMN_DEGREES, TableBuilder.SQL_TYPE_REAL, TableBuilder.CONSTRAINT_NOT_NULL)
                .addForeignKey(WeatherEntry.COLUMN_LOC_KEY, LocationEntry.TABLE_NAME, LocationEntry._ID)
                .addUniqueReplace(WeatherEntry.COLUMN_DATE, WeatherEntry.COLUMN_LOC_KEY)
                ;

//        final String SQL_CREATE_TABLE = TableBuilder.CREATE_TABLE_IF_NOT_EXISTS + TableBuilder.WHITE_SPACE +
//                WeatherEntry.TABLE_NAME + " (" +
//                // Why AutoIncrement here, and not above?
//                // Unique keys will be auto-generated in either case.  But for weather
//                // forecasting, it's reasonable to assume the user will want information
//                // for a certain date and all dates *following*, so the forecast data
//                // should be sorted accordingly.
//                WeatherEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
//
//                // the ID of the location entry associated with this weather data
//                WeatherEntry.COLUMN_LOC_KEY + " INTEGER NOT NULL, " +
//                WeatherEntry.COLUMN_DATE + " INTEGER NOT NULL, " +
//                WeatherEntry.COLUMN_SHORT_DESC + " TEXT NOT NULL, " +
//                WeatherEntry.COLUMN_WEATHER_ID + " INTEGER NOT NULL, " +
//
//                WeatherEntry.COLUMN_MIN_TEMP + " REAL NOT NULL, " +
//                WeatherEntry.COLUMN_MAX_TEMP + " REAL NOT NULL, " +
//
//                WeatherEntry.COLUMN_HUMIDITY + " REAL NOT NULL, " +
//                WeatherEntry.COLUMN_PRESSURE + " REAL NOT NULL, " +
//                WeatherEntry.COLUMN_WIND_SPEED + " REAL NOT NULL, " +
//                WeatherEntry.COLUMN_DEGREES + " REAL NOT NULL, " +
//
//                // Set up the location column as a foreign key to location table.
//                " FOREIGN KEY (" + WeatherEntry.COLUMN_LOC_KEY + ") REFERENCES " +
//                LocationEntry.TABLE_NAME + " (" + LocationEntry._ID + "), " +
//
//                // To assure the application have just one weather entry per day
//                // per location, it's created a UNIQUE constraint with REPLACE strategy
//                " UNIQUE (" + WeatherEntry.COLUMN_DATE + ", " +
//                WeatherEntry.COLUMN_LOC_KEY + ") ON CONFLICT REPLACE);";

        Log.i(TAG, "createWeatherTable()"
//                        +"\n\t -- SQL_CREATE_TABLE:            "+SQL_CREATE_TABLE
                        +"\n\t -- weatherTableBuilder.build(): "+weatherTableBuilder.build()
        );

        sqLiteDatabase.execSQL(weatherTableBuilder.build());
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        // Note that this only fires if you change the version number for your database.
        // It does NOT depend on the version number for your application.
        // If you want to update the schema without wiping data, commenting out the next 2 lines
        // should be your top priority before modifying this method.
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + LocationEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + WeatherEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void printCursor(Cursor cursor) {
        boolean moveSucceeded = false;
        int columnCount = cursor.getColumnCount();
        int rowCount = cursor.getCount();
        Log.d(TAG, "printCursor()"
                        +"\t -- columnCount: "+columnCount
                        +"\t -- rowCount: "+rowCount
        );
        if (cursor.isBeforeFirst() || cursor.isAfterLast()){
            cursor.moveToFirst();
        }
        StringBuilder rowAsString = new StringBuilder("printCursor():");
        for (int j=0; j<rowCount; j++) {
            rowAsString.append("\n");
            for (int i = 0; i < columnCount; i++) {
                rowAsString.append("\t - ")
                        .append(cursor.getColumnName(i))
                        .append(": ")
                ;
                switch (cursor.getType(i)){
                    case Cursor.FIELD_TYPE_BLOB:
                        rowAsString.append("BLOB");
                        break;
                    case Cursor.FIELD_TYPE_FLOAT:
                        rowAsString.append(cursor.getFloat(i));
                        break;
                    case Cursor.FIELD_TYPE_INTEGER:
                        rowAsString.append(cursor.getInt(i));
                        break;
                    case Cursor.FIELD_TYPE_STRING:
                        rowAsString.append(cursor.getString(i));
                        break;
                    case Cursor.FIELD_TYPE_NULL:
                        rowAsString.append("NULL");
                        break;
                    default:
                        break;
                }
                moveSucceeded = cursor.moveToNext();
                if (!moveSucceeded){
                    cursor.moveToFirst();
                }
            }
        }
        Log.v(TAG, rowAsString.toString());
    }

    /**
     *
     */
    private class TableBuilder {
        public final String TAG = TableBuilder.class.getSimpleName();
        // Include a space before and after all SQL String constants so we never have to manually add white space
        public static final String CREATE_TABLE_IF_NOT_EXISTS = "CREATE TABLE IF NOT EXISTS";
        public static final String SQL_TYPE_INTEGER = "INTEGER";
        public static final String SQL_TYPE_TEXT = "TEXT";
        public static final String SQL_TYPE_REAL = "REAL";
        public static final String CONSTRAINT_NOT_NULL = "NOT NULL";
        public static final String CONSTRAINT_AUTOINCREMENT = "AUTOINCREMENT";
        public static final String CONSTRAINT_UNIQUE = "UNIQUE";
        public static final String CONSTRAINT_PRIMARY_KEY = "PRIMARY KEY AUTOINCREMENT";
        public static final String COLUMN_SEPARATOR = ", ";
        private static final String WHITE_SPACE = " ";
        private final String mTableName;
        private Set<TableColumn> columnSet;
        private String mForeignKeyString;
        private String mUniqueReplace;

        public TableBuilder(String tableName) {
            mTableName = tableName;
            columnSet = new LinkedHashSet<>();
        }

        /**
         *
         * @param columnName
         * @param type
         * @param constraint
         * @return the {@link com.example.android.sunshine.app.data.WeatherDbHelper.TableBuilder} object so more builder items can be added
         */
        public TableBuilder addColumn(String columnName, String type, String... constraint) {
            TableColumn tableColumn = new TableColumn(columnName, type);
            for (int i=0; i<constraint.length; i++){
                tableColumn.addConstraint(constraint[i]);
            }
            columnSet.add(tableColumn);
            return this;
        }

        /**
         *
         * @param column
         * @param foreignTableName
         * @param foreignColumn
         * @return the {@link com.example.android.sunshine.app.data.WeatherDbHelper.TableBuilder} object so more builder items can be added
         */
        public TableBuilder addForeignKey(String column, String foreignTableName, String foreignColumn) {
            mForeignKeyString = " FOREIGN KEY (" + column + ") REFERENCES " +
                    foreignTableName + " (" + foreignColumn + ")";
            return this;
        }

        /**
         * Add a "UNIQUE" constraint to {@code column}
         * @param column the name of the column to add the constraint to
         * @param replaceWithColumn
         * @return the {@link com.example.android.sunshine.app.data.WeatherDbHelper.TableBuilder} object so more builder items can be added
         */
        public TableBuilder addUniqueReplace(String column, String replaceWithColumn) {
            mUniqueReplace =
                    " UNIQUE ("
                            + column
                            + ", " +
                            replaceWithColumn
                            + ") ON CONFLICT REPLACE";
            return this;
        }

        /**
         * Build a single raw SQLite statement to create an {@link SQLiteDatabase} table using
         * all the items (columns, constraints, etc) added to this
         * {@link com.example.android.sunshine.app.data.WeatherDbHelper.TableBuilder}.  The
         * statement does not terminate in a semicolon because SQLiteDatabase.execRaw() does not
         * support multiple statements, so a statement delimiter is neither required nor desirable.
         *
         * @return
         */
        public String build() {
            StringBuilder locationTableBuilder = new StringBuilder();
            locationTableBuilder
                    .append(CREATE_TABLE_IF_NOT_EXISTS)
                    .append(WHITE_SPACE).append(mTableName)
                    .append(" (").append(LocationEntry._ID)
                    .append(WHITE_SPACE).append(TableBuilder.SQL_TYPE_INTEGER)
                    .append(WHITE_SPACE).append(TableBuilder.CONSTRAINT_PRIMARY_KEY);
            for (TableColumn eachColumn : columnSet){
                locationTableBuilder.append(eachColumn.toBuilder());
            }
            if (mForeignKeyString != null){
                locationTableBuilder.append(COLUMN_SEPARATOR);
                locationTableBuilder.append(mForeignKeyString);
            }
            if (mUniqueReplace != null){
                locationTableBuilder.append(COLUMN_SEPARATOR);
                locationTableBuilder.append(mUniqueReplace);
            }
            locationTableBuilder.append(")");
//            locationTableBuilder.append(";");
            String builderString = locationTableBuilder.toString();
            Log.i(TAG, "build()"
                            + "\t -- builderString: " + builderString
            );
            return builderString;
        }

        @NonNull
        private StringBuilder toBuilder() {
            return new StringBuilder()
                    .append(TableBuilder.COLUMN_SEPARATOR)
                    .append(LocationEntry.COLUMN_COORD_LONG).append(TableBuilder.SQL_TYPE_REAL).append(TableBuilder.CONSTRAINT_NOT_NULL);
        }

        /**
         *
         */
        private class TableColumn implements Comparable{
            private final String mColumnName;
            private final String mDataType;
            private Set<String> mConstaintSet = new LinkedHashSet<>();

            public TableColumn(String columnName, String dataType) {
                mColumnName = columnName;
                mDataType = dataType;
            }

            public void addConstraint(String constraint) {
                mConstaintSet.add(constraint);
            }

            public StringBuilder toBuilder() {
                StringBuilder stringBuilder = new StringBuilder()
                        .append(TableBuilder.COLUMN_SEPARATOR)
                        .append(mColumnName)
                        .append(WHITE_SPACE)
                        .append(mDataType);
                for (String eachConstraint : mConstaintSet){
                    stringBuilder.append(WHITE_SPACE).append(eachConstraint);
                }
                return stringBuilder;
            }

            @Override
            public int compareTo(Object another) {
                return this.mColumnName.compareTo(((TableColumn)another).mColumnName);
            }
        }
    }
}
