package com.ghelius.narodmon;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.*;
import android.widget.*;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends ActionBarActivity implements
		SharedPreferences.OnSharedPreferenceChangeListener,
        NarodmonApi.onResultReceiveListener,
//        LoginDialog.LoginEventListener,
        SensorInfoFragment.SensorConfigChangeListener,
        FragmentManager.OnBackStackChangedListener,
        FilterFragment.OnFilterChangeListener
    {

	private SensorInfoFragment sensorInfoFragment;
	private FilterFragment filterFragment;
	private SensorListFragment sensorListFragment;
	private boolean showProgress;
    private Menu mOptionsMenu;
	private long lastUpdateTime;
	private final static int gpsUpdateIntervalMs = 20*60*1000; // time interval for updateFilter coordinates and sensor list
	//	private final static int gpsUpdateIntervalMs = 1*60*1000; // time interval for updateFilter coordinates and sensor list
	private ArrayList<View> menuItems = new ArrayList<View>();


	@Override
	public void favoritesChange() {
		int favoritesCnt = new DatabaseHandler(getApplicationContext()).getFavorites().size();
		((TextView)menuItems.get(1).findViewById(R.id.cnt)).setText(String.valueOf(favoritesCnt));
	}

	@Override
	public void alarmChanged() {
		//TODO: need to calc alarms and show it
	}

    @Override
    public void onBackStackChanged() {
        Log.d(TAG,"onBackStackChanged");
        shouldDisplayHomeUp();
    }

    public void shouldDisplayHomeUp(){
        //Enable Up button only  if there are entries in the back stack
        boolean canBack = getSupportFragmentManager().getBackStackEntryCount()>0;
        Log.d(TAG,"shouldDisplayHomeUp is " + canBack);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(canBack);
        if (canBack) {
            mOptionsMenu.clear();
        } else {
            supportInvalidateOptionsMenu();
        }
        if (mDrawerToggle!=null)
            mDrawerToggle.setDrawerIndicatorEnabled(!canBack);
    }

    @Override
    public boolean onSupportNavigateUp() {
        Log.d(TAG,"onSupportNavigateUp");
        //This method is called when the up button is pressed. Just the pop back stack.
        getSupportFragmentManager().popBackStack();
        return true;
    }

    @Override
    public void filterChange() {
        listAdapter.updateFilter();
    }

    @Override
    public UiFlags returnUiFlags() {
        return uiFlags;
    }


    enum LoginStatus {LOGIN, LOGOUT, ERROR}

	private static final String api_key = "85UneTlo8XBlA";
	private final String TAG = "narodmon-main";
	private ArrayList<Sensor> sensorList;
	private SensorItemAdapter listAdapter;
	private Timer updateTimer = null;
	private Timer gpsUpdateTimer = null;
	private LoginDialog loginDialog;
	private UiFlags uiFlags;
	private NarodmonApi narodmonApi;
	private String apiHeader;
	private LoginStatus loginStatus = LoginStatus.LOGOUT;
	String uid;

	private DrawerLayout mDrawerLayout = null;
	private View mDrawerMenu = null;
	private ActionBarDrawerToggle mDrawerToggle = null;
	private CharSequence mTitle;



	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Log.d(TAG, "onSharedPreferenceChanged " + key);
		if (key.equals(getString(R.string.pref_key_interval))) { // updateFilter interval changed
			scheduleAlarmWatcher();
			startUpdateTimer();
		} else if (key.equals(getString(R.string.pref_key_geoloc)) || key.equals(getString(R.string.pref_key_use_geocode))) {
			initLocationUpdater();
			updateSensorsList(true);
		}
	}

	@Override
	public void onPause() {
		Log.i(TAG, "onPause");
		stopUpdateTimer();
		stopGpsTimer();
		super.onPause();
	}

	void initLocationUpdater() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		boolean useGps = !prefs.getBoolean(getString(R.string.pref_key_use_geocode), false);
		Float lat = prefs.getFloat("lat", 0.0f);
		Float lng = prefs.getFloat("lng", 0.0f);
		// it's first start, gps data may be not ready or gps not used, so we use prev coordinates always first time
		if (lat != 0.0f && lng != 0.0f)
			narodmonApi.setLocation(lat, lng);
		if (useGps) { // if use gps, just updateFilter location periodically, set to api, don't use saved value
			Log.d(TAG, "init location updater: we use gps");
			startGpsTimer();
			updateLocation();
		} else { // if use address, use this value and set to api, this value updateFilter and save in location result callback
			Log.d(TAG, "init location updater: we use address");
			narodmonApi.sendLocation(prefs.getString(getString(R.string.pref_key_geoloc), ""));
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "onResume: " + System.currentTimeMillis() + " but saved is " + lastUpdateTime + ", diff is " + (System.currentTimeMillis()-lastUpdateTime));

		FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
		trans.replace(R.id.content_frame, sensorListFragment);
		trans.commit();

		updateSensorsList(false);
		startUpdateTimer();
		showProgress = true;
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		if (!pref.getBoolean(getString(R.string.pref_key_use_geocode),false)) {
			startGpsTimer();
		}

	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "onDestroy");
		uiFlags.save(this);
		stopUpdateTimer();
		PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
		stopGpsTimer();
		super.onDestroy();
	}


	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
//		savedInstanceState.putBoolean("MyBoolean", true);
//		savedInstanceState.putDouble("myDouble", 1.9);
//		savedInstanceState.putInt("MyInt", 1);
//		savedInstanceState.putString("MyString", "Welcome back to Android");
//		savedInstanceState.putLong("lastTime" ,System.currentTimeMillis());
	}

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        getSupportFragmentManager().addOnBackStackChangedListener(this);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		mTitle = "All";
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (mDrawerLayout != null) {
            mDrawerMenu = findViewById(R.id.left_menu_view);
            // set a custom shadow that overlays the main content when the drawer opens
            mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
            // enable ActionBar app icon to behave as action to toggle nav drawer

            // ActionBarDrawerToggle ties together the the proper interactions
            // between the sliding drawer and the action bar app icon
            mDrawerToggle = new ActionBarDrawerToggle(
                    this,                  /* host Activity */
                    mDrawerLayout,         /* DrawerLayout object */
                    R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
                    R.string.drawer_open,  /* "open drawer" description for accessibility */
                    R.string.drawer_close  /* "close drawer" description for accessibility */
            ) {
                public void onDrawerClosed(View view) {
                    getSupportActionBar().setTitle(mTitle);
//				invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                }

                public void onDrawerOpened(View drawerView) {
                    getSupportActionBar().setTitle(mTitle);
//				invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                }
            };
            mDrawerLayout.setDrawerListener(mDrawerToggle);
        }

        final int menuBackgroundColor = Color.argb(0xff,0x01,0x34,0x6E);
		// collect menu item views
		menuItems.add(findViewById(R.id.menu_item0));
		menuItems.add(findViewById(R.id.menu_item1));
		menuItems.add(findViewById(R.id.menu_item2));
		menuItems.add(findViewById(R.id.menu_item3));
		menuItems.add(findViewById(R.id.menu_item5));
        menuItems.get(0).setBackgroundColor(menuBackgroundColor);

		int i = 0;
		for (View view : menuItems) {
			view.setTag(i++);
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					clearMenuSelection();
					Log.d(TAG,"click!" + v.getTag());
					switch ((Integer)v.getTag()) {
						case 0: // all
							menuAllClicked();
							break;
						case 1: // watched
							menuWatchedClicked();
							break;
						case 2: // my
							menuMyClicked();
							break;
						case 3: // alarm
							menuAlarmClicked();
							break;
						case 4: // graph
							menuGraphClicked();
							setTitle("Graphs");
							break;
						default:
							Log.d(TAG, "unknown tag");
							break;
					}
                    if (mDrawerLayout != null)
					    mDrawerLayout.closeDrawer(mDrawerMenu);
					v.setBackgroundColor(menuBackgroundColor);
				}
			});
		}


		sensorInfoFragment = new SensorInfoFragment();
		sensorInfoFragment.setFavoritesChangeListener(this);
		filterFragment = new FilterFragment();
		sensorListFragment = new SensorListFragment();
		sensorListFragment.setOnListItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				sensorItemClick(position);
			}
		});

		uiFlags = UiFlags.load(this);

		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

		sensorList = new ArrayList<Sensor>();

		// get android UUID
		final ConfigHolder config = ConfigHolder.getInstance(getApplicationContext());
		apiHeader = config.getApiHeader();
		uid = NarodmonApi.md5(Settings.Secure.getString(getBaseContext().getContentResolver(), Settings.Secure.ANDROID_ID));
		Log.d(TAG, "android ID: " + uid);
		apiHeader = "{\"uuid\":\"" + uid +
				"\",\"api_key\":\"" + api_key + "\",";
		config.setApiHeader(apiHeader);


		listAdapter = new SensorItemAdapter(getApplicationContext(), sensorList);
		listAdapter.setUiFlags(uiFlags);
		sensorListFragment.setListAdapter(listAdapter);

		loginDialog = new LoginDialog();
        loginDialog.setOnChangeListener(new LoginDialog.LoginEventListener() {
            @Override
            public void login() {
               doAuthorisation();
            }

            @Override
            public void logout() {
                closeAutorisation();
            }

            @Override
            public LoginStatus loginStatus() {
                return loginStatus;
            }
        });

		narodmonApi = new NarodmonApi(apiHeader);
		narodmonApi.setOnResultReceiveListener(this);

		narodmonApi.restoreSensorList(this, sensorList);
        updateMenuSensorCounts();

		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.pref_key_autologin), false))
			doAuthorisation();
		sendVersion();
		narodmonApi.getTypeDictionary(this);

		Intent intent = new Intent(this, OnBootReceiver.class);
		sendBroadcast(intent);
		scheduleAlarmWatcher();

		initLocationUpdater();
        setTitle(mTitle);
	}



	private void updateMenuSensorCounts () {
		((TextView)menuItems.get(0).findViewById(R.id.cnt)).setText(String.valueOf(listAdapter.getCount()));
		((TextView)menuItems.get(2).findViewById(R.id.cnt)).setText(String.valueOf(listAdapter.getMyCount()));
		favoritesChange();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        mOptionsMenu = menu;
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.icon_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	/* Called whenever we call invalidateOptionsMenu() */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// If the nav drawer is open, hide action items related to the content view
//        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
//        menu.findItem(R.id.action_websearch).setVisible(!drawerOpen);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// The action bar home/up action should open or close the drawer.
		// ActionBarDrawerToggle will take care of this.


        if (item.getItemId() == android.R.id.home && getSupportFragmentManager().getBackStackEntryCount()>0) {
            getSupportFragmentManager().popBackStack();
            return true;
        }

		// Handle action buttons
		switch(item.getItemId()) {
			case R.id.menu_settings:
				Log.d(TAG, "start settings activity");
				startActivity(new Intent(MainActivity.this, PreferActivity.class));
				break;
			case R.id.menu_refresh:
				Log.d(TAG, "refresh sensor list");
				updateSensorsList(true);
				break;
			case R.id.menu_login:
				Log.d(TAG, "show login dialog");
				loginDialog.show(getSupportFragmentManager(), "dlg2");
				break;
			case R.id.menu_help:
				String url = "http://helius.github.com/narmon-client/";
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(url));
				startActivity(i);
				break;
			default:
		}

		if (mDrawerToggle!=null && mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}


	@Override
	public void setTitle(CharSequence title) {
		mTitle = title;
		getSupportActionBar().setTitle(mTitle);
	}

	/**
	 * When using the ActionBarDrawerToggle, you must call it during
	 * onPostCreate() and onConfigurationChanged()...
	 */

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
        if (mDrawerToggle != null)
		    mDrawerToggle.syncState();
	}

//	@Override
//	public void onConfigurationChanged(Configuration newConfig) {
//		super.onConfigurationChanged(newConfig);
//		// Pass any configuration change to the drawer toggls
//		mDrawerToggle.onConfigurationChanged(newConfig);
//	}


	private void menuGraphClicked() {
		Toast.makeText(getApplicationContext(), "Graph", Toast.LENGTH_SHORT).show();
	}

	private void menuFilterClicked() {
		FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
		trans.replace(R.id.content_frame , filterFragment);
        trans.addToBackStack(null);
		trans.commit();
        mOptionsMenu.clear();
	}

	private void menuWatchedClicked() {
		listAdapter.setGroups(SensorItemAdapter.SensorGroups.Watched);
		setTitle("Favourites");
        if (listAdapter.getMyCount() == 0) {
            //TODO: show message
        }
	}

	private void menuMyClicked() {
		listAdapter.setGroups(SensorItemAdapter.SensorGroups.My);
        if (listAdapter.getMyCount() == 0) {
            //TODO: show message
        }
		setTitle("My");
	}

	private void menuAlarmClicked() {
		//To change body of created methods use File | Settings | File Templates.
	}

	private void menuAllClicked() {
		listAdapter.setGroups(SensorItemAdapter.SensorGroups.All);
		FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
		trans.replace(R.id.content_frame , sensorListFragment);
		trans.commit();
		setTitle("All");
	}

	private void clearMenuSelection () {
		for (View item: menuItems ) {
			item.setBackgroundColor(Color.BLACK);
		}
	}




	public void updateSensorsList(boolean force) {
		if (force) {
			lastUpdateTime = 0;
			Log.d(TAG,"force updateFilter sensor list");
		}

		if (System.currentTimeMillis()-lastUpdateTime < gpsUpdateIntervalMs) {
			Log.d(TAG,"list was not updated, timeout not gone");
			return;
		}

		Log.d(TAG,"start list updating...");
		setRefreshProgress(true);
		narodmonApi.getSensorList(sensorList, uiFlags.radiusKm);
		lastUpdateTime = System.currentTimeMillis();
	}

	public void updateSensorsValue() {

		Log.d(TAG, "------------ updateFilter sensor value ---------------");
		setRefreshProgress(true);
		narodmonApi.updateSensorsValue(sensorList);
	}


	private void sendVersion() {
		narodmonApi.sendVersion(getString(R.string.app_version_name));
	}

	private void doAuthorisation() {
		Log.d(TAG, "doAuthorisation");
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
		String login = prefs.getString(String.valueOf(getText(R.string.pref_key_login)), "");
		String passwd = prefs.getString(String.valueOf(getText(R.string.pref_key_passwd)), "");
		if (!login.equals("")) {// don't try if login is empty
			narodmonApi.doAuthorisation(login, passwd, uid);
		} else {
			Log.w(TAG, "login is empty, do not authorisation");
		}
	}

	private void closeAutorisation() {
		narodmonApi.closeAuthorisation();
	}

	void updateLocation () {
		MyLocation myLocation = new MyLocation();
		myLocation.getLocation(this, new MyLocation.LocationResult() {
			@Override
			public void gotLocation(Location location) {
				Log.d(TAG, "got location");
				if (location == null) return;
				double lat = location.getLatitude();
				double lon = location.getLongitude();
				// use API to send location
				Log.d(TAG, "location was updated and set into api : " + lat + " " + lon);
				narodmonApi.setLocation((float)lat, (float)lon);
				SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
				pref.edit().putFloat("lat",(float)lat).putFloat("lng",(float)lon).commit();
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						updateSensorsList(true);
					}
				});
			}
		});
	}

	/**
	 * <p>Calls for result on sending address string (Russia, Moscow, Lenina 1) to server</p>
	 * @param ok
	 * @param addr - contain address (if lat/lng was sended), empty if address was sended
	 * @param lat,lng - contain coordinates, if address string was sended
	 */
	@Override
	public void onLocationResult(boolean ok, String addr, Float lat, Float lng) {
//		Log.d(TAG, "on location Result (server answer): " + addr);
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		// if use gps save address
		if (ok && !pref.getBoolean(getString(R.string.pref_key_use_geocode),false)) {
			Log.d(TAG,"on location result: we use gps, so sawe address string to shared pref: " + addr);
			pref.edit().putString(getString(R.string.pref_key_geoloc), addr).commit();
//			Toast.makeText(getApplicationContext(), addr, Toast.LENGTH_SHORT);
		} else if (ok) { // if use address, save coordinates and set it to api
			Log.d(TAG,"on location result: we use addres, so save coordinates to shared pref: " + lat + ", "+ lng);
			pref.edit().putFloat("lat",lat).putFloat("lng",lng).commit();
			narodmonApi.setLocation(lat, lng);
		}
		updateSensorsList(true);
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
			loginDialog.updateLoginStatus();
		}
		updateSensorsList(true);
	}

	@Override
	public void onSendVersionResult(boolean ok, String res) {
	}

	@Override
	public void onSensorListResult(boolean ok, String res) {
		Log.d(TAG, "---------------- List updated --------------");
		setRefreshProgress(false);
		listAdapter.updateFilter();
		updateMenuSensorCounts();
	}

	@Override
	public void onSensorTypeResult(boolean ok, String res) {
		//parse res to container
		Log.d(TAG, "---------------- TypeDict updated --------------");
		if (!ok) return;
		SensorTypeProvider.getInstance(getApplicationContext()).setTypesFromString(res);
		listAdapter.notifyDataSetChanged();
	}



	private void sensorItemClick(int position) {

		sensorInfoFragment.setId(listAdapter.getItem(position).id);
		FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
		trans.replace(R.id.content_frame , sensorInfoFragment);
		trans.addToBackStack(null);
		trans.commit();
	}

	// called by action (define via xml onClick)
	public void showFilterDialog(MenuItem item) {
        menuFilterClicked();
	}

	// called by pressing refresh button (define via xml onClick)
	public void onUpdateBtnPress(MenuItem item) {
		updateSensorsValue();
	}

	private void setRefreshProgress(boolean refreshing) {
		if (mOptionsMenu != null) {
			final MenuItem refreshItem = mOptionsMenu.findItem(R.id.menu_refresh);
			if (refreshItem != null && (Build.VERSION.SDK_INT > 10)) {
				if (refreshing) {
					refreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
				} else {
					refreshItem.setActionView(null);
				}
			}
		}
	}

	final Handler updateTimerHandler = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			Log.d(TAG, "updateTimer fired");
			updateSensorsValue();
			return false;
		}
	});

	final Handler gpsTimerHandler = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			Log.d(TAG, "GPS updateTimer fired");
			updateLocation();
			return false;
		}
	});

	void stopGpsTimer () {
		if (gpsUpdateTimer != null) {
			gpsUpdateTimer.cancel();
			gpsUpdateTimer.purge();
			gpsUpdateTimer = null;
		}
	}

	void startGpsTimer () {
		stopGpsTimer();
		gpsUpdateTimer = new Timer("gpsUpdateTimer", true);
		gpsUpdateTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				gpsTimerHandler.sendEmptyMessage(0);
			}
		}, gpsUpdateIntervalMs, gpsUpdateIntervalMs); // updateFilter gps data timeout 10 min

	}

	void startUpdateTimer() {
		stopUpdateTimer();
		updateTimer = new Timer("updateTimer", true);
		updateTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				updateTimerHandler.sendEmptyMessage(0);
			}
		}, 60000, 60000 * Integer.valueOf(PreferenceManager.
				getDefaultSharedPreferences(this).
				getString(getString(R.string.pref_key_interval), "5")));

	}

	void stopUpdateTimer() {
		if (updateTimer != null) {
			updateTimer.cancel();
			updateTimer.purge();
			updateTimer = null;
		}
	}


	void scheduleAlarmWatcher() {
		AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(this, OnAlarmReceiver.class);
		PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
		try {
			am.cancel(pi);
		} catch (Exception e) {
			Log.e(TAG, "cancel pending intent of AlarmManager failed");
			e.getMessage();
		}

		Log.d(TAG, "Alarm watcher new updateInterval " + Integer.valueOf(PreferenceManager.
				getDefaultSharedPreferences(this).
				getString(getString(R.string.pref_key_interval), "5")));

		am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
				SystemClock.elapsedRealtime() + (1 * 60000), // 1 minute
				(60000 * Integer.valueOf(PreferenceManager.
						getDefaultSharedPreferences(this).
						getString(getString(R.string.pref_key_interval), "5"))),
				pi);
	}

}

