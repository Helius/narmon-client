package com.ghelius.narodmon;


import android.os.AsyncTask;
import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.*;
import java.net.MalformedURLException;


class ServerDataGetter extends AsyncTask<String, String, String> {
    private final static String TAG = "narodmon-getter";
    private final static Boolean DEBUG = true;
    boolean asyncJobFail = false;
    private AsyncJobCallbackInterface asyncCallback;

    interface OnResultListener {
        void onResultReceived(String result);
        void onNoResult ();
    }
    interface AsyncJobCallbackInterface {
        boolean asyncJobWithResult(String result);
    }

    OnResultListener listener;

    void setOnListChangeListener (OnResultListener l) {
        listener = l;
    }
    void setAsyncJobCallback (AsyncJobCallbackInterface l) {
        asyncCallback = l;
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
            e.printStackTrace();
        }
        return s;
    }
    @Override
    protected void onPreExecute () {
        super.onPreExecute();
    }

    public static HttpResponse makeRequest(String uri, String json) {
        try {
            HttpPost httpPost = new HttpPost(uri);
            httpPost.setEntity(new StringEntity(json));
//	        if(DEBUG) Log.d(TAG,json);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            return new DefaultHttpClient().execute(httpPost);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG,e.getMessage());
        } catch (ClientProtocolException e) {
            Log.e(TAG,e.getMessage());
        } catch (IOException e) {
            Log.e(TAG,e.getMessage());
        }
        return null;
    }


    @Override
    protected String doInBackground(String... uri) {
        if(DEBUG) Log.d(TAG,"doInBackground");
        String responseString = null;
        asyncJobFail = true;
        try {
            if(DEBUG) Log.d(TAG, uri[0] + ":" + uri[1]);
            HttpResponse r = makeRequest(uri[0],uri[1]);
            if (r == null) {
                Log.e(TAG,"HttpResponse is null");
                return "";
            }
            InputStream in = r.getEntity().getContent();
            responseString = inputStreamToString(in);
            if (asyncCallback!=null && !isCancelled()) {
                if(DEBUG) Log.d(TAG,"call asyncJob");
                if (asyncCallback.asyncJobWithResult(responseString)) {
                    if(DEBUG) Log.d(TAG,"asyncJob return ok");
                    asyncJobFail = false;
                }
            } else if (asyncCallback == null)
                asyncJobFail = false; // if there is no asyncJob, it can't fail
            if (asyncJobFail) {
                Log.e(TAG,"asyncJob report fail to asyncTask");
            }
        } catch (MalformedURLException e) {
            Log.e(TAG,"MalformedURLException:" + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG,"IOException:" + e.getMessage());
            e.printStackTrace();
        }
        return responseString;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if(DEBUG) Log.d(TAG,"onPostExecute");
        //if(DEBUG) Log.d(TAG,"result: " + result);
        if (isCancelled()) {
            Log.w(TAG,"task was cancelled");
            return;
        }
        if ((result == null) ||(asyncJobFail)) {
            Log.e(TAG,"asyncJob report about fail, so finished with NoResult");
            listener.onNoResult();
        } else {
            if(DEBUG) Log.d(TAG,"call listener onResultReceived");
            listener.onResultReceived(result);
        }
    }

    @Override
    protected void onCancelled() {
        Log.e(TAG,"task was cancelled");
    }

}
