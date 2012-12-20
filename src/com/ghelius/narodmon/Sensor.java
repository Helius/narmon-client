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
    // constructor
    Sensor (int id_, int type_, String location_, String name_, String value_, String distance_)
    {
        id = id_;
        type = type_;
        location = location_;
        name = name_;
        value = value_;
        distance = distance_;
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
}
