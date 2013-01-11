package com.ghelius.narodmon;


import android.os.AsyncTask;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


class CustomComparator implements Comparator<Sensor> {
    @Override
    public int compare(Sensor o1, Sensor o2) {
        return o1.getDistance().compareTo(o2.getDistance());
    }
}


class FullSensorListUpdater extends AsyncTask<String, String, String> {

    interface OnListChangeListener {
       void onListChange ();
    }

    OnListChangeListener listener;

    void setOnListChangeListener (OnListChangeListener l) {
        listener = l;
    }

    private ArrayList<Sensor> sensorList = new ArrayList<Sensor>();

    private String inputStreamToString(InputStream is) {
        String s = "";
        String line = "";
        // Wrap a BufferedReader around the InputStream
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        // Read response until the end
        try {
            while ((line = rd.readLine()) != null) { s += line; }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return s;
    }
    @Override
    protected void onPreExecute () {
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(String... uri) {
        String responseString = null;
        URL url = null;
        HttpURLConnection urlConnection = null;
        try {
            Log.d("narodmon", uri[0]);
            url = new URL(uri[0]);
            urlConnection = (HttpURLConnection) url.openConnection();
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            responseString = inputStreamToString(in);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            urlConnection.disconnect();
        }
        return responseString;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if (result != null) {
            sensorList.clear();
            try {
                JSONObject jObject = new JSONObject(result);
                JSONArray devicesArray = jObject.getJSONArray("devices");
                for (int i = 0; i < devicesArray.length(); i++) {
                    String location = devicesArray.getJSONObject(i).getString("location");
                    float distance = Float.parseFloat(devicesArray.getJSONObject(i).getString("distance"));
                    boolean my      = (devicesArray.getJSONObject(i).getInt("my") != 0);
                    //Log.d(TAG, + i + ": " + location);
                    JSONArray sensorsArray = devicesArray.getJSONObject(i).getJSONArray("sensors");
                    for (int j = 0; j < sensorsArray.length(); j++) {
                        String values = sensorsArray.getJSONObject(j).getString("value");
                        String name   = sensorsArray.getJSONObject(j).getString("name");
                        int type      = sensorsArray.getJSONObject(j).getInt("type");
                        int id        = sensorsArray.getJSONObject(j).getInt("id");
                        boolean pub   = (sensorsArray.getJSONObject(j).getInt("pub") != 0);
                        long times    = sensorsArray.getJSONObject(j).getLong("time");
                        sensorList.add(new Sensor(id, type, location, name, values, distance, my, pub, times));
                    }
                }
                // sort by distance
                Collections.sort(sensorList, new CustomComparator());
                listener.onListChange();
            } catch (JSONException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    ArrayList<Sensor> getSensorList () {
        return sensorList;
    }
}
