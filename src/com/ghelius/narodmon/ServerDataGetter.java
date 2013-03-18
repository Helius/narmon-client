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
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return s;
    }
    @Override
    protected void onPreExecute () {
        super.onPreExecute();
    }



//    Log.d(TAG,"=========== POST =============");
//    HttpResponse r = makeRequest("http://narodmon.ru/client.php","{\"uuid\":\"ce6a134409741618f1a2f30fe11c26db\",\"api_key\":\"85UneTlo8XBlA\",\"cmd\":\"sensorList\",\"radius\":\"1024\"}");
//    if (r!=null) {
//        try {
//            InputStream is = r.getEntity().getContent();
//            Log.d(TAG, "HTTP POST result:["+inputStreamToString(is)+"]");
//        } catch (IOException e) {
//            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//        }
//    }
    public static HttpResponse makeRequest(String uri, String json) {
        try {
            HttpPost httpPost = new HttpPost(uri);
            httpPost.setEntity(new StringEntity(json));
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
        Log.d(TAG,"doInBackground");
        String responseString = null;
//        URL url;
//        HttpURLConnection urlConnection = null;
        try {
            Log.d(TAG, uri[0] + ":" + uri[1]);
            HttpResponse r = makeRequest(uri[0],uri[1]);
            if (r == null) {
                Log.e(TAG,"HttpResponse is null");
                return "";
            }
//            try {
//                InputStream is = r.getEntity().getContent();
//                Log.d(TAG, "HTTP POST result:["+inputStreamToString(is)+"]");
//            } catch (IOException e) {
//                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//            }
//
//            Log.d(TAG,"========= END ========");
//            url = new URL(uri[0]);
//            urlConnection = (HttpURLConnection) url.openConnection();
//            urlConnection.setConnectTimeout(5000);
//            urlConnection.setReadTimeout(10000);
            InputStream in = r.getEntity().getContent();
            responseString = inputStreamToString(in);
            if (asyncCallback!=null && !isCancelled()) {
                Log.d(TAG,"call asyncJob");
                if (!asyncCallback.asyncJobWithResult(responseString)) {
                    asyncJobFail = true;
                }
            }
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
        Log.d(TAG,"---------getter finished------");
        //Log.d(TAG,"result: " + result);
        if (isCancelled()) {
            Log.d(TAG,"task was cancelled");
            return;
        }
        if ((result == null)||(asyncJobFail)) {
            Log.e(TAG,"asyncJob report about fail, so finished with NoResult");
            listener.onNoResult();
        } else {
            listener.onResultReceived(result);
        }
    }

}
