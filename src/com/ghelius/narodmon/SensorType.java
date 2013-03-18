package com.ghelius.narodmon;


import java.io.Serializable;

public class SensorType implements Serializable {
    SensorType (int code, String name, String unit) {
        this.code = code;
        this.name = name;
        this.unit = unit;
    }
    int code;
    String name;
    String unit;
}
