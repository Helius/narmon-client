package com.ghelius.narodmon;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

public class OnBootReceiver extends BroadcastReceiver {
    static final String TAG = "narodmon-broadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG,"onReceive intent");
        AlarmManager mgr=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, OnAlarmReceiver.class);
        PendingIntent pi=PendingIntent.getBroadcast(context, 0, i, 0);

	    mgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
			    SystemClock.elapsedRealtime() + (1 * 60000), // 1 minute
			    (60000 * Integer.valueOf(PreferenceManager.
					    getDefaultSharedPreferences(context).
					    getString(context.getString(R.string.pref_key_interval), "5"))),
			    pi);

    }
}