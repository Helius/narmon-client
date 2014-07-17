package com.ghelius.narodmon;

import java.text.DecimalFormat;

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

    String getTextValue (String pattern) {
        if (pattern.equals("-"))
            return String.valueOf(curValue);
        DecimalFormat df = new DecimalFormat(pattern);
        return df.format(curValue);
    }

	Widget(int widgetId, int sensorId, String screenName, int type) {
		this.widgetId = widgetId;
		this.sensorId = sensorId;
		this.screenName = screenName;
		this.type = type;
		this.curValue = 0;
		this.lastValue = 0;
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
