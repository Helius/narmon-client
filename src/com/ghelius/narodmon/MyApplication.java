package com.ghelius.narodmon;

import android.app.Application;
import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

import java.util.ArrayList;

@ReportsCrashes(formUri = "http://www.bugsense.com/api/acra?api_key=a9bbcd9b", formKey="")
public class MyApplication extends Application {
    private ArrayList<Sensor> sensorList;
    long updateTimeStamp;
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
        this.updateTimeStamp = timeStamp;
    }
}
