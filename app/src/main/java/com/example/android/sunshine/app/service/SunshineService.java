package com.example.android.sunshine.app.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.format.Time;
import android.util.Log;

import com.example.android.sunshine.app.BuildConfig;
import com.example.android.sunshine.app.data.WeatherContract;
import com.example.android.sunshine.app.data.WeatherDbHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Vector;

/**
 * Use this {@link IntentService} to fetch weather data in the background.
 * Created by jeffmcknight on 9/24/16.
 */

public class SunshineService extends IntentService {
    private static final String LOG_TAG = SunshineService.class.getSimpleName();
    private static final String KEY_LOCATION = SunshineService.class.getCanonicalName() + ".location";

    public SunshineService(){
        super(LOG_TAG);

    }
    public SunshineService(String name) {
        super(name);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "onStartCommand()"
                + "\t -- intent: " +intent
                + "\t -- flags: " +flags
                + "\t -- startId: " +startId
        );
        return super.onStartCommand(intent, flags, startId);
    }

    public static void start(Context context, String zipCode){
        Log.d(LOG_TAG, "start()"
                + "\t -- zipCode: " +zipCode
                + "\t -- context: " +context
        );
        Intent intent = new Intent(context, SunshineService.class);
        intent.putExtra(KEY_LOCATION, zipCode);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String locationQuery = intent.getStringExtra(KEY_LOCATION);

        Log.i(LOG_TAG, "onHandleIntent()"
                +"\t -- locationQuery: "+locationQuery
                +"\t -- currentThread(): "+Thread.currentThread().getName()
        );

        // If there's no zip code, there's nothing to look up.  Verify size of params.
        if (locationQuery == null) {
            return;
        }

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String forecastJsonStr = null;

        String format = "json";
        String units = "metric";
        int numDays = 14;

        URL url = null;
        try {
//            String pingCommand = "ping ";
//            String hostName = "api.openweathermap.org";
//            new ProcessBuilder(pingCommand, hostName).start();
            // Construct the URL for the OpenWeatherMap query
            // Possible parameters are available at OWM's forecast API page, at
            // http://openweathermap.org/API#forecast
            final String FORECAST_BASE_URL =
                    "http://api.openweathermap.org/data/2.5/forecast/daily?";
            final String QUERY_PARAM = "q";
            final String FORMAT_PARAM = "mode";
            final String UNITS_PARAM = "units";
            final String DAYS_PARAM = "cnt";
            final String API_KEY_PARAM = "APPID";

            Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_PARAM, locationQuery)
                    .appendQueryParameter(FORMAT_PARAM, format)
                    .appendQueryParameter(UNITS_PARAM, units)
                    .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                    .appendQueryParameter(API_KEY_PARAM, BuildConfig.OPEN_WEATHER_MAP_API_KEY)
                    .build();

            url = new URL(builtUri.toString());
            Log.i(LOG_TAG, "onHandleIntent()"
                    +"\t -- url: "+url
            );

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                Log.w(LOG_TAG, "onHandleIntent()"
                        +"\t *** inputStream: "+inputStream
                );
                return;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }
            Log.i(LOG_TAG, "onHandleIntent()"
                    +"\n\t -- line: "+line
                    +"\n\t -- buffer: "+buffer
            );

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return;
            }
            forecastJsonStr = buffer.toString();
        } catch (IOException e) {
            String errorLog = "onHandleIntent()"
                    + "\n\t -- url: " + url
                    + "\n\t -- e: " + e;
            Log.e(LOG_TAG, errorLog, e);
//            publishProgress(errorLog);
            // If the code didn't successfully get the weather data, there's no point in attempting
            // to parse it.
            return;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }

        try {
            getWeatherDataFromJson(forecastJsonStr, locationQuery);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        // This will only happen if there was an error getting or parsing the forecast.
        return;

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
    long addLocation(String locationSetting, String cityName, double lat, double lon) {
        long rowId;
        // Students: First, check if the location with this city name exists in the db
        String selection = WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ?";
        String[] selectionArgs = {locationSetting};
        String sortOrder = WeatherContract.SORT_ORDER_ASCENDING;
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(WeatherContract.LocationEntry.CONTENT_URI, null, selection, selectionArgs, null);
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
            int idColumnIndex = cursor.getColumnIndex(WeatherContract.LocationEntry._ID);
            rowId = cursor.getLong(idColumnIndex);
        } else {
            // Otherwise, insert it using the content resolver and the base URI
            ContentValues contentValues =
                    WeatherContract.LocationEntry.buildContentValues(locationSetting, cityName, lat, lon);
            returnUri = contentResolver.insert(WeatherContract.LocationEntry.CONTENT_URI, contentValues);
            rowId = WeatherContract.LocationEntry.getRowIdFromReturnUri(returnUri);
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
    private String[] getWeatherDataFromJson(String forecastJsonStr,
                                            String locationSetting)
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

            long locationId = addLocation(locationSetting, cityName, cityLatitude, cityLongitude);

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

                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, locationId);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATE, dateTimeMsec);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, humidity);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, pressure);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, windDirection);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, high);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, low);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, description);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, weatherId);

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
                getContentResolver().delete(WeatherContract.WeatherEntry.CONTENT_URI, null, null);
                getContentResolver().bulkInsert(WeatherContract.WeatherEntry.CONTENT_URI, contentValues);
            }

            WeatherDbHelper.displayWeatherLocationTable(locationSetting, this);


//            String[] resultStrs = convertContentValuesToUXFormat(cVVector);
            String[] resultStrs = null;
            return resultStrs;

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Use this {@link BroadcastReceiver} to schedule weather updates
     */
    public static class AlarmReceiver extends BroadcastReceiver {
        private static final String TAG = AlarmReceiver.class.getSimpleName();
        private static final int REQUEST_CODE = AlarmReceiver.class.hashCode();
        private static final long DELAY_FIVE_SECONDS_MSEC = 5 * 1000;

        @Override
        public void onReceive(Context context, Intent intent) {
            String location = intent.getStringExtra(KEY_LOCATION);
            Log.d(TAG, "onReceive()"
                    + "\t -- location: " + location
                    + "\t -- intent: " + intent
            );
            SunshineService.start(context, location);
        }

        public static void scheduleBroadcast(String location, Context context){
            Log.d(TAG, "scheduleBroadcast()"
                    + "\t -- location: " + location
            );
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.putExtra(KEY_LOCATION, location);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, PendingIntent.FLAG_ONE_SHOT);
            alarmManager.set(AlarmManager.ELAPSED_REALTIME, DELAY_FIVE_SECONDS_MSEC, pendingIntent);

        }
    }
}
