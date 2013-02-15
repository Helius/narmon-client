package com.ghelius.narodmon;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class NarodmonApi {

    private String apiUrl;
    private onResultReceiveListener listener;
    private ListUpdater listUpdater;
    private LocationSender locationSender;
    private Loginer loginer;
    private VersionSender versionSender;
    private String uid;
    private static final String TAG = "nm-api";



    interface onResultReceiveListener {
        void onLocationResult (boolean ok, String addr);
        void onAuthorisationResult (boolean ok, String res);
        void onSendVersionResult (boolean ok, String res);
        void onSensorListResult (boolean ok, String res);
    }

    NarodmonApi (String uid, String jsonApiUrl) {
        listUpdater    = new ListUpdater();
        locationSender = new LocationSender();
        loginer        = new Loginer();
        versionSender  = new VersionSender();
        this.apiUrl    = jsonApiUrl;
        this.uid       = uid;
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

    public void doAuthorisation (String login, String passwd) {
        loginer.login(login,passwd);
    }


    /*
    * Class for get full sensor list from server, parse it and put to sensorList and update listAdapter
    * */
    private class ListUpdater implements ServerDataGetter.OnResultListener, ServerDataGetter.AsyncJobCallbackInterface {
        ServerDataGetter getter;
        ArrayList<Sensor> sensorList;
        void updateList (ArrayList<Sensor> sensorList, int radius) {
            if (getter != null)
                getter.cancel(true);
            this.sensorList = sensorList;
            getter = new ServerDataGetter ();
            getter.setOnListChangeListener(this);
            getter.setAsyncJobCallback(this);
            getter.execute(apiUrl+"{\"cmd\":\"sensorList\",\"uuid\":\"" + uid + "\",\"radius\":\""+ String.valueOf(radius) +"\"}");
        }
        @Override
        public void onResultReceived(String result) {
            Log.d(TAG,"listUpdate: result receive");
            if (listener != null)
                listener.onSensorListResult(true, "");
        }
        @Override
        public void onNoResult() {
            getter = null;
            Log.w(TAG, "listUpdater: Server not responds");
            if (listener != null)
                listener.onSensorListResult(false, "");
        }

        @Override
        public boolean asyncJobWithResult(String result) {
            Log.d(TAG,"do asyncJob");
            try {
                makeSensorListFromJson(result);
                Log.d(TAG,"asyncJob done with " + sensorList.size() + " sensor");
                Log.d(TAG,"make sensor list done");
            } catch (JSONException e) {
                return false;
            }
            return true;
        }

        private void makeSensorListFromJson (String result) throws JSONException {
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
            }
        }
    }

    /*
    * Class for login procedure
    * */
    private class Loginer implements ServerDataGetter.OnResultListener {
        ServerDataGetter getter;
        void login (String login, String passwd)
        {
            getter = new ServerDataGetter();
            getter.setOnListChangeListener(this);
            Log.d(TAG, "password: " + passwd + " md5: " + md5(passwd));
            getter.execute(apiUrl + "{\"cmd\":\"login\",\"uuid\":\""+uid+"\",\"login\":\""+login+"\",\"hash\":\""+md5(uid+md5(passwd)) +"\"}");
        }
        @Override
        public void onResultReceived(String result) {
            //{"error":"auth error"} or {"login":"mylogin"}
            Log.d(TAG,"Login result: " + result);
            try {
                JSONObject jObject = new JSONObject(result);
                String login = jObject.getString("login");
                Log.d(TAG,"Login result: " + login);
            } catch (JSONException e) {
                Log.e(TAG, "Authorisation: wrong json, " + e.getMessage());
            }
            if (listener != null) {
                listener.onAuthorisationResult(true, result);
            }
        }
        @Override
        public void onNoResult() {
            Log.e(TAG,"Authorisation: Server not responds");
            listener.onAuthorisationResult(false, "");
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
            getter.execute(apiUrl+"{\"cmd\":\"location\",\"uuid\":\"" + uid + "\",\"addr\":\"" +
                    String.valueOf((double) Math.round(l2 * 1000000) / 1000000) + "," + String.valueOf((double) Math.round(l1 * 1000000) / 1000000) + "\"}");
        }
        void sendLocation (String geoCode) {
            getter = new ServerDataGetter();
            getter.setOnListChangeListener(this);
            getter.execute(apiUrl+"{\"cmd\":\"location\",\"uuid\":\"" + uid + "\",\"addr\":\"" + JSONEncoder.encode(geoCode) + "\"}");
        }
        @Override
        public void onResultReceived(String result) {
            JSONObject jsonObject;
            try {
                jsonObject = new JSONObject(result);
                String addr = jsonObject.getString("addr");
                Log.d(TAG, "location result, addr: " + addr);
                if (listener!=null) {
                    listener.onLocationResult(true,addr);
                }
            } catch (JSONException e) {
                Log.e(TAG,"location result: wrong json - " + e.getMessage());
                if (listener!=null) {
                    listener.onLocationResult(false,"");
                }
            }
        }
        @Override
        public void onNoResult() {
            if (listener!=null) {
                listener.onLocationResult(false,"");
            }
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
            getter.execute(apiUrl+"{\"cmd\":\"version\",\"uuid\":\"" + uid + "\",\"version\":\"" + version + "\"}");
        }
        @Override
        public void onResultReceived(String result) {
            Log.d(TAG, "Version result: " + result);
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
