package com.ghelius.narodmon;


import android.os.AsyncTask;
import android.util.Log;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


class ServerDataGetter extends AsyncTask<String, String, String> {
    private final static String TAG = "narodmon-getter";
    interface OnResultListener {
        void onResultReceived(String result);
        void onNoResult ();
    }

    OnResultListener listener;

    void setOnListChangeListener (OnResultListener l) {
        listener = l;
    }


    private String inputStreamToString(InputStream is) {
        String s = "";
        String line = "";
        // Wrap a BufferedReader around the InputStream
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        // Read response until the end
        try {
            while ((line = rd.readLine()) != null) { s += line; }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return s;
    }
    @Override
    protected void onPreExecute () {
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(String... uri) {
        Log.d(TAG,"doInBackground");
        String responseString = null;
        URL url;
        HttpURLConnection urlConnection = null;
        try {
            Log.d(TAG, uri[0]);
            url = new URL(uri[0]);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(10000);
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            responseString = inputStreamToString(in);
        } catch (MalformedURLException e) {
            Log.e(TAG,"MalformedURLException:" + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG,"IOException:" + e.getMessage());
            e.printStackTrace();
        } finally {
            urlConnection.disconnect();
        }
        return responseString;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        Log.d(TAG,"---------getter finished------");
        if (isCancelled()) {
            Log.d(TAG,"task was cancelled");
        }
        if ((result == null)) {
            listener.onNoResult();
        } else {
            listener.onResultReceived(result);
        }
    }

}
