package com.ghelius.narodmon;

public class FilterFlags {
    final public static int type_unknown = 0;
    final public static int type_temperature = 1;
    final public static int type_pressure = 2;
    final public static int type_humidity = 3;
    boolean showingMyOnly;
    boolean sortingDistance;
    boolean types [] = new boolean[4];
    FilterFlags () {
        showingMyOnly = false;
        sortingDistance = true;
        types[type_unknown] = true;
        types[type_temperature] = true;
        types[type_pressure] = true;
        types[type_humidity] = true;
    }
}
