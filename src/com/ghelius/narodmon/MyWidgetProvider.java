package com.ghelius.narodmon;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MyWidgetProvider extends AppWidgetProvider {

	private static final String LOG = "narodmon-widgetProvider";
	private PendingIntent service = null;

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
	                     int[] appWidgetIds) {

		Log.w(LOG, "onUpdate method called");
		// Get all ids
		ComponentName thisWidget = new ComponentName(context,
				MyWidgetProvider.class);
		int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

		// Build the intent to call the service
		Intent intent = new Intent(context.getApplicationContext(),
				WatchService.class);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds);

		// Update the widgets via the service
		context.startService(intent);

//		final AlarmManager m = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//
//		final Calendar TIME = Calendar.getInstance();
//		TIME.set(Calendar.MINUTE, 0);
//		TIME.set(Calendar.SECOND, 0);
//		TIME.set(Calendar.MILLISECOND, 0);
//
//		final Intent i = new Intent(context, UpdateWidgetService.class);
//		i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds);
//
//		if (service == null)
//		{
//			service = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
//		}
//
//		m.setRepeating(AlarmManager.RTC, TIME.getTime().getTime(), 1000 * 20, service);
	}

//	@Override
//	public void onDisabled(Context context)
//	{
//		final AlarmManager m = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//
//		m.cancel(service);
//	}
}
