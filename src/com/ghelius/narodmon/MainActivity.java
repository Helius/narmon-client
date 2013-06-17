package com.ghelius.narodmon;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.*;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends SherlockFragmentActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener, NarodmonApi.onResultReceiveListener, LoginDialog.LoginEventListener, CheckedListItemAdapter.ItemChangeInterface {

	private Menu mOptionsMenu = null;
	private boolean showProgress;
	private CheckedListItemAdapter typeAdapter;
	private int prevScreen = 1;

	@Override
	public boolean isItemChecked(int position) {
//		Log.d(TAG, "isItemChecked: " + position + ", in " + uiFlags.hidenTypes);
		for (int i = 0; i < uiFlags.hidenTypes.size(); i++) {
			if (typeAdapter.getItem(position).code == uiFlags.hidenTypes.get(i)) {
				return false;
			}
		}
		return true;
	}

	enum LoginStatus {LOGIN,LOGOUT,ERROR}

    private static final String api_key = "85UneTlo8XBlA";
    private final String TAG = "narodmon-main";
    private ArrayList<Sensor> sensorList;
    private ArrayList<Sensor> watchedList;
    private SensorItemAdapter listAdapter;
    private WatchedItemAdapter watchAdapter;
    private Timer updateTimer = null;
    private HorizontalPager mPager;
//    private FilterDialog filterDialog;
	private LoginDialog loginDialog;
    private UiFlags uiFlags;
    private NarodmonApi narodmonApi;
    private boolean locationSended;
    private boolean authorisationDone;
    private int oldRadiusKm;
    private String apiHeader;
	private LoginStatus loginStatus = LoginStatus.LOGOUT;
	String uid;


	@Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(TAG,"onSharedPreferenceChanged " + key);
        if (key.equals(getString(R.string.pref_key_interval))) { // update interval changed
            scheduleAlarmWatcher();
            startTimer();
        } else if (key.equals(getString(R.string.pref_key_geoloc)) || key.equals(getString(R.string.pref_key_use_geocode))) {
            sendLocation();

        }
    }

//    @Override
//    public void onFilterChange() {
//        Log.d(TAG,"new Radius is " + uiFlags.radiusKm);
//        listAdapter.update();
//    }

//    @Override
//    public void onDialogClose() {
//        Log.d(TAG, "onDialogClose, new radius: " + uiFlags.radiusKm + " saved: " + oldRadiusKm);
//        if (uiFlags.radiusKm > oldRadiusKm) {
//            Log.d(TAG,"update sensor list");
//            updateSensorList();
//            oldRadiusKm = uiFlags.radiusKm;
//        }
//    }

    @Override
    public void onPause ()
    {
        Log.i(TAG, "onPause");
        stopTimer();
        super.onPause();
    }

    @Override
    public void onResume () {
        super.onResume();
	    Log.d(TAG,"onResume");
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        listAdapter.notifyDataSetChanged();
        updateSensorList();
        startTimer();
        if (uiFlags.uiMode == UiFlags.UiMode.watched) {
	        Log.d(TAG, "switch to watched");
            mPager.setCurrentScreen(2,false);
            getSupportActionBar().setSelectedNavigationItem(2);
        } else {
	        Log.d(TAG, "switch to list");
            mPager.setCurrentScreen(1,false);
            getSupportActionBar().setSelectedNavigationItem(1);
        }
	    showProgress = true;
    }

    @Override
    public void onDestroy () {
        Log.i(TAG,"onDestroy");
        uiFlags.save(this);
        stopTimer();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_BACK:
			case KeyEvent.KEYCODE_ESCAPE:
				if (prevScreen == 0) {
					mPager.setCurrentScreen(1, true);
					return true;
				}
		}
		return super.onKeyDown(keyCode, event);
	}

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG,"onCreate");
        setTheme(com.actionbarsherlock.R.style.Theme_Sherlock);
        super.onCreate(savedInstanceState);
        authorisationDone = false;
        locationSended = false;
        setContentView(R.layout.main);
        mPager = (HorizontalPager) findViewById(R.id.horizontal_pager);
        mPager.setOnScreenSwitchListener(new HorizontalPager.OnScreenSwitchListener() {
            @Override
            public void onScreenSwitched(int screen) {
	            Log.d(TAG,"onScreenSwitch: " + screen);
                getSupportActionBar().setSelectedNavigationItem(screen);
                if (screen == 2) {
                    uiFlags.uiMode = UiFlags.UiMode.watched;
	                if (prevScreen == 0)
		                listAdapter.update();

                } else if (screen == 1) {
                    uiFlags.uiMode = UiFlags.UiMode.list;
	                if (prevScreen == 0)
		                listAdapter.update();

                } else if (screen == 0) {
	                setFilterData();
                }
	            prevScreen = screen;
            }
        });

        uiFlags = UiFlags.load(this);
        oldRadiusKm = uiFlags.radiusKm;

        ListView fullListView = (ListView) findViewById(R.id.fullListView);
        ListView watchedListView = (ListView) findViewById(R.id.watchedListView);
        sensorList = new ArrayList<Sensor>();

		// get android UUID
        final ConfigHolder config = ConfigHolder.getInstance(getApplicationContext());
        apiHeader = config.getApiHeader();
//        if ((apiHeader == null) || (apiHeader.length() < 2)) {
	        uid = NarodmonApi.md5(Settings.Secure.getString(getBaseContext().getContentResolver(), Settings.Secure.ANDROID_ID));
	        Log.d(TAG,"android ID: " + uid);
            apiHeader = "{\"uuid\":\"" +  uid +
                    "\",\"api_key\":\"" + api_key + "\",";
            config.setApiHeader(apiHeader);
//        }


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

        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setListNavigationCallbacks(ArrayAdapter.createFromResource(this, R.array.action_list,
                android.R.layout.simple_spinner_dropdown_item), new ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int itemPosition, long itemId) {
                mPager.setCurrentScreen(itemPosition, true);
                return true;
            }
        });

	    loginDialog = new LoginDialog();
	    loginDialog.setOnChangeListener(this);

        narodmonApi = new NarodmonApi(apiHeader);
        narodmonApi.setOnResultReceiveListener(this);

        narodmonApi.restoreSensorList(this,sensorList);
	    if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.pref_key_autologin),false))
            doAuthorisation();
        sendLocation();
        sendVersion();
        narodmonApi.getTypeDictionary(this);

        Intent i = new Intent(this, OnBootReceiver.class);
        sendBroadcast(i);
        scheduleAlarmWatcher();

	    typeAdapter = new CheckedListItemAdapter (this, SensorTypeProvider.getInstance(this).getTypesList());
	    typeAdapter.setItemChangeInterface(this);
	    ListView typeListView = (ListView) findViewById(R.id.typeListView);
	    typeListView.setAdapter(typeAdapter);
	    typeListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
		    @Override
		    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			    if (uiFlags.hidenTypes.contains(typeAdapter.getItem(position).code)) {
				    uiFlags.hidenTypes.remove((Integer) typeAdapter.getItem(position).code);
			    } else {
				    uiFlags.hidenTypes.add((Integer) typeAdapter.getItem(position).code);
			    }
			    typeAdapter.notifyDataSetChanged();
			    listAdapter.update();
		    }
	    });
    }

	// init filter ui from uiFlags
	private void setFilterData() {
		RadioGroup radioGroup1 = (RadioGroup) findViewById(R.id.radiogroupe_sort);
		if (uiFlags.sortType == UiFlags.SortType.distance)
			radioGroup1.check(R.id.radioButtonSortDistance);
		else if (uiFlags.sortType == UiFlags.SortType.name)
			radioGroup1.check(R.id.radioButtonSortName);
		else if (uiFlags.sortType == UiFlags.SortType.time)
			radioGroup1.check(R.id.radioButtonSortTime);
		else if (uiFlags.sortType == UiFlags.SortType.type)
			radioGroup1.check(R.id.radioButtonSortType);

		RadioButton btSortDistance = (RadioButton) findViewById(R.id.radioButtonSortDistance);
		btSortDistance.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (!isChecked)
					return;
				Log.d(TAG,"check distance");
				uiFlags.sortType = UiFlags.SortType.distance;
				((RadioButton)findViewById(R.id.radioButtonSortName)).setChecked(false);
				((RadioButton)findViewById(R.id.radioButtonSortType)).setChecked(false);
				((RadioButton)findViewById(R.id.radioButtonSortTime)).setChecked(false);
				listAdapter.update();
			}
		});
		RadioButton btSortName = (RadioButton) findViewById(R.id.radioButtonSortName);
		btSortName.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (!isChecked)
					return;
				Log.d(TAG,"check name");
				uiFlags.sortType = UiFlags.SortType.name;
				((RadioButton)findViewById(R.id.radioButtonSortDistance)).setChecked(false);
				((RadioButton)findViewById(R.id.radioButtonSortType)).setChecked(false);
				((RadioButton)findViewById(R.id.radioButtonSortTime)).setChecked(false);
				listAdapter.update();
			}
		});
		RadioButton btSortType = (RadioButton) findViewById(R.id.radioButtonSortType);
		btSortType.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (!isChecked)
					return;
				Log.d(TAG,"check type");
				uiFlags.sortType = UiFlags.SortType.type;
				((RadioButton)findViewById(R.id.radioButtonSortName)).setChecked(false);
				((RadioButton)findViewById(R.id.radioButtonSortDistance)).setChecked(false);
				((RadioButton)findViewById(R.id.radioButtonSortTime)).setChecked(false);
				listAdapter.update();
			}
		});
		RadioButton btSortTime = (RadioButton) findViewById(R.id.radioButtonSortTime);
		btSortTime.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (!isChecked)
					return;
				Log.d(TAG,"check time");
				uiFlags.sortType = UiFlags.SortType.time;
				((RadioButton)findViewById(R.id.radioButtonSortName)).setChecked(false);
				((RadioButton)findViewById(R.id.radioButtonSortType)).setChecked(false);
				((RadioButton)findViewById(R.id.radioButtonSortDistance)).setChecked(false);
				listAdapter.update();
			}
		});


		SeekBar radius = (SeekBar) findViewById(R.id.radius_seekerbar);
		radius.setMax(15);
		radius.setProgress((int) (Math.log(uiFlags.radiusKm)/Math.log(2)));
		final TextView radiusValue = (TextView) findViewById(R.id.radius_value);
		radiusValue.setText(String.valueOf(uiFlags.radiusKm));
		radius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				int distance = (int) Math.pow(2,progress);
				radiusValue.setText(String.valueOf(distance));
				if (distance != 0)
					uiFlags.radiusKm = distance;
				else
					uiFlags.radiusKm = 1;
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				listAdapter.update();
			}
		});

		RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radiogroupe_All_My);
		if (uiFlags.showingMyOnly)
			radioGroup.check(R.id.radioButtonMyOnly);
		else
			radioGroup.check(R.id.radioButtonAll);
		radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				uiFlags.showingMyOnly = checkedId == R.id.radioButtonMyOnly;
				listAdapter.update();
			}
		});

		((Button)findViewById(R.id.filter_select_all)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				uiFlags.hidenTypes.clear();
				typeAdapter.notifyDataSetChanged();
				listAdapter.update();
			}
		});
		((Button)findViewById(R.id.filter_select_none)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				for (int i = 0; i < SensorTypeProvider.getInstance(getApplicationContext()).getTypesList().size(); i++) {
					uiFlags.hidenTypes.add(SensorTypeProvider.getInstance(getApplicationContext()).getTypesList().get(i).code);
				}
				typeAdapter.notifyDataSetChanged();
				listAdapter.update();
			}
		});
	}


	public void updateSensorList () {
        Log.d(TAG,"------------ update sensor list ---------------");
	    setRefreshProgress(true);
        narodmonApi.getSensorList(sensorList, uiFlags.radiusKm);
    }

    public void updateSensorsValue () {
        Log.d(TAG,"------------ update sensor value ---------------");
	    setRefreshProgress(true);
        narodmonApi.updateSensorsValue(sensorList);
    }


    private void sendVersion () {
        narodmonApi.sendVersion(getString(R.string.app_version_name));
    }

    private void doAuthorisation () {
        Log.d(TAG,"doAuthorisation");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        String login = prefs.getString(String.valueOf(getText(R.string.pref_key_login)), "");
        String passwd = prefs.getString(String.valueOf(getText(R.string.pref_key_passwd)),"");
        if (!login.equals("")) {// don't try if login is empty
            narodmonApi.doAuthorisation(login, passwd, uid);
        } else {
            Log.w(TAG,"login is empty, do not authorisation");
        }
    }

	private void closeAutorisation () {
		narodmonApi.closeAuthorisation();
	}

    void sendLocation () {
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.pref_key_use_geocode),false)) {
            // use address
            Log.d(TAG,"location: use address");
            narodmonApi.sendLocation(PreferenceManager.getDefaultSharedPreferences(this).
                    getString(getString(R.string.pref_key_geoloc),getString(R.string.text_Russia_novosibirsk)));
        } else {
            Log.d(TAG,"location: use gps");

	        MyLocation myLocation = new MyLocation();
	        myLocation.getLocation(this,new MyLocation.LocationResult() {
		        @Override
		        public void gotLocation(Location location) {
			        Log.d(TAG,"got location");
			        if (location == null) return;
			        double lat=location.getLatitude();
			        double lon=location.getLongitude();
			        // use API to send location
			        Log.d(TAG,"my location: " + lat +" "+lon);
			        narodmonApi.sendLocation(lat, lon);
		        }
	        });


//            // use gps
//            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//            Criteria criteria = new Criteria();
//            criteria.setAccuracy(Criteria.ACCURACY_FINE);
//            String provider = lm.getBestProvider(criteria, true);
//            if (provider == null) {
//                Log.e(TAG,"location provider is NULL");
//                return;
//            }
//            Location mostRecentLocation = lm.getLastKnownLocation(provider);
//            if(mostRecentLocation == null) {
//                Log.e(TAG,"mostRecentLocation is NULL");
////                return;
//                Location locationGPS = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//                Location locationNet = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
//
//                long GPSLocationTime = 0;
//                if (null != locationGPS) { GPSLocationTime = locationGPS.getTime();}
//                long NetLocationTime = 0;
//                if (null != locationNet) {
//                    NetLocationTime = locationNet.getTime();
//                }
//                if ( 0 < GPSLocationTime - NetLocationTime )
//                    mostRecentLocation = locationGPS;
//                else
//                    mostRecentLocation = locationNet;
//            }
//            if (mostRecentLocation == null) {
//                Log.e(TAG,"location still null");
//                return;
//            }
//            double lat=mostRecentLocation.getLatitude();
//            double lon=mostRecentLocation.getLongitude();
//            // use API to send location
//            Log.d(TAG,"my location: " + lat +" "+lon);
//            narodmonApi.sendLocation(lat, lon);
        }
    }





    @Override
    public void onLocationResult(boolean ok, String addr) {
        Log.d(TAG, "on Location Result");
        if (ok) {
            PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putString(getString(R.string.pref_key_geoloc), addr).commit();
        }
        locationSended = true;
        if (authorisationDone) // update list if both finished
            updateSensorList();

    }

    @Override
    public void onAuthorisationResult(boolean ok, String res) {
        if (ok) {
	        if (res.equals("")) {
				loginStatus = LoginStatus.LOGOUT;
		        loginDialog.updateLoginStatus();
	        } else {
		        Log.d(TAG, "authorisation: ok, result:" + res);
		        loginStatus = LoginStatus.LOGIN;
		        loginDialog.updateLoginStatus();
	        }
        } else {
            Log.e(TAG, "authorisation: fail, result: " + res);
	        loginStatus = LoginStatus.ERROR;
	        loginDialog.updateLoginStatus ();
        }
        authorisationDone = true;
//        if (locationSended) // update list if both finished
        updateSensorList();
    }

    @Override
    public void onSendVersionResult(boolean ok, String res) {
    }

    @Override
    public void onSensorListResult(boolean ok, String res) {
        Log.d(TAG,"---------------- List updated --------------");
	    setRefreshProgress(false);
        listAdapter.update();
        updateWatchedList();
    }

    @Override
    public void onSensorTypeResult(boolean ok, String res) {
        //parse res to container
        Log.d(TAG,"---------------- TypeDict updated --------------");
        if (!ok) return;
        SensorTypeProvider.getInstance(this).setTypesFromString(res);
        listAdapter.notifyDataSetChanged();
        watchAdapter.notifyDataSetChanged();
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
        // for compatibility reason
//        watchAdapter.addAll(watchedList);
        for (Sensor aWatchedList : watchedList) {
            watchAdapter.add(aWatchedList);
        }
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

	// called by action (define via xml onClick)
	public void showFilterDialog (MenuItem item) {
		mPager.setCurrentScreen (0, true);
	}

	// called by pressing refresh button (define via xml onClick)
	public void onUpdateBtnPress (MenuItem item) {
		setRefreshProgress(true);
		updateSensorsValue();
	}

	private void setRefreshProgress (boolean refreshing) {
		if (mOptionsMenu != null) {
			final MenuItem refreshItem = mOptionsMenu.findItem(R.id.menu_refresh);
			if (refreshItem != null) {
				if (refreshing) {
					refreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
				} else {
					refreshItem.setActionView(null);
				}
			}
		}
	}

    final Handler h = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Log.d(TAG, "updateTimer fired");
            updateSensorsValue();
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
        }, 60000, 60000*Integer.valueOf(PreferenceManager.
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
    public boolean onCreateOptionsMenu(Menu menu) {
	    mOptionsMenu = menu;
        final MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.icon_menu, menu);
	    if (showProgress) {
		    setRefreshProgress(true);
		    showProgress = false;
	    }
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	    Log.d(TAG,"item: " + item + ", itemID: " + item.getItemId());

        switch (item.getItemId()) {
	        case R.id.menu_settings :
		        Log.d(TAG,"start settings activity");
		        startActivity(new Intent(MainActivity.this, PreferActivity.class));
				break;
	        case R.id.menu_refresh :
		        Log.d(TAG,"refresh sensor list");
		        updateSensorList();
				break;
	        case R.id.menu_login :
		        Log.d(TAG,"show login dialog");
		        loginDialog.show(getSupportFragmentManager(), "dlg2");
				break;
	        case R.id.menu_help :
		        String url = "http://helius.github.com/narmon-client/";
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(url));
				startActivity(i);
		        break;
            default:
                return super.onOptionsItemSelected(item);
        }
	    return false;
    }

	@Override
	public void login() {
		doAuthorisation();
	}

	@Override
	public void logout() {
		closeAutorisation();
	}

	@Override
	public SharedPreferences getPreference() {
		return PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
	}

	@Override
	public LoginStatus loginStatus() {
		return loginStatus;
	}
}

