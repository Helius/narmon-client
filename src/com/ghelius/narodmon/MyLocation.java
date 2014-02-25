package com.ghelius.narodmon;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

public class MyLocation {
	Timer timer1;
	LocationManager lm;
	LocationResult locationResult;
	boolean gps_enabled=false;
	boolean network_enabled=false;
    private final static String TAG = "narodmon-location";

	public boolean getLocation(Context context, LocationResult result)
	{
		//I use LocationResult callback class to pass location value from MyLocation to user code.
		locationResult=result;
		if(lm==null)
			lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

		//exceptions will be thrown if provider is not permitted.
		try{gps_enabled=lm.isProviderEnabled(LocationManager.GPS_PROVIDER);}catch(Exception ex){}
		try{network_enabled=lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);}catch(Exception ex){}

		//don't start listeners if no provider is enabled
		if(!gps_enabled && !network_enabled)
			return false;

		if(gps_enabled)
			lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListenerGps);
		if(network_enabled)
			lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListenerNetwork);
		timer1=new Timer();
		timer1.schedule(new GetLastLocation(), 20000);
		return true;
	}

	LocationListener locationListenerGps = new LocationListener() {
		public void onLocationChanged(Location location) {
            Log.d(TAG,"gps location fired");
			timer1.cancel();
			locationResult.gotLocation(location);
			lm.removeUpdates(this);
			lm.removeUpdates(locationListenerNetwork);
		}
		public void onProviderDisabled(String provider) {}
		public void onProviderEnabled(String provider) {}
		public void onStatusChanged(String provider, int status, Bundle extras) {}
	};

	LocationListener locationListenerNetwork = new LocationListener() {
		public void onLocationChanged(Location location) {
            Log.d(TAG,"network location fired");
			timer1.cancel();
			locationResult.gotLocation(location);
			lm.removeUpdates(this);
			lm.removeUpdates(locationListenerGps);
		}
		public void onProviderDisabled(String provider) {}
		public void onProviderEnabled(String provider) {}
		public void onStatusChanged(String provider, int status, Bundle extras) {}
	};

	class GetLastLocation extends TimerTask {
		@Override
		public void run() {
            Log.d(TAG,"location timer fired");
			lm.removeUpdates(locationListenerGps);
			lm.removeUpdates(locationListenerNetwork);

			Location net_loc=null, gps_loc=null;
			if(gps_enabled)
				gps_loc=lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if(network_enabled)
				net_loc=lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);


			//if there are both values use the latest one
			if(gps_loc!=null && net_loc!=null){
//                Log.d(TAG,"gps accuracy is: " + gps_loc.getAccuracy() + ", time is " + gps_loc.getTime());
//                Log.d(TAG,"network accuracy is: " + net_loc.getAccuracy() + ", time is " + net_loc.getTime());
				if(gps_loc.getTime() > net_loc.getTime() && (System.currentTimeMillis() - gps_loc.getTime()) < 10*60*1000) {
                    Log.d(TAG,"gps is newer");
					locationResult.gotLocation(gps_loc);
                } else {
                    Log.d(TAG, "network is newer");
					locationResult.gotLocation(net_loc);
                }
				return;
			}

			if(gps_loc!=null){
				locationResult.gotLocation(gps_loc);
				return;
			}
			if(net_loc!=null){
				locationResult.gotLocation(net_loc);
				return;
			}
			locationResult.gotLocation(null);
		}
	}

	public static abstract class LocationResult{
		public abstract void gotLocation(Location location);
	}
}
