package com.ghelius.narodmon;

import android.util.Log;

public class AlarmSensorTask {
	final public static int NOTHING = 0;
	final public static int MORE_THAN = 1;
	final public static int LESS_THAN = 2;
	final public static int OUT_OF = 3;
	final public static int WITHIN_OF = 4;
    final private static String TAG = "narodmon-alarm";

	AlarmSensorTask(Integer id, Integer job, Float hi, Float lo, Float value, String name) {
        this.name = name;
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

    @Override
    public String toString() {
       return "Alarm [id: " + id + ", job: " + job + ", hi:" + hi + ", lo:" + lo + ", lastValue: " + lastValue + ", timestamp:" + timestamp + "]";
    }

    public boolean checkAlarm(Float value) {
        Log.d(TAG, "Check limit for job: " + this.job);
        if (this.job == AlarmSensorTask.NOTHING) {

        } else if (this.job == AlarmSensorTask.MORE_THAN) {
            if (value > this.hi && this.lastValue <= this.hi) {
                return true;
            }
        } else if (this.job == AlarmSensorTask.LESS_THAN) {
            if (value < this.lo && this.lastValue >= this.lo) {
                return true;
            }
        } else if (this.job == AlarmSensorTask.OUT_OF) {
            if (value > this.hi && this.lastValue <= this.hi) {
                return true;
            } else if (value < this.lo && this.lastValue >= this.lo) {
                return true;
            }
        } else if (this.job == AlarmSensorTask.WITHIN_OF) {
            if (value > this.lo && this.lastValue <= this.lo) {
                return true;
            } else if (value < this.hi && this.lastValue >= this.hi) {
                return true;
            }
        }
        return false;
    }

    public boolean isAlarmNow(Float value) {

        if (this.job == AlarmSensorTask.MORE_THAN) {
            if (value > this.hi) {
                return true;
            }
        } else if (this.job == AlarmSensorTask.LESS_THAN) {
            if (value < this.lo) {
                return true;
            }
        } else if (this.job == AlarmSensorTask.OUT_OF) {
            if (value > this.hi) {
                return true;
            } else if (value < this.lo) {
                return true;
            }
        } else if (this.job == AlarmSensorTask.WITHIN_OF) {
            if (value > this.lo) {
                return true;
            } else if (value < this.hi) {
                return true;
            }
        }
        return false;
    }
}
