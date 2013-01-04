package com.ghelius.narodmon;

import android.content.Context;
import android.util.Log;

import java.io.*;
import java.util.ArrayList;


public class ConfigSaver {

    final private String TAG="narodmon-config";
    final private String fileName = "internal.data";
    private static ConfigSaver ourInstance = null;
    private Config config = null;
    private Context context = null;

    private class Config implements Serializable {
        ArrayList <Integer> watchedID;
        Config ()
        {
            watchedID = new ArrayList<Integer>();
        }
    }

    public static ConfigSaver getInstance (Context context)
    {
        //Log.d("narodmon-config","get instance");
        if (ourInstance == null) {
          //  Log.d("narodmon-config","no instance, create now...");
            ourInstance = new ConfigSaver(context);
        }
        return ourInstance;
    }

    private ConfigSaver (Context context) { // load data at constructor
        this.context = context;
        try {
            FileInputStream fis = context.openFileInput(fileName);
            ObjectInputStream is = new ObjectInputStream(fis);
            config = (Config) is.readObject();
            is.close();
            fis.close();
        } catch (Exception e) {
            // file was not found, class not found, etc, first start?
            Log.d(TAG,"config file not found, create new configuration");
            config = new Config();
        }
    }

    private void saveConfig ()
    {
        try {
            //FileOutputStream fos = context.openFileOutput(fileName, 0);
            //ObjectOutputStream os = new ObjectOutputStream(fos);
            FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            ObjectOutputStream os = null;
            try {
                os = new ObjectOutputStream(fos);
            } catch (IOException e) {
              e.printStackTrace();
              e.getMessage();
              Log.e(TAG,"can't create objectOutputStream");
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
        for (int i = 0; i < config.watchedID.size(); i++) {
            if (config.watchedID.get(i) == id)
                return true;
        }
        return false;
    }

    // add ID to list or remove from list, depends of 'watch'
    public void setSensorWatched (int id, boolean watch) {
        if (!watch) {
            for (int i = 0; i < config.watchedID.size(); i++) {
                if (config.watchedID.get(i) == id)
                    config.watchedID.remove(i);
            }
        } else {
            for (int i = 0; i < config.watchedID.size(); i++) {
                if (config.watchedID.get(i) == id)
                    return;
            }
            config.watchedID.add(id);
        }
        saveConfig();
    }

}
