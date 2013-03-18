package com.ghelius.narodmon;

import java.io.Serializable;


public class Sensor implements Serializable {
    public int id;
    public int type;
    final static int TYPE_UNKNOWN     = 0;
    final static int TYPE_TEMPERATURE = 1;
    final static int TYPE_HUMIDITY    = 2;
    final static int TYPE_PRESSURE    = 3;
    public String location;
    public String name;
    public String value;
    public float distance;
    public long  time;
    public boolean my;
    public boolean pub;
    public boolean online;


    // constructor
    Sensor (int id,
            int type,
            String location,
            String name,
            String value,
            float distance,
            boolean my,
            boolean pub,
            Long time)
    {
        this.id = id;
        this.type = type;
        this.location = location;
        this.name = name;
        this.value = value;
        this.distance = distance;
        this.my = my;
        this.time = time;
        this.pub = pub;
    }

    public Sensor(Configuration.SensorTask storedItem) {
        this.id = storedItem.id;
        this.name = storedItem.name;
        this.value = String.valueOf(storedItem.lastValue);
        this.time = storedItem.timestamp;
        this.online = false;
    }
}
