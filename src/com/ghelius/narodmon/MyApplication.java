package com.ghelius.narodmon;

import android.app.Application;
import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

@ReportsCrashes(formUri = "http://www.bugsense.com/api/acra?api_key=a9bbcd9b", formKey="")
public class MyApplication extends Application {
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
	}
}
