package com.ghelius.narodmon;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class DatabaseManager {

    private AtomicInteger mOpenCounter = new AtomicInteger();
    private static final String TAG = "narodmon-DatabaseManager";
    private static DatabaseManager instance;
    private static SQLiteOpenHelper mDatabaseHelper;
    private SQLiteDatabase mDatabase;

    public static synchronized void initializeInstance(SQLiteOpenHelper helper) {
        if (instance == null) {
            instance = new DatabaseManager();
            mDatabaseHelper = helper;
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException(DatabaseManager.class.getSimpleName() +
                    " is not initialized, call initializeInstance(..) method first.");
        }
        return instance;
    }

    public synchronized SQLiteDatabase openDatabase() {
        Log.d(TAG,"open db");
        if(mOpenCounter.incrementAndGet() == 1) {
            Log.d(TAG,"real open db");
            // Opening new database
            mDatabase = mDatabaseHelper.getWritableDatabase();
        }
        return mDatabase;
    }

    public synchronized void closeDatabase() {
        Log.d(TAG,"close db");
        if(mOpenCounter.decrementAndGet() == 0) {
            Log.d(TAG,"real close db");
            // Closing database
            mDatabase.close();

        }
    }




    void updateType(SensorType t) {
        SQLiteDatabase db = getInstance().openDatabase();

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.KEY_TYPES_CODE, t.code);
        values.put(DatabaseHelper.KEY_TYPES_NAME, t.name);
        values.put(DatabaseHelper.KEY_TYPES_UNIT, t.unit);

        // Inserting Row or Replace if exist
        db.insertWithOnConflict(DatabaseHelper.TABLE_TYPES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        getInstance().closeDatabase();
    }

    ArrayList<SensorType> getAllTypes () {
        ArrayList<SensorType> types = new ArrayList<SensorType>();

        SQLiteDatabase db = getInstance().openDatabase();

        String selectQuery = "SELECT  * FROM " + DatabaseHelper.TABLE_TYPES;
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor != null) {
            cursor.moveToFirst();
            if (cursor.getCount() != 0)
                do {
                    types.add(new SensorType(Integer.parseInt(cursor.getString(0)),cursor.getString(1), cursor.getString(2)));
                } while (cursor.moveToNext());
            cursor.close();
        }
        getInstance().closeDatabase();
        return types;
    }

    void addWidget(Widget widget) {
        Log.d(TAG, "added widget: " + widget.widgetId + ", " + widget.sensorId + ", " + widget.screenName + ", " + widget.type);

        SQLiteDatabase db = getInstance().openDatabase();

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.KEY_WIDGET_ID, widget.widgetId);
        values.put(DatabaseHelper.KEY_SENSOR_ID, widget.sensorId);
        values.put(DatabaseHelper.KEY_NAME, widget.screenName);
        values.put(DatabaseHelper.KEY_TYPE, widget.type);

        // Inserting Row
        db.insert(DatabaseHelper.TABLE_WIDGETS, null, values);
        getInstance().closeDatabase();
    }

    ArrayList<Widget> getWidgetsBySensorId(int id) {

        SQLiteDatabase db = getInstance().openDatabase();

        Cursor cursor = db.query(DatabaseHelper.TABLE_WIDGETS, new String[] { DatabaseHelper.KEY_WIDGET_ID, DatabaseHelper.KEY_SENSOR_ID, DatabaseHelper.KEY_NAME, DatabaseHelper.KEY_TYPE, DatabaseHelper.KEY_LAST_VALUE, DatabaseHelper.KEY_CUR_VALUE },
                DatabaseHelper.KEY_SENSOR_ID + "=?",
                new String[] { String.valueOf(id) }, null, null, null, null);
        ArrayList<Widget> widgets = new ArrayList<Widget>();
        if (cursor != null) {
            cursor.moveToFirst();
            if (cursor.getCount() != 0)
                do {
                    widgets.add (new Widget(Integer.parseInt(cursor.getString(0)),Integer.parseInt(cursor.getString(1)), cursor.getString(2), cursor.getInt(3), cursor.getString(4), cursor.getString(5)));
                } while (cursor.moveToNext());
            cursor.close();
        }
        getInstance().closeDatabase();
        return widgets;
    }

    public ArrayList<Widget> getAllWidgets() {
        ArrayList<Widget> widgetList = new ArrayList<Widget>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + DatabaseHelper.TABLE_WIDGETS;


        SQLiteDatabase db = getInstance().openDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor != null && cursor.getCount()!= 0 && cursor.moveToFirst()) {
            do {
                // Adding widgets to list
                widgetList.add(new Widget(Integer.parseInt(cursor.getString(0)), Integer.parseInt(cursor.getString(1)), cursor.getString(2), cursor.getInt(3), cursor.getString(4), cursor.getString(5)));
            } while (cursor.moveToNext());
        }
        if (cursor != null)
            cursor.close();
        getInstance().closeDatabase();
        return widgetList;
    }

    public void deleteWidgetByWidgetId(int w) {
        Log.d(TAG, "widget with widgetId " + w + "was deleted");

        SQLiteDatabase db = getInstance().openDatabase();
        db.delete(DatabaseHelper.TABLE_WIDGETS, DatabaseHelper.KEY_WIDGET_ID + " = ?",
                new String[] { String.valueOf(w) });
        getInstance().closeDatabase();
    }

    public void updateValueByWidgetId(Widget w) {

        SQLiteDatabase db = getInstance().openDatabase();

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.KEY_LAST_VALUE, w.lastValue);
        values.put(DatabaseHelper.KEY_CUR_VALUE, w.curValue);

        // updating row
        db.update(DatabaseHelper.TABLE_WIDGETS, values, DatabaseHelper.KEY_WIDGET_ID + " = ?", new String[] { String.valueOf(w.widgetId) });
        getInstance().closeDatabase();
    }


    /**--------- FAVORITES ---------*/

    public ArrayList<Integer> getFavorites () {
        ArrayList<Integer> favorList = new ArrayList<Integer>();
        // Select All Query
        String selectQuery = "SELECT * FROM " + DatabaseHelper.TABLE_FAVORITES;


        SQLiteDatabase db = getInstance().openDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor != null && cursor.getCount()!= 0 && cursor.moveToFirst()) {
            do {
                // fill list with row
                favorList.add(cursor.getInt(0));
            } while (cursor.moveToNext());
        }
        if (cursor != null)
            cursor.close();
        getInstance().closeDatabase();
        return favorList;
    }

    void addFavorites (Integer id) {
        Log.d(TAG, "add favorites: " + id);

        SQLiteDatabase db = getInstance().openDatabase();

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.KEY_FAVORITES_ID, id);

        // Inserting Row
        db.insertWithOnConflict(DatabaseHelper.TABLE_FAVORITES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        getInstance().closeDatabase();
    }

    void removeFavorites (Integer id) {
        Log.d(TAG, "del favorites: " + id);

        SQLiteDatabase db = getInstance().openDatabase();
        db.delete(DatabaseHelper.TABLE_FAVORITES, DatabaseHelper.KEY_FAVORITES_ID + " = ?", new String[]{String.valueOf(id)});
        getInstance().closeDatabase();
    }


    /**--------- ALARM TASKS ---------*/
    ArrayList<AlarmSensorTask> getAlarmTask () {
        ArrayList<AlarmSensorTask> tasks = new ArrayList<AlarmSensorTask>();
        // Select All Query
        String selectQuery = "SELECT * FROM " + DatabaseHelper.TABLE_ALARMS;

        SQLiteDatabase db = getInstance().openDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor != null && cursor.getCount()!= 0 && cursor.moveToFirst()) {
            do {
                // fill list with row
                Float hi    = Float.valueOf(cursor.getString(2));
                Float lo    = Float.valueOf(cursor.getString(3));
                Float value = Float.valueOf(cursor.getString(4));
                String name = cursor.getString(5);
                tasks.add(new AlarmSensorTask(cursor.getInt(0),cursor.getInt(1),hi,lo,value, name));
            } while (cursor.moveToNext());
        }
        if (cursor != null)
            cursor.close();
        getInstance().closeDatabase();
        return tasks;
    }

    AlarmSensorTask getAlarmById(Integer id) {

        SQLiteDatabase db = getInstance().openDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_ALARMS, new String[] {DatabaseHelper.KEY_ALARM_SID,DatabaseHelper.KEY_ALARM_JOB,DatabaseHelper.KEY_ALARM_HI,DatabaseHelper.KEY_ALARM_LO,DatabaseHelper.KEY_ALARM_OLDVALUE,DatabaseHelper.KEY_ALARM_NAME}, DatabaseHelper.KEY_ALARM_SID + " =?",  new String[] {String.valueOf(id)}, null, null,null,null);
        AlarmSensorTask task = null;
        // looping through all rows and adding to list
        if (cursor != null && cursor.getCount() != 0 && cursor.moveToFirst()) {

            // fill list with row
            Float hi = Float.valueOf(cursor.getString(2));
            Float lo = Float.valueOf(cursor.getString(3));
            Float value = Float.valueOf(cursor.getString(4));
            String name = cursor.getString(5);
            task = new AlarmSensorTask(cursor.getInt(0), cursor.getInt(1), hi, lo, value, name);
        }
        if (cursor!=null)
            cursor.close();

        getInstance().closeDatabase();
        return task;
    }

    void addAlarmTask(AlarmSensorTask task) {
        Log.d(TAG, "add alarm: " + task.toString());

        SQLiteDatabase db = getInstance().openDatabase();

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.KEY_ALARM_SID, task.id);
        values.put(DatabaseHelper.KEY_ALARM_JOB, task.job);
        values.put(DatabaseHelper.KEY_ALARM_HI, String.valueOf(task.hi));
        values.put(DatabaseHelper.KEY_ALARM_LO, String.valueOf(task.lo));
        values.put(DatabaseHelper.KEY_ALARM_OLDVALUE, String.valueOf(task.lastValue));
        values.put(DatabaseHelper.KEY_ALARM_NAME, task.name);

        // Inserting Row
        db.insertWithOnConflict(DatabaseHelper.TABLE_ALARMS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        getInstance().closeDatabase();
    }

    void removeAlarm (Integer id) {
        Log.d(TAG, "del alarm: " + id);

        SQLiteDatabase db = getInstance().openDatabase();
        db.delete(DatabaseHelper.TABLE_ALARMS, DatabaseHelper.KEY_ALARM_SID + " = ?", new String[] { String.valueOf(id) });
        getInstance().closeDatabase();
    }

}

