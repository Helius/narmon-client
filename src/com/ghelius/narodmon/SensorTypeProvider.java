package com.ghelius.narodmon;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class SensorTypeProvider {
    private final static String TAG = "narodmon-typeProvider";
    private static SensorTypeProvider instance = null;
    private ArrayList<SensorType> typesList;
    Context context;


	private SensorTypeProvider (Context context) {
		DatabaseHandler dbh = new DatabaseHandler(context);
		typesList = dbh.getAllTypes();
	}

    static public SensorTypeProvider getInstance (Context context) {
        if (instance == null) {
	        Log.d(TAG,"sensorTypeProvider created");
            instance = new SensorTypeProvider(context);
        }
	    instance.context = context;
        return instance;
    }

    private ArrayList<SensorType> parseString (String res) {

	    Log.d(TAG,"try to parse dict str: [" + res + "]");
	    if (res != null) {
		    try {
			    JSONObject jsonObject = new JSONObject(res);
			    JSONArray types = jsonObject.getJSONArray("types");
			    typesList.clear();
			    for (int i = 0; i < types.length(); i++) {
				    int type = Integer.valueOf(types.getJSONObject(i).getString("type"));
				    String name = types.getJSONObject(i).getString("name");
				    String unit = types.getJSONObject(i).getString("unit");
				    typesList.add(new SensorType(type, name, unit));
			    }
		    } catch (JSONException e) {
			    Log.e(TAG, "wrong json");
		    }
	    }
        return typesList;
    }

    public void setTypesFromString (String res) {
	    Log.d(TAG,"updateFilter types");
        DatabaseHandler dbh = new DatabaseHandler(context);
	    parseString(res);
	    for (SensorType t : typesList) {
		    dbh.updateType(t);
	    }
	    WakefulIntentService.sendWakefulWork(context, WatchService.class);
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
            case 10:
                return context.getResources().getDrawable(R.drawable.switch_icon);
            case 11:
                return context.getResources().getDrawable(R.drawable.sun_icon);
            case 12:
                return context.getResources().getDrawable(R.drawable.radiation_icon);
            case 13:
                return context.getResources().getDrawable(R.drawable.voltage_icon);
		}
		return context.getResources().getDrawable(R.drawable.unknown_icon);
	}
}
