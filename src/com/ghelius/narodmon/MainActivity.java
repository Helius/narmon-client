package com.ghelius.narodmon;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class MainActivity extends Activity implements FullSensorListUpdater.OnListChangeListener {

    private final String TAG = "narodmon";
    private ArrayList<Sensor> sensorList = null;
    private  SensorItemAdapter adapter = null;
    private ImageButton btFavour = null;
    private ImageButton btList = null;


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ListView listView = (ListView)findViewById(R.id.listView);
        sensorList = new ArrayList<Sensor>();

		// get wifi mac address for UUID
        //WifiManager wifiMan = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        //WifiInfo wifiInf;
        String uid;
        //if (wifiMan == null) {
        uid = Settings.Secure.getString(getBaseContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        Log.d(TAG,"my id is: " + uid);
        //} else {
        //    wifiInf = wifiMan.getConnectionInfo();
        //    uid = md5(wifiInf.getMacAddress());
       // }


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

        FullSensorListUpdater updater = new FullSensorListUpdater();
        updater.setOnListChangeListener(this);
        sensorList = updater.getSensorList();
        adapter = new SensorItemAdapter(getApplicationContext(), sensorList);
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                sensorItemClick (position);
            }
        });

        btFavour = (ImageButton) findViewById(R.id.imageButton2);
        btFavour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFavourites ();
            }
        });
        btList = (ImageButton) findViewById(R.id.imageButton1);
        btList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showList ();
            }
        });
        //new SensorListUpdater().execute("http://narodmon.ru/client.php?json={\"cmd\":\"sensorList\",\"uuid\":\"" + uid + "\"}");

        updater.execute("http://narodmon.ru/client.php?json={\"cmd\":\"sensorList\",\"uuid\":\"" + uid + "\"}");
    }

    @Override
    public void onListChange() {
        adapter.addAll(sensorList);
        adapter.notifyDataSetChanged();
        Toast toast = Toast.makeText(getApplicationContext(), sensorList.size() + " sensors online", Toast.LENGTH_SHORT);
        toast.show();
        setTitle(sensorList.size() + " sensors online");
        //todo showList of showWatched depend of last user choise, if we are started from notification - show watched
        showList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.icon_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.quit:
                // blabla
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showFavourites ()
    {
        Log.d(TAG, "switch to watched");
        adapter.getFilter().filter("watch");
        //adapter.notifyDataSetChanged();
        btFavour.setImageResource(R.drawable.yey_blue);
        btList.setImageResource(R.drawable.list_gray);
    }

    private void showList ()
    {
        Log.d(TAG,"switch to list " + sensorList.size());
        adapter.getFilter().filter("");
        adapter.notifyDataSetChanged();
        btFavour.setImageResource(R.drawable.yey_gray);
        btList.setImageResource(R.drawable.list_blue);
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






    private void sensorItemClick (int position)
    {
        Intent i = new Intent (this, SensorInfo.class);
        i.putExtra("Sensor", sensorList.get(position));
        startActivity(i);
    }

}

// get update for sensors value (may be needed for threshold-alarms)
// request
// http://narodmon.ru/client.php?json={"cmd":"sensorinfo","uuid":12345,"sensor":[115,125]}
// answer
// {"sensors":[{"id":115,"value":-7.75,"time":"1356060145"},{"id":125,"value":-16.75,"time":"1356059853"}]}


