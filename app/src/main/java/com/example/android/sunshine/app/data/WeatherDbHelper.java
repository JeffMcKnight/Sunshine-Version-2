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
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;

import com.example.android.sunshine.app.FetchWeatherTask;
import com.example.android.sunshine.app.data.WeatherContract.LocationEntry;
import com.example.android.sunshine.app.data.WeatherContract.WeatherEntry;

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
