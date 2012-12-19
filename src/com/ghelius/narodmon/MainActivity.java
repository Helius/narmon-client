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
import java.util.HashMap;

public class MainActivity extends Activity {

    private ArrayList<HashMap<String, Object>> listItem;
    private static final String NAME =     "name";
    private static final String LOCATION = "location";
    private static final String VALUE =    "value";
    private static final String DISTANCE = "distance";
    private static final String IMGKEY =   "iconfromraw";
    private final String TAG = "narodmon";
    private SimpleAdapter adapter = null;

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
                        String distans = devicesArray.getJSONObject(i).getString("distance");
                        Log.d("narodmon", + i + ": " + location);
                        JSONArray sensorsArray = devicesArray.getJSONObject(i).getJSONArray("sensors");
                        for (int j = 0; j < sensorsArray.length(); j++) {
                            String values =sensorsArray.getJSONObject(j).getString("value");
                            String name =sensorsArray.getJSONObject(j).getString("name");
                            String type =sensorsArray.getJSONObject(j).getString("type");
                            HashMap<String, Object> hm = new HashMap<String, Object>();
                            hm.put(VALUE, values);
                            hm.put(LOCATION, location);
                            hm.put(NAME,name);
                            hm.put(DISTANCE,distans);
                            if (type.equals("1")) {
                                hm.put(IMGKEY, android.R.drawable.ic_menu_compass);
                            } else if (type.equals("2")) {
                                hm.put(IMGKEY, android.R.drawable.ic_menu_camera);
                            } else if (type.equals("3")) {
                                hm.put(IMGKEY, android.R.drawable.ic_menu_gallery);
                            } else {
                                hm.put(IMGKEY, android.R.drawable.ic_menu_mylocation);
                            }
                            listItem.add(hm);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    Toast toast = Toast.makeText(getApplicationContext(), listItem.size() + " sensors online", Toast.LENGTH_SHORT);
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
        listItem = new ArrayList<HashMap<String,Object>>();


        WifiManager wifiMan = (WifiManager) this.getSystemService(
                Context.WIFI_SERVICE);
        WifiInfo wifiInf = wifiMan.getConnectionInfo();
        String uid = md5(wifiInf.getMacAddress());

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
        new SensorListUpdater().execute("http://narodmon.ru/client.php?json={\"cmd\":\"sensorList\",\"uuid\":\"" + uid + "\"}");
    }
}



