package com.ghelius.narodmon;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;
import com.commonsware.cwac.wakeful.WakefulIntentService;

import java.util.ArrayList;

public class WatchService extends WakefulIntentService {
    private final static String TAG = "narodmon-service";
    private int NOTIFICATION = R.string.local_service_started;
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
            //parse json, check alarm - buzzz and show notification
            showNotification(1);
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
        Log.d(TAG, "endWakefulWork");
    }


    /**
     * Show a notification while this service is running.
     */
    private void showNotification(int nmb) {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        //CharSequence text = getText(R.string.local_service_started);
        NotificationManager mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.app_icon, "Sensor alarm",
                System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, "Alarm", nmb + " sensor", contentIntent);
        //notification.

        // Send the notification.
        if (mNM!=null)
            mNM.notify(NOTIFICATION, notification);
        else
            Log.e(TAG,"notification manager is NULL!!");
    }
}