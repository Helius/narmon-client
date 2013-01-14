package com.ghelius.narodmon;

import android.content.Intent;
import android.util.Log;
import com.commonsware.cwac.wakeful.WakefulIntentService;

public class WatchService extends WakefulIntentService {
    public WatchService() {
        super("Narodmon watcher");
    }

    @Override
    protected void doWakefulWork(Intent intent) {
        Log.d("narodmon-service","nmWatcher work...");
    }
}