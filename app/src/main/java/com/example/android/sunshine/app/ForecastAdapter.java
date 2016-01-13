package com.example.android.sunshine.app;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.android.sunshine.app.data.WeatherContract;

/**
 * {@link ForecastAdapter} exposes a list of weather forecasts
 * from a {@link android.database.Cursor} to a {@link android.widget.ListView}.
 */
public class ForecastAdapter extends CursorAdapter {
    private static final String TAG = ForecastAdapter.class.getSimpleName();

    private static final int VIEW_TYPE_TODAY = 0;
    private static final int VIEW_TYPE_FUTURE_DAY = 1;

    /**
     *
     * @param context
     * @param c
     * @param flags
     */
    public ForecastAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @NonNull
    public String formatTemperature(double temperature) {
        boolean isMetric = Utility.isMetric(mContext);
        return Utility.formatTemperature(temperature, isMetric, mContext);
    }

    /**
     * Prepare the weather high/lows for presentation.
     */
    private String formatHighLows(double high, double low) {
        boolean isMetric = Utility.isMetric(mContext);
        String highLowStr = Utility.formatTemperature(high, isMetric, mContext) + "/" + Utility.formatTemperature(low, isMetric, mContext);
        return highLowStr;
    }

    /*
        This is ported from FetchWeatherTask --- but now we go straight from the cursor to the
        string.
     */
    private String convertCursorRowToUXFormat(Cursor cursor) {
        double maxTemp = cursor.getDouble(WeatherContract.COL_WEATHER_MAX_TEMP);
        double minTemp = cursor.getDouble(WeatherContract.COL_WEATHER_MIN_TEMP);

        String highAndLow = formatHighLows(maxTemp, minTemp);

        String uxFormat = Utility.formatDate(cursor.getLong(WeatherContract.COL_WEATHER_DATE)) +
                " - " + cursor.getString(WeatherContract.COL_WEATHER_DESC) +
                " - " + highAndLow;
        return uxFormat;
    }

    /*
        Remember that these views are reused as needed.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
//        Log.i(TAG, "newView()"
//            +"\t -- parent: "+parent
//        );
        View view;
        if (getCursor().getPosition() == VIEW_TYPE_TODAY){
            view = LayoutInflater.from(context).inflate(R.layout.list_item_forecast_today, parent, false);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.list_item_forecast, parent, false);
        }
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
        return view;
    }

    /*
        This is where we fill-in the views with the contents of the cursor.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
//        Log.i(TAG, "bindView()"
//                        + "\t -- cursor: " + cursor
//        );

        ViewHolder viewHolder = (ViewHolder) view.getTag();
        int iconResourceId
                = (cursor.getPosition() == VIEW_TYPE_TODAY)
                ? Utility.getArtResourceForWeatherCondition(cursor.getInt(WeatherContract.COL_WEATHER_CONDITION_ID))
                : Utility.getIconResourceForWeatherCondition(cursor.getInt(WeatherContract.COL_WEATHER_CONDITION_ID))
                ;
        viewHolder.mIconView.setImageDrawable(ContextCompat.getDrawable(context, iconResourceId));
        viewHolder.mDateView.setText(Utility.getFriendlyDayString(context, cursor.getLong(WeatherContract.COL_WEATHER_DATE)));
        viewHolder.mForecastDescriptionView.setText(cursor.getString(WeatherContract.COL_WEATHER_DESC));
        viewHolder.mMaxTempView.setText(formatTemperature(cursor.getDouble(WeatherContract.COL_WEATHER_MAX_TEMP)));
        viewHolder.mMinTempView.setText(formatTemperature(cursor.getDouble(WeatherContract.COL_WEATHER_MIN_TEMP)));
    }

    @Override
    protected void onContentChanged() {
        Log.i(TAG, "onContentChanged()"
            + "\t -- mCursor: "+mCursor
        );
        super.onContentChanged();
    }

    /**
     * We have two types of {@link View}s
     * @return
     */
    @Override
    public int getViewTypeCount() {
        return 2;
    }

    /**
     * Return the ID for the Today view-type for the first item in the list, or the Future-Day ID
     * for all other items
     * @param position
     * @return
     */
    @Override
    public int getItemViewType(int position) {
        return (position == 0 ? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY);
    }


    /**
     *
     */
    public static class ViewHolder {
        final ImageView mIconView;
        final TextView mDateView;
        final TextView mForecastDescriptionView;
        final TextView mMaxTempView;
        final TextView mMinTempView;

        public ViewHolder(View view){
            mIconView = (ImageView) view.findViewById(R.id.list_item_icon);
            mDateView = (TextView) view.findViewById(R.id.list_item_date_textview);
            mForecastDescriptionView = (TextView)view.findViewById(R.id.list_item_forecast_textview);
            mMaxTempView = (TextView)view.findViewById(R.id.list_item_high_textview);
            mMinTempView = (TextView)view.findViewById(R.id.list_item_low_textview);
        }
    }
}