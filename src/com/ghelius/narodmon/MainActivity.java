package com.ghelius.narodmon;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends Activity {

    private ArrayList<HashMap<String, Object>> listItem;
    private static final String NAME = "sname";
    private static final String LOCATION = "slocation";
    private static final String VALUE = "svalue";
    private static final String IMGKEY = "iconfromraw";
    private final String TAG = "narodmon";
    private SimpleAdapter adapter = null;


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

            // Return full string
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
                listItem.clear();
                try {
                    JSONObject jObject = new JSONObject(result);
                    JSONArray devicesArray = jObject.getJSONArray("devices");
                    for (int i = 0; i < devicesArray.length(); i++) {
                        String location = devicesArray.getJSONObject(i).getString("location");
                        Log.d("narodmon", + i + ": " + location);
                        JSONArray sensorsArray = devicesArray.getJSONObject(i).getJSONArray("sensors");
                        for (int j = 0; j < sensorsArray.length(); j++) {
                            String values =sensorsArray.getJSONObject(j).getString("value");
                            String name =sensorsArray.getJSONObject(j).getString("name");
                            //Log.d("narodmon","    " + values);
                            HashMap<String, Object> hm = new HashMap<String, Object>();
                            hm.put(VALUE, values);
                            hm.put(LOCATION, location);
                            hm.put(NAME,name);
                            hm.put(IMGKEY, R.drawable.ic_launcher);
                            listItem.add(hm);
                        }
                    }
                    adapter.notifyDataSetChanged();

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
        listItem = new ArrayList<HashMap<String,Object>>();
        HashMap<String, Object> hm;

        WifiManager wifiMan = (WifiManager) this.getSystemService(
                Context.WIFI_SERVICE);
        WifiInfo wifiInf = wifiMan.getConnectionInfo();
        String macAddr = wifiInf.getMacAddress();

        adapter = new SimpleAdapter(this,
                listItem,
                R.layout.list, new String[]{
                NAME,
                LOCATION,
                VALUE,
                IMGKEY,
        }, new int[]{
                R.id.text1,
                R.id.text2,
                R.id.text3,
                R.id.img});

        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        new SensorListUpdater().execute("http://narodmon.ru/client.php?json={\"cmd\":\"sensorList\",\"uuid\":\"" + macAddr + "\"}");
    }
}



