package com.ghelius.narodmon;

import android.app.Application;
import android.util.Log;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

import java.util.ArrayList;

@ReportsCrashes(formUri = "http://www.bugsense.com/api/acra?api_key=a9bbcd9b", formKey="")
public class MyApplication extends Application {
    final static private String TAG = "narodmon-app";
    private ArrayList<Sensor> sensorList;
    long updateTimeStamp = 0;
	@Override
	public void onCreate() {
		super.onCreate();
		// The following line triggers the initialization of ACRA
		ACRA.init(this);
        try {
            Class.forName("android.os.AsyncTask");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        DatabaseManager.initializeInstance(new DatabaseHelper(getApplicationContext()));
        updateTimeStamp = System.currentTimeMillis();
        sensorList = new ArrayList<Sensor>();
	}

    public ArrayList<Sensor> getSensorList() {
        return sensorList;
    }

    public long getUpdateTimeStamp () {
        return updateTimeStamp;
    }

    public void setUpdateTimeStamp (long timeStamp) {
        Log.d(TAG, "load... setUpdateTimeStamp " + timeStamp);
        this.updateTimeStamp = timeStamp;
    }
}
