package com.example.android.sunshine.app.data;

import android.content.ContentValues;
import android.net.Uri;
import android.test.AndroidTestCase;

/**
 * Created by jeffmcknight on 10/1/16.
 */
public class WeatherProviderTest extends AndroidTestCase {

    private static final long LOCATION_ID = 94043;
    private static final String LOCATION_ID_STRING = String.valueOf(LOCATION_ID);
    private ContentValues mWeatherContent;

    /**
     * Constructor.
     *
     */
    public WeatherProviderTest() {
        super();
    }

    public void setUp() throws Exception {
        super.setUp();
        setupWeatherContent();

    }

    public void setupWeatherContent() {
        mWeatherContent = new ContentValues();
        mWeatherContent.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, LOCATION_ID);
        mWeatherContent.put(WeatherContract.WeatherEntry.COLUMN_DATE,System.currentTimeMillis());
        mWeatherContent.put(WeatherContract.WeatherEntry.COLUMN_DEGREES,21);
        mWeatherContent.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY,81);
        mWeatherContent.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,31);
        mWeatherContent.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,11);
        mWeatherContent.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE,777);
        mWeatherContent.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,0);
        mWeatherContent.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,0);
        mWeatherContent.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,0);
    }

    //    public void tearDown() throws Exception {
//        super.tearDown();
//    }
//
    public void testGetType() throws Exception {
        String weatherType = getContext().getContentResolver().getType(
                WeatherContract.WeatherEntry.buildWeatherUri(LOCATION_ID)
        );
        String weatherLocationType = getContext().getContentResolver().getType(
                WeatherContract.WeatherEntry.buildWeatherLocation(LOCATION_ID_STRING)
        );
        String weatherLocationWithDateType = getContext().getContentResolver().getType(
                WeatherContract.WeatherEntry.buildWeatherLocationWithDate(LOCATION_ID_STRING, System.currentTimeMillis())
        );
        String locationUriType = getContext().getContentResolver().getType(
                WeatherContract.LocationEntry.buildLocationUri(LOCATION_ID)
        );
        assertEquals(WeatherContract.WeatherEntry.CONTENT_TYPE,weatherType);
        assertEquals(WeatherContract.WeatherEntry.CONTENT_TYPE,weatherLocationType);
        assertEquals(WeatherContract.WeatherEntry.CONTENT_ITEM_TYPE,weatherLocationWithDateType);
        assertEquals(WeatherContract.LocationEntry.CONTENT_TYPE,locationUriType);
    }

//    public void testQuery() throws Exception {}

    public void testInsert() throws Exception {
        Uri weatherUri = WeatherContract.WeatherEntry.buildWeatherUri();
        Uri insertUri = getContext().getContentResolver().insert(weatherUri, mWeatherContent);

    }

//    public void testDelete() throws Exception {}

//    public void testUpdate() throws Exception {}

//    public void testBulkInsert() throws Exception {}

}