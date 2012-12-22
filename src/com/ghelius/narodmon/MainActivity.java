package com.ghelius.narodmon;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends Activity {

    private final String TAG = "narodmon";
    private ArrayList<Sensor> sensorList;
    private  SensorItemAdapter adapter = null;


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ListView listView = (ListView)findViewById(R.id.listView);
        sensorList = new ArrayList<Sensor>();

        WifiManager wifiMan = (WifiManager) this.getSystemService(
                Context.WIFI_SERVICE);
        WifiInfo wifiInf = wifiMan.getConnectionInfo();
        String uid = md5(wifiInf.getMacAddress());

        adapter = new SensorItemAdapter(getApplicationContext(), sensorList);
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                sensorItemClick (position);
            }
        });

        ImageButton btFavour = (ImageButton) findViewById(R.id.imageButton2);
        btFavour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFavourites ();
            }
        });
        new SensorListUpdater().execute("http://narodmon.ru/client.php?json={\"cmd\":\"sensorList\",\"uuid\":\"" + uid + "\"}");
    }

    private void showFavourites ()
    {
        // get file with faworites id
        // get data
        // make filtering
        // change buttons name
    }

    private String md5(String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i=0; i<messageDigest.length; i++)
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public class CustomComparator implements Comparator<Sensor> {
        @Override
        public int compare(Sensor o1, Sensor o2) {
            return o1.getDistance().compareTo(o2.getDistance());
        }
    }

    class SensorListUpdater extends AsyncTask<String, String, String> {

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
                Log.d("narodmon",uri[0]);
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
                        Log.d(TAG, + i + ": " + location);
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

                    adapter.notifyDataSetChanged();
                    Toast toast = Toast.makeText(getApplicationContext(), sensorList.size() + " sensors online", Toast.LENGTH_SHORT);
                    toast.show();
                    setTitle(sensorList.size() + " sensors online");

                } catch (JSONException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }
    }

    private void sensorItemClick (int position)
    {
        Intent i = new Intent (this, SensorInfo.class);
        i.putExtra("Sensor", sensorList.get(position));
        startActivity(i);
    }

}

// request
// http://narodmon.ru/client.php?json={"cmd":"sensorinfo","uuid":12345,"sensor":[115,125]}
// answer
// {"sensors":[{"id":115,"value":-7.75,"time":"1356060145"},{"id":125,"value":-16.75,"time":"1356059853"}]}


