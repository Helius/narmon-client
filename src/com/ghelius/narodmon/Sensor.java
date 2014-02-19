package com.ghelius.narodmon;

import java.io.Serializable;


public class Sensor implements Serializable {
    public int id;
    public int type;
    public String location;
    public String name;
    public String value;
    public float distance;
    public long  time;
    public boolean my;
    public boolean pub;
    public boolean online;
    public boolean hasAlarm;
    public boolean alarmed;
    public boolean favorite;

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
        this.hasAlarm = false;
        this.alarmed = false;
        this.favorite = false;
    }


    public Float valueToFloat () {
        Float v = null;
        try {
            v = Float.valueOf(value);
        } catch (Exception e) {}
        return v;
    }
}
