package com.ghelius.narodmon;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;
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

    private void updateNotify (String name, String value, String job, String limit) {
        showNotification (name, value + " " + job + " " + limit);
    }

    private void checkLimits(Integer id, Float value, Long timeStamp) {
        Configuration.SensorTask task = ConfigHolder.getInstance(this).getSensorTask(id);
        task.lastValue = value;
        task.timestamp = timeStamp;
        if (task.job == Configuration.NOTHING) {
            return;
        }
        if (task.job == Configuration.MORE_THAN) {
            if (value > task.hi) {
                updateNotify(task.name, String.valueOf(value), getString(R.string.text_notify_more_than), String.valueOf(task.hi));
            }
        } else if (task.job == Configuration.LESS_THAN) {
            if (value < task.lo) {
                updateNotify(task.name, String.valueOf(value), getString(R.string.text_notify_less_than), String.valueOf(task.lo));
            }
        } else if (task.job == Configuration.OUT_OF) {
            if (value > task.hi) {
                updateNotify(task.name, String.valueOf(value), getString(R.string.text_notify_more_than), String.valueOf(task.hi));
            } else if (value < task.lo) {
                updateNotify(task.name, String.valueOf(value), getString(R.string.text_notify_less_than), String.valueOf(task.lo));
            }
        } else if (task.job == Configuration.WITHIN_OF) {
            if ((value > task.lo) && (value < task.hi)) {
                updateNotify(task.name, String.valueOf(value), getString(R.string.text_notify_within), String.valueOf(task.lo) + ".." + String.valueOf(task.hi));
            }
        }
        task.lastValue = value;
        task.timestamp = timeStamp;
    }

    class SensorDataUpdater implements ServerDataGetter.OnResultListener {
        ServerDataGetter getter;
        void updateData (ArrayList<Integer> ids) {
            if (getter!=null) {
                getter.cancel(true);
            }
            getter = new ServerDataGetter();
            getter.setOnListChangeListener(this);
            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < ids.size(); ++i) {
                if (i != 0) {
                    buf.append(",");
                }
                buf.append(ids.get(i));
            }
            String queryId = buf.toString();
            getter.execute(NarodmonApi.apiUrl, ConfigHolder.getInstance(WatchService.this).getApiHeader() + "\"cmd\":\"sensorInfo\",\"sensor\":["+ queryId +"]}");

        }

        @Override
        public void onResultReceived(String result) {
            getter = null;
            if (result != null) {
                try {
                    JSONObject JObject = new JSONObject(result);
                    JSONArray sensArray = JObject.getJSONArray("sensors");
                    for (int i = 0; i < sensArray.length(); i++) {
                        String id = sensArray.getJSONObject(i).getString("id");
                        String value = sensArray.getJSONObject(i).getString("value");
                        String time = sensArray.getJSONObject(i).getString("time");
                        Log.d(TAG,"for " + id + " val: " + value + ", time " + time);
                        checkLimits(Integer.valueOf(id), Float.valueOf(value), Long.valueOf(time));

                    }
                } catch (JSONException e) {
                    Log.e(TAG,"Wrong JSON");
                }
            }
        }

        @Override
        public void onNoResult() {
            getter = null;
            Log.w(TAG,"noResult!!!!");
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
    private void showNotification(String name, String value) {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        int dash = 500;     // Length of a Morse Code "dash" in milliseconds
        int gap = 200;    // Length of Gap Between dots/dashes
        long[] pattern = {
                0, dash, gap, dash, gap, dash
        };
        v.vibrate(pattern,-1);
        // In this sample, we'll use the same text for the ticker and the expanded notification
        //CharSequence text = getText(R.string.local_service_started);
        NotificationManager mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.app_icon, "Sensor alarm",
                System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        //TODO: we must show AlarmInfo or watched list, do it later...
        Intent i = new Intent (this, MainActivity.class);
        //i.putExtra("Mode","watch");
        //i.putExtra("Sensor", watchAdapter.getItem(position));
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i, 0);

        // Set the info for the views that show in the notification panel.

        notification.setLatestEventInfo(this, name, value, contentIntent);
        //notification.

        // Send the notification.
        if (mNM!=null)
            mNM.notify(NOTIFICATION, notification);
        else
            Log.e(TAG,"notification manager is NULL!!");
    }
}