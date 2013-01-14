package com.ghelius.narodmon;

import android.content.Intent;
import android.util.Log;
import com.commonsware.cwac.wakeful.WakefulIntentService;

public class WatchService extends WakefulIntentService {
    private final static String TAG = "narodmon-service";
    public WatchService() {
        super("Narodmon watcher");
    }

    @Override
    protected void doWakefulWork(Intent intent) {
        Log.d("TAG","nmWatcher work...");
        //Configuration config = (Configuration) intent.getSerializableExtra("sensorTasks");
        //String msg = (String) intent.getStringExtra("msg");
        Configuration config = ConfigHolder.getInstance(this).getConfig();

        Log.d(TAG,"config size: "+ config.watchedId.size());
    }
}