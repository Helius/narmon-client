package com.ghelius.narodmon;

import android.content.Context;
import android.util.Log;

import java.io.*;


public class ConfigHolder {

    final static private String TAG="narodmon-config";
    final static private String fileName = "internal.data";
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

    public boolean isSensorWatchJob (int id)
    {
        for (int i = 0; i < config.watchedId.size(); i++) {
            if (config.watchedId.get(i).id == id)
                return (!config.watchedId.get(i).job.equals(Configuration.NOTHING));
        }
        return false;
    }

    // add ID to list or remove from list, depends of 'watch'
    public void setSensorWatched (Sensor sensor, boolean watch) {
        if (!watch) {
            for (int i = 0; i < config.watchedId.size(); i++) {
                if (config.watchedId.get(i).id == sensor.id)
                    config.watchedId.remove(i);
            }
        } else {
            for (int i = 0; i < config.watchedId.size(); i++) {
                if (config.watchedId.get(i).id == sensor.id)
                    return;
            }
            config.insert(sensor.id, sensor.name);
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
    /*
    * return true if limit are exceeded , false otherwise*/
    public boolean checkLimits(Integer id, Float value, Long timeStamp) {
        for (int i = 0; i < config.watchedId.size(); i++) {
            if (config.watchedId.get(i).id.equals(id)) {
                if (config.watchedId.get(i).job.equals(Configuration.NOTHING)) {
                    // just save value if it's needed
                    config.watchedId.get(i).lastValue = value;
                    config.watchedId.get(i).timestamp = timeStamp;
                    return false;
                }
                if (config.watchedId.get(i).job == Configuration.MORE_THAN) {
                    if (value > config.watchedId.get(i).hi)
                        return true;
                }
                if (config.watchedId.get(i).job == Configuration.LESS_THAN) {
                    if (value < config.watchedId.get(i).lo)
                        return true;
                }
                if (config.watchedId.get(i).job == Configuration.OUT_OF) {
                    if ((value > config.watchedId.get(i).hi) || (value < config.watchedId.get(i).lo))
                        return true;
                }
                if (config.watchedId.get(i).job == Configuration.WITHIN_OF) {
                    if ((value < config.watchedId.get(i).hi) || (value > config.watchedId.get(i).lo))
                        return true;
                }
            }
        }
        return false;
    }
}
