package com.ghelius.narodmon;

import java.io.Serializable;
import java.util.ArrayList;


public class Configuration implements Serializable{
    final public static int NOTHING      = 0;
    final public static int MORE_THAN    = 1;
    final public static int LESS_THAN    = 2;
    final public static int OUT_OF       = 3;
    final public static int WITHIN_OF    = 4;
    public String uid;


    public class SensorTask implements Serializable {
        SensorTask (Integer id, String name) {
            this.id = id;
            this.job = NOTHING;
            this.name = name;
        }
        int    id;
        float  hi;
        float  lo;
        int    job;
        float  lastValue;
        long   timestamp;
        String name;
    }

    ArrayList<SensorTask> watchedId;
    Configuration () {
        watchedId = new ArrayList<SensorTask>();
    }
    public void insert (Integer id, String name) {
        watchedId.add(new SensorTask(id, name.substring(0, (name.length() <= 32) ? name.length() : 32)));
    }
}
