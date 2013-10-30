package com.ghelius.narodmon;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
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
	DatabaseHandler dbh = null;

    public WatchService() {
        super("Narodmon watcher");
        ids = new ArrayList<Integer>();
    }

    private void updateNotify (String name, String value, String job, String limit) {
        showNotification (name, value + " " + job + " " + limit);
    }

    private void checkLimits(Integer id, Float value, Long timeStamp) {
        Configuration.SensorTask task = ConfigHolder.getInstance(this).getSensorTask(id);
        if (task.job == Configuration.NOTHING) {
        }
        if (task.job == Configuration.MORE_THAN) {
            if (value > task.hi && task.lastValue <= task.hi) {
                updateNotify(task.name, String.valueOf(value), getString(R.string.text_notify_more_than), String.valueOf(task.hi));
            }
        } else if (task.job == Configuration.LESS_THAN) {
            if (value < task.lo && task.lastValue >= task.lo) {
                updateNotify(task.name, String.valueOf(value), getString(R.string.text_notify_less_than), String.valueOf(task.lo));
            }
        } else if (task.job == Configuration.OUT_OF) {
            if (value > task.hi && task.lastValue <= task.hi) {
                updateNotify(task.name, String.valueOf(value), getString(R.string.text_notify_more_than), String.valueOf(task.hi));
            } else if (value < task.lo && task.lastValue >= task.lo) {
                updateNotify(task.name, String.valueOf(value), getString(R.string.text_notify_less_than), String.valueOf(task.lo));
            }
        } else if (task.job == Configuration.WITHIN_OF) {
            if (value > task.lo && task.lastValue <= task.lo) {
	            updateNotify(task.name, String.valueOf(value), getString(R.string.text_notify_within), String.valueOf(task.lo) + ".." + String.valueOf(task.hi));
	        } else if (value < task.hi && task.lastValue >= task.hi) {
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
//	        RemoteViews remoteViews = new RemoteViews(getApplicationContext().getPackageName(), R.layout.widget_layout);
//	        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
            getter = null;
	        boolean widgetsFound = false;
            if (result != null) {
                try {
                    JSONObject JObject = new JSONObject(result);
                    JSONArray sensArray = JObject.getJSONArray("sensors");
                    for (int i = 0; i < sensArray.length(); i++) {
                        int id = Integer.valueOf(sensArray.getJSONObject(i).getString("id"));
                        String value = sensArray.getJSONObject(i).getString("value");
                        String time = sensArray.getJSONObject(i).getString("time");
                        Log.d(TAG,"for " + id + " val: " + value + ", time " + time);

                        // check limits for watched item
	                    if (ConfigHolder.getInstance(getApplicationContext()).isSensorWatched(id))
	                        checkLimits(id, Float.valueOf(value), Long.valueOf(time));

//	                    update widgets value
	                    Log.d(TAG,"\nwidget for:" + id);
	                    ArrayList<Widget> widgets = dbh.getWidgetsBySensorId(id);
	                    for (Widget w: widgets) {
		                    widgetsFound = true;
		                    Log.d(TAG,"sensor is widget, update value: " + w.screenName + "w.last=" + w.lastValue + "w.cur=" + w.curValue + "will set cur=" + value);
		                    w.lastValue = w.curValue;
		                    w.curValue = Float.valueOf(value);
		                    dbh.updateValueByWidgetId(w);
	                    }
                    }
	                if (widgetsFound) {
		                // Build the intent to call the service
		                Intent intent = new Intent(getApplicationContext(), UpdateWidgetService.class);
		                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new Integer[0]);
		                // Update the widgets via the service
		                getApplicationContext().startService(intent);
	                }
                } catch (JSONException e) {
                    Log.e(TAG,"Wrong JSON");
                }
            }
        }

        @Override
        public void onNoResult() {
            getter = null;
            Log.w(TAG, "noResult!!!!");
        }
    }

    @Override
    protected void doWakefulWork(Intent intent) {
	    if (dbh == null)
	        dbh = new DatabaseHandler(getApplicationContext());
        Log.d(TAG,"nmWatcher work...");
        Configuration config = ConfigHolder.getInstance(this).getConfig();
	    ArrayList<Widget> widgetsList = dbh.getAllWidgets();
	    Log.d(TAG,"widget size: "+ widgetsList.size());
        updater = new SensorDataUpdater();
        Log.d(TAG, "start update");
        ids.clear();
        for (int i = 0; i < config.watchedId.size(); i++) {
            ids.add(config.watchedId.get(i).id);
        }
	    for (int i = 0; i < widgetsList.size(); i++) {
		    ids.add(widgetsList.get(i).sensorId);
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
        int dash = 300;     // Length of a Morse Code "dash" in milliseconds
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
	    notification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_AUTO_CANCEL;

        // The PendingIntent to launch our activity if the user selects this notification
        //TODO: we must show AlarmInfo or watched list, do it later...
        Intent i = new Intent (this, MainActivity.class);
	    i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
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