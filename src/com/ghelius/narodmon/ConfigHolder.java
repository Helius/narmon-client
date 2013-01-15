package com.ghelius.narodmon;

import android.content.Context;
import android.util.Log;

import java.io.*;


public class ConfigHolder {

    final private String TAG="narodmon-config";
    final private String fileName = "internal.data";
    private static ConfigHolder ourInstance = null;
    private Configuration config = null;
    private Context context = null;

    public static ConfigHolder getInstance (Context context)
    {
        //Log.d("narodmon-config","get instance");
        if (ourInstance == null) {
          //  Log.d("narodmon-config","no instance, create now...");
            ourInstance = new ConfigHolder(context);
        }
        return ourInstance;
    }

    private ConfigHolder(Context context) { // load data at constructor
        this.context = context;
        try {
            FileInputStream fis = context.openFileInput(fileName);
            ObjectInputStream is = new ObjectInputStream(fis);
            config = (Configuration) is.readObject();
            is.close();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
            e.getMessage();
            // file was not found, class not found, etc, first start?
            Log.w(TAG,"config file not found, create new configuration");
            config = new Configuration();
        }
    }

    private void saveConfig ()
    {
        if (config == null) {
            Log.e(TAG,"Config is null!!!");
        }
        try {
            FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            ObjectOutputStream os = null;
            try {
                os = new ObjectOutputStream(fos);
            } catch (IOException e) {
              e.printStackTrace();
              e.getMessage();
              Log.e(TAG,"can't create objectOutputStream");
              return;
            }
            try {
                os.writeObject(config);
            } catch (IOException e) {
                e.printStackTrace();
                e.getMessage();
                Log.e(TAG,"can't create write object");
            }
            try {
                os.close();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
                e.getMessage();
                Log.e(TAG,"can't close file and stream");
            }


        } catch (FileNotFoundException e) {
            Log.e(TAG,"Can't open config file");
        }

    }

    public boolean isSensorWatched (int id)
    {
        for (int i = 0; i < config.watchedId.size(); i++) {
            if (config.watchedId.get(i).id == id)
                return true;
        }
        return false;
    }

    // add ID to list or remove from list, depends of 'watch'
    public void setSensorWatched (int id, boolean watch) {
        if (!watch) {
            for (int i = 0; i < config.watchedId.size(); i++) {
                if (config.watchedId.get(i).id == id)
                    config.watchedId.remove(i);
            }
        } else {
            for (int i = 0; i < config.watchedId.size(); i++) {
                if (config.watchedId.get(i).id == id)
                    return;
            }
            config.insert(id);
        }
        saveConfig();
    }

    public Configuration getConfig ()
    {
        return config;
    }

    public String getUid () {
        return config.uid;
    }
    public void setUid (String uid) {
        config.uid = uid;
    }

}
