package com.example.android.sunshine.app.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.example.android.sunshine.app.BuildConfig;
import com.example.android.sunshine.app.data.WeatherDbHelper;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Use this {@link IntentService} to fetch weather data in the background.
 * Created by jeffmcknight on 9/24/16.
 */

public class SunshineService extends IntentService {
    private static final String LOG_TAG = SunshineService.class.getSimpleName();
    private static final String KEY_LOCATION = SunshineService.class.getCanonicalName() + ".location";

    public SunshineService(){
        super(LOG_TAG + "_" + Thread.currentThread().getName());

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
            WeatherDbHelper.getWeatherDataFromJson(forecastJsonStr, locationQuery, this);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        // This will only happen if there was an error getting or parsing the forecast.
        return;

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

        /**
         *
         * @param context
         * @param location
         */
        public static void scheduleBroadcast(Context context, String location){
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
