package com.ghelius.narodmon;

import android.app.ActionBar;
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
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity implements
        SharedPreferences.OnSharedPreferenceChangeListener, FilterDialog.OnChangeListener, NarodmonApi.onResultReceiveListener {

    private final String TAG = "narodmon";
    private ArrayList<Sensor> sensorList;
    private ArrayList<Sensor> watchedList;
    private SensorItemAdapter listAdapter;
    private WatchedItemAdapter watchAdapter;
    private Timer updateTimer = null;
    private HorizontalPager mPager;
    private FilterDialog filterDialog;
    private UiFlags uiFlags;
    private NarodmonApi narodmonApi;

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(TAG,"onSharedPreferenceChanged " + key);
        if (key.equals(getString(R.string.pref_key_interval))) { // update interval changed
            scheduleAlarmWatcher();
            startTimer();
        } else if (key.equals(getString(R.string.pref_key_login)) || key.equals(getString(R.string.pref_key_passwd))) {
            doAuthorisation();
        }
    }

    @Override
    public void onFilterChange() {
        listAdapter.update();
    }

    @Override
    public void onPause ()
    {
        Log.i(TAG,"onPause");
        stopTimer();
        uiFlags.save(this);
        super.onPause();
    }

    @Override
    public void onResume () {
        super.onResume();
        Log.i(TAG,"onResume");
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        startTimer();
        if (uiFlags.uiMode == UiFlags.UiMode.watched) {
            Log.d(TAG,"onResume uiMode is watched, switch Watched");
            mPager.setCurrentScreen(1,false);
            getActionBar().setSelectedNavigationItem(1);
        } else {
            Log.d(TAG,"onResume uiMode is list, switch list");
            mPager.setCurrentScreen(0,false);
            getActionBar().setSelectedNavigationItem(0);
        }
    }

    @Override
    public void onDestroy () {
        Log.i(TAG,"onDestroy");
        uiFlags.save(this);
        stopTimer();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG,"onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mPager = (HorizontalPager) findViewById(R.id.horizontal_pager);
        mPager.setOnScreenSwitchListener(new HorizontalPager.OnScreenSwitchListener() {
            @Override
            public void onScreenSwitched(int screen) {
                getActionBar().setSelectedNavigationItem(screen);
                if (screen == 1) {
                    uiFlags.uiMode = UiFlags.UiMode.watched;
                    Log.d(TAG,"uiMode is watched");
                } else {
                    uiFlags.uiMode = UiFlags.UiMode.list;
                    Log.d(TAG,"uiMode is list");
                }
            }
        });

        uiFlags = UiFlags.load(this);

        ListView fullListView = (ListView) findViewById(R.id.fullListView);
        ListView watchedListView = (ListView) findViewById(R.id.watchedListView);
        sensorList = new ArrayList<Sensor>();

		// get android UUID
        final ConfigHolder config = ConfigHolder.getInstance(getApplicationContext());
        String uid = config.getUid();
        if ((uid == null) || (uid.length() < 2)) {
            config.setUid (NarodmonApi.md5(Settings.Secure.getString(getBaseContext().getContentResolver(),
                    Settings.Secure.ANDROID_ID)));
            uid = config.getUid();
        }

        listAdapter = new SensorItemAdapter(getApplicationContext(), sensorList);
        listAdapter.setUiFlags(uiFlags);
        fullListView.setAdapter(listAdapter);
        fullListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        fullListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                sensorItemClick(position);
            }
        });
        watchedList = new ArrayList<Sensor>();
        watchAdapter = new WatchedItemAdapter(getApplicationContext(), watchedList);
        watchedListView.setAdapter(watchAdapter);
        watchedListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        watchedListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                watchedItemClick(position);
            }
        });

        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME);
        actionBar.setCustomView(R.layout.actionbar_top); //load your layout
        actionBar.setListNavigationCallbacks(ArrayAdapter.createFromResource(this, R.array.action_list,
                android.R.layout.simple_spinner_dropdown_item), new ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int itemPosition, long itemId) {
                mPager.setCurrentScreen(itemPosition, true);
                return true;
            }
        });

        FilterDialog.setUiFlags(uiFlags);
        filterDialog = new FilterDialog();
        filterDialog.setOnChangeListener(this);

        narodmonApi = new NarodmonApi(this, uid, "http://narodmon.ru/client.php?json=");
        narodmonApi.setOnResultReceiveListener(this);

        doAuthorisation();
        sendLocation();
        sendVersion();

        Intent i = new Intent(this, OnBootReceiver.class);
        sendBroadcast(i);
        scheduleAlarmWatcher();
    }

    public void updateSensorList () {
        findViewById(R.id.marker_progress).setVisibility(View.INVISIBLE);
        narodmonApi.getSensorList(sensorList);
    }

    private void sendVersion () {
        narodmonApi.sendVersion(getString(R.string.app_version_name));
    }

    private void doAuthorisation () {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        String login = prefs.getString(String.valueOf(getText(R.string.pref_key_login)), "");
        String passwd = prefs.getString(String.valueOf(getText(R.string.pref_key_passwd)),"");
        if (!login.equals("")) {// don't try if login is empty
            narodmonApi.doAuthorisation(login,passwd);
        }
    }

    void sendLocation () {
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.pref_key_use_geocode),false)) {
            // use address
            narodmonApi.sendLocation(PreferenceManager.getDefaultSharedPreferences(this).
                    getString(getString(R.string.pref_key_geoloc),getString(R.string.text_Russia_novosibirsk)));
        } else {
            // use gps
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            String provider = lm.getBestProvider(criteria, true);
            Location mostRecentLocation = lm.getLastKnownLocation(provider);
            if(mostRecentLocation != null){
                double lat=mostRecentLocation.getLatitude();
                double lon=mostRecentLocation.getLongitude();
                // use API to send location
                Log.d(TAG,"my location: " + lat +" "+lon);
                narodmonApi.sendLocation(lat, lon);
            } else {
                narodmonApi.sendLocation(PreferenceManager.getDefaultSharedPreferences(this).
                        getString(getString(R.string.pref_key_geoloc),getString(R.string.text_Russia_novosibirsk)));
            }
        }
    }

    private void updateWatchedList() {
        watchedList.clear();
        for (Configuration.SensorTask storedItem : ConfigHolder.getInstance(this).getConfig().watchedId) {
            boolean found = false;
            for (Sensor aSensorList : sensorList) {
                if (storedItem.id == aSensorList.id) {
                    // watched item online
                    aSensorList.online = true;
                    watchedList.add(aSensorList);
                    found = true;
                    break;
                }
            }
            if (!found) {
                // watched item offline
                watchedList.add(new Sensor(storedItem));
            }
        }
        watchAdapter.clear();
        watchAdapter.addAll(watchedList);
        watchAdapter.notifyDataSetChanged();
    }




    private void watchedItemClick(int position) {
        Intent i = new Intent (this, SensorInfo.class);
        i.putExtra("Sensor", watchAdapter.getItem(position));
        startActivity(i);
    }

    private void sensorItemClick (int position)
    {
        Intent i = new Intent (this, SensorInfo.class);
        i.putExtra("Sensor", listAdapter.getItem(position));
        startActivity(i);
    }


    public void actionBtnClick (View view)
    {
        if (view == findViewById(R.id.btn_sort)) {
            filterDialog.show(getFragmentManager(), "dlg1");
        } else if (view == findViewById(R.id.btn_settings)) {
            startActivity(new Intent(MainActivity.this, PreferActivity.class));
        }
    }

    final Handler h = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Log.d(TAG, " ---- updateTimer fired! ----");
            updateSensorList();
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

    @Override
    public void onLocationResult(boolean ok, String addr) {
        if (ok) {
            PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putString(getString(R.string.pref_key_geoloc), addr).commit();
        }
    }

    @Override
    public void onAuthorisationResult(boolean ok, String res) {
        if (ok) {
            Log.d(TAG, "authorisation: ok, result:" + res);
        } else {
            Log.e(TAG, "authorisation: fail, result: " + res);
        }
    }

    @Override
    public void onSendVersionResult(boolean ok, String res) {
        if (ok)
            Log.d(TAG,"sendVerion ok, result: " + res);
    }

    @Override
    public void onSensorListResult(boolean ok, String res) {
        findViewById(R.id.marker_progress).setVisibility(View.INVISIBLE);
        listAdapter.update();
        updateWatchedList();
    }
}










// TODO: for future, if we need more icons, than use this menu, it splits actionBar and uses space effective
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        Log.d(TAG, "onOptionMenuItemSelected " + item);
//        switch (item.getItemId()) {
//            default:
//                return super.onOptionsItemSelected(item);
//        }
//    }
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        MenuInflater menuInflater = getMenuInflater();
//        menuInflater.inflate(R.menu.icon_menu, menu);
//
//        return super.onCreateOptionsMenu(menu);
//    }
