package com.ghelius.narodmon;

import java.io.Serializable;


public class Sensor implements Serializable {
    private int id;
    public int type;
    final static int TYPE_UNKNOWN     = 0;
    final static int TYPE_TEMPERATURE = 1;
    final static int TYPE_PRESSURE    = 2;
    final static int TYPE_HUMIDITY    = 3;
    private String location;
    private String name;
    private String value;
    private Float distance;
    public Long time;
    public boolean my;
    public boolean pub;


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

    public Float getDistance() {
        return distance;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getLocation() {
        return location;
    }

    public int getType() {
        return type;
    }

    public int getId() {
        return id;
    }
}
