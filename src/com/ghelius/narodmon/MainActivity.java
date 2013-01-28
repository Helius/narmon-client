package com.ghelius.narodmon;

import android.app.*;
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

public class MainActivity extends Activity implements View.OnTouchListener, SharedPreferences.OnSharedPreferenceChangeListener, ActionBar.TabListener {

    private static final String AppApiVersion = "Av1.1a";
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
    private ImageButton btFiltering;

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(TAG,"onSharedPreferenceChanged " + key);
        if (key.equals(getString(R.string.pref_key_interval))) { // update interval changed
            scheduleAlarmWatcher();
            startTimer();
        } else if (key.equals(getString(R.string.pref_key_login)) || key.equals(getString(R.string.pref_key_passwd))) {
            loginer.login();
        }
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    /*
    * Class for get full sensor list from server, parse it and put to sensorList and update listAdapter
    * */
    private class ListUpdater implements ServerDataGetter.OnResultListener {
        ServerDataGetter getter;
        void updateList () {
            findViewById(R.id.marker_progress).setVisibility(View.VISIBLE);
            if (getter != null)
                getter.cancel(true);
            getter = new ServerDataGetter ();
            getter.setOnListChangeListener(this);
            getter.execute("http://narodmon.ru/client.php?json={\"cmd\":\"sensorList\",\"uuid\":\"" + uid + "\"}");
            setTitle("Connecting...");
        }
        @Override
        public void onResultReceived(String result) {
            Log.d(TAG,"result resived " + result);
            getter = null;
            try {
                makeSensorListFromJson(result);
                //todo: probably we could place gui updating in MainActivity class
                listAdapter.addAll(sensorList);
                listAdapter.notifyDataSetChanged();
                watchAdapter.addAll(sensorList);
                watchAdapter.notifyDataSetChanged();
                watchAdapter.getFilter().filter("watch");
                fullListView.setVisibility(View.VISIBLE);
                switchList();
            } catch (JSONException e) {
                Toast.makeText(getApplicationContext(), "Wrong server respond, try later", Toast.LENGTH_SHORT).show();
            }
            findViewById(R.id.marker_progress).setVisibility(View.INVISIBLE);
        }
        @Override
        public void onNoResult() {
            getter = null;
            Log.w(TAG,"Server not responds");
            Toast.makeText(getApplicationContext(), "Server not responds", Toast.LENGTH_SHORT).show();
            setTitle("Server not responds");
            fullListView.setVisibility(View.INVISIBLE);
            findViewById(R.id.marker_progress).setVisibility(View.INVISIBLE);
        }
    }


    /*
    * Class for login procedure
    * */
    private class Loginer implements ServerDataGetter.OnResultListener {
        ServerDataGetter getter;
        void login ()
        {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
            String userLogin = prefs.getString(String.valueOf(getText(R.string.pref_key_login)), "");
            String passwd = prefs.getString(String.valueOf(getText(R.string.pref_key_passwd)),"");
            Log.d(TAG,"my id is: " + uid + ", login: " + userLogin + ", passwd: " + passwd);
            if (userLogin.equals("")) {// don't try if login is empty
                Log.d(TAG,"Loginer: login is empty, don't try");
                return;
            }
            getter = new ServerDataGetter();
            getter.setOnListChangeListener(this);
            Log.d(TAG,"password: " + passwd + " md5: " + md5(passwd));
            getter.execute("http://narodmon.ru/client.php?json={\"cmd\":\"login\",\"uuid\":\"" + uid + "\",\"login\":\"" + userLogin +"\",\"hash\":\"" + md5(uid+md5(passwd)) +"\"}");
        }
        @Override
        public void onResultReceived(String result) {
            //{"error":"auth error"}
            //{"login":"mylogin"}
            Log.d(TAG,"Login result: " + result);
            try {
                JSONObject jObject = new JSONObject(result);
                String login = jObject.getString("login");
                Log.d(TAG,"Login result: " + login);
            } catch (JSONException e) {
                Toast.makeText(getApplicationContext(), "Login failed (wrong answer)", Toast.LENGTH_SHORT).show();
            }
            listUpdater.updateList();
        }
        @Override
        public void onNoResult() {
            Log.e(TAG,"Server not responds");
        }
    }

    /*
    * Class for location send procedure
    * */
    private class LocationSender implements ServerDataGetter.OnResultListener {
        ServerDataGetter getter;
        void sendLocation (Double l1, Double l2)
        {
            getter = new ServerDataGetter();
            getter.setOnListChangeListener(this);
            getter.execute("http://narodmon.ru/client.php?json={\"cmd\":\"location\",\"uuid\":\"" + uid + "\",\"addr\":\"" + Math.round(l1*1000000) +" "+ Math.round(l2*1000000) + "\"}");
        }
        @Override
        public void onResultReceived(String result) {
            Log.d(TAG, "Location result: " + result);
            listUpdater.updateList();
        }
        @Override
        public void onNoResult() {
            //Toast.makeText(getApplicationContext(), "Server not responds", Toast.LENGTH_SHORT).show();
        }
    }

    /*
    * Class for AppApiVersion send procedure
    * */
    private class VersionSender implements ServerDataGetter.OnResultListener {
        ServerDataGetter getter;
        void sendVersion ()
        {
            getter = new ServerDataGetter();
            getter.setOnListChangeListener(this);
            getter.execute("http://narodmon.ru/client.php?json={\"cmd\":\"version\",\"uuid\":\"" + uid + "\",\"version\":\"" + AppApiVersion + "\"}");
        }
        @Override
        public void onResultReceived(String result) {
            Log.d(TAG, "Version result: " + result);
            listUpdater.updateList();
        }
        @Override
        public void onNoResult() {
            Log.e(TAG,"No result while send AppApiVersion");
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
        Log.i(TAG,"onResume");
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        startTimer();
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

        btFiltering = (ImageButton) findViewById(R.id.imageButton3);
        btFiltering.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listAdapter.getFilter().filter("temperature");
            }
        });


        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(R.layout.actionbar_top); //load your layout
        actionBar.setListNavigationCallbacks(ArrayAdapter.createFromResource(this, R.array.action_list,
                android.R.layout.simple_spinner_dropdown_item),new ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int itemPosition, long itemId) {
                return false;
            }
        });



        VersionSender versionSender = new VersionSender();
        listUpdater = new ListUpdater();
        loginer = new Loginer();

        versionSender.sendVersion();
        loginer.login();
        listUpdater.updateList();
        //send location
        LocationSender locationSender = new LocationSender();
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
            locationSender.sendLocation(latid, longid);
        }

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

    public static final String md5(final String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest
                    .getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                String h = Integer.toHexString(0xFF & messageDigest[i]);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
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

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        MenuItem mi = menu.add(0, 1, 0, "Preferences");
//        mi.setIntent(new Intent(this, PreferActivity.class));
//        return super.onCreateOptionsMenu(menu);
//    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        MenuInflater menuInflater = getMenuInflater();
//        menuInflater.inflate(R.menu.icon_menu, menu);
//
//        return super.onCreateOptionsMenu(menu);
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle item selection
//        switch (item.getItemId()) {
////            case R.id.preference:
////
////                return true;
//            default:
//                return super.onOptionsItemSelected(item);
//        }
//    }

    final Handler h = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Log.d(TAG," ---- updateTimer fired! ----");
            listUpdater.updateList();
            return false;
        }
    });
    void startTimer () {
        Log.d(TAG,"start timer");
        stopTimer();
        updateTimer = new Timer("updateTimer",true);
        updateTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                h.sendEmptyMessage(0);
            }
        }, 500, 60000*Integer.valueOf(PreferenceManager.
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

// android vector icons
//http://www.yay.se/resources/android-native-icons

