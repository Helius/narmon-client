package com.ghelius.narodmon;

import android.content.Context;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;


public class SensorTypeProvider {
    private final static String TAG = "narodmon-typeProvider";
    private static SensorTypeProvider instance = null;
    private ArrayList<SensorType> typesList;
    private final static String filename = "sensor_types.inf";
    Context context;


    private SensorTypeProvider (Context context) {
        typesList = new ArrayList<SensorType>();
        FileInputStream fis;
        try {
            fis = context.openFileInput(filename);
            InputStreamReader inputreader = new InputStreamReader(fis);
            BufferedReader buffreader = new BufferedReader(inputreader);
	        Log.d(TAG, "get saved types");
            parseString (buffreader.readLine());
            fis.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

    }

    static public SensorTypeProvider getInstance (Context context) {
        if (instance == null) {
            instance = new SensorTypeProvider(context);
            instance.context = context;
        }
        return instance;
    }

    private boolean parseString (String res) {
        try {
            JSONObject jsonObject = new JSONObject(res);
            JSONArray types = jsonObject.getJSONArray("types");
            typesList.clear();
            for (int i = 0; i < types.length(); i++) {
                int type = Integer.valueOf(types.getJSONObject(i).getString("type"));
                String name = types.getJSONObject(i).getString("name");
                String unit = types.getJSONObject(i).getString("unit");
                Log.d(TAG, "add type: " + type + ", " + name + ", " + unit);
                typesList.add(new SensorType(type, name, unit));
            }
            return true;
        } catch (JSONException e) {
            Log.e(TAG, "wrong json");
        }
        return false;
    }

    public void setTypesFromString (String res) {
	    Log.d(TAG,"update types");
        if (parseString(res)) {
            try {
                FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
                fos.write(res.getBytes());
                fos.close();
            } catch (FileNotFoundException e) {
                Log.e(TAG,e.getMessage());
            } catch (IOException e) {
                Log.e(TAG,e.getMessage());
            }
        }
    }

    public String getNameForType (int type) {
        for (SensorType t : typesList) {
            if (t.code == type) {
                return t.name;
            }
        }
        Log.d(TAG,"unknown type: " + type);
        return "unknown";
    }

    public String getUnitForType (int type) {
        for (SensorType t : typesList) {
            if (t.code == type) {
                return t.unit;
            }
        }
        Log.d(TAG,"unknown type: " + type);
        return "?";
    }

	public ArrayList<SensorType> getTypesList ()  {
		return typesList;
	}
}
