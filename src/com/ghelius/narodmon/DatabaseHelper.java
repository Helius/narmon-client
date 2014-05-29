package com.ghelius.narodmon;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {
	// All Static variables
	private final static String TAG = "narodmon-dbh";
	// Database Version
	private static final int DATABASE_VERSION = 10;
	// Database Name
	private static final String DATABASE_NAME = "miscDataBase";

		/* Widgets table name */
	public static final String TABLE_WIDGETS = "widget";
	// Widgets Table Columns names
	public static final String KEY_WIDGET_ID = "widget_id";
	public static final String KEY_SENSOR_ID = "sensor_id";
	public static final String KEY_NAME = "name";
	public static final String KEY_TYPE = "type";
	public static final String KEY_LAST_VALUE = "last_value";
	public static final String KEY_CUR_VALUE = "cur_value";

		/* Types Table name */
	public static final String TABLE_TYPES = "types";
	public static final String KEY_TYPES_CODE = "code";
	public static final String KEY_TYPES_NAME = "name";
	public static final String KEY_TYPES_UNIT = "unit";

		/* Favorites Table name */
	public static final String TABLE_FAVORITES = "favorites";
	public static final String KEY_FAVORITES_SID = "id";
    public static final String KEY_FAVORITES_DID = "device_id";

	/* Alarm tasks Table name */
	public static final String TABLE_ALARMS = "alarms";
	public static final String KEY_ALARM_SID = "id";
    public static final String KEY_ALARM_DID = "device_id";
	public static final String KEY_ALARM_JOB = "job";
	public static final String KEY_ALARM_HI = "hi";
	public static final String KEY_ALARM_LO = "lo";
	public static final String KEY_ALARM_OLDVALUE = "oldvalue";
    public static final String KEY_ALARM_NAME = "name";

		/* Sensors Table name */
	public static final String TABLE_SENSORS = "sensors";
	public static final String KEY_SENSOR_CODE = "code";
	public static final String KEY_SENSOR_NAME = "name";
	public static final String KEY_SENSOR_TYPE = "type";
	public static final String KEY_SENSOR_LOCATION = "location";
	public static final String KEY_SENSOR_DISTANCE = "distance";
	public static final String KEY_SENSOR_TIMESTAMP = "timestamp";
	public static final String KEY_SENSOR_VALUE = "value";



	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		Log.d(TAG,"create db helper");
	}

	// Creating Tables
	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.d(TAG,"on create");
		String CREATE_TABLE = "CREATE TABLE " + TABLE_WIDGETS + "("
				+ KEY_WIDGET_ID + " INTEGER PRIMARY KEY,"
				+ KEY_SENSOR_ID + " INTEGER,"
				+ KEY_NAME + " TEXT,"
				+ KEY_TYPE + " INTEGER,"
				+ KEY_LAST_VALUE + " TEXT, "
				+ KEY_CUR_VALUE + " TEXT"
				+ ")";
		db.execSQL(CREATE_TABLE);
		CREATE_TABLE = "CREATE TABLE " + TABLE_TYPES + "("
				+ KEY_TYPES_CODE + " INTEGER PRIMARY KEY,"
				+ KEY_TYPES_NAME + " TEXT,"
				+ KEY_TYPES_UNIT + " TEXT"
				+ ")";
		db.execSQL(CREATE_TABLE);
		CREATE_TABLE = "CREATE TABLE " + TABLE_FAVORITES + "("
				+ KEY_FAVORITES_SID + " INTEGER PRIMARY KEY,"
                + KEY_FAVORITES_DID + " INTEGER"
				+ ")";
		db.execSQL(CREATE_TABLE);
		CREATE_TABLE = "CREATE TABLE " + TABLE_ALARMS + "("
				+ KEY_ALARM_SID + " INTEGER PRIMARY KEY,"
                + KEY_ALARM_DID + " INTEGER,"
				+ KEY_ALARM_JOB + " INTEGER,"
				+ KEY_ALARM_HI + " TEXT,"
				+ KEY_ALARM_LO + " TEXT,"
				+ KEY_ALARM_OLDVALUE + " TEXT,"
                + KEY_ALARM_NAME + " TEXT"
				+ ")";
		db.execSQL(CREATE_TABLE);
//		CREATE_TABLE = "CREATE TABLE " + TABLE_SENSORS + "("
//				+ KEY_SENSOR_ID + " INTEGER PRIMARY KEY,"
//				+ KEY_SENSOR_NAME + " TEXT,"
//				+ KEY_SENSOR_TYPE + " INTEGER,"
//				+ KEY_SENSOR_LOCATION + " TEXT,"
//				+ KEY_SENSOR_DISTANCE + " INTEGER,"
//				+ KEY_SENSOR_TIMESTAMP + " INTEGER,"
//				+ KEY_SENSOR_VALUE + " TEXT"
//				+ ")";
//		db.execSQL(CREATE_TABLE);
	}

	// Upgrading database
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.d(TAG,"on upgrade");
		ArrayList<Widget> widgets = new ArrayList<Widget>();
		// get all widgets
		String selectQuery = "SELECT * FROM " + TABLE_WIDGETS;
		Cursor cursor = db.rawQuery(selectQuery, null);
		// looping through all rows and adding to list
		if (cursor != null && cursor.getCount()!= 0 && cursor.moveToFirst()) {
			do {
				// Adding widgets to list
				widgets.add(new Widget(Integer.parseInt(cursor.getString(0)), Integer.parseInt(cursor.getString(1)), cursor.getString(2), cursor.getInt(3)));
			} while (cursor.moveToNext());
			cursor.close();
		}
		// drop old table
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_WIDGETS);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_TYPES);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITES);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_ALARMS);
		// Create tables again
		onCreate(db);
		// fill with data
		for (Widget w: widgets) {
			ContentValues values = new ContentValues();
			values.put(KEY_WIDGET_ID, w.widgetId);
			values.put(KEY_SENSOR_ID, w.sensorId);
			values.put(KEY_NAME, w.screenName);
			values.put(KEY_TYPE, w.type);

			// Inserting Row
			db.insert(TABLE_WIDGETS, null, values);
		}
	}
}

