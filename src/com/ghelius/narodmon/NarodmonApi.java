package com.ghelius.narodmon;

import android.content.Context;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Locale;

public class NarodmonApi {
    private static final String TAG = "narodmon-api";
    private static final boolean DEBUG = true;

    private String apiUrl;
    private onResultReceiveListener listener;
    private ListUpdater listUpdater;
    private LocationSender locationSender;
    private Loginer loginer;
    private SensorTypeDictionaryGetter typeDictionaryGetter;
    private VersionSender versionSender;
    private final ValueUpdater valueUpdater;
    private final String fileName = "sensorList.obj";
    private String apiHeader;

	public void setLocation(Float lat, Float lng) {
		if(DEBUG) Log.d(TAG,"set new location to Api " + lat +" " + lng);
		listUpdater.setLocation (lat, lng);
	}


	interface onResultReceiveListener {
        void onLocationResult (boolean ok, String addr, Float lat, Float lng);
        void onAuthorisationResult (boolean ok, String res);
        void onSendVersionResult (boolean ok, String res);
        void onSensorListResult (boolean ok, String res);
        void onSensorTypeResult (boolean ok, String res);
    }

    NarodmonApi (String apiUrl, String apiHeader) {
        listUpdater    = new ListUpdater();
        locationSender = new LocationSender();
        loginer        = new Loginer();
        versionSender  = new VersionSender();
        valueUpdater   = new ValueUpdater();
        typeDictionaryGetter = new SensorTypeDictionaryGetter();
        this.apiHeader = apiHeader;
        this.apiUrl = apiUrl;
    }

    public void setOnResultReceiveListener (onResultReceiveListener listener) {
        this.listener = listener;
    }

    public void getSensorList (ArrayList<Sensor> list, int radius) {
        listUpdater.updateList(list, radius);
    }

    public void sendLocation (String addr) {
        locationSender.sendLocation(addr);
    }
    public void sendLocation (Double l1, Double l2) {
        locationSender.sendLocation(l1, l2);
    }

    public void sendVersion (String version) {
        versionSender.sendVersion(version);
    }

    public void doAuthorisation (String login, String passwd, String uid) {
        loginer.login(login,passwd,uid);
    }

	public void doLogout() {
		loginer.logout();
	}

    public void updateSensorsValue (ArrayList<Sensor> list) {
        valueUpdater.updateValue(list);
    }

    public void restoreSensorList (Context context, ArrayList<Sensor> list) {
        listUpdater.restoreList(context,list);
    }

    public void getTypeDictionary() {
        typeDictionaryGetter.getDictionary();
    }

    public String makeRequestHeader(String cmd) {
        return apiHeader + "\"cmd\":\""+cmd+"\"";
    }

    /*
    * Class for get full sensor list from server, parse it and put to sensorList and updateFilter listAdapter
    * */
    private class ListUpdater implements ServerDataGetter.OnResultListener, ServerDataGetter.AsyncJobCallbackInterface {
        ServerDataGetter getter;
        ArrayList<Sensor> sensorList;
        Context context;
	    private Float lat;
	    private Float lng;

	    void restoreList (Context context, ArrayList<Sensor> sensorList) {
            this.context = context;
            this.sensorList = sensorList;
            if(DEBUG) Log.d(TAG,"------restore list start-------");
            FileInputStream fis;
            try {
                fis = context.openFileInput(fileName);
                ObjectInputStream is = new ObjectInputStream(fis);
                sensorList.clear();
                sensorList.addAll((ArrayList<Sensor>) is.readObject());
                is.close();
                fis.close();
                for (Sensor aSensorList : sensorList) aSensorList.value = "--";
                if(DEBUG) Log.d(TAG,"------restored list end------- " + sensorList.size());
                if (listener != null)
                    listener.onSensorListResult(true,"");
            } catch (Exception e) {
               Log.e(TAG,"Can't read sensorList: " + e.getMessage());
            }
        }

        void updateList (ArrayList<Sensor> sensorList, int radius) {
            if (getter != null)
                getter.cancel(true);
            this.sensorList = sensorList;
            getter = new ServerDataGetter ();
            getter.setOnListChangeListener(this);
            getter.setAsyncJobCallback(this);
            getter.execute(apiUrl, makeRequestHeader("sensorNear") + ",\"limit\":"+ String.valueOf(radius) + ",\"lat\":" + String.valueOf(lat) + ",\"lng\":" + String.valueOf(lng) +",\"lang\":\"" + Locale.getDefault().getLanguage() + "\"}");
        }
        @Override
        public void onResultReceived(String result) {
            if(DEBUG) Log.d(TAG,"listUpdater: result received, call listener");
            if (listener != null)
                listener.onSensorListResult(true, "");
            else
                Log.e(TAG,"listUpdater, listener is null!");
        }
        @Override
        public void onNoResult() {
            getter = null;
            Log.e(TAG, "listUpdater: Server not responds");
            if (listener != null)
                listener.onSensorListResult(false, "");
            else
                Log.e(TAG,"listUpdater, listener is null!");
        }

        @Override
        public boolean asyncJobWithResult(String result) {
            if(DEBUG) Log.d(TAG,"do asyncJob");
            try {
                makeSensorListFromJson(result);
                if(DEBUG) Log.d(TAG,"asyncJob done with " + sensorList.size() + " sensor");
            } catch (JSONException e) {
                Log.e(TAG,e.getMessage());
                return false;
            }
            return true;
        }

        private void makeSensorListFromJson (String result) throws JSONException {
            if (result != null) {
                sensorList.clear();
                JSONObject jObject = new JSONObject(result);
                JSONArray devicesArray = jObject.getJSONArray("devices");
                Log.d(TAG,"receive " + devicesArray.length() + " devices");
                for (int i = 0; i < devicesArray.length(); i++) {
                    String location = devicesArray.getJSONObject(i).getString("location");
                    float distance = Float.parseFloat(devicesArray.getJSONObject(i).getString("distance"));
                    boolean my      = (devicesArray.getJSONObject(i).getInt("my") != 0);
                    //if(DEBUG) Log.d(TAG, + i + ": " + location);
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
                Log.d(TAG,"receive " + sensorList.size() + " sensors");
//                if (!sensorList.isEmpty() && context!=null) {
//                    FileOutputStream fos;
//                    try {
//                        fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
//                        ObjectOutputStream os = null;
//                        os = new ObjectOutputStream(fos);
//                        os.writeObject(sensorList);
//                    } catch (Exception e) {
//                        Log.e(TAG, "Can't serialise sensor list: " + e.getMessage());
//                    }
//                }
            }
        }

	    public void setLocation(Float lat, Float lng) {
		    this.lat = lat;
		    this.lng = lng;
	    }
    }

    /*
    * Class for updateFilter values for sensors id set
    * */
    private class ValueUpdater implements ServerDataGetter.OnResultListener, ServerDataGetter.AsyncJobCallbackInterface {
        ServerDataGetter getter;
        ArrayList<Sensor> sensorList;
        void updateValue (ArrayList<Sensor> sensorList) {
            if (getter != null)
                getter.cancel(true);
            this.sensorList = sensorList;
            getter = new ServerDataGetter ();
            getter.setOnListChangeListener(this);
            getter.setAsyncJobCallback(this);

            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < sensorList.size(); ++i) {
                if (i != 0) {
                    buf.append(",");
                }
                buf.append(sensorList.get(i).id);
            }
            String queryId = buf.toString();
            getter.execute(apiUrl, makeRequestHeader("sensorInfo") +",\"sensor\":["+ queryId +"]}");

        }
        @Override
        public void onResultReceived(String result) {
            if(DEBUG) Log.d(TAG,"valueUpdate: result receive");
            if (listener != null)
                listener.onSensorListResult(true, "");
            else
                Log.e(TAG,"valueUpdate: listener is null!");
        }
        @Override
        public void onNoResult() {
            getter = null;
            Log.e(TAG, "valueUpdater: Server not responds");
            if (listener != null)
                listener.onSensorListResult(false, "");
            else
                Log.e(TAG,"valueUpdate: listener is null!");
        }

        @Override
        public boolean asyncJobWithResult(String result) {
            if(DEBUG) Log.d(TAG,"do asyncJob");
            try {
                parseValue(result);
                if(DEBUG) Log.d(TAG,"asyncJob done with " + sensorList.size() + " sensor");
                if(DEBUG) Log.d(TAG,"valueUpdater: make sensor list done");
            } catch (JSONException e) {
                Log.e(TAG,e.getMessage());
                return false;
            }
            return true;
        }

        private void parseValue (String result) throws JSONException {
            if (result != null) {
                JSONObject jObject = new JSONObject(result);
                JSONArray sensArray = jObject.getJSONArray("sensors");
                for (int i = 0; i < sensArray.length(); i++) {
                    String id = sensArray.getJSONObject(i).getString("id");
                    String value = sensArray.getJSONObject(i).getString("value");
                    String time = sensArray.getJSONObject(i).getString("time");
                    //if(DEBUG) Log.d(TAG,"for " + id + " val: " + value + ", time " + time);
                    updateSensorValue (Integer.valueOf(id), value, Long.valueOf(time));
                }
            }
        }

        private void updateSensorValue(int id, String value, long time) {
            for (Sensor aSensorList : sensorList) {
                if (aSensorList.id == id) {
                    aSensorList.value = value;
                    aSensorList.time = time;
                }
            }
        }
    }

    /*
    * Class for login procedure
    * */
    private class Loginer implements ServerDataGetter.OnResultListener {
        ServerDataGetter getter;
        void login (String login, String passwd, String uid) {
            getter = new ServerDataGetter();
            getter.setOnListChangeListener(this);
            if(DEBUG) Log.d(TAG, "password: " + passwd + " md5: " + md5(passwd));
//            getter.execute(apiUrl, makeRequestHeader("login") + ",\"login\":\""+ login + "\",\"hash\":\""+md5(uid+md5(passwd)) +"\"}");
	        getter.execute(apiUrl, makeRequestHeader("login") + ",\"login\":\"" + login + "\",\"hash\":\"" + md5(uid + md5(passwd)) + "\",\"lang\":\"" + Locale.getDefault().getLanguage() + "\"}");
        }
	    void logout () {
		    getter = new ServerDataGetter();
		    getter.setOnListChangeListener(this);
		    getter.execute(apiUrl, makeRequestHeader("logout") + "}");
	    }
        @Override
        public void onResultReceived(String result) {
            //{"error":"auth error"} or {"login":"mylogin"}
            if(DEBUG) Log.d(TAG,"Login result: " + result);
            try {
                JSONObject jObject = new JSONObject(result);
                String login = jObject.getString("login");
                if(DEBUG) Log.d(TAG,"Login result: " + login);
	            if (login != null) {
		            if (listener != null) {
		                listener.onAuthorisationResult(true, login);
			            return;
		            } else
                       Log.e(TAG,"Login result, listener is null!");
	            }
            } catch (JSONException e) {
                Log.e(TAG, "Authorisation: wrong json, " + e.getMessage());
            }
            if (listener != null) {
                listener.onAuthorisationResult(false, result);
            } else
                Log.e(TAG,"Login result, listener is null!");
        }
        @Override
        public void onNoResult() {
            Log.e(TAG,"Authorisation: Server not responds");
            if (listener!=null)
                listener.onAuthorisationResult(false, "");
            else
                Log.e(TAG,"Login result, listener is null!");
        }
    }

    /*
    * Class for location send procedure
    * */
    private class LocationSender implements ServerDataGetter.OnResultListener {
        ServerDataGetter getter;
        void sendLocation (Double l1, Double l2) {
            getter = new ServerDataGetter();
            getter.setOnListChangeListener(this);
            // fucking hack!
            getter.execute(apiUrl, makeRequestHeader("location") + ",\"addr\":\"" + String.valueOf((double) Math.round(l2 * 1000000) / 1000000) + "," + String.valueOf((double) Math.round(l1 * 1000000) / 1000000) + "\"}");
        }
        void sendLocation (String geoCode) {
            getter = new ServerDataGetter();
            getter.setOnListChangeListener(this);
            getter.execute(apiUrl, makeRequestHeader("location") + ",\"addr\":\"" + JSONEncoder.encode(geoCode) + "\"}");
        }
        @Override
        public void onResultReceived(String result) {
            JSONObject jsonObject;
	        if(DEBUG) Log.d(TAG,result);
            try {
                jsonObject = new JSONObject(result);
                String addr = jsonObject.getString("addr");
	            Float lat = (float)jsonObject.getDouble("lat");
	            Float lng = (float)jsonObject.getDouble("lng");
                if(DEBUG) Log.d(TAG, "location result, addr: " + addr);
                if (listener!=null) {
                    listener.onLocationResult(true,addr,lat,lng);
                }
            } catch (JSONException e) {
                Log.e(TAG,"location result: wrong json - " + e.getMessage() + " ["+result+"]");
                if (listener!=null) {
                    listener.onLocationResult(false,"",0.0f,0.0f);
                }
            }
        }
        @Override
        public void onNoResult() {
            if (listener!=null) {
                listener.onLocationResult(false,"",0.0f,0.0f);
            }
        }
    }

    /*
    * Class for get sensor type dictionary
    * */
    private class SensorTypeDictionaryGetter implements ServerDataGetter.OnResultListener {
        ServerDataGetter getter;
        void getDictionary () {
	        if(DEBUG) Log.d(TAG,"getDictionary");
            getter = new ServerDataGetter();
            getter.setOnListChangeListener(this);
            getter.execute(apiUrl, makeRequestHeader("sensorType") +",\"lang\":\"" + Locale.getDefault().getLanguage() + "\"}");
        }
        @Override
        public void onResultReceived(String result) {
            if(DEBUG) Log.d(TAG, "Dictionary get result: " + result);
            if (listener!=null)
                listener.onSensorTypeResult(true,result);
        }
        @Override
        public void onNoResult() {
            Log.e(TAG,"No sensor dictionary result");
            if (listener!=null)
                listener.onSensorTypeResult(false,"");
        }
    }
    /*
    * Class for AppApiVersion send procedure
    * */
    private class VersionSender implements ServerDataGetter.OnResultListener {
        ServerDataGetter getter;
        void sendVersion (String version) {
            getter = new ServerDataGetter();
            getter.setOnListChangeListener(this);
            getter.execute(apiUrl, makeRequestHeader("version") + ",\"version\":\"" + version + "\"}");
        }
        @Override
        public void onResultReceived(String result) {
            if(DEBUG) Log.d(TAG, "Version result: " + result);
            if (listener!=null)
                listener.onSendVersionResult(true,result);
        }
        @Override
        public void onNoResult() {
            Log.e(TAG,"No result while send AppApiVersion");
            if (listener!=null)
                listener.onSendVersionResult(false,"");
        }
    }


    public static String md5(final String s) {
        if(DEBUG) Log.d(TAG,"string to md5: ["+s+"]");
        if (s == null)
            return "";
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

    //{"cmd":"sensorLog","uuid":"38c070002121a4fc852d8d8251c18cfb","id":"1296","period":"week","offset":"2"}


}
