package com.ghelius.narodmon;


import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.ArrayList;

public class UpdateWidgetService extends Service {
	private static final String TAG = "narodmon-UpdateWidgetService";

	@Override
	public void onStart(Intent intent, int startId) {
		Log.i(TAG, "Called");
		// Create some random data

//		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this
//				.getApplicationContext());
//
//		int[] allWidgetIds = intent
//				.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
//
//		ComponentName thisWidget = new ComponentName(getApplicationContext(),
//				MyWidgetProvider.class);
//		int[] allWidgetIds2 = appWidgetManager.getAppWidgetIds(thisWidget);
//		Log.w(LOG, "From Intent" + String.valueOf(allWidgetIds.length));
//		Log.w(LOG, "Direct" + String.valueOf(allWidgetIds2.length));
//
//		for (int widgetId : allWidgetIds) {
//			// Create some random data
//			int number = (new Random().nextInt(100));
//
//			RemoteViews remoteViews = new RemoteViews(this
//					.getApplicationContext().getPackageName(),
//					R.layout.widget_layout);
//			Log.w("WidgetExample", String.valueOf(number));
//			// Set the text
//			remoteViews.setTextViewText(R.id.update,
//					"Random: " + String.valueOf(number));
//
//			// Register an onClickListener
//			Intent clickIntent = new Intent(this.getApplicationContext(),
//					MyWidgetProvider.class);
//
//			clickIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
//			clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,
//					allWidgetIds);
//
//			PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, clickIntent,
//					PendingIntent.FLAG_UPDATE_CURRENT);
//			remoteViews.setOnClickPendingIntent(R.id.update, pendingIntent);
//			appWidgetManager.updateAppWidget(widgetId, remoteViews);
//		}


		RemoteViews remoteViews = new RemoteViews(getApplicationContext().getPackageName(), R.layout.widget_layout);
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
		// update widgets for with sensor
		DatabaseHandler dbh = new DatabaseHandler(getApplicationContext());
		ArrayList<Widget> widgets = dbh.getAllWidgets();
		for (Widget w: widgets) {
			Log.d(TAG,"update: " + w.screenName + ", curr: " + w.curValue + ", last: " + w.lastValue);
			remoteViews.setTextViewText(R.id.value, String.valueOf(w.curValue));
			remoteViews.setTextViewText(R.id.name, w.screenName);
			remoteViews.setImageViewBitmap(R.id.imageView, ((BitmapDrawable) SensorTypeProvider.getInstance(getApplicationContext()).getIcon(w.type)).getBitmap());
			remoteViews.setTextViewText(R.id.unit, SensorTypeProvider.getInstance(getApplicationContext()).getUnitForType(w.type));
			if (w.lastValue > w.curValue) {
				remoteViews.setTextViewText(R.id.arrowDown, "▼");
				remoteViews.setTextViewText(R.id.arrowUp, "");
			} else if (w.lastValue < w.curValue) {
				remoteViews.setTextViewText(R.id.arrowDown, "");
				remoteViews.setTextViewText(R.id.arrowUp, "▲");
			} else {
				remoteViews.setTextViewText(R.id.arrowDown, "");
				remoteViews.setTextViewText(R.id.arrowUp, "");
			}
			// When we click the widget, we want to open our main activity.
			//TODO!!! we need to open sensor info!!
//			Intent launchActivity = new Intent(getApplicationContext(), SensorInfo.class);
//			launchActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//			launchActivity.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
//			launchActivity.putExtra("sensorId", w.sensorId);
//			PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), w.widgetId, launchActivity, 0);
//			remoteViews.setOnClickPendingIntent(R.id.widget_body, pendingIntent);
			appWidgetManager.updateAppWidget(w.widgetId, remoteViews);
		}
		dbh.close();

		stopSelf();

		super.onStart(intent, startId);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
