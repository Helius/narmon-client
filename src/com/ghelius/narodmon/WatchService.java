package com.ghelius.narodmon;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;
import com.commonsware.cwac.wakeful.WakefulIntentService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class WatchService extends WakefulIntentService {
    private final static String TAG = "narodmon-service";
    private int NOTIFICATION = R.string.local_service_started;
    private ArrayList<Integer> ids;
    SensorDataUpdater updater;

    public WatchService() {
        super("Narodmon watcher");
        ids = new ArrayList<Integer>();
    }

    private boolean checkLimits(Integer id, Integer value, Long timeStamp) {
        return ConfigHolder.getInstance(this).checkLimits(id, value, timeStamp);
    }

    class SensorDataUpdater implements ServerDataGetter.OnResultListener {

        void updateData (ArrayList<Integer> ids) {
            ServerDataGetter getter = new ServerDataGetter();
            getter.setOnListChangeListener(this);
            String queryId = "";
            for (int i = 0; i < ids.size(); i++) {
                if (i != 0) {
                    queryId += ",";
                }
                queryId += ids.get(i);
            }
            getter.execute("http://narodmon.ru/client.php?json={\"cmd\":\"sensorInfo\",\"uuid\":\""+
                    ConfigHolder.getInstance(WatchService.this).getUid() +"\",\"sensor\":["+ queryId +"]}");
        }

        @Override
        public void onResultReceived(String result) {
            //Log.d(TAG,"ResultReceived: " + result);
            // response:
            //{"sensors":[{"id":115,"value":-2,"time":"1358220969"},{"id":551,"value":-6.88,"time":"1358207098"}]}
            if (result != null) {
                try {
                    JSONObject JObject = new JSONObject(result);
                    JSONArray sensArray = JObject.getJSONArray("sensors");
                    for (int i = 0; i < sensArray.length(); i++) {
                        String id = sensArray.getJSONObject(i).getString("id");
                        String value = sensArray.getJSONObject(i).getString("value");
                        String time = sensArray.getJSONObject(i).getString("time");
                        Log.d(TAG,"for " + id + " val: " + value + ", time " + time);
                        checkLimits(Integer.valueOf(id), Integer.valueOf(value), Long.valueOf(time));
                    }
                } catch (JSONException e) {
                    // todo: replace it
                    Log.e(TAG,"Wrong JSON");
                    e.printStackTrace();

                }
            }
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
        //Log.d(TAG,"config size: "+ config.watchedId.size());
        updater = new SensorDataUpdater();
        Log.d(TAG, "start update");
        ids.clear();
        for (int i = 0; i < config.watchedId.size(); i++) {
            ids.add(config.watchedId.get(i).id);
        }
        if (!ids.isEmpty()) {
            Log.d(TAG, "start watched with " + ids.size() + " sensors");
            updater.updateData(ids);
        } else {
            Log.d(TAG, "no watched id, just exit");
        }
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