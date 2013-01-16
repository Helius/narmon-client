package com.ghelius.narodmon;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.*;
import android.view.animation.AnimationUtils;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends Activity implements View.OnTouchListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private final String TAG = "narodmon";
    private ListUpdater listUpdater;
    private ArrayList<Sensor> sensorList = null;
    private  SensorItemAdapter adapter = null;
    private ImageButton btFavour = null;
    private ImageButton btList = null;
    private ListView fullListView = null;
    private ListView watchedListView = null;
    private String uid;
    private ViewFlipper flipper;
    private float fromPosition;

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(TAG,"onSharedPreferenceChanged " + key);
    }

    /*
    * Class for get full sensor list from server, parse it and put to sensorList and update adapter
    * */
    private class ListUpdater implements ServerDataGetter.OnResultListener {
        void updateList () {
            ServerDataGetter getter = new ServerDataGetter ();
            getter.setOnListChangeListener(this);
            getter.execute("http://narodmon.ru/client.php?json={\"cmd\":\"sensorList\",\"uuid\":\"" + uid + "\"}");
        }
        @Override
        public void onResultReceived(String result) {
            Log.d(TAG,"result: " + result);
            try {
                makeSensorListFromJson(result);
                //todo: probably we could place gui updating in MainActivity class
                adapter.addAll(sensorList);
                adapter.notifyDataSetChanged();
                //Toast.makeText(getApplicationContext(), sensorList.size() + " sensors online", Toast.LENGTH_SHORT).show();
                //todo switchList of showWatched depend of last user choise, if we are started from notification - show watched
                switchList();
            } catch (JSONException e) {
                Toast.makeText(getApplicationContext(), "Wrong server respond, try later", Toast.LENGTH_SHORT).show();
            }
        }
        @Override
        public void onNoResult() {
            Toast.makeText(getApplicationContext(), "Server not responds", Toast.LENGTH_SHORT).show();
        }
    }

    /*
    * Class for login procedure
    * */
    private class Loginer implements ServerDataGetter.OnResultListener {
        ServerDataGetter getter;
        void login (String userLogin, String userHash)
        {
            getter = new ServerDataGetter();
            getter.setOnListChangeListener(this);
            getter.execute("http://narodmon.ru/client.php?json={\"cmd\":\"login\",\"uuid\":\"" + uid + "\",\"login\":\"" + userLogin +"\",\"hash\":\"" + userHash +"\"}");
        }
        @Override
        public void onResultReceived(String result) {
            //{"error":"auth error"}
            Log.d(TAG,"Login result: " + result);
            try {
                JSONObject jObject = new JSONObject(result);
                String error = jObject.getString("error");
                if ((error != null) && (!error.equals("")) && error.equals("auth error")) {
                    // do something
                }
                Toast.makeText(getApplicationContext(), "Login: " + result, Toast.LENGTH_SHORT).show();
            } catch (JSONException e) {
                Toast.makeText(getApplicationContext(), "Login failed (wrong answer)", Toast.LENGTH_SHORT).show();
            }
            listUpdater.updateList();
        }
        @Override
        public void onNoResult() {
            Toast.makeText(getApplicationContext(), "Server not responds", Toast.LENGTH_SHORT).show();
        }
    }


    public boolean onTouch(View view, MotionEvent event)
    {
        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                fromPosition = event.getX();
                break;
            case MotionEvent.ACTION_UP:
                float toPosition = event.getX();
                if (fromPosition > toPosition)
                {
                    flipper.setInAnimation(AnimationUtils.loadAnimation(this,R.anim.go_next_in));
                    flipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.go_next_out));
                    flipper.showNext();
                }
                else if (fromPosition < toPosition)
                {
                    flipper.setInAnimation(AnimationUtils.loadAnimation(this,R.anim.go_prev_in));
                    flipper.setOutAnimation(AnimationUtils.loadAnimation(this,R.anim.go_prev_out));
                    flipper.showPrevious();
                }
            default:
                break;
        }
        return true;
    }

    @Override
    public void onPause ()
    {
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
        // todo: stop update timer
        super.onPause();
    }

    @Override
    public void onResume ()
    {
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        // todo: setup update timer for get full list
        listUpdater.updateList();
        super.onResume();
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        flipper = (ViewFlipper) findViewById( R.id.viewFlipper);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        int layouts[] = new int[]{ R.layout.full_list_view, R.layout.watched_list_view};
        for (int layout : layouts)
            flipper.addView(inflater.inflate(layout, null));

        fullListView = (ListView)findViewById(R.id.fullListView);
        watchedListView = (ListView)findViewById(R.id.watchedListView);
        sensorList = new ArrayList<Sensor>();


		// get android UUID
        final ConfigHolder config = ConfigHolder.getInstance(getApplicationContext());
        if ((config.getUid() == null) || (config.getUid().length() < 2)) {
            config.setUid (md5(Settings.Secure.getString(getBaseContext().getContentResolver(),
                    Settings.Secure.ANDROID_ID)));
        }
        uid = config.getUid();
        Log.d(TAG,"my id is: " + uid);

		//get location
		LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
  		Criteria criteria = new Criteria();
  		criteria.setAccuracy(Criteria.ACCURACY_FINE);
  		String provider = lm.getBestProvider(criteria, true);
  		Location mostRecentLocation = lm.getLastKnownLocation(provider);
  		if(mostRecentLocation != null){
  			double latid=mostRecentLocation.getLatitude();
  			double longid=mostRecentLocation.getLongitude();
			// use API to send location
            Log.d(TAG,"my location: " + latid +" "+longid);
  		}

        Log.d(TAG,"START LOGIN");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String userLogin = prefs.getString(String.valueOf(getText(R.string.pref_key_login)), "");
        Log.d(TAG,"my login is: " + userLogin);
        if ((userLogin != null) && (!userLogin.equals(""))) {
            String passwd = prefs.getString(String.valueOf(getText(R.string.pref_key_passwd)),"");
            Log.d(TAG,"my password is: " + passwd);
            Loginer loginer = new Loginer();
            loginer.login("ghelius@gmail.com", md5(uid+md5(passwd)));
//            if (!loginer.waitLogin()) {
//                Log.e(TAG,"Error while waiting login");
//            }
        } else {
            Log.w(TAG,"no login");
        }
        Log.d(TAG,"LOGIN DONE");



        adapter = new SensorItemAdapter(getApplicationContext(), sensorList);
        fullListView.setAdapter(adapter);
        fullListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        fullListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                sensorItemClick(position);
            }
        });

        watchedListView.setAdapter(adapter);
        watchedListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        watchedListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                sensorItemClick(position);
            }
        });

        btFavour = (ImageButton) findViewById(R.id.imageButton2);
        btFavour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //switchFavourites();
                flipper.setInAnimation(AnimationUtils.loadAnimation(MainActivity.this,R.anim.go_next_in));
                flipper.setOutAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.anim.go_next_out));
                flipper.showNext();
            }
        });
        btList = (ImageButton) findViewById(R.id.imageButton1);
        btList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //switchList();
                flipper.setInAnimation(AnimationUtils.loadAnimation(MainActivity.this,R.anim.go_prev_in));
                flipper.setOutAnimation(AnimationUtils.loadAnimation(MainActivity.this,R.anim.go_prev_out));
                flipper.showPrevious();
            }
        });

        listUpdater = new ListUpdater();

        ImageButton btRefresh = (ImageButton) findViewById(R.id.imageButton);
        btRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listUpdater.updateList();
            }
        });

        Intent i = new Intent(this, OnBootReceiver.class);
        sendBroadcast(i);
    }

    class CustomComparator implements Comparator<Sensor> {
        @Override
        public int compare(Sensor o1, Sensor o2) {
            return o1.getDistance().compareTo(o2.getDistance());
        }
    }
    void makeSensorListFromJson (String result) throws JSONException {
        if (result != null) {
            sensorList.clear();
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
        }
    }

    private void switchFavourites()
    {
        Log.d(TAG, "switch to watched");
        adapter.getFilter().filter("watch");
        btFavour.setImageResource(R.drawable.yey_blue);
        btList.setImageResource(R.drawable.list_gray);
        setTitle(fullListView.getCount() + " watched sensors");
    }

    private void switchList()
    {
        Log.d(TAG,"switch to list " + sensorList.size());
        adapter.getFilter().filter("");
        adapter.notifyDataSetChanged();
        btFavour.setImageResource(R.drawable.yey_gray);
        btList.setImageResource(R.drawable.list_blue);
        setTitle(sensorList.size() + " sensors online");
    }

    private String md5(String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (int i=0; i<messageDigest.length; i++)
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    private void sensorItemClick (int position)
    {
        Intent i = new Intent (this, SensorInfo.class);
        i.putExtra("Sensor", sensorList.get(position));
        startActivity(i);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem mi = menu.add(0, 1, 0, "Preferences");
        mi.setIntent(new Intent(this, PreferActivity.class));
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.preference:

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

