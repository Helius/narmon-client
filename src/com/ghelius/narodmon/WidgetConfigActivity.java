package com.ghelius.narodmon;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RemoteViews;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import java.util.ArrayList;

public class WidgetConfigActivity extends SherlockFragmentActivity {
	private int mAppWidgetId;
	public void onCreate(Bundle savedInstanceState) {
		setTheme(com.actionbarsherlock.R.style.Theme_Sherlock);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.widget_config_activity);
		setTitle(getString(R.string.select_sensor_text));
		setResult(RESULT_CANCELED);
		Intent intent = getIntent();
		ListView list = (ListView) findViewById(R.id.listView);
		SensorItemAdapter adapter = new SensorItemAdapter(getApplicationContext(), new ArrayList<Sensor>());
		list.setAdapter(adapter);
		list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
				RemoteViews views = new RemoteViews(getApplicationContext().getPackageName(),
						R.layout.widget_layout);
				appWidgetManager.updateAppWidget(mAppWidgetId, views);
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
}
