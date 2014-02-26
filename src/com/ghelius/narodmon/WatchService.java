package com.ghelius.narodmon;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;


public class WatchService extends WakefulIntentService {
    private final static String TAG = "narodmon-service";
    private int NOTIFICATION = R.string.local_service_started;
    private ArrayList<Integer> ids;

    private int lastNotifyId = 0;

    public WatchService() {
        super("Narodmon watcher");
        ids = new ArrayList<Integer>();
    }

    private void updateNotify (String message, String name, int id) {
        Log.d(TAG, "!!!!!! Alarm exist: update Notify !!!!!");
        createInfoNotification(message, name, id);
//        showNotification(name, value + " " + job + " " + limit);
    }


    private String inputStreamToString(InputStream is) {
        String s = "";
        String line = "";
        // Wrap a BufferedReader around the InputStream
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        // Read response until the end
        try {
            while ((line = rd.readLine()) != null) { s += line; }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return s;
    }
    private boolean updateData (ArrayList<Integer> ids) {
        Log.d(TAG,"start updating");
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < ids.size(); ++i) {
            if (i != 0) {
                buf.append(",");
            }
            buf.append(ids.get(i));
        }
        String queryId = buf.toString();
        HttpResponse r = ServerDataGetter.makeRequest(getApplicationContext().getString(R.string.api_url), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("apiHeader","") + "\"cmd\":\"sensorInfo\",\"sensor\":[" + queryId + "]}");
        if (r == null) {
            Log.e(TAG,"HttpResponse is null");
            return false;
        }
        InputStream in = null;
        try {
            in = r.getEntity().getContent();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        String responseString = inputStreamToString(in);
        Log.d(TAG,"result: " + responseString);
        handleResult(responseString);
        Log.d(TAG,"------ stop updating ------");
        return true;
    }


    public void handleResult (String result) {
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
                    AlarmSensorTask task = DatabaseManager.getInstance().getAlarmById(id);
                    Float v = null;
                    try {
                        v = Float.valueOf(value);
                    } catch (Exception e ) { }
                    if (task != null && v != null) {
                        if (task.checkAlarm(v)) {
                            //ALARM!!!
                            updateNotify(value, task.name, task.id);
                        }
                        task.lastValue = v;
                        task.timestamp = Long.valueOf(time);
                        DatabaseManager.getInstance().addAlarmTask(task);
                    }

//	                    updateFilter widgets value
                    //Log.d(TAG,"\nwidget for:" + id);
                    ArrayList<Widget> widgets = DatabaseManager.getInstance().getWidgetsBySensorId(id);
                    for (Widget w: widgets) {
                        widgetsFound = true;
                        Log.d(TAG,"sensor is widget, updateFilter value: " + w.screenName + ", w.last=" + w.lastValue + ", w.cur=" + w.curValue + ", will set cur=" + value);
                        w.lastValue = w.curValue;
                        w.curValue = Float.valueOf(value);
                        DatabaseManager.getInstance().updateValueByWidgetId(w);
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
    protected void doWakefulWork(Intent intent) {
        //createInfoNotification("Temperature -35 C", "б-р Молодежи, 4");
        Log.d(TAG,"#thread: " + Thread.currentThread().getName());

        Log.d(TAG,"nmWatcher start working...");
	    ArrayList<Widget> widgetsList = DatabaseManager.getInstance().getAllWidgets();
        ArrayList<AlarmSensorTask> alarmList = DatabaseManager.getInstance().getAlarmTask();
	    Log.d(TAG,"widget amount: "+ widgetsList.size());
        ids.clear();
        for (int i = 0; i < alarmList.size(); i++) {
            if (alarmList.get(i).job != AlarmSensorTask.NOTHING)
                ids.add(alarmList.get(i).id);
        }
	    for (int i = 0; i < widgetsList.size(); i++) {
		    ids.add(widgetsList.get(i).sensorId);
	    }
        if (!ids.isEmpty()) {
            Log.d(TAG, "start watched with " + ids.size() + " sensors");
            if (!updateData (ids)) {
                Log.d(TAG,"problem update data");
                Intent i = new Intent(getApplicationContext(), UpdateWidgetService.class);
                i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new Integer[0]);
                i.putExtra("problem",true);
                // Update the widgets via the service
                getApplicationContext().startService(i);
            }
        } else {
            Log.d(TAG, "no watched id, just exit");
        }
        Log.d(TAG,"nmWatcher stop working...");
    }


    /**
     * Show a notification while this service is running.
     */

    public int createInfoNotification(String message, String name, int id) {
        Context context = getApplicationContext();
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent notificationIntent = new Intent(context, MainActivity.class); // по клику на уведомлении откроется HomeActivity
        notificationIntent.putExtra("sensorId", id);
        NotificationCompat.Builder nb = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.app_icon) //иконка уведомления
                .setAutoCancel(true) //уведомление закроется по клику на него
                .setTicker("Public monitoring sensor alarm")   //текст, который отобразится вверху статус-бара при создании уведомления
                //.setSubText("г.Бердск ул.Горького")
                .setContentText(name) // Основной текст уведомления
                .setContentIntent(PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT))
//                .setWhen(System.currentTimeMillis()) //отображаемое время уведомления
                .setUsesChronometer(true)
                .setContentTitle(message) //заголовок уведомления
                .setDefaults(Notification.DEFAULT_ALL); // звук, вибро и диодный индикатор выставляются по умолчанию

        Notification notification = nb.build(); //генерируем уведомление
        manager.notify(id, notification); // отображаем его пользователю.
        return 0;
    }

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
        //TODO: we must show AlarmInfo or watched list
        Intent i = new Intent (getApplicationContext(),MainActivity.class);
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