package com.ghelius.narodmon;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

public class DatabaseHandler extends SQLiteOpenHelper {
	// All Static variables
	private final static String TAG = "narodmon-dbh";
	// Database Version
	private static final int DATABASE_VERSION = 4;
	// Database Name
	private static final String DATABASE_NAME = "miscDataBase";
	// Widgets table name
	private static final String TABLE_WIDGETS = "widget";

	// Widgets Table Columns names
	private static final String KEY_WIDGET_ID = "widget_id";
	private static final String KEY_SENSOR_ID = "sensor_id";
	private static final String KEY_NAME = "name";
	private static final String KEY_TYPE = "type";
	private static final String KEY_LAST_VALUE = "last_value";
	private static final String KEY_CUR_VALUE = "cur_value";

	public DatabaseHandler(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		Log.d(TAG,"create db helper");
	}

	// Creating Tables
	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.d(TAG,"on create");
		String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_WIDGETS + "("
				+ KEY_WIDGET_ID + " INTEGER,"
				+ KEY_SENSOR_ID + " INTEGER,"
				+ KEY_NAME + " TEXT,"
				+ KEY_TYPE + " INTEGER,"
				+ KEY_LAST_VALUE + " TEXT, "
				+ KEY_CUR_VALUE + " TEXT"
				+ ")";
		db.execSQL(CREATE_CONTACTS_TABLE);
//		db.enableWriteAheadLogging();
	}

	// Upgrading database
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.d(TAG,"on upgrade");
		// Drop older table if existed
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_WIDGETS);

		// Create tables again
		onCreate(db);
	}

	/**
	 * All CRUD(Create, Read, Update, Delete) Operations
	 */

	// Adding new contact
	void addWidget(Widget widget) {
		Log.d(TAG, "added widget: " + widget.widgetId + ", " + widget.sensorId + ", " + widget.screenName + ", " + widget.type);
		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(KEY_WIDGET_ID, widget.widgetId);
		values.put(KEY_SENSOR_ID, widget.sensorId);
		values.put(KEY_NAME, widget.screenName);
		values.put(KEY_TYPE, widget.type);

		// Inserting Row
		db.insert(TABLE_WIDGETS, null, values);
		db.close(); // Closing database connection
	}

	// Getting single contact
	ArrayList<Widget> getWidgetsBySensorId(int id) {
		SQLiteDatabase db = this.getReadableDatabase();

		Cursor cursor = db.query(TABLE_WIDGETS, new String[] { KEY_WIDGET_ID, KEY_SENSOR_ID, KEY_NAME, KEY_TYPE, KEY_LAST_VALUE, KEY_CUR_VALUE },
				KEY_SENSOR_ID + "=?",
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
		db.close();
		return widgets;
	}

	// Getting All Widget
	public ArrayList<Widget> getAllWidgets() {
		ArrayList<Widget> widgetList = new ArrayList<Widget>();
		// Select All Query
		String selectQuery = "SELECT  * FROM " + TABLE_WIDGETS;

		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);

		// looping through all rows and adding to list
		if (cursor != null && cursor.getCount()!= 0 && cursor.moveToFirst()) {
			do {
				// Adding widgets to list
				widgetList.add(new Widget(Integer.parseInt(cursor.getString(0)), Integer.parseInt(cursor.getString(1)), cursor.getString(2), cursor.getInt(3), cursor.getString(4), cursor.getString(5)));
			} while (cursor.moveToNext());
			cursor.close();
		}
		db.close();
		return widgetList;
	}

//	public int updateWidget(Contact contact) {
//		SQLiteDatabase db = this.getWritableDatabase();
//
//		ContentValues values = new ContentValues();
//		values.put(KEY_NAME, contact.getName());
//		values.put(KEY_PH_NO, contact.getPhoneNumber());
//
//		// updating row
//		return db.update(TABLE_WIDGETS, values, KEY_ID + " = ?",
//				new String[] { String.valueOf(contact.getID()) });
//	}

//
//	// Getting widgetCount
//	public int getWidgetCount() {
//		String countQuery = "SELECT  * FROM " + TABLE_WIDGETS;
//		SQLiteDatabase db = this.getReadableDatabase();
//		Cursor cursor = db.rawQuery(countQuery, null);
//		int count = cursor.getCount();
//		cursor.close();
//		db.close();
//		return count;
//	}

	public void deleteWidgetByWidgetId(int w) {
		Log.d(TAG, "widget with widgetId " + w + "was deleted");
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(TABLE_WIDGETS, KEY_WIDGET_ID + " = ?",
				new String[] { String.valueOf(w) });
		db.close();
	}

//	public Widget getWidgetByWidgetId(int wID) {
//		SQLiteDatabase db = this.getReadableDatabase();
//
//		Cursor cursor = db.query(TABLE_WIDGETS, new String[] { KEY_WIDGET_ID,
//				KEY_SENSOR_ID, KEY_NAME, KEY_TYPE }, KEY_WIDGET_ID + "=?",
//				new String[] { String.valueOf(wID) }, null, null, null, null);
//		Widget widget = new Widget();
//		if (cursor != null) {
//			cursor.moveToFirst();
//			if (cursor.getCount() != 0)
//				do {
//					widget = new Widget(Integer.parseInt(cursor.getString(0)),Integer.parseInt(cursor.getString(1)), cursor.getString(2), cursor.getInt(3));
//				} while (cursor.moveToNext());
//			cursor.close();
//		}
//		db.close();
//		return widget;
//	}

	//	public int updateWidget(Contact contact) {
//		SQLiteDatabase db = this.getWritableDatabase();
//
//		ContentValues values = new ContentValues();
//		values.put(KEY_NAME, contact.getName());
//		values.put(KEY_PH_NO, contact.getPhoneNumber());
//
//		// updating row
//		return db.update(TABLE_WIDGETS, values, KEY_ID + " = ?",
//				new String[] { String.valueOf(contact.getID()) });
//	}
	public void updateValueByWidgetId(Widget w) {
//		Log.d(TAG,"for " + w.screenName + ", last " + w.lastValue + ", cur " + w.curValue);
		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(KEY_LAST_VALUE, w.lastValue);
		values.put(KEY_CUR_VALUE, w.curValue);

		// updating row
		db.update(TABLE_WIDGETS, values, KEY_WIDGET_ID + " = ?", new String[] { String.valueOf(w.widgetId) });
		db.close();
	}
}

