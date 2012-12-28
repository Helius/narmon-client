package com.ghelius.narodmon;

import android.content.Context;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;


public class ConfigSaver {
    private class Config {
        ArrayList <Integer> watchedID;
    }
    private static ConfigSaver ourInstance = new ConfigSaver();
    private Config config = null;
    final private String fileName = "internal.data";

    public static ConfigSaver getInstance ()
    {
        return ourInstance;
    }

    private ConfigSaver (Context context) { // load data at constructor
        FileInputStream fis = null;
        try {
            fis = context.openFileInput(fileName);
            ObjectInputStream is = new ObjectInputStream(fis);
            config = (Config) is.readObject();
            is.close();
        } catch (Exception e) {
            // file was not found, class not found, etc, first start?
            config = new Config();
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
    }

}
