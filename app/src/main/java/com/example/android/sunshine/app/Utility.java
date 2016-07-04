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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

public class Utility {
    private static final String TAG = Utility.class.getSimpleName();
    private static final long MSEC_PER_DAY = 24 * 60 * 60 * 1000;
    // Format used for storing dates in the database.  ALso used for converting those strings
    // back into date objects for comparison/processing.
    public static final String DATE_FORMAT = "yyyyMMdd";

    public static String getPreferredLocation(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_location_key),
                context.getString(R.string.pref_location_default));
    }

    public static boolean isMetric(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_units_key),
                context.getString(R.string.pref_units_metric))
                .equals(context.getString(R.string.pref_units_metric));
    }

    static String formatTemperature(double temperature, boolean isMetric, Context context) {
        double temp;
        if ( !isMetric ) {
            temp = 9*temperature/5+32;
        } else {
            temp = temperature;
        }
        return context.getString(R.string.format_temperature, temp);
    }

    /**
     *
     * @param dateInSeconds
     * @return
     */
    static String formatDate(long dateInSeconds) {
        Date date = new Date(TimeUnit.SECONDS.toMillis(dateInSeconds));
        return DateFormat.getDateInstance().format(date);
    }

    /**
     *
     * @param dateMsec
     * @return
     */
    public static String formatFriendlyDate(long dateMsec) {
        long offsetMsec = dateMsec - System.currentTimeMillis();
        Log.i(TAG, "formatFriendlyDate()"
                +"\t -- offsetMsec:"+offsetMsec
                +"\t -- dateMsec:"+dateMsec
        );
        if (offsetMsec < 0){
            return formatDate(dateMsec);
        }
        int dayOffset = (int) (offsetMsec/MSEC_PER_DAY);
        Log.i(TAG, "formatFriendlyDate()"
            +"\t -- dayOffset:"+dayOffset
        );
        if (dayOffset == 0){
            return "Today";
        } else if (dayOffset == 1){
            return "Tomorrow";
        } else if (dayOffset<7){
            Calendar calendar = new GregorianCalendar();
            calendar.setTimeInMillis(dateMsec);
            int dayOfWeekIndex = calendar.get(Calendar.DAY_OF_WEEK);
            switch (dayOfWeekIndex){
                case Calendar.SUNDAY:
                    return "SUNDAY";
                case Calendar.MONDAY:
                    return "MONDAY";
                case Calendar.TUESDAY:
                    return "TUESDAY";
                case Calendar.WEDNESDAY:
                    return "WEDNESDAY";
                case Calendar.THURSDAY:
                    return "THURSDAY";
                case Calendar.FRIDAY:
                    return "FRIDAY";
                case Calendar.SATURDAY:
                    return "SATURDAY";
                default:
                    return formatDate(dateMsec);
            }
        } else {
            return formatDate(dateMsec);

        }
    }

    /**
     * Helper method to convert the database representation of the date into something to display
     * to users.  As classy and polished a user experience as "20140102" is, we can do better.
     *
     * @param context Context to use for resource localization
     * @param dateInSeconds The date in seconds
     * @return a user-friendly representation of the date.
     */
    public static String getFriendlyDayString(Context context, long dateInSeconds) {
        // The day string for forecast uses the following logic:
        // For today: "Today, June 8"
        // For tomorrow:  "Tomorrow"
        // For the next 5 days: "Wednesday" (just the day name)
        // For all days after that: "Mon Jun 8"

        Time time = new Time();
        time.setToNow();
        long currentTime = System.currentTimeMillis();
        long dateInMsec = TimeUnit.SECONDS.toMillis(dateInSeconds);
        int julianDay = Time.getJulianDay(dateInMsec, time.gmtoff);
        int currentJulianDay = Time.getJulianDay(currentTime, time.gmtoff);

        // If the date we're building the String for is today's date, the format
        // is "Today, June 24"
        if (julianDay == currentJulianDay) {
            String today = context.getString(R.string.today);
            int formatId = R.string.format_full_friendly_date;
            return String.format(context.getString(
                    formatId,
                    today,
                    getFormattedMonthDay(context, dateInMsec)));
        } else if ( julianDay < currentJulianDay + 7 ) {
            // If the input date is less than a week in the future, just return the day name.
            return getDayName(context, dateInSeconds);
        } else {
            // Otherwise, use the form "Mon Jun 3"
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(dateInMsec);
        }
    }

    /**
     * Given a day, returns just the name to use for that day.
     * E.g "today", "tomorrow", "wednesday".
     *
     * @param context Context to use for resource localization
     * @param dateInSeconds The date in seconds
     * @return
     */
    public static String getDayName(Context context, long dateInSeconds) {
        // If the date is today, return the localized version of "Today" instead of the actual
        // day name.

        Time t = new Time();
        t.setToNow();
        long dateInMillis = TimeUnit.SECONDS.toMillis(dateInSeconds);
        int julianDay = Time.getJulianDay(dateInMillis, t.gmtoff);
        int currentJulianDay = Time.getJulianDay(System.currentTimeMillis(), t.gmtoff);
        if (julianDay == currentJulianDay) {
            return context.getString(R.string.today);
        } else if ( julianDay == currentJulianDay +1 ) {
            return context.getString(R.string.tomorrow);
        } else {
            Time time = new Time();
            time.setToNow();
            // Otherwise, the format is just the day of the week (e.g "Wednesday".
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE");
            return dayFormat.format(dateInMillis);
        }
    }

    /**
     * Converts db date format to the format "Month day", e.g "June 24".
     * @param context Context to use for resource localization
     * @param dateInMillis The db formatted date string, expected to be of the form specified
     *                in Utility.DATE_FORMAT
     * @return The day in the form of a string formatted "December 6"
     */
    public static String getFormattedMonthDay(Context context, long dateInMillis ) {
        Time time = new Time();
        time.setToNow();
        SimpleDateFormat dbDateFormat = new SimpleDateFormat(Utility.DATE_FORMAT);
        SimpleDateFormat monthDayFormat = new SimpleDateFormat("MMMM dd");
        String monthDayString = monthDayFormat.format(dateInMillis);
        return monthDayString;
    }

    public static CharSequence formatHumidity(long humidity, Context context) {
        return context.getString(R.string.format_humidity, humidity);
    }

    public static CharSequence formatWindSpeed(double windSpeed, double windDirectionDegrees, Context context) {
        Log.i(TAG, "formatWindSpeed()"
                +"\t -- windDirectionDegrees: "+windDirectionDegrees
        );
        final String[] CARDINAL_POINTS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        int windDirectionIndex = (int) (((windDirectionDegrees + 22.5f)%360)/45);
        String windDirection = CARDINAL_POINTS[windDirectionIndex];
        return context.getString(R.string.format_wind, windSpeed, windDirection);
    }

    public static CharSequence formatPressure(double pressure, Context context) {
        return context.getString(R.string.format_pressure, pressure);
    }

    /**
     * Helper method to provide the icon resource id according to the weather condition id returned
     * by the OpenWeatherMap call.
     * @param weatherId from OpenWeatherMap API response
     * @return resource id for the corresponding icon. -1 if no relation is found.
     */
    public static int getIconResourceForWeatherCondition(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.ic_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.drawable.ic_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.drawable.ic_rain;
        } else if (weatherId == 511) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.drawable.ic_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.drawable.ic_fog;
        } else if (weatherId == 761 || weatherId == 781) {
            return R.drawable.ic_storm;
        } else if (weatherId == 800) {
            return R.drawable.ic_clear;
        } else if (weatherId == 801) {
            return R.drawable.ic_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.ic_cloudy;
        }
        return -1;
    }

    /**
     * Helper method to provide the art resource id according to the weather condition id returned
     * by the OpenWeatherMap call.
     * @param weatherId from OpenWeatherMap API response
     * @return resource id for the corresponding image. -1 if no relation is found.
     */
    public static int getArtResourceForWeatherCondition(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.art_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.drawable.art_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.drawable.art_rain;
        } else if (weatherId == 511) {
            return R.drawable.art_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.drawable.art_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.art_snow;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.drawable.art_fog;
        } else if (weatherId == 761 || weatherId == 781) {
            return R.drawable.art_storm;
        } else if (weatherId == 800) {
            return R.drawable.art_clear;
        } else if (weatherId == 801) {
            return R.drawable.art_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.art_clouds;
        }
        return -1;
    }
}