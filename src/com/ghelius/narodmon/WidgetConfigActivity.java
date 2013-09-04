package com.ghelius.narodmon;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RemoteViews;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;

public class WidgetConfigActivity extends SherlockFragmentActivity {
	private final static String TAG = "narodmon-widgetConfig";
	private int mAppWidgetId;
	private SensorItemAdapter adapter;
	private DatabaseHandler dbh;
	public void onCreate(Bundle savedInstanceState) {
		setTheme(com.actionbarsherlock.R.style.Theme_Sherlock);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.widget_config_activity);
		setTitle(getString(R.string.select_sensor_text));
		setResult(RESULT_CANCELED);
		Intent intent = getIntent();
		ListView list = (ListView) findViewById(R.id.listView);
		adapter = new SensorItemAdapter(getApplicationContext(), getSavedList());
		adapter.update();
		dbh = new DatabaseHandler(getApplicationContext());

		list.setAdapter(adapter);
		list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
				RemoteViews views = new RemoteViews(getApplicationContext().getPackageName(),R.layout.widget_layout);
				appWidgetManager.updateAppWidget(mAppWidgetId, views);
				dbh.addWidget(new Widget(mAppWidgetId, adapter.getItem(position).id, ((EditText) findViewById(R.id.editName)).getText().toString()));
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

	ArrayList<Sensor> getSavedList () {
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
				Log.d(TAG,"------restored list end------- " + sensorList.size());
			} catch (Exception e) {
				Log.e(TAG,"Can't read sensorList: " + e.getMessage());
			}
		return sensorList;
	}
}
