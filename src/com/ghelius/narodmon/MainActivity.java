package com.ghelius.narodmon;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;
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

public class MainActivity extends Activity {

    private final String TAG = "narodmon";
    private ArrayList<Sensor> sensorList;
    private  SensorItemAdapter adapter = null;

    public String md5(String s) {
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
                        String distans = devicesArray.getJSONObject(i).getString("distance");
                        Log.d(TAG, + i + ": " + location);
                        JSONArray sensorsArray = devicesArray.getJSONObject(i).getJSONArray("sensors");
                        for (int j = 0; j < sensorsArray.length(); j++) {
                            String values =sensorsArray.getJSONObject(j).getString("value");
                            String name =sensorsArray.getJSONObject(j).getString("name");
                            int type =sensorsArray.getJSONObject(j).getInt("type");
                            int id =sensorsArray.getJSONObject(j).getInt("id");
                            sensorList.add(new Sensor(id, type, location, name, values, distans));
                        }
                    }
                    adapter.notifyDataSetChanged();
                    Toast toast = Toast.makeText(getApplicationContext(), sensorList.size() + " sensors online", Toast.LENGTH_SHORT);
                    toast.show();

                } catch (JSONException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }

            }
        }
    }


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
        new SensorListUpdater().execute("http://narodmon.ru/client.php?json={\"cmd\":\"sensorList\",\"uuid\":\"" + uid + "\"}");
    }
}



