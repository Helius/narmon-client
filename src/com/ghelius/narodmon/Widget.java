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
	int type;
	float lastValue;

	Widget() {
		screenName = "sensor";
		widgetId = -1;
		sensorId = -1;
		lastValue = 0;
		type = 0;
	}
	Widget(int widgetId, int sensorId, String screenName, int type) {
		this.widgetId = widgetId;
		this.sensorId = sensorId;
		this.screenName = screenName;
		this.type = type;
	}
	Widget(int widgetId, int sensorId, String screenName, int type, String value) {
		this.widgetId = widgetId;
		this.sensorId = sensorId;
		this.screenName = screenName;
		this.type = type;
		try{
			this.lastValue = Float.valueOf(value);
		} catch (Exception e) {
			this.lastValue = 0.f;
		}
	}
}
