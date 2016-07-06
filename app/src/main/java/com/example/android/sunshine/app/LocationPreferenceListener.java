package com.example.android.sunshine.app;

/**
 * We use {@link LocationPreferenceListener} as sort of a backwards listener, so
 * {@link android.app.Activity}s can notify hosted {@link android.support.v4.app.Fragment}s that the
 * user has changed their preferred geographic location.  (Is this an anti-pattern?)
 *
 * Created by jeffmcknight on 7/5/16.
 */
public interface LocationPreferenceListener {
    void onLocationChanged(String location);
}
