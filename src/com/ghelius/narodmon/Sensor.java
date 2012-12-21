package com.ghelius.narodmon;

/**
 * Created with IntelliJ IDEA.
 * User: eugene
 * Date: 12/20/12
 * Time: 11:49 AM
 * To change this template use File | Settings | File Templates.
 */
public class Sensor {
    private int id;
    private int type;
    private String location;
    private String name;
    private String value;
    private String distance;
    private Long time;
    private boolean my;
    private boolean pub;
    // constructor
    Sensor (int id,
            int type,
            String location,
            String name,
            String value,
            String distance,
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

    public String getDistance() {
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
}
