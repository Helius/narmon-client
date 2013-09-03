package com.ghelius.narodmon;

/**
 * User: eugene
 * Date: 9/3/13
 * Time: 2:34 PM
 * Hold home screen widget info
 */
public class Widget {
	String screenName;
	int widgetId;
	int sensorId;

	Widget() {
		screenName = "sensor";
		widgetId = -1;
		sensorId = -1;
	}
	Widget(int widgetId, int sensorId, String screenName) {
		this.widgetId = widgetId;
		this.sensorId = sensorId;
		this.screenName = screenName;
	}
}
