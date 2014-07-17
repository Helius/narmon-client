package com.ghelius.narodmon;


import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.ArrayList;

public class UpdateWidgetService extends Service {
	private static final String TAG = "narodmon-UpdateWidgetService";

	@Override
	public void onStart(Intent intent, int startId) {
		Log.i(TAG, "Called");

        RemoteViews remoteViews;
        if (PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(getString(R.string.key_widget_light_theme), false)) {
            remoteViews = new RemoteViews(getApplicationContext().getPackageName(), R.layout.widget_layout_light);
        } else {
            remoteViews = new RemoteViews(getApplicationContext().getPackageName(), R.layout.widget_layout);
        }
        boolean problem = intent.getBooleanExtra("problem", false);
        Log.d(TAG,"problem is " + problem);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
		// updateFilter widgets for with sensor
		ArrayList<Widget> widgets = DatabaseManager.getInstance().getAllWidgets();
		for (Widget w: widgets) {
			Log.d(TAG,"updateFilter: " + w.screenName + ", curr: " + w.curValue + ", last: " + w.lastValue);
            if (!problem)
                remoteViews.setTextViewText(R.id.value, w.getTextValue(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(
                       getString(R.string.pref_key_digits_amount) ,"-"
                )));
            else
                remoteViews.setTextViewText(R.id.value, "--");
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


//			// When we click the widget, we want to open our main activity.
//			//TODO!!! we need to open sensor info!!
			Intent launchActivity = new Intent(getApplicationContext(), MainActivity.class);
			launchActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			launchActivity.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
			launchActivity.putExtra("sensorId", w.sensorId);
			PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), w.widgetId, launchActivity, PendingIntent.FLAG_UPDATE_CURRENT);
			remoteViews.setOnClickPendingIntent(R.id.widget_body, pendingIntent);
			appWidgetManager.updateAppWidget(w.widgetId, remoteViews);
		}

		stopSelf();

		super.onStart(intent, startId);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
