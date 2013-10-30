package com.ghelius.narodmon;

public class AlarmSensorTask {
	final public static int NOTHING = 0;
	final public static int MORE_THAN = 1;
	final public static int LESS_THAN = 2;
	final public static int OUT_OF = 3;
	final public static int WITHIN_OF = 4;

	AlarmSensorTask(Integer id, Integer job, Float hi, Float lo, Float value) {
		this.id  = id;
		this.job = job;
		this.hi  = hi;
		this.lo  = lo;
		this.lastValue = value;
	}

	int id;
	float hi;
	float lo;
	int job;
	float lastValue;
	long timestamp;
	String name;
}
