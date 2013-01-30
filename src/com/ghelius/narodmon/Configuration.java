package com.ghelius.narodmon;

import java.io.Serializable;
import java.util.ArrayList;


public class Configuration implements Serializable{
    final public static Integer NOTHING      = 0;
    final public static Integer LEVEL        = 1;
    final public static Integer LEVEL_INVERT = 2;
    public String uid;

    public class SensorTask implements Serializable {
        SensorTask (Integer id, String name) {
            this.id = id;
            this.job = NOTHING;
            this.name = name;
        }
        Integer id;
        Integer hi;
        Integer lo;
        Integer job;
        Float lastValue;
        Long timestamp;
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
