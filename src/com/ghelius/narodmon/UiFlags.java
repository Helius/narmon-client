package com.ghelius.narodmon;

import android.content.Context;
import android.util.Log;

import java.io.*;

public class UiFlags implements Serializable {
    final static private String TAG = "narodmon-filter";
    final static private String fileName = "filter";

    final public static int type_unknown = 0;
    final public static int type_temperature = 1;
    final public static int type_pressure = 2;
    final public static int type_humidity = 3;
    boolean showingMyOnly;
    boolean sortingDistance;
    boolean types [] = new boolean[4];
    UiMode uiMode;

    enum UiMode {list, watched}

    UiFlags() {
        showingMyOnly = false;
        sortingDistance = true;
        types[type_unknown] = true;
        types[type_temperature] = true;
        types[type_pressure] = true;
        types[type_humidity] = true;
        uiMode = UiMode.list;
    }

    public static UiFlags load (Context context) {
        UiFlags uiFlags;
        try {
            FileInputStream fis = context.openFileInput(fileName);
            ObjectInputStream is = new ObjectInputStream(fis);
            uiFlags = (UiFlags) is.readObject();
            is.close();
            fis.close();
        } catch (Exception e) {
            e.getMessage();
            // file was not found, class not found, etc, first start?
            Log.w(TAG, "can't open object file, create new filter");
            uiFlags = new UiFlags();
        }
        return uiFlags;
    }

    public void save (Context context) {
        try {
            FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            ObjectOutputStream os;
            os = new ObjectOutputStream(fos);
            os.writeObject(this);
            os.close();
            fos.close();
        } catch (IOException e) {
            e.getMessage();
            Log.e(TAG,"can't save file");
        }
    }
}
