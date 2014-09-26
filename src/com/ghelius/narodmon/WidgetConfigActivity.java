package com.ghelius.narodmon;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;

public class WidgetConfigActivity extends ActionBarActivity {
    private final static String TAG = "narodmon-widgetConfig";
    private int mAppWidgetId;
    private SensorItemAdapter adapter;
    private TextView emptyTextMessage;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.widget_config_activity);
        setTitle(getString(R.string.select_sensor_text));
        setResult(RESULT_CANCELED);
        Intent intent = getIntent();
        ListView list = (ListView) findViewById(R.id.listView);
        emptyTextMessage = noItems(list, "");
        list.setEmptyView(emptyTextMessage);
        adapter = new SensorItemAdapter(getApplicationContext(), getSavedList());
        adapter.hideValue(true);
        adapter.updateFilter();
        if (adapter.getCount() == 0) {
            emptyTextMessage.setText(getString(R.string.empty_sensor_for_widget_message));
        }


        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
                RemoteViews views = new RemoteViews(getApplicationContext().getPackageName(), R.layout.widget_layout);
                String name = ((EditText) findViewById(R.id.editName)).getText().toString();
                if (name.length() == 0) {
                    name = adapter.getItem(position).name;
                }
                Sensor sensor = adapter.getItem(position);

                // save to db
                DatabaseManager.getInstance().addWidget(new Widget(mAppWidgetId, sensor.id, name, adapter.getItem(position).type));
                DatabaseManager.getInstance().addFavorites(sensor.id,sensor.deviceId);

                // set up widget icon and name
//				views.setTextViewText(R.id.name, name);
//				views.setImageViewBitmap(R.id.imageView,((BitmapDrawable)SensorTypeProvider.getInstance(getApplicationContext()).getIcon(sensor.type)).getBitmap());
//				views.setTextViewText(R.id.unit, SensorTypeProvider.getInstance(getApplicationContext()).getUnitForType(sensor.type));
//				/* did it in watchService in updating */
                // When we click the widget, we want to open our main activity.
//				Intent launchActivity = new Intent(getApplicationContext(), SensorInfo.class);
//				launchActivity.putExtra("sensorId", adapter.getItem(position).id);
//				PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, launchActivity, 0);
//				views.setOnClickPendingIntent(R.id.widget_body, pendingIntent);
                // When we click the widget, we want to open our main activity.
                //TODO!!! we need to open sensor info!!
//                Intent launchActivity = new Intent(getApplicationContext(), MainActivity.class);
//                launchActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                launchActivity.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
//                launchActivity.putExtra("sensorId", sensor.id);
//                PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), sensor.id, launchActivity, 0);
//                views.setOnClickPendingIntent(R.id.widget_body, pendingIntent);
////                appWidgetManager.updateAppWidget(w.widgetId, remoteViews);
//                appWidgetManager.updateAppWidget(mAppWidgetId, views);

                // start watch service for updateFilter data
                WakefulIntentService.sendWakefulWork(getApplicationContext(), WatchService.class);

                // config done
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                setResult(RESULT_OK, resultValue);
                finish();
            }
        });

        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
    }

    ArrayList<Sensor> getSavedList() {
        final String fileName = "sensorList.obj";
        ArrayList<Sensor> sensorList = new ArrayList<Sensor>();
        Log.d(TAG, "------restore list start-------");
        FileInputStream fis;
        try {
            fis = getApplicationContext().openFileInput(fileName);
            ObjectInputStream is = new ObjectInputStream(fis);
            sensorList.addAll((ArrayList<Sensor>) is.readObject());
            is.close();
            fis.close();
            for (Sensor aSensorList : sensorList) aSensorList.value = "--";
            Log.d(TAG, "------restored list end------- " + sensorList.size());
        } catch (Exception e) {
            Log.e(TAG, "Can't read sensorList: " + e.getMessage());
        }
        return sensorList;
    }

    private TextView noItems(ListView listView, String text) {
        TextView emptyView = new TextView(this);
        //Make sure you import android.widget.LinearLayout.LayoutParams;
        emptyView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        //Instead of passing resource id here I passed resolved color
        //That is, getResources().getColor((R.color.gray_dark))
//        emptyView.setTextColor(getResources().getColor(R.color.));
        emptyView.setText(text);
        emptyView.setTextSize(14);
        emptyView.setVisibility(View.GONE);
        emptyView.setGravity(Gravity.CENTER_VERTICAL
                | Gravity.CENTER_HORIZONTAL);

        //Add the view to the list view. This might be what you are missing
        ((ViewGroup) listView.getParent()).addView(emptyView);

        return emptyView;
    }
}
