package com.ghelius.narodmon;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

public class SensorProviderFragment extends Fragment {
    private static final String TAG = "narodmon-sensorProvider";
    private long timeStamp = 0;

    // call first
    @Override
    public void onAttach (Activity activity) {
        super.onAttach(activity);
        Log.d(TAG, "onAttach: " + timeStamp);
        if (timeStamp == 0) {
            timeStamp = System.currentTimeMillis();
            Log.d(TAG,"init timeStamp with " + timeStamp);
        }
    }

    // call second
    @Override
    public void onCreate (Bundle savedInstance) {
        super.onCreate(savedInstance);
        setRetainInstance(true);
        Log.d(TAG, "onCreate: " + timeStamp);
    }
    // call third
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: " + timeStamp);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: " + timeStamp);
    }

    @Override
    public void onDetach() {
        super.onDestroy();
        Log.d(TAG, "onDetach: " + timeStamp);
    }
}
