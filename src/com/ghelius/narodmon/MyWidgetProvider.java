package com.ghelius.narodmon;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.widget.RemoteViews;
import com.commonsware.cwac.wakeful.WakefulIntentService;

public class MyWidgetProvider extends AppWidgetProvider {

	private static final String LOG = "narodmon-widgetProvider";

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
	                     int[] appWidgetIds) {
		DatabaseHandler dbh = new DatabaseHandler(context);
		Log.w(LOG, "onUpdate method called");
		// Get all ids
		ComponentName thisWidget = new ComponentName(context,
				MyWidgetProvider.class);
		int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
		AppWidgetManager manager = AppWidgetManager.getInstance(context);
		RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
		for (int wID : allWidgetIds) {
			Widget widget = dbh.getWidgetByWidgetId(wID);
			updateViews.setTextViewText(R.id.name, widget.screenName);
			updateViews.setImageViewBitmap(R.id.imageView, ((BitmapDrawable) SensorTypeProvider.getInstance(context).getIcon(widget.type)).getBitmap());
			updateViews.setTextViewText(R.id.unit, SensorTypeProvider.getInstance(context).getUnitForType(widget.type));
			manager.updateAppWidget(thisWidget, updateViews);
			// start data update service
			WakefulIntentService.sendWakefulWork(context, WatchService.class);
		}
		dbh.close();
	}
	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
		DatabaseHandler dbh = new DatabaseHandler(context);
		dbh.deleteWidgetByWidgetId(appWidgetIds[0]);
		dbh.close();
	}
//	@Override
//	public void onDisabled(Context context)	{}
}
