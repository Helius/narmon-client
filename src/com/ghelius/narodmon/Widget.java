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
	float curValue;
	float lastValue;

	Widget() {
		screenName = "sensor";
		widgetId = -1;
		sensorId = -1;
		lastValue = 0;
		curValue = 0;
		type = 0;
	}
	Widget(int widgetId, int sensorId, String screenName, int type) {
		this.widgetId = widgetId;
		this.sensorId = sensorId;
		this.screenName = screenName;
		this.type = type;
	}

	Widget(int widgetId, int sensorId, String screenName, int type, String lastValue, String curValue) {
		this.widgetId = widgetId;
		this.sensorId = sensorId;
		this.screenName = screenName;
		this.type = type;
		try{
			this.curValue = Float.valueOf(curValue);
		} catch (Exception e) {
			this.curValue = 0.f;
		}
		try{
			this.lastValue = Float.valueOf(lastValue);
		} catch (Exception e) {
			this.lastValue = 0.f;
		}
	}
}
