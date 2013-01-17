package com.ghelius.narodmon;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
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
import java.util.*;

public class MainActivity extends Activity implements View.OnTouchListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private final String TAG = "narodmon";
    private ListUpdater listUpdater;
    private ArrayList<Sensor> sensorList = null;
    private SensorItemAdapter listAdapter = null;
    private SensorItemAdapter watchAdapter = null;
    private ImageButton btFavour = null;
    private ImageButton btList = null;
    private ListView fullListView = null;
    private ListView watchedListView = null;
    private String uid;
    private ViewFlipper flipper;
    private float fromPosition;
    private Loginer loginer;
    private Timer updateTimer = null;

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(TAG,"onSharedPreferenceChanged " + key);
        if (key.equals(getString(R.string.pref_key_interval))) { // update interval changed
            scheduleAlarmWatcher();
            startTimer();
        } else if (key.equals(getString(R.string.pref_key_login)) || key.equals(getString(R.string.pref_key_passwd))) {
            loginer.login(sharedPreferences.getString(getString(R.string.pref_key_login),""),
                          sharedPreferences.getString(getString(R.string.pref_key_passwd),""));
        }
    }

    /*
    * Class for get full sensor list from server, parse it and put to sensorList and update listAdapter
    * */
    private class ListUpdater implements ServerDataGetter.OnResultListener {
        void updateList () {
            findViewById(R.id.marker_progress).setVisibility(View.VISIBLE);
            ServerDataGetter getter = new ServerDataGetter ();
            getter.setOnListChangeListener(this);
            getter.execute("http://narodmon.ru/client.php?json={\"cmd\":\"sensorList\",\"uuid\":\"" + uid + "\"}");
        }
        @Override
        public void onResultReceived(String result) {
            //Log.d(TAG,"result: " + result);
            try {
                makeSensorListFromJson(result);
                //todo: probably we could place gui updating in MainActivity class
                listAdapter.addAll(sensorList);
                listAdapter.notifyDataSetChanged();
                watchAdapter.addAll(sensorList);
                watchAdapter.notifyDataSetChanged();
                watchAdapter.getFilter().filter("watch");
                switchList();
            } catch (JSONException e) {
                Toast.makeText(getApplicationContext(), "Wrong server respond, try later", Toast.LENGTH_SHORT).show();
            }
            findViewById(R.id.marker_progress).setVisibility(View.GONE);
        }
        @Override
        public void onNoResult() {
            Toast.makeText(getApplicationContext(), "Server not responds", Toast.LENGTH_SHORT).show();
            findViewById(R.id.marker_progress).setVisibility(View.GONE);
        }
    }


    /*
    * Class for login procedure
    * */
    private class Loginer implements ServerDataGetter.OnResultListener {
        ServerDataGetter getter;
        void login (String userLogin, String passwd)
        {
            if (userLogin.equals("")) {// don't try if login is empty
                Log.d(TAG,"Loginer: login is empty, don't try");
                return;
            }
            getter = new ServerDataGetter();
            getter.setOnListChangeListener(this);
            getter.execute("http://narodmon.ru/client.php?json={\"cmd\":\"login\",\"uuid\":\"" + uid + "\",\"login\":\"" + userLogin +"\",\"hash\":\"" + md5(uid+md5(passwd)) +"\"}");
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
        stopTimer();
        super.onPause();
    }

    @Override
    public void onResume () {
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        startTimer();
        listUpdater.updateList();
        super.onResume();
    }

    @Override
    public void onDestroy () {
        stopTimer();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
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
        String passwd = prefs.getString(String.valueOf(getText(R.string.pref_key_passwd)),"");
        Log.d(TAG,"my password is: " + passwd);
        loginer = new Loginer();
        loginer.login(userLogin, passwd);



        listAdapter = new SensorItemAdapter(getApplicationContext(), sensorList);
        fullListView.setAdapter(listAdapter);
        fullListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        fullListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                sensorItemClick(position);
            }
        });

        watchAdapter = new SensorItemAdapter(getApplicationContext(), sensorList);
        watchedListView.setAdapter(watchAdapter);
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
        scheduleAlarmWatcher();
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
        listAdapter.getFilter().filter("watch");
        btFavour.setImageResource(R.drawable.yey_blue);
        btList.setImageResource(R.drawable.list_gray);
        setTitle(fullListView.getCount() + " watched sensors");
    }

    private void switchList()
    {
        Log.d(TAG,"switch to list " + sensorList.size());
        listAdapter.getFilter().filter("");
        listAdapter.notifyDataSetChanged();
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

    final Handler h = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Log.d(TAG," ---- updateTimer fired! ----");
            listUpdater.updateList();
            return false;
        }
    });
    void startTimer () {
        stopTimer();
        updateTimer = new Timer("updateTimer",true);
        updateTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                h.sendEmptyMessage(0);
            }
        }, 30000, 60000*Integer.valueOf(PreferenceManager.
                getDefaultSharedPreferences(this).
                getString(getString(R.string.pref_key_interval),"5")));
    }
    void stopTimer () {
        if (updateTimer != null) {
            updateTimer.cancel();
            updateTimer.purge();
            updateTimer = null;
        }
    }

    void scheduleAlarmWatcher () {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(this, OnAlarmReceiver.class);
        PendingIntent pi=PendingIntent.getBroadcast(this, 0, i, 0);
        try {
            am.cancel(pi);
        } catch (Exception e) {
            Log.e(TAG,"cancel pending intent of AlarmManager failed");
            e.getMessage();
        }

        Log.d(TAG,"Alarm watcher new updateInterval " + Integer.valueOf(PreferenceManager.
                        getDefaultSharedPreferences(this).
                        getString(getString(R.string.pref_key_interval),"5")));

        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + (1 * 60000), // 1 minute
                (60000 * Integer.valueOf(PreferenceManager.
                        getDefaultSharedPreferences(this).
                        getString(getString(R.string.pref_key_interval),"5"))),
                pi);
    }

}

