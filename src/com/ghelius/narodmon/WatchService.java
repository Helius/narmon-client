package com.ghelius.narodmon;

import android.content.Intent;
import android.util.Log;
import com.commonsware.cwac.wakeful.WakefulIntentService;

import java.util.ArrayList;

public class WatchService extends WakefulIntentService {
    private final static String TAG = "narodmon-service";
    SensorDataUpdater updater;
    public WatchService() {
        super("Narodmon watcher");
    }

    class SensorDataUpdater implements ServerDataGetter.OnResultListener {

        void updateData (ArrayList<Integer> ids) {
            ServerDataGetter getter = new ServerDataGetter();
            getter.setOnListChangeListener(this);
            getter.execute("http://narodmon.ru/client.php?json={\"cmd\":\"sensorInfo\",\"uuid\":\"38c07002121a4fc852d8d8251c18cfb\",\"sensor\":[551,115,47]}");
        }

        @Override
        public void onResultReceived(String result) {
            Log.d(TAG,"ResultReceived: " + result);
        }

        @Override
        public void onNoResult() {
            Log.d(TAG,"noResult!!!!");
        }
    }

    @Override
    protected void doWakefulWork(Intent intent) {
        Log.d(TAG,"nmWatcher work...");
        Configuration config = ConfigHolder.getInstance(this).getConfig();
        Log.d(TAG,"config size: "+ config.watchedId.size());
        updater = new SensorDataUpdater();
        Log.d(TAG,"start update");
        updater.updateData(new ArrayList<Integer>());
        Log.d(TAG,"endWakefulWork");
    }
}