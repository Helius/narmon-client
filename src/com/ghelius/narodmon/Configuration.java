package com.ghelius.narodmon;

import java.io.Serializable;
import java.util.ArrayList;


public class Configuration implements Serializable{
    final Integer NOTHING      = 0;
    final Integer LEVEL        = 1;
    final Integer LEVEL_INVERT = 2;
    public String uid;

    public class SensorTask implements Serializable {
        SensorTask (Integer id) {
            this.id = id;
            this.job = NOTHING;
        }
        Integer id;
        Integer hi;
        Integer lo;
        Integer job;
    }

    ArrayList<SensorTask> watchedId;
    Configuration () {
        watchedId = new ArrayList<SensorTask>();
    }
    public void insert (Integer id) {
        watchedId.add(new SensorTask(id));
    }
    public ArrayList<SensorTask> getWatchedId () {
        return watchedId;
    }
}
