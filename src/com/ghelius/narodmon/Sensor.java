package com.ghelius.narodmon;

import java.io.Serializable;


public class Sensor implements Serializable {
    private int id;
    private int type;
    private String location;
    private String name;
    private String value;
    private Float distance;
    private Long time;
    private boolean my;
    private boolean pub;
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

    public void setValue (String val) {
        value = val;
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

    public boolean getMy() {
        return this.my;
    }

    public Long getTime() {
        return time;
    }
}
