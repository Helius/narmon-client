package com.ghelius.narodmon;

import android.content.Context;
import android.util.Log;

import java.io.*;
import java.util.ArrayList;

public class UiFlags implements Serializable {
    final static private String TAG = "narodmon-filter";
    final static private String fileName = "filter";

	// class fields to save
    boolean showingMyOnly;
    SortType sortType;
	ArrayList<Integer> hidenTypes = new ArrayList<Integer>();
    UiMode uiMode;
    int radiusKm;
    enum UiMode {list, watched}
	enum SortType {name, distance, type, time}
	//----------------------

    UiFlags() {
        showingMyOnly = false;
        sortType = SortType.distance;
        uiMode = UiMode.list;
        radiusKm=20000;
    }

    public static UiFlags load (Context context) {
        UiFlags uiFlags;
        try {
            FileInputStream fis = context.openFileInput(fileName);
            ObjectInputStream is = new ObjectInputStream(fis);
            uiFlags = (UiFlags) is.readObject();
            is.close();
            fis.close();
            Log.d(TAG,"uiFlags.radiusKm: " + uiFlags.radiusKm);
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
