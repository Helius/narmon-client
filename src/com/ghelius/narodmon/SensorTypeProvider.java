package com.ghelius.narodmon;

import android.content.Context;
import android.graphics.drawable.Drawable;
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
//                Log.d(TAG, "add type: " + type + ", " + name + ", " + unit);
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
        parseString(res);

	    try {
		    FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
		    OutputStreamWriter writer = new OutputStreamWriter(fos);
		    BufferedWriter bufferedWriter = new BufferedWriter(writer);
		    try {
			    bufferedWriter.write(res);
			    bufferedWriter.flush();
			    bufferedWriter.close();
		    } catch (IOException e) {
			    Log.e(TAG,"Something wrong while write types dict");
		    }

	    } catch (FileNotFoundException e) {
		    Log.e(TAG,"Can't open config file");
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

	public Drawable getIcon(int code) {
		switch (code) {
			case 0:
				return context.getResources().getDrawable(R.drawable.unknown_icon);
			case 1:
				return context.getResources().getDrawable(R.drawable.termo_icon);
			case 2:
				return context.getResources().getDrawable(R.drawable.humid_icon);
			case 3:
				return context.getResources().getDrawable(R.drawable.pressure_icon);
			case 4:
				return context.getResources().getDrawable(R.drawable.wind_icon);
			case 5:
				return context.getResources().getDrawable(R.drawable.compas_icon);
			case 6:
				return context.getResources().getDrawable(R.drawable.lamp_icon);
			case 7:
				return context.getResources().getDrawable(R.drawable.storage_icon);
			case 8:
				return context.getResources().getDrawable(R.drawable.ethernet_icon);
			case 9:
				return context.getResources().getDrawable(R.drawable.rain_icon);
		}
		return context.getResources().getDrawable(R.drawable.unknown_icon);
	}
}
